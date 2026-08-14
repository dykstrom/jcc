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

import se.dykstrom.jcc.col.ast.expression.AnonymousFunctionExpression;
import se.dykstrom.jcc.col.ast.expression.BecomeExpression;
import se.dykstrom.jcc.col.ast.expression.ChainedRelationalExpression;
import se.dykstrom.jcc.col.ast.expression.MalformedFloatLiteral;
import se.dykstrom.jcc.col.ast.expression.MalformedStringLiteral;
import se.dykstrom.jcc.col.ast.statement.AliasStatement;
import se.dykstrom.jcc.col.ast.statement.FunCallStatement;
import se.dykstrom.jcc.col.ast.statement.ImportStatement;
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement;
import se.dykstrom.jcc.col.semantics.BecomeSemanticsUtils;
import se.dykstrom.jcc.col.semantics.LambdaLifter;
import se.dykstrom.jcc.col.semantics.expression.AnonymousFunctionSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.BecomeSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.ChainedRelationalSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.MalformedFloatSemanticsParser;
import se.dykstrom.jcc.col.semantics.expression.MalformedStringSemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.AliasPass1SemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.FunCallSemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.FunDefPass1SemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.FunDefPass2SemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.ImportPass1SemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.ValSemanticsParser;
import se.dykstrom.jcc.col.semantics.statement.WhileSemanticsParser;
import se.dykstrom.jcc.col.type.ColTypeManager;
import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.AndExpression;
import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.ast.DivExpression;
import se.dykstrom.jcc.common.ast.EqualExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FloatLiteral;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.GreaterExpression;
import se.dykstrom.jcc.common.ast.GreaterOrEqualExpression;
import se.dykstrom.jcc.common.ast.IDivExpression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.ast.IfExpression;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.LessExpression;
import se.dykstrom.jcc.common.ast.LessOrEqualExpression;
import se.dykstrom.jcc.common.ast.LogicalAndExpression;
import se.dykstrom.jcc.common.ast.LogicalNotExpression;
import se.dykstrom.jcc.common.ast.LogicalOrExpression;
import se.dykstrom.jcc.common.ast.LogicalXorExpression;
import se.dykstrom.jcc.common.ast.ModExpression;
import se.dykstrom.jcc.common.ast.MulExpression;
import se.dykstrom.jcc.common.ast.NegateExpression;
import se.dykstrom.jcc.common.ast.NotEqualExpression;
import se.dykstrom.jcc.common.ast.NotExpression;
import se.dykstrom.jcc.common.ast.OrExpression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.SubExpression;
import se.dykstrom.jcc.common.ast.WhileStatement;
import se.dykstrom.jcc.common.ast.XorExpression;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.semantics.expression.BinarySemanticsParser;
import se.dykstrom.jcc.common.semantics.expression.ExpressionSemanticsParser;
import se.dykstrom.jcc.common.semantics.expression.FloatSemanticsParser;
import se.dykstrom.jcc.common.semantics.expression.FunctionCallSemanticsParser;
import se.dykstrom.jcc.common.semantics.expression.IdentifierDerefSemanticsParser;
import se.dykstrom.jcc.common.semantics.expression.IfSemanticsParser;
import se.dykstrom.jcc.common.semantics.expression.IntegerSemanticsParser;
import se.dykstrom.jcc.common.semantics.expression.UnarySemanticsParser;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static se.dykstrom.jcc.col.semantics.expression.ColOperandTypeRules.NOT_STRINGS;
import static se.dykstrom.jcc.common.semantics.expression.OperandTypeRule.BOOLEAN;
import static se.dykstrom.jcc.common.semantics.expression.OperandTypeRule.FLOAT;
import static se.dykstrom.jcc.common.semantics.expression.OperandTypeRule.INTEGER;
import static se.dykstrom.jcc.common.semantics.expression.OperandTypeRule.NUMERIC;
import static se.dykstrom.jcc.common.semantics.expression.OperandTypeRule.STRINGS;
import static se.dykstrom.jcc.common.semantics.expression.OperandValueRule.NON_ZERO_DIVISOR;
import static se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE;

public class ColSemanticsParser extends AbstractSemanticsParser<ColTypeManager> {

    private final Map<Class<? extends Statement>, StatementSemanticsParser<? extends Statement>> statementComponentsPass1 = new HashMap<>();
    private final Map<Class<? extends Statement>, StatementSemanticsParser<? extends Statement>> statementComponentsPass2 = new HashMap<>();
    private final Map<Class<? extends Expression>, ExpressionSemanticsParser<? extends Expression>> expressionComponents = new HashMap<>();

    /** Tracks variable declaration and usage for unused variable warnings. */
    private final VariableUsageTracker usageTracker = new VariableUsageTracker();

    /** Collects the top-level functions synthesized from anonymous functions. */
    private final LambdaLifter lambdaLifter = new LambdaLifter();

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
        statementComponentsPass2.put(FunctionDefinitionStatement.class, new FunDefPass2SemanticsParser<>(this, usageTracker));
        statementComponentsPass2.put(ValDeclarationStatement.class, new ValSemanticsParser<>(this, usageTracker));
        statementComponentsPass2.put(WhileStatement.class, new WhileSemanticsParser<>(this));

