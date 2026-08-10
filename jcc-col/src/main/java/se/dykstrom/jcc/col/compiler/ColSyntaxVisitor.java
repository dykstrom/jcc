/*
 * Copyright (C) 2023 Johan Dykstrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.dykstrom.jcc.col.compiler;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import se.dykstrom.jcc.col.ast.expression.AnonymousFunctionExpression;
import se.dykstrom.jcc.col.ast.expression.BecomeExpression;
import se.dykstrom.jcc.col.ast.expression.ChainedRelationalExpression;
import se.dykstrom.jcc.col.ast.expression.MalformedFloatLiteral;
import se.dykstrom.jcc.col.ast.expression.MalformedStringLiteral;
import se.dykstrom.jcc.col.ast.statement.AliasStatement;
import se.dykstrom.jcc.col.ast.statement.FunCallStatement;
import se.dykstrom.jcc.col.ast.statement.ImportStatement;
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement;
import se.dykstrom.jcc.col.compiler.ColParser.*;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.F32;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.NamedType;
import se.dykstrom.jcc.common.types.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static se.dykstrom.jcc.antlr4.Antlr4Utils.isValid;
import static se.dykstrom.jcc.common.utils.FormatUtils.normalizeFloatNumber;

public class ColSyntaxVisitor extends ColBaseVisitor<Node> {

    // Group 1 = optional sign
    // Group 2 = complete number
    // Group 3 = decimal point and fraction
    // Group 4 = complete exponent
    // Group 5 = optional exponent sign
    // Group 6 = optional type suffix
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^(-)?(\\d+(\\.\\d+)?)([eE]([-+])?\\d+)?(f32|f64)?$");

    private static final int MIN_SURROGATE = 0xD800;
    private static final int MAX_SURROGATE = 0xDFFF;
    private static final int MAX_CODE_POINT_DIGITS = 6;

    private static final String NUL_MESSAGE = "a string cannot contain the NUL character: COL strings are NUL-terminated";

    @Override
    public Node visitProgram(final ProgramContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var statements = ctx.stmt().stream()
                                  .map(stmtContext -> stmtContext.accept(this))
                                  .map(Statement.class::cast)
                                  .toList();

        return new AstProgram(line, column, statements);
    }

    @Override
    public Node visitAliasStmt(final AliasStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var aliasName = ctx.ident().getText();
        final var type = getType(ctx.type());
        return new AliasStatement(line, column, aliasName, type);
    }

    @Override
    public Node visitFunctionCallStmt(final FunctionCallStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var fce = (FunctionCallExpression) ctx.functionCall().accept(this);
        return new FunCallStatement(line, column, fce, isValid(ctx.CALL()));
    }

    @Override
    public Node visitFunctionDefinitionStmt(final FunctionDefinitionStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var functionName = ctx.ident(0).getText();
        // A missing return type resolves to void, reported in FunDefPass1SemanticsParser
        final var returnType = getType(ctx.returnType());
        final var expression = (Expression) ctx.expr().accept(this);

        // ident(0) is the function name; parameters start at index 1
        final var identCtxs = ctx.ident().subList(1, ctx.ident().size());
        final var declarations = createDeclarations(identCtxs, ctx.type(), ctx.AS());
        final var argTypes = declarations.stream().map(Declaration::type).toList();

        final var functionType = Fun.from(argTypes, returnType);
        final var functionIdentifier = new Identifier(functionName, functionType);
        return new FunctionDefinitionStatement(line, column, functionIdentifier, declarations, expression);
    }

    @Override
    public Node visitAnonymousFunction(final AnonymousFunctionContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        // Unlike a named function, an omitted return type means "infer from the body"
        final var returnType = isValid(ctx.returnType()) ? getType(ctx.returnType()) : null;
        final var expression = (Expression) ctx.expr().accept(this);

        final var declarations = createDeclarations(ctx.ident(), ctx.type(), ctx.AS());
        return new AnonymousFunctionExpression(line, column, declarations, expression, returnType);
    }

    /**
     * Pairs each parameter name with its optional {@code as type}. A parameter has a type exactly
     * when an {@code as} token immediately follows its name; the parameter types appear in the same
     * order, so they are consumed in sequence. A parameter whose type is omitted is recorded as void
     * and reported in semantic analysis.
     */
    private static List<Declaration> createDeclarations(final List<IdentContext> identCtxs,
                                                        final List<TypeContext> typeCtxs,
                                                        final List<TerminalNode> asNodes) {
        final List<Declaration> declarations = new ArrayList<>();
        int typeIndex = 0;
        for (final var identCtx : identCtxs) {
            final var typeCtx = hasTypeAfter(identCtx, asNodes) ? typeCtxs.get(typeIndex++) : null;
            declarations.add(createDeclaration(identCtx, typeCtx));
        }
        return declarations;
    }

    private static boolean hasTypeAfter(final IdentContext identCtx, final List<TerminalNode> asNodes) {
        final var nextTokenIndex = identCtx.getStop().getTokenIndex() + 1;
        return asNodes.stream().anyMatch(as -> as.getSymbol().getTokenIndex() == nextTokenIndex);
    }

    private static Declaration createDeclaration(final IdentContext identCtx, final TypeContext typeCtx) {
        final var line = identCtx.getStart().getLine();
        final var column = identCtx.getStart().getCharPositionInLine();
        final var name = identCtx.getText();
        // A null type context resolves to void, reported in FunDefPass1SemanticsParser
        final var type = getType(typeCtx);
        return new Declaration(line, column, name, type);
    }

    @Override
    public Node visitValStmt(final ValStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var name = ctx.ident().getText();
        final var type = isValid(ctx.type()) ? getType(ctx.type()) : null;
        final var expression = isValid(ctx.expr()) ? (Expression) ctx.expr().accept(this) : null;
        // Binding with '=' instead of ':=' is reported in ValSemanticsParser
        final var usesEquals = isValid(ctx.EQUALS());
        return new ValDeclarationStatement(line, column, new DeclarationAssignment(line, column, name, type, expression), usesEquals);
    }

    @Override
    public Node visitWhileStmt(final WhileStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var expression = (Expression) ctx.expr().accept(this);
        final var statements = ctx.stmt().stream()
                                  .map(stmtContext -> stmtContext.accept(this))
                                  .map(Statement.class::cast)
                                  .toList();
        return new WhileStatement(line, column, expression, statements);
    }

    @Override
    public Node visitImportStmt(final ImportStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var fullName = ctx.libFunIdent().getText();
        final var strings = fullName.split("\\.");
        final var libraryName = strings[0];
        final var libraryFunctionName = strings[1];
        final var functionName = isValid(ctx.ident()) ? ctx.ident().getText() : libraryFunctionName;
        final var functionType = (Fun) getType(ctx.funType());

        final LibraryFunction libraryFunction = new LibraryFunction(
                functionName,
                functionType.getArgTypes(),
                functionType.getReturnType(),
                libraryName,
                new ExternalFunction(libraryFunctionName)
        );
        return new ImportStatement(line, column, libraryFunction);
    }

    @Override
    public Node visitOrExpr(OrExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            final var left = (Expression) ctx.orExpr().accept(this);
            final var right = (Expression) ctx.andExpr().accept(this);

            if (isValid(ctx.BAR())) {
                return new OrExpression(line, column, left, right);
            } else if (isValid(ctx.CIRCUMFLEX())) {
                return new XorExpression(line, column, left, right);
            } else if (isValid(ctx.OR())) {
                return new LogicalOrExpression(line, column, left, right);
            } else { // ctx.XOR()
                return new LogicalXorExpression(line, column, left, right);
            }
        }
    }

    @Override
    public Node visitAndExpr(AndExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            final var left = (Expression) ctx.andExpr().accept(this);
            final var right = (Expression) ctx.relExpr().accept(this);

            if (isValid(ctx.AMPERSAND())) {
                return new AndExpression(line, column, left, right);
            } else { // ctx.AND()
                return new LogicalAndExpression(line, column, left, right);
            }
        }
    }

    @Override
    public Node visitRelExpr(RelExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            final var left = (Expression) ctx.relExpr().accept(this);
            final var right = (Expression) ctx.addExpr().accept(this);

            final Expression re;
            if (isValid(ctx.EQ())) {
                re = new EqualExpression(line, column, left, right);
            } else if (isValid(ctx.GE())) {
                re = new GreaterOrEqualExpression(line, column, left, right);
            } else if (isValid(ctx.GT())) {
                re = new GreaterExpression(line, column, left, right);
            } else if (isValid(ctx.LE())) {
                re = new LessOrEqualExpression(line, column, left, right);
            } else if (isValid(ctx.LT())) {
                re = new LessExpression(line, column, left, right);
            } else { // ctx.NE()
                re = new NotEqualExpression(line, column, left, right);
            }

            // An unparenthesized chain (e.g. '1 < 2 < 3') has a relational production as its left
            // operand, while '(a == b) == c' descends through a parenthesized factor. Mark the chain
            // by parse shape so semantics can reject it without misjudging the latter.
            if (ctx.relExpr().getChildCount() > 1) {
                return new ChainedRelationalExpression(line, column, re);
            }
            return re;
        }
    }

    @Override
    public Node visitAddExpr(AddExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            final var left = (Expression) ctx.addExpr().accept(this);
            final var right = (Expression) ctx.term().accept(this);

            if (isValid(ctx.PLUS())) {
                return new AddExpression(line, column, left, right);
            } else { // ctx.MINUS()
                return new SubExpression(line, column, left, right);
            }
        }
    }

    @Override
    public Node visitTerm(final TermContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            final var left = (Expression) ctx.term().accept(this);
            final var right = (Expression) ctx.factor().accept(this);

            if (isValid(ctx.ASTERISK())) {
                return new MulExpression(line, column, left, right);
            } else if (isValid(ctx.SLASH())) {
                return new DivExpression(line, column, left, right);
            } else if (isValid(ctx.DIV())) {
                return new IDivExpression(line, column, left, right);
            } else { // ctx.MOD()
                return new ModExpression(line, column, left, right);
            }
        }
    }

    @Override
    public Node visitFactor(FactorContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();

        if (isValid(ctx.MINUS())) {
            final var expression = (Expression) ctx.factor().accept(this);
            if (expression instanceof IntegerLiteral integerLiteral) {
                // For negative integer literals, we can just update the value
                return integerLiteral.withValue("-" + integerLiteral.getValue());
            } else if (expression instanceof FloatLiteral floatLiteral) {
                // And for negative float literals, the same
                return floatLiteral.withValue("-" + floatLiteral.getValue());
            } else {
                // For other expressions, we have to construct a negate expression
                return new NegateExpression(line, column, expression);
            }
        } else if (isValid(ctx.TILDE())) {
            final var expression = (Expression) ctx.factor().accept(this);
            return new NotExpression(line, column, expression);
        } else if (isValid(ctx.NOT())) {
            final var expression = (Expression) ctx.factor().accept(this);
            return new LogicalNotExpression(line, column, expression);
        } else if (isValid(ctx.BECOME())) {
            final var functionCall = (FunctionCallExpression) ctx.functionCall().accept(this);
            return new BecomeExpression(line, column, functionCall);
        } else if (isSubExpression(ctx)) {
            return ctx.expr().accept(this);
        } else {
            Node factor = visitChildren(ctx);
            if (factor instanceof IdentifierExpression identifierExpression) {
                factor = IdentifierDerefExpression.from(identifierExpression);
            }
            return factor;
        }
    }

    @Override
    public Node visitIdent(final IdentContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var name = ctx.getText();
        // We know the name of the identifier, but not the type
        return new IdentifierExpression(line, column, new Identifier(name, null));
    }

    @Override
    public Node visitFunctionCall(FunctionCallContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var expressions = ctx.expr().stream()
                                   .map(e -> e.accept(this))
                                   .map(e -> (Expression) e)
                                   .toList();

        // We know the identifier is a function, but we don't know
        // the return type or the argument types
        final var functionName = ctx.ident().getText();
        final var functionType = Fun.from(Collections.nCopies(expressions.size(), null), null);
        final var identifier = new Identifier(functionName, functionType);
        return new FunctionCallExpression(line, column, identifier, expressions);
    }

    @Override
    public Node visitIfExpr(IfExprContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var ifExpr = (Expression) ctx.expr(0).accept(this);
        final var thenExpr = (Expression) ctx.expr(1).accept(this);
        // A missing else branch is a semantic error, reported in IfSemanticsParser
        final var elseExpr = isValid(ctx.expr(2)) ? (Expression) ctx.expr(2).accept(this) : null;
        return new IfExpression(line, column, ifExpr, thenExpr, elseExpr);
    }

    @Override
    public Node visitBooleanLiteral(BooleanLiteralContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return BooleanLiteral.from(line, column, ctx.getText());
    }

    @Override
    public Node visitIntegerLiteral(IntegerLiteralContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final String normalizedNumber;
        Type type = I64.INSTANCE;
        if (isValid(ctx.DEC_NUMBER())) {
            normalizedNumber = parseDecimalNumber(ctx.DEC_NUMBER());
        } else if (isValid(ctx.DEC_NUMBER_TYPED())) {
            normalizedNumber = parseTypedDecimalNumber(ctx.DEC_NUMBER_TYPED());
            type = parseTypeSuffix(ctx.DEC_NUMBER_TYPED());
        } else if (isValid(ctx.BIN_NUMBER())) {
            normalizedNumber = parseBinaryNumber(ctx.BIN_NUMBER());
        } else if (isValid(ctx.HEX_NUMBER())) {
            normalizedNumber = parseHexadecimalNumber(ctx.HEX_NUMBER());
        } else {
            throw new IllegalArgumentException("invalid number: " + ctx.getText());
        }
        return new IntegerLiteral(line, column, normalizedNumber, type);
    }

    private String parseBinaryNumber(TerminalNode node) {
        final var text = node.getText().replace("_", "").substring(2);
        return Long.valueOf(text, 2).toString();
    }

    private String parseHexadecimalNumber(TerminalNode node) {
        final var text = node.getText().replace("_", "").substring(2);
        return Long.valueOf(text, 16).toString();
    }

    private static String parseDecimalNumber(TerminalNode node) {
        return node.getText().replace("_", "");
    }

    private static String parseTypedDecimalNumber(TerminalNode node) {
        final var text = node.getText().replace("_", "");
        return text.substring(0, text.length() - 3);
    }

    private static Type parseTypeSuffix(TerminalNode node) {
        return node.getText().endsWith("i32") ? I32.INSTANCE : I64.INSTANCE;
    }

    @Override
    public Node visitFloatLiteral(FloatLiteralContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        if (isValid(ctx.MALFORMED_FLOAT())) {
            // A decimal point with digits on only one side; rejected in MalformedFloatSemanticsParser
            return new MalformedFloatLiteral(line, column, ctx.getText());
        }
        final Matcher matcher = FLOAT_PATTERN.matcher(ctx.getText().replace("_", ""));
        if (matcher.matches()) {
            final String normalizedNumber = normalizeFloatNumber(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(4),
                    matcher.group(5),
                    "E"
            );
            final Type type = "f32".equals(matcher.group(6)) ? F32.INSTANCE : F64.INSTANCE;
            return new FloatLiteral(line, column, normalizedNumber, type);
        } else {
            throw new IllegalArgumentException("Input '" + ctx.getText().trim() + "' failed to match regexp");
        }
    }

    @Override
    public Node visitStringLiteral(final StringLiteralContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var text = ctx.getText();
        // The token cannot match without both delimiters, so stripping them is safe
        final var body = text.substring(1, text.length() - 1);
        try {
            return new StringLiteral(line, column, decodeEscapes(body));
        } catch (final MalformedStringException e) {
            // Rejected in MalformedStringSemanticsParser, so the rest of the file is still parsed
            return new MalformedStringLiteral(line, column, text, e.getMessage());
        }
    }

    /**
     * Decodes the escape sequences in the body of a string literal: the C-style {@code \n},
     * {@code \t}, {@code \r}, {@code \\} and {@code \"}, plus the Rust-style {@code &#92;u{...}}
     * codepoint escape. Throws {@link MalformedStringException} if an escape is unknown or
     * names something that cannot appear in a COL string.
     */
    private static String decodeEscapes(final String text) {
        final var builder = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            final char c = text.charAt(index++);
            if (c != '\\') {
                builder.append(c);
                continue;
            }
            // The lexer only matches a backslash that is followed by a character on the same line
            final char escape = text.charAt(index++);
            switch (escape) {
                case 'n' -> builder.append('\n');
                case 't' -> builder.append('\t');
                case 'r' -> builder.append('\r');
                case '\\' -> builder.append('\\');
                case '"' -> builder.append('"');
                case 'u' -> index = appendCodePoint(text, index, builder);
                default -> throw new MalformedStringException(
                        "unknown escape '\\" + escape + "': COL supports \\n, \\t, \\r, \\\\, \\\" and \\u{...}");
            }
        }
        final var decoded = builder.toString();
        // A NUL may also arrive verbatim from the source file, not only from a codepoint escape
        if (decoded.indexOf('\0') >= 0) {
            throw new MalformedStringException(NUL_MESSAGE);
        }
        return decoded;
    }

    /**
     * Decodes a {@code &#92;u{...}} escape whose braces start at {@code start}, appends the codepoint
     * to the given builder, and returns the index just past the closing brace.
     */
    private static int appendCodePoint(final String text, final int start, final StringBuilder builder) {
        final var end = (start < text.length() && text.charAt(start) == '{') ? text.indexOf('}', start) : -1;
        if (end < 0) {
            throw new MalformedStringException("a unicode escape must be written \\u{...}, for example \\u{1F600}");
        }
        builder.appendCodePoint(parseCodePoint(text.substring(start + 1, end)));
        return end + 1;
    }

    /**
     * Parses the hexadecimal digits of a codepoint escape, rejecting anything that is not a
     * unicode scalar value COL can hold.
     */
    private static int parseCodePoint(final String digits) {
        if (digits.isEmpty() || digits.length() > MAX_CODE_POINT_DIGITS
                || digits.chars().anyMatch(c -> Character.digit(c, 16) < 0)) {
            throw new MalformedStringException("'\\u{" + digits + "}' is not a hexadecimal unicode codepoint");
        }
        final var codePoint = Integer.parseInt(digits, 16);
        if (codePoint == 0) {
            throw new MalformedStringException(NUL_MESSAGE);
        }
        // Surrogates are not scalar values: they exist only as UTF-16 pairs, and cannot be encoded
        if (!Character.isValidCodePoint(codePoint) || (codePoint >= MIN_SURROGATE && codePoint <= MAX_SURROGATE)) {
            throw new MalformedStringException("'\\u{" + digits + "}' is not a valid unicode scalar value");
        }
        return codePoint;
    }

    /**
     * Signals a string literal that cannot be decoded. Carries the complete error message, which
     * ends up on the {@link MalformedStringLiteral} that replaces the literal.
     */
    private static class MalformedStringException extends RuntimeException {
        MalformedStringException(final String message) {
            super(message);
        }
    }

    /**
     * Returns a type matching the given node; either a function type or a named type.
     */
    private static Type getType(final ParseTree node) {
        switch (node) {
            case ReturnTypeContext ctx -> {
                return getType(ctx.type());
            }
            case TypeContext ctx -> {
                if (isValid(ctx.funType())) {
                    return getType(ctx.funType());
                } else {
                    return new NamedType(ctx.getText());
                }
            }
            case FunTypeContext ctx -> {
                final var argTypes = ctx.type().stream()
                        .map(ColSyntaxVisitor::getType)
                        .toList();
                final var returnType = getType(ctx.returnType());
                return Fun.from(argTypes, returnType);
            }
            case null, default -> {
                return new NamedType("void");
            }
        }
    }

    /**
     * Returns {@code true} if the given factor is a subexpression.
     */
    private static boolean isSubExpression(final FactorContext factor) {
        return isValid(factor.OPEN()) && isValid(factor.CLOSE());
    }
}
