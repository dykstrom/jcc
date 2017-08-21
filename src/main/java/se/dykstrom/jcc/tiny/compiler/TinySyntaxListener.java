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

package se.dykstrom.jcc.tiny.compiler;

import org.antlr.v4.runtime.tree.ParseTree;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;
import se.dykstrom.jcc.tiny.compiler.TinyParser.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The syntax listener for the Tiny language, that listens to events from class TinyParser.
 *
 * @author Johan Dykstrom
 */
class TinySyntaxListener extends TinyBaseListener {

    private Program program;
    private List<Statement> statementList;
    private List<Expression> expressionList;
    private List<Identifier> identifierList;
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
    public void enterRead_stmt(Read_stmtContext ctx) {
        identifierList = new ArrayList<>();
    }

    @Override
    public void exitRead_stmt(Read_stmtContext ctx) {
        int line = ctx.getStart().getLine();
		int column = ctx.getStart().getCharPositionInLine();
		statementList.add(new ReadStatement(line, column, identifierList));
        identifierList = null;
    }

    @Override
    public void enterWrite_stmt(Write_stmtContext ctx) {
        expressionList = new ArrayList<>();
    }

    @Override
    public void exitWrite_stmt(Write_stmtContext ctx) {
        int line = ctx.getStart().getLine();
		int column = ctx.getStart().getCharPositionInLine();
		statementList.add(new WriteStatement(line, column, expressionList));
        expressionList = null;
    }

    @Override
    public void exitAssign_stmt(Assign_stmtContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        Identifier identifier = parseIdentifier(ctx.ident());
        statementList.add(new AssignStatement(line, column, identifier, expression));
    }

    @Override
    public void exitId_list(Id_listContext ctx) {
        identifierList.add(parseIdentifier(ctx.ident()));
    }

    @Override
    public void exitExpr_list(Expr_listContext ctx) {
        expressionList.add(expression);
        expression = null;
    }

    @Override
    public void exitExpr(ExprContext ctx) {
        expression = parseExpr(ctx);
    }

    private Expression parseExpr(ExprContext ctx) {
        if (ctx.getChildCount() == 1) {
            return parseFactor(ctx.factor());
        } else {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();

			Expression left = parseExpr(ctx.expr());
			Expression right = parseFactor(ctx.factor());

            if (isPlus(ctx.op())) {
                return new AddExpression(line, column, left, right);
            } else {
                return new SubExpression(line, column, left, right);
            }
        }
    }

    private Expression parseFactor(FactorContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();

        ParseTree factor = ctx.getChild(0);
        if (isInteger(factor)) {
            return new IntegerLiteral(line, column, parseInteger(factor));
        } else if (isIdent(factor)) {
            return new IdentifierDerefExpression(line, column, parseIdentifier(factor));
        }

        // Return a dummy expression so we can continue parsing
        return new DummyExpression();
    }

    private boolean isPlus(ParseTree operation) {
        return operation.getText().equals("+");
    }

    private boolean isInteger(ParseTree factor) {
        return factor instanceof IntegerContext;
    }

    private boolean isIdent(ParseTree factor) {
        return factor instanceof IdentContext;
    }

    private Identifier parseIdentifier(ParseTree identifier) {
        return new Identifier(identifier.getText(), I64.INSTANCE);
    }

    private String parseInteger(ParseTree integer) {
        return integer.getText();
    }
}
