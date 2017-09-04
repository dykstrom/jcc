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
import se.dykstrom.jcc.basic.compiler.BasicParser.*;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

import java.util.ArrayList;
import java.util.List;

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
        int line = ctx.getStart().getLine();
		int column = ctx.getStart().getCharPositionInLine();
		program = new Program(line, column, statementList);
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
    public void exitEndStmt(EndStmtContext ctx) {
        int line = ctx.getStart().getLine();
		int column = ctx.getStart().getCharPositionInLine();
		lineStatementList.add(new EndStatement(line, column));
    }

    @Override
    public void exitAssignStmt(AssignStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        Identifier identifier = parseIdentifier(ctx.ident());
        lineStatementList.add(new AssignStatement(line, column, identifier, expression));
    }

    @Override
    public void exitGotoStmt(GotoStmtContext ctx) {
        if (isValid(ctx.NUMBER())) {
            String gotoLine = parseInteger(ctx.NUMBER());
            int line = ctx.getStart().getLine();
			int column = ctx.getStart().getCharPositionInLine();
			lineStatementList.add(new GotoStatement(line, column, gotoLine));
        }
    }

    @Override
    public void enterPrintStmt(PrintStmtContext ctx) {
        printList = new ArrayList<>();
    }

    @Override
    public void exitPrintStmt(PrintStmtContext ctx) {
        int line = ctx.getStart().getLine();
		int column = ctx.getStart().getCharPositionInLine();
		lineStatementList.add(new PrintStatement(line, column, printList));
        printList = null;
    }

    @Override
    public void exitPrintList(PrintListContext ctx) {
        printList.add(expression);
        expression = null;
    }

    @Override
    public void exitCommentStmt(CommentStmtContext ctx) {
        int line = ctx.getStart().getLine();
		int column = ctx.getStart().getCharPositionInLine();
		lineStatementList.add(new RemStatement(line, column));
    }
    
    // Expression parsing:
    
    @Override
    public void exitExpr(ExprContext ctx) {
        expression = parseExpr(ctx);
    }

    private Expression parseExpr(ExprContext ctx) {
    	return parseOrExpr(ctx.orExpr());
    }
    
    private Expression parseOrExpr(OrExprContext ctx) {
    	if (ctx.getChildCount() == 0) {
            return null;
        } else if (ctx.getChildCount() == 1) {
            return parseAndExpr(ctx.andExpr());
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();

            Expression left = parseOrExpr(ctx.orExpr());
            Expression right = parseAndExpr(ctx.andExpr());

            return new OrExpression(line, column, left, right);
        }
	}

	private Expression parseAndExpr(AndExprContext ctx) {
		if (ctx.getChildCount() == 0) {
            return null;
        } else if (ctx.getChildCount() == 1) {
            return parseRelExpr(ctx.relExpr());
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();

            Expression left = parseAndExpr(ctx.andExpr());
            Expression right = parseRelExpr(ctx.relExpr());

            return new AndExpression(line, column, left, right);
        }
	}

	private Expression parseRelExpr(RelExprContext ctx) {
		if (ctx.getChildCount() == 0) {
            return null;
        } else if (ctx.getChildCount() == 1) {
            return parseAddSubExpr(ctx.addSubExpr().get(0));
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();

            Expression left = parseAddSubExpr(ctx.addSubExpr().get(0));
            Expression right = parseAddSubExpr(ctx.addSubExpr().get(1));

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
            } else if (isValid(ctx.NE())) {
                return new NotEqualExpression(line, column, left, right);
            }

            // Return a dummy expression so we can continue parsing
            return new DummyExpression();
        }
	}

	private Expression parseAddSubExpr(AddSubExprContext ctx) {
		if (ctx.getChildCount() == 0) {
            return null;
        } else if (ctx.getChildCount() == 1) {
            return parseTerm(ctx.term());
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();

            Expression left = parseAddSubExpr(ctx.addSubExpr());
            Expression right = parseTerm(ctx.term());

            if (isValid(ctx.PLUS())) {
                return new AddExpression(line, column, left, right);
            } else {
                return new SubExpression(line, column, left, right);
            }
        }
    }

    private Expression parseTerm(TermContext ctx) {
    	if (ctx.getChildCount() == 0) {
            return null;
        } else if (ctx.getChildCount() == 1) {
            return parseFactor(ctx.factor());
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();

            Expression left = parseTerm(ctx.term());
            Expression right = parseFactor(ctx.factor());

            if (isValid(ctx.STAR())) {
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
        } else if (isBoolean(factor)) {
            return new BooleanLiteral(line, column, parseBoolean(factor));
        } else if (isIdent(factor)) {
            return new IdentifierDerefExpression(line, column, parseIdentifier(factor));
        } else if (isSubExpression(factor)) {
            return parseExpr(ctx.expr());
        }

        // Return a dummy expression so we can continue parsing
        return new DummyExpression();
    }

    /**
     * Returns {@code true} if the given node is valid.
     */
    private static boolean isValid(ParseTree node) {
        return node != null && !(node instanceof ErrorNode);
    }

    private boolean isString(ParseTree factor) {
        return factor instanceof StringContext;
    }

    private boolean isInteger(ParseTree factor) {
        return factor instanceof IntegerContext;
    }

    private boolean isBoolean(ParseTree factor) {
        return factor instanceof BoolContext;
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

    private static String parseBoolean(ParseTree bool) {
        BoolContext ctx = (BoolContext) bool;
        return isValid(ctx.FALSE()) ? "0" : "-1";
    }
}
