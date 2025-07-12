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
import se.dykstrom.jcc.col.ast.statement.AliasStatement;
import se.dykstrom.jcc.col.ast.statement.FunCallStatement;
import se.dykstrom.jcc.col.ast.statement.ImportStatement;
import se.dykstrom.jcc.col.compiler.ColParser.*;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.Fun;
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
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^(-)?(\\d+(\\.\\d*)?|\\.\\d+)(E([-+])?\\d+)?$");

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
        return new FunCallStatement(line, column, fce);
    }

    @Override
    public Node visitFunctionDefinitionStmt(final FunctionDefinitionStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var functionName = ctx.ident(0).getText();
        final var returnType = getType(ctx.returnType());
        final var expression = (Expression) ctx.expr().accept(this);

        final List<Declaration> declarations = new ArrayList<>();
        // Ident index starts at 1 while type index starts at 0,
        // because the first ident is the function name
        for (int i = 1; i < ctx.ident().size(); i++) {
            declarations.add(createDeclaration(ctx, i));
        }
        final var argTypes = declarations.stream().map(Declaration::type).toList();

        final var functionType = Fun.from(argTypes, returnType);
        final var functionIdentifier = new Identifier(functionName, functionType);
        return new FunctionDefinitionStatement(line, column, functionIdentifier, declarations, expression);
    }

    private static Declaration createDeclaration(final FunctionDefinitionStmtContext ctx, final int index) {
        final var argLine = ctx.ident(index).getStart().getLine();
        final var argColumn = ctx.ident(index).getStart().getCharPositionInLine();
        final var argName = ctx.ident(index).getText();
        final var argType = getType(ctx.type(index - 1));
        return new Declaration(argLine, argColumn, argName, argType);
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
            final var left = (Expression) ctx.addExpr(0).accept(this);
            final var right = (Expression) ctx.addExpr(1).accept(this);

            if (isValid(ctx.EQ())) {
                return new EqualExpression(line, column, left, right);
            } else if (isValid(ctx.GE())) {
                return new GreaterOrEqualExpression(line, column, left, right);
            } else if (isValid(ctx.GT())) {
                return new GreaterExpression(line, column, left, right);
            } else if (isValid(ctx.LE())) {
                return new LessOrEqualExpression(line, column, left, right);
            } else if (isValid(ctx.LT())) {
                return new LessExpression(line, column, left, right);
            } else { // ctx.NE()
                return new NotEqualExpression(line, column, left, right);
            }
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
        if (isValid(ctx.DEC_NUMBER())) {
            normalizedNumber = parseDecimalNumber(ctx.DEC_NUMBER());
        } else if (isValid(ctx.BIN_NUMBER())) {
            normalizedNumber = parseBinaryNumber(ctx.BIN_NUMBER());
        } else if (isValid(ctx.HEX_NUMBER())) {
            normalizedNumber = parseHexadecimalNumber(ctx.HEX_NUMBER());
        } else {
            throw new IllegalArgumentException("invalid number: " + ctx.getText());
        }
        return new IntegerLiteral(line, column, normalizedNumber);
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

    @Override
    public Node visitFloatLiteral(FloatLiteralContext ctx) {
        final Matcher matcher = FLOAT_PATTERN.matcher(ctx.getText().replace("_", ""));
        if (matcher.matches()) {
            final String normalizedNumber = normalizeFloatNumber(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(4),
                    matcher.group(5),
                    "E"
            );
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            return new FloatLiteral(line, column, normalizedNumber);
        } else {
            throw new IllegalArgumentException("Input '" + ctx.getText().trim() + "' failed to match regexp");
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
