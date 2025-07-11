/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.col.compiler;

import se.dykstrom.jcc.col.ast.statement.AliasStatement;
import se.dykstrom.jcc.col.ast.statement.FunCallStatement;
import se.dykstrom.jcc.col.ast.statement.ImportStatement;
import se.dykstrom.jcc.col.semantics.statement.*;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.expression.*;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ColSemanticsParser extends AbstractSemanticsParser<ColTypeManager> {

    private final Map<Class<? extends Statement>, StatementSemanticsParser<? extends Statement>> statementComponentsPass1 = new HashMap<>();
    private final Map<Class<? extends Statement>, StatementSemanticsParser<? extends Statement>> statementComponentsPass2 = new HashMap<>();
    private final Map<Class<? extends Expression>, ExpressionSemanticsParser<? extends Expression>> expressionComponents = new HashMap<>();

    public ColSemanticsParser(final CompilationErrorListener errorListener,
                              final SymbolTable symbolTable,
                              final ColTypeManager typeManager) {
        super(errorListener, symbolTable, typeManager);

        // Statements, pass 1
        statementComponentsPass1.put(AliasStatement.class, new AliasPass1SemanticsParser<>(this));
        statementComponentsPass1.put(FunctionDefinitionStatement.class, new FunDefPass1SemanticsParser<>(this));
        statementComponentsPass1.put(ImportStatement.class, new ImportPass1SemanticsParser<>(this));

        // Statements, pass 2
        statementComponentsPass2.put(FunCallStatement.class, new FunCallSemanticsParser<>(this));
        statementComponentsPass2.put(FunctionDefinitionStatement.class, new FunDefPass2SemanticsParser<>(this));

        // Expressions
        expressionComponents.put(AddExpression.class, new AddSemanticsParser<>(this));
        expressionComponents.put(AndExpression.class, new BitwiseBinarySemanticsParser<>(this, "and"));
        expressionComponents.put(DivExpression.class, new DivSemanticsParser<>(this));
        expressionComponents.put(EqualExpression.class, new EqualSemanticsParser<>(this));
        expressionComponents.put(FloatLiteral.class, new FloatSemanticsParser<>(this));
        expressionComponents.put(FunctionCallExpression.class, new FunctionCallSemanticsParser<>(this));
        expressionComponents.put(GreaterExpression.class, new RelationalSemanticsParser<>(this));
        expressionComponents.put(GreaterOrEqualExpression.class, new RelationalSemanticsParser<>(this));
        expressionComponents.put(IdentifierDerefExpression.class, new IdentifierDerefSemanticsParser<>(this));
        expressionComponents.put(IDivExpression.class, new IDivSemanticsParser<>(this));
        expressionComponents.put(IntegerLiteral.class, new IntegerSemanticsParser<>(this));
        expressionComponents.put(LessExpression.class, new RelationalSemanticsParser<>(this));
        expressionComponents.put(LessOrEqualExpression.class, new RelationalSemanticsParser<>(this));
        expressionComponents.put(LogicalAndExpression.class, new LogicalBinarySemanticsParser<>(this, "and"));
        expressionComponents.put(LogicalNotExpression.class, new LogicalNotSemanticsParser<>(this));
        expressionComponents.put(LogicalOrExpression.class, new LogicalBinarySemanticsParser<>(this, "or"));
        expressionComponents.put(LogicalXorExpression.class, new LogicalBinarySemanticsParser<>(this, "xor"));
        expressionComponents.put(ModExpression.class, new ModSemanticsParser<>(this));
        expressionComponents.put(MulExpression.class, new MulSemanticsParser<>(this));
        expressionComponents.put(NegateExpression.class, new NegateSemanticsParser<>(this));
        expressionComponents.put(NotEqualExpression.class, new NotEqualSemanticsParser<>(this));
        expressionComponents.put(NotExpression.class, new BitwiseNotSemanticsParser<>(this));
        expressionComponents.put(OrExpression.class, new BitwiseBinarySemanticsParser<>(this, "or"));
        expressionComponents.put(SubExpression.class, new SubSemanticsParser<>(this));
        expressionComponents.put(XorExpression.class, new BitwiseBinarySemanticsParser<>(this, "xor"));
    }

    @Override
    public AstProgram parse(final AstProgram program) throws SemanticsException {
        final var statementsAfterPass1 = program.getStatements().stream().map(this::pass1).toList();
        final var statementsAfterPass2 = statementsAfterPass1.stream().map(this::statement).toList();
        if (errorListener.hasErrors()) {
            throw new SemanticsException("Semantics error: " + errorListener.getErrors());
        }
        return program.withStatements(statementsAfterPass2);
    }

    private Statement pass1(final Statement statement) {
        return getPass1Component(statement).map(c -> c.parse(statement)).orElse(statement);
    }

    private Statement pass2(final Statement statement) {
        return getPass2Component(statement).map(c -> c.parse(statement)).orElse(statement);
    }

    @Override
    public Statement statement(final Statement statement) {
        return pass2(statement);
    }

    @Override
    public Expression expression(final Expression expression) {
        return getComponent(expression).map(c -> c.parse(expression)).orElse(expression);
    }

    @SuppressWarnings("unchecked")
    private Optional<StatementSemanticsParser<Statement>> getPass1Component(final Statement statement) {
        return Optional.ofNullable((StatementSemanticsParser<Statement>) statementComponentsPass1.get(statement.getClass()));
    }

    @SuppressWarnings("unchecked")
    private Optional<StatementSemanticsParser<Statement>> getPass2Component(final Statement statement) {
        return Optional.ofNullable((StatementSemanticsParser<Statement>) statementComponentsPass2.get(statement.getClass()));
    }

    @SuppressWarnings("unchecked")
    private Optional<ExpressionSemanticsParser<Expression>> getComponent(final Expression expression) {
        final var clazz = (expression != null) ? expression.getClass() : null;
        return Optional.ofNullable((ExpressionSemanticsParser<Expression>) expressionComponents.get(clazz));
    }
}
