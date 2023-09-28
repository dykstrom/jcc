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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import se.dykstrom.jcc.col.ast.AliasStatement;
import se.dykstrom.jcc.col.ast.FunCallStatement;
import se.dykstrom.jcc.col.ast.ImportStatement;
import se.dykstrom.jcc.col.ast.PrintlnStatement;
import se.dykstrom.jcc.col.compiler.ColParser.*;
import se.dykstrom.jcc.col.types.NamedType;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;

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

        return new Program(line, column, statements);
    }

    @Override
    public Node visitAliasStmt(final AliasStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var aliasName = ctx.ident().getText();
        final var typeName = ctx.type().getText();
        return new AliasStatement(line, column, aliasName, new NamedType(typeName));
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
        final var returnType = getTypeOrDefault(ctx.returnType());
        final var expression = (Expression) ctx.expr().accept(this);

        final List<Declaration> declarations = new ArrayList<>();
        // Ident index starts at 1 while type index starts at 0,
        // because the first ident is the function name
        for (int i = 1; i < ctx.ident().size(); i++) {
            final var argName = ctx.ident(i).getText();
            final var argType = new NamedType(ctx.type(i - 1).getText());
            final var argLine = ctx.ident(i).getStart().getLine();
            final var argColumn = ctx.ident(i).getStart().getCharPositionInLine();
            declarations.add(new Declaration(argLine, argColumn, argName, argType));
        }
        final var argTypes = declarations.stream().map(Declaration::type).toList();

        final var functionType = Fun.from(argTypes, returnType);
        final var functionIdentifier = new Identifier(functionName, functionType);
        return new FunctionDefinitionStatement(line, column, functionIdentifier, declarations, expression);
    }

    @Override
    public Node visitImportStmt(final ColParser.ImportStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var fullName = ctx.libFunIdent().getText();
        final var strings = fullName.split("\\.");
        final var libraryName = strings[0];
        final var libraryFunctionName = strings[1];
        final var functionName = isValid(ctx.ident()) ? ctx.ident().getText() : libraryFunctionName;
        final var argTypes = ctx.type().stream()
                                .map(t -> new NamedType(t.getText()))
                                .map(Type.class::cast)
                                .toList();
        final var returnType = getTypeOrDefault(ctx.returnType());

        final LibraryFunction libraryFunction = new LibraryFunction(
                functionName,
                argTypes,
                returnType,
                libraryName,
                new ExternalFunction(libraryFunctionName)
        );
        return new ImportStatement(line, column, libraryFunction);
    }

    @Override
    public Node visitPrintlnStmt(final PrintlnStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();

        if (isValid(ctx.expr())) {
            final var expression = (Expression) ctx.expr().accept(this);
            return new PrintlnStatement(line, column, expression);
        } else {
            return new PrintlnStatement(line, column, null);
        }
    }

    @Override
    public Node visitAddSubExpr(AddSubExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            // A single term
            return visitChildren(ctx);
        } else {
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            final var left = (Expression) ctx.addSubExpr().accept(this);
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
            // A single factor
            return visitChildren(ctx);
        } else {
            final var line = ctx.getStart().getLine();
            final var column = ctx.getStart().getCharPositionInLine();
            final var left = (Expression) ctx.term().accept(this);
            final var right = (Expression) ctx.factor().accept(this);

            if (isValid(ctx.STAR())) {
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
                final var line = ctx.getStart().getLine();
                final var column = ctx.getStart().getCharPositionInLine();
                return new NegateExpression(line, column, expression);
            }
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
    public Node visitIntegerLiteral(IntegerLiteralContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new IntegerLiteral(line, column, ctx.NUMBER().getText().replace("_", ""));
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
     * Returns a {@code NamedType} matching the text of the given node,
     * or the default type if the node is not valid.
     */
    private static Type getTypeOrDefault(final ParseTree node) {
        return new NamedType(isValid(node) ? node.getText() : "void");
    }

    /**
     * Returns {@code true} if the given node is valid.
     */
    private static boolean isValid(final ParseTree node) {
        return node != null && !(node instanceof ErrorNode);
    }

    /**
     * Returns {@code true} if the given factor is a subexpression.
     */
    private static boolean isSubExpression(FactorContext factor) {
        return isValid(factor.OPEN()) && isValid(factor.CLOSE());
    }
}
