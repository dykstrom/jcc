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
import se.dykstrom.jcc.col.ast.PrintlnStatement;
import se.dykstrom.jcc.col.compiler.ColParser.*;
import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.IdentifierExpression;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.Node;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.SubExpression;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;

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
    public Node visitAliasStmt(final AliasStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var name = ctx.ident().getText();
        final var value = ctx.type().getText();
        return new AliasStatement(line, column, name, value);
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
        return new IntegerLiteral(line, column, ctx.NUMBER().getText());
    }

    @Override
    public Node visitIdent(IdentContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var name = ctx.getText();
        // TODO: We must allow the type to be unknown.
        return new IdentifierExpression(line, column, new Identifier(name, I64.INSTANCE));
    }

    /**
     * Returns {@code true} if the given node is valid.
     */
    private static boolean isValid(final ParseTree node) {
        return node != null && !(node instanceof ErrorNode);
    }
}
