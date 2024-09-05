/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.tiny.compiler;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;
import se.dykstrom.jcc.tiny.compiler.TinyParser.*;

/**
 * The syntax visitor for the Tiny language, used to build an AST from an ANTLR parse tree.
 *
 * @author Johan Dykstrom
 */
class TinySyntaxVisitor extends TinyBaseVisitor<Node> {

    @Override
    public Node visitProgram(ProgramContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var statements = ctx.stmt().stream()
                .map(c -> (Statement) c.accept(this))
                .toList();
        return new AstProgram(line, column, statements);
    }

    @Override
    public Node visitAssignStmt(AssignStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var lhs = ((IdentifierExpression) ctx.ident().accept(this));
        final var rhs = (Expression) ctx.expr().accept(this);
        return new AssignStatement(line, column, IdentifierNameExpression.from(lhs, lhs.getIdentifier()), rhs);
    }

    @Override
    public Node visitReadStmt(ReadStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var identifiers = ctx.ident().stream()
                .map(c -> (IdentifierExpression) c.accept(this))
                .map(IdentifierExpression::getIdentifier)
                .toList();
        return new ReadStatement(line, column, identifiers);
    }

    @Override
    public Node visitWriteStmt(WriteStmtContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var expressions = ctx.expr().stream()
                .map(c -> (Expression) c.accept(this))
                .toList();
        return new WriteStatement(line, column, expressions);
    }

    @Override
    public Node visitExpr(ExprContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        if (ctx.PLUS() != null) {
            final var left = (Expression) ctx.expr().accept(this);
            final var right = (Expression) ctx.factor().accept(this);
            return new AddExpression(line, column, left, right);
        } else if (ctx.MINUS() != null) {
            final var left = (Expression) ctx.expr().accept(this);
            final var right = (Expression) ctx.factor().accept(this);
            return new SubExpression(line, column, left, right);
        } else {
            return ctx.factor().accept(this);
        }
    }

    @Override
    public Node visitFactor(FactorContext ctx) {
        final var factor = visitChildren(ctx);
        if (factor instanceof IdentifierExpression ie) {
            return IdentifierDerefExpression.from(ie);
        } else {
            return factor;
        }
    }

    @Override
    public Node visitInteger(IntegerContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new IntegerLiteral(line, column, ctx.getText());
    }

    @Override
    public Node visitIdent(IdentContext ctx) {
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new IdentifierExpression(line, column, new Identifier(ctx.getText(), I64.INSTANCE));
    }
}
