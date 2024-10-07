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

package se.dykstrom.jcc.assembunny.compiler;

import se.dykstrom.jcc.assembunny.ast.*;
import se.dykstrom.jcc.assembunny.compiler.AssembunnyParser.*;
import se.dykstrom.jcc.common.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The syntax visitor for the Assembunny language, used to build an AST from an ANTLR parse tree.
 *
 * @author Johan Dykstrom
 */
class AssembunnySyntaxVisitor extends AssembunnyBaseVisitor<Node> {

    private int statementIndex = 0;
    
    @Override
    public Node visitProgram(ProgramContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        List<Statement> statements = new ArrayList<>();
        for (StatementContext statementCtx : ctx.statement()) {
            statements.add((Statement) statementCtx.accept(this));
        }
        return new AstProgram(line, column, statements);
    }

    @Override
    public Node visitStatement(StatementContext ctx) {
        Statement statement = (Statement) visitChildren(ctx);
        return new LabelledStatement(Integer.toString(statementIndex++), statement);
    }
    
    @Override
    public Node visitInc(IncContext ctx) {
        final var expression = (RegisterExpression) ctx.register().accept(this);
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new IncStatement(line, column, expression);
    }

    @Override
    public Node visitDec(DecContext ctx) {
        final var expression = (RegisterExpression) ctx.register().accept(this);
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new DecStatement(line, column, expression);
    }

    @Override
    public Node visitCpyFromRegister(CpyFromRegisterContext ctx) {
        final var source = (RegisterExpression) ctx.register(0).accept(this);
        final var destination = (RegisterExpression) ctx.register(1).accept(this);
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new CpyStatement(line, column, source, destination);
    }

    @Override
    public Node visitCpyFromInteger(CpyFromIntegerContext ctx) {
        final var source = (IntegerLiteral) ctx.integer().accept(this);
        final var destination = (RegisterExpression) ctx.register().accept(this);
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new CpyStatement(line, column, source, destination);
    }

    @Override
    public Node visitJnzOnRegister(JnzOnRegisterContext ctx) {
        Expression expression = (Expression) ctx.register().accept(this);
        IntegerLiteral offset = (IntegerLiteral) ctx.integer().accept(this);
        String target = Long.toString(statementIndex + offset.asLong());
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new JnzStatement(line, column, expression, target);
    }

    @Override
    public Node visitJnzOnInteger(JnzOnIntegerContext ctx) {
        Expression expression = (Expression) ctx.integer(0).accept(this);
        IntegerLiteral offset = (IntegerLiteral) ctx.integer(1).accept(this);
        String target = Long.toString(statementIndex + offset.asLong());
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new JnzStatement(line, column, expression, target);
    }
    
    @Override
    public Node visitOutn(OutnContext ctx) {
        Expression expression = (Expression) ctx.register().accept(this);
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new OutnStatement(line, column, expression);
    }

    @Override
    public Node visitInteger(IntegerContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new IntegerLiteral(line, column, ctx.getText());
    }

    @Override
    public Node visitRegister(RegisterContext ctx) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return new RegisterExpression(line, column, AssembunnyRegister.from(ctx.getText().charAt(0)));
    }
}
