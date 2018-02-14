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
import se.dykstrom.jcc.basic.ast.OnGotoStatement;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * The syntax visitor for the Basic language, used to build an AST from an ANTLR parse tree.
 *
 * @author Johan Dykstrom
 */
@SuppressWarnings("unchecked")
public class BasicSyntaxVisitor extends BasicBaseVisitor<Node> {

    // Group 1 = optional sign
    // Group 2 = complete number
    // Group 3 = decimal point and fraction
    // Group 4 = complete exponent
    // Group 5 = optional exponent sign
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^(-)?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([deDE]([-+])?[0-9]+)?#?$");

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

    @Override
    public Node visitLine(LineContext ctx) {
        ListNode<Statement> stmtList = (ListNode<Statement>) visitChildren(ctx);
        // Set a line number label on the first statement on the line if available
        if (isValid(ctx.NUMBER())) {
            stmtList.getContents().get(0).setLabel(ctx.NUMBER().getText());
        }
        return stmtList;
    }

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

    @Override
    public Node visitOnGotoStmt(OnGotoStmtContext ctx) {
        List<String> labels = new ArrayList<>();
        if (isValid(ctx.numberList())) {
            ListNode<String> labelList = (ListNode<String>) ctx.numberList().accept(this);
            labels.addAll(labelList.getContents());
        }
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        Expression expression = (Expression) ctx.expr().accept(this);
        return new OnGotoStatement(line, column, expression, labels);
    }

