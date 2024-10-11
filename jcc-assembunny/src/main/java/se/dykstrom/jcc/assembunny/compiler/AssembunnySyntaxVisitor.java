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
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.ArrayList;
import java.util.List;

import static se.dykstrom.jcc.assembunny.compiler.AssembunnyUtils.lineNumberLabel;

/**
 * The syntax visitor for the Assembunny language, used to build an AST from an ANTLR parse tree.
 *
 * @author Johan Dykstrom
 */
class AssembunnySyntaxVisitor extends AssembunnyBaseVisitor<Node> {

    private long statementIndex = 0;
    
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
        return new LabelledStatement(lineNumberLabel(statementIndex++), statement);
    }

    @Override
    public Node visitInc(IncContext ctx) {
        final var expression = parseRegisterName(ctx.register());
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new IncStatement(line, column, expression);
    }

    @Override
    public Node visitDec(DecContext ctx) {
        final var expression = parseRegisterName(ctx.register());
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new DecStatement(line, column, expression);
    }

    @Override
    public Node visitCpyFromRegister(CpyFromRegisterContext ctx) {
        final var source = parseRegisterExpression(ctx.register(0));
        final var destination = parseRegisterName(ctx.register(1));
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new CpyStatement(line, column, source, destination);
    }

    @Override
    public Node visitCpyFromInteger(CpyFromIntegerContext ctx) {
        final var source = (IntegerLiteral) ctx.integer().accept(this);
        final var destination = parseRegisterName(ctx.register());
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new CpyStatement(line, column, source, destination);
    }

    @Override
    public Node visitJnzOnRegister(JnzOnRegisterContext ctx) {
        final var expression = parseRegisterExpression(ctx.register());
        final var offset = (IntegerLiteral) ctx.integer().accept(this);
        final var target = lineNumberLabel(statementIndex + offset.asLong());
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new JnzStatement(line, column, expression, target);
    }

    @Override
    public Node visitJnzOnInteger(JnzOnIntegerContext ctx) {
        final var expression = (IntegerLiteral) ctx.integer(0).accept(this);
        final var offset = (IntegerLiteral) ctx.integer(1).accept(this);
        final var target = lineNumberLabel(statementIndex + offset.asLong());
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        return new JnzStatement(line, column, expression, target);
    }

    @Override
    public Node visitOutn(OutnContext ctx) {
        final var expression = parseRegisterExpression(ctx.register());
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
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
        final var line = ctx.getStart().getLine();
        final var column = ctx.getStart().getCharPositionInLine();
        final var name = ctx.getText().substring(0, 1).toUpperCase();
        return new IdentifierExpression(line, column, new Identifier(name, I64.INSTANCE));
    }

    private IdentifierNameExpression parseRegisterName(final RegisterContext ctx) {
        return IdentifierNameExpression.from((IdentifierExpression) ctx.accept(this));
    }

    private IdentifierDerefExpression parseRegisterExpression(final RegisterContext ctx) {
        return IdentifierDerefExpression.from((IdentifierExpression) ctx.accept(this));
    }
}
