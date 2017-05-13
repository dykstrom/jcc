/*
 * Copyright (C) 2016 Johan Dykstrom
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
import org.antlr.v4.runtime.tree.TerminalNode;
import se.dykstrom.jcc.basic.ast.EndStatement;
import se.dykstrom.jcc.basic.ast.GotoStatement;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.basic.ast.RemStatement;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

import java.util.ArrayList;
import java.util.List;

import static se.dykstrom.jcc.basic.compiler.BasicParser.*;

/**
 * The syntax listener for the Basic language, that listens to events from class BasicParser.
 *
 * @author Johan Dykstrom
 */
class BasicSyntaxListener extends BasicBaseListener {

    private Program program;
    // The list of all statements in the program
    private List<Statement> statementList;
    // The list of all statements on the current line
    private List<Statement> lineStatementList;
    private List<Expression> printList;
    private Expression expression;

    Program getProgram() {
        return program;
    }

    @Override
    public void enterProgram(ProgramContext ctx) {
        statementList = new ArrayList<>();
    }

    @Override
    public void exitProgram(ProgramContext ctx) {
        program = new Program(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), statementList);
        statementList = null;
    }

    @Override
    public void enterLine(LineContext ctx) {
        lineStatementList = new ArrayList<>();
    }

    @Override
    public void exitLine(LineContext ctx) {
        if (!lineStatementList.isEmpty()) {
            // Set a line number label on the first statement on the line
            Statement firstStatement = lineStatementList.get(0);
            firstStatement.setLabel(parseInteger(ctx.NUMBER()));
            statementList.addAll(lineStatementList);
        }
        lineStatementList = null;
    }

    @Override
    public void exitEnd_stmt(End_stmtContext ctx) {
        lineStatementList.add(new EndStatement(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine()));
    }

    @Override
    public void exitAssign_stmt(Assign_stmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        Identifier identifier = parseIdentifier(ctx.ident());
        lineStatementList.add(new AssignStatement(line, column, identifier, expression));
    }

    @Override
    public void exitGoto_stmt(Goto_stmtContext ctx) {
        if (isValid(ctx.NUMBER())) {
            String gotoLine = parseInteger(ctx.NUMBER());
            lineStatementList.add(new GotoStatement(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), gotoLine));
        }
    }

    @Override
    public void enterPrint_stmt(Print_stmtContext ctx) {
        printList = new ArrayList<>();
    }

    @Override
    public void exitPrint_stmt(Print_stmtContext ctx) {
        lineStatementList.add(new PrintStatement(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), printList));
        printList = null;
    }

    @Override
    public void exitPrint_list(Print_listContext ctx) {
        printList.add(expression);
        expression = null;
    }

    @Override
    public void exitComment_stmt(Comment_stmtContext ctx) {
        lineStatementList.add(new RemStatement(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine()));
    }

    @Override
    public void exitExpr(ExprContext ctx) {
        expression = parseExpr(ctx);
    }

    private Expression parseExpr(ExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return parseTerm(ctx.term());
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();

            Expression left = parseExpr(ctx.expr());
            Expression right = parseTerm(ctx.term());

            ParseTree operation = ctx.getChild(1);
            if (isPlus(operation)) {
                return new AddExpression(line, column, left, right);
            } else {
                return new SubExpression(line, column, left, right);
            }
        }
    }

    private Expression parseTerm(TermContext ctx) {
        if (ctx.getChildCount() == 1) {
            return parseFactor((FactorContext) ctx.getChild(0));
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();

            Expression left = parseTerm((TermContext) ctx.getChild(0));
            Expression right = parseFactor((FactorContext) ctx.getChild(2));

            ParseTree operation = ctx.getChild(1);
            if (isStar(operation)) {
                return new MulExpression(line, column, left, right);
            } else {
                return new DivExpression(line, column, left, right);
            }
        }
    }

    private Expression parseFactor(FactorContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();

        ParseTree factor = ctx.getChild(0);
        if (isString(factor)) {
            return new StringLiteral(line, column, parseString(factor));
        } else if (isInteger(factor)) {
            return new IntegerLiteral(line, column, parseInteger(factor));
        } else if (isIdent(factor)) {
            return new IdentifierDerefExpression(line, column, parseIdentifier(factor));
        } else if (isSubExpression(factor)) {
            return parseExpr((ExprContext) ctx.getChild(1));
        }

        // Return a dummy expression so we can continue parsing
        return new DummyExpression();
    }

    /**
     * Returns {@code true} if the given terminal node is valid.
     */
    private boolean isValid(TerminalNode node) {
        return node != null && !(node instanceof ErrorNode);
    }

    private boolean isPlus(ParseTree operation) {
        return operation.getText().equals("+");
    }

    private boolean isStar(ParseTree operation) {
        return operation.getText().equals("*");
    }

    private boolean isString(ParseTree factor) {
        return factor instanceof StringContext;
    }

    private boolean isInteger(ParseTree factor) {
        return factor instanceof IntegerContext;
    }

    private boolean isIdent(ParseTree factor) {
        return factor instanceof IdentContext;
    }

    private boolean isSubExpression(ParseTree factor) {
        return (factor instanceof TerminalNode) && (((TerminalNode) factor).getSymbol().getType() == BasicLexer.OPEN);
    }

    private Identifier parseIdentifier(ParseTree identifier) {
        String text = identifier.getText().trim();
        Type type = text.endsWith("%") ? I64.INSTANCE : text.endsWith("$") ? Str.INSTANCE : Unknown.INSTANCE;
        return new Identifier(text, type);
    }

    private static String parseString(ParseTree string) {
        String text = string.getText();
        return text.substring(1, text.length() - 1);
    }

    private static String parseInteger(ParseTree integer) {
        return integer.getText();
    }
}