    @Override
    public Node visitNumberList(NumberListContext ctx) {
        List<String> labels = new ArrayList<>();
        if (isValid(ctx.numberList())) {
            ListNode<String> labelList = (ListNode<String>) ctx.numberList().accept(this);
            labels.addAll(labelList.getContents());
        }
        labels.add(ctx.NUMBER().getText());

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, labels);
    }

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
            return emptyList();
        }
    }

    @Override
    public Node visitIfThenBlock(IfThenBlockContext ctx) {
        // IF expr THEN 
        //   statements 
        // ELSEIF expr THEN 
        //   statements 
        // ELSE 
        //   statements 
        // ENDIF
        
        List<Statement> thenStatements = new ArrayList<>();
        List<Statement> elseStatements = new ArrayList<>();

        // Visit the parts in reverse order:
        
        // ENDIF
        if (isValid(ctx.endIf())) {
            ListNode<Statement> stmtList = (ListNode<Statement>) ctx.endIf().accept(this);
            elseStatements.addAll(0, stmtList.getContents());
        }
        
        // ELSE block
        if (isValid(ctx.elseBlock())) {
            ListNode<Statement> stmtList = (ListNode<Statement>) ctx.elseBlock().accept(this);
            elseStatements.addAll(0, stmtList.getContents());
        }

        // ELSEIF blocks
        for (int i = ctx.elseIfBlock().size() - 1; i >= 0; i--) {
            ElseIfBlockContext elseIfCtx = ctx.elseIfBlock(i);
            Expression elseIfExpression = (Expression) elseIfCtx.expr().accept(this);
            ListNode<Statement> stmtList = (ListNode<Statement>) elseIfCtx.accept(this);
            List<Statement> elseIfStatements = stmtList.getContents();
            
            int line = elseIfCtx.getStart().getLine();
            int column = elseIfCtx.getStart().getCharPositionInLine();
            elseStatements = singletonList(new IfStatement(line, column, elseIfExpression, elseIfStatements, elseStatements));
        }

        // THEN block
        for (LineContext lineCtx : ctx.line()) {
            ListNode<Statement> stmtList = (ListNode<Statement>) lineCtx.accept(this);
            thenStatements.addAll(stmtList.getContents());
        }
        
        Expression ifExpression = (Expression) ctx.expr().accept(this);
        
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new IfStatement(line, column, ifExpression, thenStatements, elseStatements);
    }

    @Override
    public Node visitElseIfBlock(ElseIfBlockContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        
        List<Statement> statements = new ArrayList<>();
        if (isValid(ctx.NUMBER())) {
            // If there is a line number before ELSEIF, add a comment just to preserve the line number
            statements.add(new CommentStatement(line, column, "ELSEIF", ctx.NUMBER().getText()));
        }
        statements.addAll(parseBlock(ctx.line()));

        return new ListNode<>(line, column, statements);
    }

    @Override
    public Node visitElseBlock(ElseBlockContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        
        List<Statement> statements = new ArrayList<>();
        if (isValid(ctx.NUMBER())) {
            // If there is a line number before ELSE, add a comment just to preserve the line number
            statements.add(new CommentStatement(line, column, "ELSE", ctx.NUMBER().getText()));
        }
        statements.addAll(parseBlock(ctx.line()));
        
        return new ListNode<>(line, column, statements);
    }

    @Override
    public Node visitEndIf(EndIfContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();

        List<Statement> statements = new ArrayList<>();
        if (isValid(ctx.NUMBER())) {
            // If there is a line number before ENDIF, add a comment just to preserve the line number
            statements.add(new CommentStatement(line, column, "ENDIF", ctx.NUMBER().getText()));
        }
        return new ListNode<>(line, column, statements);
    }
    
    // While statements:

    @Override
    public Node visitWhileStmt(WhileStmtContext ctx) {
        // WHILE expr 
        //   statements 
        // WEND
        
        List<Statement> statements = new ArrayList<>();

        // Visit the parts in reverse order:
        
        // WEND
        if (isValid(ctx.NUMBER())) {
            int line = ctx.NUMBER().getSymbol().getLine();
            int column = ctx.NUMBER().getSymbol().getCharPositionInLine();
            statements.add(new CommentStatement(line, column, "WEND", ctx.NUMBER().getText()));
        }

        // WHILE block
        statements.addAll(0, parseBlock(ctx.line()));
        
        Expression expression = (Expression) ctx.expr().accept(this);
        
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new WhileStatement(line, column, expression, statements);
    }

    /**
     * Parses a block of statements, that is, a number of lines, and returns the result as a list of statements.
     */
    private List<Statement> parseBlock(List<LineContext> block) {
        List<Statement> statements = new ArrayList<>();
        for (LineContext lineCtx : block) {
            ListNode<Statement> stmtList = (ListNode<Statement>) lineCtx.accept(this);
            statements.addAll(stmtList.getContents());
        }
        return statements;
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

            if (isValid(ctx.OR())) {
                return new OrExpression(line, column, left, right);
            } else { // ctx.XOR()
                return new XorExpression(line, column, left, right);
            }
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
            Expression right = (Expression) ctx.notExpr().accept(this);

            return new AndExpression(line, column, left, right);
        }
    }

    @Override
    public Node visitNotExpr(NotExprContext ctx) {
        if (isValid(ctx.NOT())) {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            Expression expression = (Expression) ctx.relExpr().accept(this);
            return new NotExpression(line, column, expression);
        } else {
            return visitChildren(ctx);
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
            } else if (isValid(ctx.SLASH())) {
                term = new DivExpression(line, column, left, right);
            } else if (isValid(ctx.BACKSLASH())) {
                term = new IDivExpression(line, column, left, right);
            } else {
                term = new ModExpression(line, column, left, right);
            }
        }
        return term;
    }

    @Override
    public Node visitFactor(FactorContext ctx) {
        if (isValid(ctx.MINUS())) {
            // TODO: Consider using a unary negate expression.
            Expression expression = (Expression) ctx.expr().accept(this);
            if (expression instanceof IntegerLiteral) {
                // For negative integer literals, we can just update the value
                IntegerLiteral integer = (IntegerLiteral) expression;
                return integer.withValue("-" + integer.getValue());
            } else if (expression instanceof FloatLiteral) {
                // And for negative float literals, the same
                FloatLiteral floating = (FloatLiteral) expression;
                return floating.withValue("-" + floating.getValue());
            } else {
                // For other expressions, we have to construct a subtraction expression
                int line = ctx.getStart().getLine();
                int column = ctx.getStart().getCharPositionInLine();
                return new SubExpression(line, column, new IntegerLiteral(line, column, "0"), expression);
            }
        } else if (isSubExpression(ctx)) {
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
    public Node visitFunctionCall(FunctionCallContext ctx) {
        IdentifierExpression identifier = (IdentifierExpression) ctx.ident().accept(this);
        List<Expression> expressions = new ArrayList<>();
        if (isValid(ctx.exprList())) {
            ListNode<Expression> exprList = (ListNode<Expression>) ctx.exprList().accept(this);
            expressions.addAll(exprList.getContents());
        }
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new FunctionCallExpression(line, column, identifier.getIdentifier(), expressions);
    }

    @Override
    public Node visitExprList(ExprListContext ctx) {
        List<Expression> expressions = new ArrayList<>();
        if (isValid(ctx.exprList())) {
            ListNode<Expression> exprList = (ListNode<Expression>) ctx.exprList().accept(this);
            expressions.addAll(exprList.getContents());
        }
        expressions.add((Expression) ctx.expr().accept(this));

        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new ListNode<>(line, column, expressions);
    }

    @Override
    public Node visitString(StringContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        String text = ctx.getText();
        return new StringLiteral(line, column, text.substring(1, text.length() - 1));
    }

    @Override
    public Node visitFloating(FloatingContext ctx) {
        Matcher matcher = FLOAT_PATTERN.matcher(ctx.getText().trim());
        if (matcher.matches()) {
            String sign = matcher.group(1);
            String number = matcher.group(2);
            String exponent = matcher.group(4);
            String exponentSign = matcher.group(5);

            // Normalize sign
            if (sign == null) {
                sign = "";
            }

            // Normalize number
            if (number.startsWith(".")) {
                number = "0" + number;
            }
            if (number.endsWith(".")) {
                number = number + "0";
            }

            // Normalize exponent
            if (exponent == null) {
                exponent = "";
            } else {
                exponent = exponent.replaceAll("[dDE]", "e");
                if (exponentSign == null) {
                    exponent = exponent.substring(0, 1) + "+" + exponent.substring(1);
                }
            }

            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            return new FloatLiteral(line, column, sign + number + exponent);
        } else {
            throw new IllegalArgumentException("Input '" + ctx.getText().trim() + "' failed to match regexp");
        }
    }

    @Override
    public Node visitInteger(IntegerContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        if (isValid(ctx.NUMBER())) {
            return new IntegerLiteral(line, column, ctx.NUMBER().getText());
        } else if (isValid(ctx.HEXNUMBER())) {
            String hex = ctx.HEXNUMBER().getText().substring(2);
            Long value = Long.parseLong(hex, 16);
            return new IntegerLiteral(line, column, value.toString());
        } else if (isValid(ctx.OCTNUMBER())) {
            String oct = ctx.OCTNUMBER().getText().substring(2);
            Long value = Long.parseLong(oct, 8);
            return new IntegerLiteral(line, column, value.toString());
        } else {
            String bin = ctx.BINNUMBER().getText().substring(2);
            Long value = Long.parseLong(bin, 2);
            return new IntegerLiteral(line, column, value.toString());
        }
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
