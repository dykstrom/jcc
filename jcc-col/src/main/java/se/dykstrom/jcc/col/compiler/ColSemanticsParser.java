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

import java.util.HashMap;
import java.util.Map;

import se.dykstrom.jcc.col.ast.AliasStatement;
import se.dykstrom.jcc.col.ast.FunCallStatement;
import se.dykstrom.jcc.col.ast.ImportStatement;
import se.dykstrom.jcc.col.ast.PrintlnStatement;
import se.dykstrom.jcc.col.semantics.SemanticsParserContext;
import se.dykstrom.jcc.col.semantics.expression.*;
import se.dykstrom.jcc.col.semantics.statement.*;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.symbols.SymbolTable;

public class ColSemanticsParser extends AbstractSemanticsParser {

    private final Map<Class<? extends Statement>, StatementSemanticsParser<? extends Statement>> statementComponents = new HashMap<>();
    private final Map<Class<? extends Expression>, ExpressionSemanticsParser<? extends Expression>> expressionComponents = new HashMap<>();

    public ColSemanticsParser(final CompilationErrorListener errorListener,
                              final SymbolTable symbolTable,
                              final ColTypeManager typeManager) {
        super(errorListener, symbolTable);

        final var context = new SemanticsParserContext(symbolTable, typeManager, this);
        // Statements
        statementComponents.put(AliasStatement.class, new AliasSemanticsParser(context));
        statementComponents.put(FunCallStatement.class, new FunCallSemanticsParser(context));
        statementComponents.put(ImportStatement.class, new ImportSemanticsParser(context));
        statementComponents.put(PrintlnStatement.class, new PrintlnSemanticsParser(context));
        // Expressions
        expressionComponents.put(AddExpression.class, new AddSemanticsParser(context));
        expressionComponents.put(DivExpression.class, new DivSemanticsParser(context));
        expressionComponents.put(FloatLiteral.class, new FloatSemanticsParser(context));
        expressionComponents.put(FunctionCallExpression.class, new FunctionCallSemanticsParser(context));
        expressionComponents.put(IDivExpression.class, new IDivSemanticsParser(context));
        expressionComponents.put(IntegerLiteral.class, new IntegerSemanticsParser(context));
        expressionComponents.put(ModExpression.class, new ModSemanticsParser(context));
        expressionComponents.put(MulExpression.class, new MulSemanticsParser(context));
        expressionComponents.put(NegateExpression.class, new NegateSemanticsParser(context));
        expressionComponents.put(SubExpression.class, new SubSemanticsParser(context));
    }

    @Override
    public Program parse(final Program program) throws SemanticsException {
        final var statements = program.getStatements().stream().map(this::statement).toList();
        if (errorListener.hasErrors()) {
            throw new SemanticsException("Semantics error");
        }
        return program.withStatements(statements);
    }

    @Override
    public Statement statement(final Statement statement) {
        final var semanticsParserComponent = getSemanticsParserComponent(statement);
        if (semanticsParserComponent != null) {
            return semanticsParserComponent.parse(statement);
        } else {
            return statement;
        }
    }

    @Override
    public Expression expression(final Expression expression) {
        final var semanticsParserComponent = getSemanticsParserComponent(expression);
        if (semanticsParserComponent != null) {
            return semanticsParserComponent.parse(expression);
        }
        return expression;
    }

    @SuppressWarnings("unchecked")
    private StatementSemanticsParser<Statement> getSemanticsParserComponent(final Statement statement) {
        return (StatementSemanticsParser<Statement>) statementComponents.get(statement.getClass());
    }

    @SuppressWarnings("unchecked")
    private ExpressionSemanticsParser<Expression> getSemanticsParserComponent(final Expression expression) {
        final var clazz = (expression != null) ? expression.getClass() : null;
        return (ExpressionSemanticsParser<Expression>) expressionComponents.get(clazz);
    }
}
