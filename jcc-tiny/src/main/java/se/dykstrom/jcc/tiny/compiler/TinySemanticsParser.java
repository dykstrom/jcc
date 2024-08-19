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

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.expression.*;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;
import se.dykstrom.jcc.tiny.semantics.expression.IdentifierNameSemanticsParser;
import se.dykstrom.jcc.tiny.semantics.statement.AssignSemanticsParser;
import se.dykstrom.jcc.tiny.semantics.statement.ReadSemanticsParser;
import se.dykstrom.jcc.tiny.semantics.statement.WriteSemanticsParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The semantics parser for the Tiny language.
 *
 * @author Johan Dykstrom
 */
public class TinySemanticsParser extends AbstractSemanticsParser<TypeManager> {

    private final Map<Class<? extends Statement>, StatementSemanticsParser<? extends Statement>> statementComponents = new HashMap<>();
    private final Map<Class<? extends Expression>, ExpressionSemanticsParser<? extends Expression>> expressionComponents = new HashMap<>();

    public TinySemanticsParser(final CompilationErrorListener errorListener,
                               final SymbolTable symbolTable,
                               final TypeManager typeManager) {
        super(errorListener, symbolTable, typeManager);

        // Statements
        statementComponents.put(AssignStatement.class, new AssignSemanticsParser<>(this));
        statementComponents.put(ReadStatement.class, new ReadSemanticsParser<>(this));
        statementComponents.put(WriteStatement.class, new WriteSemanticsParser<>(this));

        // Expressions
        expressionComponents.put(AddExpression.class, new AddSemanticsParser<>(this));
        expressionComponents.put(IdentifierDerefExpression.class, new IdentifierDerefSemanticsParser<>(this));
        expressionComponents.put(IdentifierNameExpression.class, new IdentifierNameSemanticsParser<>(this));
        expressionComponents.put(IntegerLiteral.class, new IntegerSemanticsParser<>(this));
        expressionComponents.put(SubExpression.class, new SubSemanticsParser<>(this));
    }

    @Override
    public AstProgram parse(final AstProgram program) throws SemanticsException {
        final var statements = program.getStatements().stream().map(this::statement).toList();
        if (errorListener.hasErrors()) {
            throw new SemanticsException("Semantics error");
        }
        return program.withStatements(statements);
    }

    @Override
    public Statement statement(final Statement statement) {
        return getComponent(statement).map(c -> c.parse(statement)).orElse(statement);
    }

    @Override
    public Expression expression(final Expression expression) {
        return getComponent(expression).map(c -> c.parse(expression)).orElse(expression);
    }

    @SuppressWarnings("unchecked")
    private Optional<StatementSemanticsParser<Statement>> getComponent(final Statement statement) {
        return Optional.ofNullable((StatementSemanticsParser<Statement>) statementComponents.get(statement.getClass()));
    }

    @SuppressWarnings("unchecked")
    private Optional<ExpressionSemanticsParser<Expression>> getComponent(final Expression expression) {
        final var clazz = (expression != null) ? expression.getClass() : null;
        return Optional.ofNullable((ExpressionSemanticsParser<Expression>) expressionComponents.get(clazz));
    }
}
