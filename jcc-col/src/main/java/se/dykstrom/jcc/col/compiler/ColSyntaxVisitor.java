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

import java.util.Collections;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import se.dykstrom.jcc.col.ast.AliasStatement;
import se.dykstrom.jcc.col.ast.ImportStatement;
import se.dykstrom.jcc.col.ast.PrintlnStatement;
import se.dykstrom.jcc.col.compiler.ColParser.AddSubExprContext;
import se.dykstrom.jcc.col.compiler.ColParser.AliasStmtContext;
import se.dykstrom.jcc.col.compiler.ColParser.FunctionCallContext;
import se.dykstrom.jcc.col.compiler.ColParser.IntegerLiteralContext;
import se.dykstrom.jcc.col.compiler.ColParser.PrintlnStmtContext;
import se.dykstrom.jcc.col.compiler.ColParser.ProgramContext;
import se.dykstrom.jcc.col.types.NamedType;
import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.Node;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.SubExpression;
import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;

public class ColSyntaxVisitor extends ColBaseVisitor<Node> {

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
}
