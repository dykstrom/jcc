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

package se.dykstrom.jcc.basic.compiler;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import se.dykstrom.jcc.basic.ast.EndStatement;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.basic.compiler.BasicParser.*;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * The syntax visitor for the Basic language, used to build an AST from an ANTLR parse tree.
 *
 * @author Johan Dykstrom
 */
public class BasicSyntaxVisitor extends BasicBaseVisitor<Node> {

    @SuppressWarnings("unchecked")
    @Override
    public Node visitProgram(ProgramContext ctx) {
        List<Statement> statements = new ArrayList<>();
        for (LineContext lineCtx : ctx.line()) {
            ListNode<Statement> stmtList = (ListNode<Statement>) lineCtx.accept(this);
            statements.addAll(stmtList.getContents());
        }

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new Program(line, column, statements);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node visitLine(LineContext ctx) {
        ListNode<Statement> stmtList = (ListNode<Statement>) visitChildren(ctx);
        // Set a line number label on the first statement on the line if available
        if (isValid(ctx.NUMBER())) {
            stmtList.getContents().get(0).setLabel(ctx.NUMBER().getText());
        }
        return stmtList;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node visitStmtList(StmtListContext ctx) {
        List<Statement> statements = new ArrayList<>();

        if (isValid(ctx.stmtList())) {
            ListNode<Statement> stmtList = (ListNode<Statement>) ctx.stmtList().accept(this);
            statements.addAll(stmtList.getContents());
        }
        statements.add((Statement) ctx.stmt().accept(this));

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, statements);
    }

    @Override
    public Node visitAssignStmt(AssignStmtContext ctx) {
        IdentifierExpression identifier = (IdentifierExpression) ctx.ident().accept(this);
        Expression expression = (Expression) ctx.expr().accept(this);
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new AssignStatement(line, column, identifier.getIdentifier(), expression);
    }

    @Override
    public Node visitCommentStmt(CommentStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new CommentStatement(line, column);
    }

    @Override
    public Node visitEndStmt(EndStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new EndStatement(line, column);
    }

    @Override
    public Node visitGotoStmt(GotoStmtContext ctx) {
        String label = ctx.NUMBER().getText();
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new GotoStatement(line, column, label);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node visitPrintStmt(PrintStmtContext ctx) {
        List<Expression> expressions = new ArrayList<>();
        if (isValid(ctx.printList())) {
            ListNode<Expression> printList = (ListNode<Expression>) ctx.printList().accept(this);
            expressions.addAll(printList.getContents());
        }

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new PrintStatement(line, column, expressions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node visitPrintList(PrintListContext ctx) {
        List<Expression> expressions = new ArrayList<>();
        if (isValid(ctx.printList())) {
            ListNode<Expression> printList = (ListNode<Expression>) ctx.printList().accept(this);
            expressions.addAll(printList.getContents());
        }
        expressions.add((Expression) ctx.expr().accept(this));

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, expressions);
    }

    // If statements:

    @Override
    public Node visitIfGoto(IfGotoContext ctx) {
        Expression expression = (Expression) ctx.expr().accept(this);
        
        int gotoLine = ctx.NUMBER().getSymbol().getLine();
        int gotoColumn = ctx.NUMBER().getSymbol().getCharPositionInLine();
        String gotoLabel = ctx.NUMBER().getText();
        List<Statement> ifStatements = singletonList(new GotoStatement(gotoLine, gotoColumn, gotoLabel));

        List<Statement> elseStatements = parseSingleLineElse(ctx.elseSingle());
        
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new IfStatement(line, column, expression, ifStatements, elseStatements);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node visitIfThenSingle(IfThenSingleContext ctx) {
        Expression expression = (Expression) ctx.expr().accept(this);
        
        List<Statement> ifStatements;
        if (isValid(ctx.NUMBER())) {
            int gotoLine = ctx.NUMBER().getSymbol().getLine();
            int gotoColumn = ctx.NUMBER().getSymbol().getCharPositionInLine();
            String gotoLabel = ctx.NUMBER().getText();
            ifStatements = singletonList(new GotoStatement(gotoLine, gotoColumn, gotoLabel));
        } else {
            ListNode<Statement> stmtList = (ListNode<Statement>) ctx.stmtList().accept(this);
            ifStatements = stmtList.getContents();
        }
        
        List<Statement> elseStatements = parseSingleLineElse(ctx.elseSingle());
        
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new IfStatement(line, column, expression, ifStatements, elseStatements);
    }

    @SuppressWarnings("unchecked")
    private List<Statement> parseSingleLineElse(ElseSingleContext elseCtx) {
        if (isValid(elseCtx)) {
            if (isValid(elseCtx.NUMBER())) {
                int line = elseCtx.NUMBER().getSymbol().getLine();
                int column = elseCtx.NUMBER().getSymbol().getCharPositionInLine();
                String label = elseCtx.NUMBER().getText();
                return singletonList(new GotoStatement(line, column, label));
            } else {
                ListNode<Statement> stmtList = (ListNode<Statement>) elseCtx.accept(this);
                return stmtList.getContents();
            }
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node visitIfThenBlock(IfThenBlockContext ctx) {
        // IF expr THEN 
        //   statements 
        // ELSEIF expr THEN 
        //   statements 
        // ELSE 
        //   statements 
        // ENDIF
        
        // Visit the parts in reverse order:
        
        // 1. ELSE block
        List<Statement> elseStatements = null;
        if (isValid(ctx.elseBlock())) {
            ListNode<Statement> stmtList = (ListNode<Statement>) ctx.elseBlock().accept(this);
            elseStatements = stmtList.getContents();
        }

        // 2. ELSEIF blocks
        for (int i = ctx.elseIfBlock().size() - 1; i >= 0; i--) {
            ElseIfBlockContext elseIfCtx = ctx.elseIfBlock(i);
            Expression elseIfExpression = (Expression) elseIfCtx.expr().accept(this);
            ListNode<Statement> stmtList = (ListNode<Statement>) elseIfCtx.accept(this);
            List<Statement> elseIfStatements = stmtList.getContents();
            
            int line = elseIfCtx.getStart().getLine();
            int column = elseIfCtx.getStart().getCharPositionInLine();
            elseStatements = singletonList(new IfStatement(line, column, elseIfExpression, elseIfStatements, elseStatements));
        }

        // 3. THEN block
        List<Statement> ifStatements = new ArrayList<>();
        for (LineContext lineCtx : ctx.line()) {
            ListNode<Statement> stmtList = (ListNode<Statement>) lineCtx.accept(this);
            ifStatements.addAll(stmtList.getContents());
        }
        
        Expression ifExpression = (Expression) ctx.expr().accept(this);
        
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new IfStatement(line, column, ifExpression, ifStatements, elseStatements);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node visitElseIfBlock(ElseIfBlockContext ctx) {
        List<Statement> statements = new ArrayList<>();
        for (LineContext lineCtx : ctx.line()) {
            ListNode<Statement> stmtList = (ListNode<Statement>) lineCtx.accept(this);
            statements.addAll(stmtList.getContents());
        }
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, statements);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node visitElseBlock(ElseBlockContext ctx) {
        List<Statement> statements = new ArrayList<>();
        for (LineContext lineCtx : ctx.line()) {
            ListNode<Statement> stmtList = (ListNode<Statement>) lineCtx.accept(this);
            statements.addAll(stmtList.getContents());
        }
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, statements);
    }
    
    // Expressions:
    
    @Override
    public Node visitOrExpr(OrExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.orExpr().accept(this);
            Expression right = (Expression) ctx.andExpr().accept(this);

            return new OrExpression(line, column, left, right);
        }
    }

    @Override
    public Node visitAndExpr(AndExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.andExpr().accept(this);
            Expression right = (Expression) ctx.relExpr().accept(this);

            return new AndExpression(line, column, left, right);
        }
    }

    @Override
    public Node visitRelExpr(RelExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.addSubExpr(0).accept(this);
            Expression right = (Expression) ctx.addSubExpr(1).accept(this);

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
    public Node visitAddSubExpr(AddSubExprContext ctx) {
        Node expr;
        if (ctx.getChildCount() == 1) {
            expr = visitChildren(ctx);
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.addSubExpr().accept(this);
            Expression right = (Expression) ctx.term().accept(this);

            if (isValid(ctx.PLUS())) {
                expr = new AddExpression(line, column, left, right);
            } else { // ctx.MINUS()
                expr = new SubExpression(line, column, left, right);
            }
        }
        return expr;
    }

    @Override
    public Node visitTerm(TermContext ctx) {
        Node term;
        if (ctx.getChildCount() == 1) {
            term = visitChildren(ctx);
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression left = (Expression) ctx.term().accept(this);
            Expression right = (Expression) ctx.factor().accept(this);

            if (isValid(ctx.STAR())) {
                term = new MulExpression(line, column, left, right);
            } else { // ctx.SLASH()
                term = new DivExpression(line, column, left, right);
            }
        }
        return term;
    }

    @Override
    public Node visitFactor(FactorContext ctx) {
        if (isSubExpression(ctx)) {
            return ctx.expr().accept(this);
        } else {
            Node factor = visitChildren(ctx);
            if (factor instanceof IdentifierExpression) {
                factor = IdentifierDerefExpression.from((IdentifierExpression) factor);
            }
            return factor;
        }
    }

    @Override
    public Node visitString(StringContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        String text = ctx.getText();
        return new StringLiteral(line, column, text.substring(1, text.length() - 1));
    }

    @Override
    public Node visitInteger(IntegerContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new IntegerLiteral(line, column, ctx.getText());
    }

    @Override
    public Node visitBool(BoolContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new BooleanLiteral(line, column, isValid(ctx.FALSE()) ? "0" : "-1");
    }

    @Override
    public Node visitIdent(IdentContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        String text = ctx.getText();
        Type type = text.endsWith("%") ? I64.INSTANCE : text.endsWith("$") ? Str.INSTANCE : Unknown.INSTANCE;
        return new IdentifierExpression(line, column, new Identifier(text, type));
    }

    /**
     * Returns {@code true} if the given node is valid.
     */
    private boolean isValid(ParseTree node) {
        return node != null && !(node instanceof ErrorNode);
    }

    /**
     * Returns {@code true} if the given factor is a subexpression.
     */
    private boolean isSubExpression(FactorContext factor) {
        return isValid(factor.OPEN()) && isValid(factor.CLOSE());
    }
}