        // Expressions
        expressionComponents.put(AddExpression.class, new BinarySemanticsParser<>(this, "add", NUMERIC.or(STRINGS)));
        expressionComponents.put(AnonymousFunctionExpression.class, new AnonymousFunctionSemanticsParser<>(this, usageTracker, lambdaLifter));
        expressionComponents.put(AndExpression.class, new BinarySemanticsParser<>(this, "bitwise-and", INTEGER));
        expressionComponents.put(BecomeExpression.class, new BecomeSemanticsParser<>(this));
        expressionComponents.put(ChainedRelationalExpression.class, new ChainedRelationalSemanticsParser<>(this));
        expressionComponents.put(MalformedFloatLiteral.class, new MalformedFloatSemanticsParser<>(this));
        expressionComponents.put(MalformedStringLiteral.class, new MalformedStringSemanticsParser<>(this));
        expressionComponents.put(DivExpression.class, new BinarySemanticsParser<>(this, "divide", NON_ZERO_DIVISOR, FLOAT));
        expressionComponents.put(EqualExpression.class, new BinarySemanticsParser<>(this, "compare"));
        expressionComponents.put(FloatLiteral.class, new FloatSemanticsParser<>(this));
        expressionComponents.put(FunctionCallExpression.class, new FunctionCallSemanticsParser<>(this));
        expressionComponents.put(GreaterExpression.class, new BinarySemanticsParser<>(this, "compare", NOT_STRINGS, NUMERIC));
        expressionComponents.put(GreaterOrEqualExpression.class, new BinarySemanticsParser<>(this, "compare", NOT_STRINGS, NUMERIC));
        expressionComponents.put(IdentifierDerefExpression.class, new IdentifierDerefSemanticsParser<>(this, usageTracker));
        expressionComponents.put(IDivExpression.class, new BinarySemanticsParser<>(this, "divide", NON_ZERO_DIVISOR, INTEGER));
        expressionComponents.put(IfExpression.class, new IfSemanticsParser<>(this));
        expressionComponents.put(IntegerLiteral.class, new IntegerSemanticsParser<>(this));
        expressionComponents.put(LessExpression.class, new BinarySemanticsParser<>(this, "compare", NOT_STRINGS, NUMERIC));
        expressionComponents.put(LessOrEqualExpression.class, new BinarySemanticsParser<>(this, "compare", NOT_STRINGS, NUMERIC));
        expressionComponents.put(LogicalAndExpression.class, new BinarySemanticsParser<>(this, "logical-and", BOOLEAN));
        expressionComponents.put(LogicalNotExpression.class, new UnarySemanticsParser<>(this, "logical-not", BOOLEAN));
        expressionComponents.put(LogicalOrExpression.class, new BinarySemanticsParser<>(this, "logical-or", BOOLEAN));
        expressionComponents.put(LogicalXorExpression.class, new BinarySemanticsParser<>(this, "logical-xor", BOOLEAN));
        expressionComponents.put(ModExpression.class, new BinarySemanticsParser<>(this, "mod", NON_ZERO_DIVISOR, INTEGER));
        expressionComponents.put(MulExpression.class, new BinarySemanticsParser<>(this, "multiply", NUMERIC));
        expressionComponents.put(NegateExpression.class, new UnarySemanticsParser<>(this, "negate", NUMERIC));
        expressionComponents.put(NotEqualExpression.class, new BinarySemanticsParser<>(this, "compare"));
        expressionComponents.put(NotExpression.class, new UnarySemanticsParser<>(this, "bitwise-not", INTEGER));
        expressionComponents.put(OrExpression.class, new BinarySemanticsParser<>(this, "bitwise-or", INTEGER));
        expressionComponents.put(SubExpression.class, new BinarySemanticsParser<>(this, "subtract", NUMERIC));
        expressionComponents.put(XorExpression.class, new BinarySemanticsParser<>(this, "bitwise-xor", INTEGER));
    }

    @Override
    public AstProgram parse(final AstProgram program) throws SemanticsException {
        lambdaLifter.clear();
        final var statementsAfterPass1 = program.getStatements().stream().map(this::pass1).toList();
        // Pass 2 runs in a top-level scope so that vals are invisible to function bodies
        // (function scopes are built from the global symbol table) and are discarded
        // before code generation, where they become locals of the synthesized main function
        final var statementsAfterPass2 = withLocalSymbolTable(
                () -> statementsAfterPass1.stream().map(this::statement).toList()
        );
        usageTracker.check((n, m) -> reportWarning(n, m, UNUSED_VARIABLE));
        // Anonymous functions have been replaced by references to lifted functions, so a become in
        // one of their bodies is now inside a function body, and no longer seen by this check
        BecomeSemanticsUtils.checkNoTopLevelBecome(statementsAfterPass2, (n, m) -> reportError(n, m, new SemanticsException(m)));
        if (errorListener.hasErrors()) {
            throw new SemanticsException("Semantics error: " + errorListener.getErrors());
        }
        // Prepend the functions lifted from anonymous functions: code generation discovers the
        // functions to emit among the top-level statements, and the FASM backend defines them as
        // it walks the list, so a lifted function must come before the statement referencing it.
        // Function definitions emit no code in place, so this does not disturb execution order.
        final var statements = new ArrayList<Statement>(lambdaLifter.functions());
        statements.addAll(statementsAfterPass2);
        return program.withStatements(statements);
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
