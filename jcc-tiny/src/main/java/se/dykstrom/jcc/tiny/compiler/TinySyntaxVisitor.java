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

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;
import se.dykstrom.jcc.tiny.compiler.TinyParser.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The syntax visitor for the Tiny language, used to build an AST from an ANTLR parse tree.
 *
 * @author Johan Dykstrom
 */
@SuppressWarnings("unchecked")
class TinySyntaxVisitor extends TinyBaseVisitor<Node> {

    @Override
    public Node visitProgram(ProgramContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        ListNode<Statement> stmtList = (ListNode<Statement>) ctx.stmt_list().accept(this);
        return new Program(line, column, stmtList.contents());
    }

    @Override
    public Node visitStmt_list(Stmt_listContext ctx) {
        List<Statement> statements = new ArrayList<>();
        if (isValid(ctx.stmt_list())) {
            ListNode<Statement> stmtList = (ListNode<Statement>) ctx.stmt_list().accept(this);
            statements = stmtList.contents();
        }
        statements.add((Statement) ctx.stmt().accept(this));

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, statements);
    }

    @Override
    public Node visitAssign_stmt(Assign_stmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        IdentifierExpression ie = ((IdentifierExpression) ctx.ident().accept(this));
        Expression expression = (Expression) ctx.expr().accept(this);
        return new AssignStatement(line, column, IdentifierNameExpression.from(ie, ie.getIdentifier()), expression);
    }

    @Override
    public Node visitRead_stmt(Read_stmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        ListNode<IdentifierExpression> exprList = (ListNode<IdentifierExpression>) ctx.id_list().accept(this);
        List<Identifier> identList = exprList.contents().stream().map(IdentifierExpression::getIdentifier).toList();
        return new ReadStatement(line, column, identList);
    }

    @Override
    public Node visitWrite_stmt(Write_stmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        ListNode<Expression> exprList = (ListNode<Expression>) ctx.expr_list().accept(this);
        return new WriteStatement(line, column, exprList.contents());
    }

    @Override
    public Node visitId_list(Id_listContext ctx) {
        List<IdentifierExpression> expressions = new ArrayList<>();
        if (isValid(ctx.id_list())) {
            ListNode<IdentifierExpression> exprList = (ListNode<IdentifierExpression>) ctx.id_list().accept(this);
            expressions = exprList.contents();
        }
        expressions.add((IdentifierExpression) ctx.ident().accept(this));

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, expressions);
    }

    @Override
    public Node visitExpr_list(Expr_listContext ctx) {
        List<Expression> expressions = new ArrayList<>();
        if (isValid(ctx.expr_list())) {
            ListNode<Expression> exprList = (ListNode<Expression>) ctx.expr_list().accept(this);
            expressions = exprList.contents();
        }
        expressions.add((Expression) ctx.expr().accept(this));

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, expressions);
    }

    @Override
    public Node visitExpr(ExprContext ctx) {
        Node expr;
        if (ctx.getChildCount() == 1) {
            expr = visitChildren(ctx);
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.expr().accept(this);
            Expression right = (Expression) ctx.factor().accept(this);

            if (isPlus(ctx.op())) {
                expr = new AddExpression(line, column, left, right);
            } else { // ctx.MINUS()
                expr = new SubExpression(line, column, left, right);
            }
        }
        return expr;
    }

    @Override
    public Node visitFactor(FactorContext ctx) {
        Node factor = visitChildren(ctx);
        if (factor instanceof IdentifierExpression identifierExpression) {
            factor = IdentifierDerefExpression.from(identifierExpression);
        }
        return factor;
    }

    @Override
    public Node visitInteger(IntegerContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new IntegerLiteral(line, column, ctx.getText());
    }

    @Override
    public Node visitIdent(IdentContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new IdentifierExpression(line, column, new Identifier(ctx.getText(), I64.INSTANCE));
    }

    /**
     * Returns {@code true} if the given node represents a plus operation.
     */
    private boolean isPlus(ParseTree node) {
        return node.getText().equals("+");
    }

    /**
     * Returns {@code true} if the given node is valid.
     */
    private boolean isValid(ParseTree node) {
        return node != null && !(node instanceof ErrorNode);
    }
}
