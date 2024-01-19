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
import se.dykstrom.jcc.col.semantics.expression.AddSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.DivSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.ExpressionSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.FloatSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.FunctionCallSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.IDivSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.IdentifierDerefSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.IntegerSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.ModSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.MulSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.NegateSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.SubSemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.AliasPass1SemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.FunCallSemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.FunDefPass1SemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.FunDefPass2SemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.ImportPass1SemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.PrintlnSemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.DivExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FloatLiteral;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.IDivExpression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.ModExpression;
import se.dykstrom.jcc.common.ast.MulExpression;
import se.dykstrom.jcc.common.ast.NegateExpression;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.SubExpression;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.symbols.SymbolTable;

public class ColSemanticsParser extends AbstractSemanticsParser<ColTypeManager> {

    private final Map<Class<? extends Statement>, StatementSemanticsParser<? extends Statement>> statementComponentsPass1 = new HashMap<>();
    private final Map<Class<? extends Statement>, StatementSemanticsParser<? extends Statement>> statementComponentsPass2 = new HashMap<>();
    private final Map<Class<? extends Expression>, ExpressionSemanticsParser<? extends Expression>> expressionComponents = new HashMap<>();

    public ColSemanticsParser(final CompilationErrorListener errorListener,
                              final SymbolTable symbolTable,
                              final ColTypeManager typeManager) {
        super(errorListener, symbolTable, typeManager);

        // Statements, pass 1
        statementComponentsPass1.put(AliasStatement.class, new AliasPass1SemanticsParser(this));
        statementComponentsPass1.put(FunctionDefinitionStatement.class, new FunDefPass1SemanticsParser(this));
        statementComponentsPass1.put(ImportStatement.class, new ImportPass1SemanticsParser(this));

        // Statements, pass 2
        statementComponentsPass2.put(FunCallStatement.class, new FunCallSemanticsParser(this));
        statementComponentsPass2.put(FunctionDefinitionStatement.class, new FunDefPass2SemanticsParser(this));
        statementComponentsPass2.put(PrintlnStatement.class, new PrintlnSemanticsParser(this));
        // Expressions
        expressionComponents.put(AddExpression.class, new AddSemanticsParser(this));
        expressionComponents.put(DivExpression.class, new DivSemanticsParser(this));
        expressionComponents.put(FloatLiteral.class, new FloatSemanticsParser(this));
        expressionComponents.put(FunctionCallExpression.class, new FunctionCallSemanticsParser(this));
        expressionComponents.put(IdentifierDerefExpression.class, new IdentifierDerefSemanticsParser(this));
        expressionComponents.put(IDivExpression.class, new IDivSemanticsParser(this));
        expressionComponents.put(IntegerLiteral.class, new IntegerSemanticsParser(this));
        expressionComponents.put(ModExpression.class, new ModSemanticsParser(this));
        expressionComponents.put(MulExpression.class, new MulSemanticsParser(this));
        expressionComponents.put(NegateExpression.class, new NegateSemanticsParser(this));
        expressionComponents.put(SubExpression.class, new SubSemanticsParser(this));
    }

    @Override
    public Program parse(final Program program) throws SemanticsException {
        final var statementsAfterPass1 = program.getStatements().stream().map(this::pass1).toList();
        final var statementsAfterPass2 = statementsAfterPass1.stream().map(this::statement).toList();
        if (errorListener.hasErrors()) {
            throw new SemanticsException("Semantics error");
        }
        return program.withStatements(statementsAfterPass2);
    }

    private Statement pass1(final Statement statement) {
        final var semanticsParserComponent = getSemanticsParserPass1Component(statement);
        if (semanticsParserComponent != null) {
            return semanticsParserComponent.parse(statement);
        } else {
            return statement;
        }
    }

    private Statement pass2(final Statement statement) {
        final var semanticsParserComponent = getSemanticsParserPass2Component(statement);
        if (semanticsParserComponent != null) {
            return semanticsParserComponent.parse(statement);
        } else {
            return statement;
        }
    }

    @Override
    public Statement statement(final Statement statement) {
        return pass2(statement);
    }

    @Override
    public Expression expression(final Expression expression) {
        final var semanticsParserComponent = getSemanticsParserComponent(expression);
        if (semanticsParserComponent != null) {
            return semanticsParserComponent.parse(expression);
        } else {
            return expression;
        }
    }

    @SuppressWarnings("unchecked")
    private StatementSemanticsParser<Statement> getSemanticsParserPass1Component(final Statement statement) {
        return (StatementSemanticsParser<Statement>) statementComponentsPass1.get(statement.getClass());
    }

    @SuppressWarnings("unchecked")
    private StatementSemanticsParser<Statement> getSemanticsParserPass2Component(final Statement statement) {
        return (StatementSemanticsParser<Statement>) statementComponentsPass2.get(statement.getClass());
    }

    @SuppressWarnings("unchecked")
    private ExpressionSemanticsParser<Expression> getSemanticsParserComponent(final Expression expression) {
        final var clazz = (expression != null) ? expression.getClass() : null;
        return (ExpressionSemanticsParser<Expression>) expressionComponents.get(clazz);
    }
}
