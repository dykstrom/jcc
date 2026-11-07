/*
 * Copyright (C) 2019 Johan Dykstrom
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

package se.dykstrom.jcc.common.optimization;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.utils.OptimizationOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * The default optimizer that performs AST optimizations applicable for all programming languages.
 *
 * @author Johan Dykstrom
 */
public class DefaultAstOptimizer implements AstOptimizer {

    private final AstExpressionOptimizer expressionOptimizer;
    private final UnaryOperator<Expression> conditionOptimizer;
    /** The symbol table of the current scope. */
    private SymbolTable symbols;

    /** The statement optimizers, checked in order: the language's own, and then the built-in ones. */
    private final List<StatementComponent<?>> components = new ArrayList<>();

    public DefaultAstOptimizer(final TypeManager typeManager, final SymbolTable symbolTable) {
        this(new DefaultAstExpressionOptimizer(typeManager), symbolTable);
    }

    public DefaultAstOptimizer(final AstExpressionOptimizer expressionOptimizer, final SymbolTable symbolTable) {
        this(expressionOptimizer, UnaryOperator.identity(), List.of(), symbolTable);
    }

    /**
     * Creates an optimizer that applies {@code conditionOptimizer} to the condition of every if
     * and while statement, after the condition has been optimized as an expression, and that
     * optimizes statements with the given components before the built-in ones. A component
     * matches its statement type and every subtype of it. The other constructors leave
     * conditions unchanged, and add no components.
     */
    public DefaultAstOptimizer(final AstExpressionOptimizer expressionOptimizer,
                               final UnaryOperator<Expression> conditionOptimizer,
                               final List<StatementComponent<?>> components,
                               final SymbolTable symbolTable) {
        this.expressionOptimizer = requireNonNull(expressionOptimizer);
        this.conditionOptimizer = requireNonNull(conditionOptimizer);
        this.symbols = requireNonNull(symbolTable);
        this.components.addAll(components);
        addBuiltIn(AssignStatement.class, this::assignStatement);
        addBuiltIn(FunctionDefinitionStatement.class, this::functionDefinitionStatement);
        addBuiltIn(IfStatement.class, this::ifStatement);
        addBuiltIn(LabelledStatement.class, this::labelledStatement);
        addBuiltIn(ConstDeclarationStatement.class, this::constDeclarationStatement);
        addBuiltIn(WhileStatement.class, this::whileStatement);
        addBuiltIn(ForStatement.class, this::forStatement);
        addBuiltIn(BlockStatement.class, this::blockStatement);
    }

    private <T extends Statement> void addBuiltIn(final Class<T> type, final Function<T, Statement> optimizer) {
        components.add(new StatementComponent<>(type, (statement, _) -> optimizer.apply(statement)));
    }

    @Override
    public AstProgram program(final AstProgram program) {
        if (isLevel1()) {
            final var statements = program.getStatements().stream().map(this::statement).toList();
            return program.withStatements(statements);
        } else {
            return program;
        }
    }

    @Override
    public AstExpressionOptimizer expressionOptimizer() {
        return expressionOptimizer;
    }

    /**
     * Optimizes the given statement with the first component that matches it. A statement that
     * no component matches is returned unchanged.
     */
    public Statement statement(final Statement statement) {
        for (final var component : components) {
            if (component.type().isInstance(statement)) {
                return component.apply(statement, this);
            }
        }
        return statement;
    }

    /**
     * Adds the constants to the symbol table, so that references to them can be replaced by their
     * values. Semantic analysis has already replaced each initializer with its value.
     */
    private Statement constDeclarationStatement(final ConstDeclarationStatement statement) {
        statement.getDeclarations().forEach(declaration -> {
            final var value = (LiteralExpression) declaration.expression();
            symbols.addConstant(new Identifier(declaration.name(), declaration.type()), value.getValue());
        });
        return statement;
    }

    /**
     * Optimizes assignment statements.
     */
    private Statement assignStatement(final AssignStatement statement) {
        return statement.withRhsExpression(expression(statement.getRhsExpression()));
    }

    /**
     * Optimizes function definition statements. The parameters are added to a local symbol table,
     * so that a parameter that shadows a constant is not replaced by the constant's value.
     */
    private Statement functionDefinitionStatement(final FunctionDefinitionStatement statement) {
        symbols = new SymbolTable(symbols);
        try {
            statement.declarations().forEach(d -> symbols.addParameter(new Identifier(d.name(), d.type())));
            return statement.withExpression(expression(statement.expression()));
        } finally {
            symbols = symbols.pop();
        }
    }

    /**
     * Optimizes IF statements.
     */
    private Statement ifStatement(final IfStatement statement) {
        final var expression = conditionOptimizer.apply(expression(statement.getExpression()));
        return blockStatement(statement.withExpression(expression));
    }

    /**
     * Optimizes labelled statements.
     */
    private Statement labelledStatement(final LabelledStatement ls) {
        return ls.withStatement(statement(ls.statement()));
    }

    /**
     * Optimizes WHILE statements.
     */
    private Statement whileStatement(final WhileStatement statement) {
        final var expression = conditionOptimizer.apply(expression(statement.getExpression()));
        return blockStatement(statement.withExpression(expression));
    }

    /**
     * Optimizes FOR statements.
     */
    private Statement forStatement(final ForStatement statement) {
        final var step = statement.getStep() == null ? null : expression(statement.getStep());
        return blockStatement(statement
                .withStart(expression(statement.getStart()))
                .withEnd(expression(statement.getEnd()))
                .withStep(step));
    }

    /**
     * Optimizes the nested statements of block statements.
     */
    private Statement blockStatement(final BlockStatement statement) {
        return statement.mapBlocks(statements -> statements.stream().map(this::statement).toList());
    }

    /**
     * Optimizes the given expression.
     */
    public Expression expression(final Expression expression) {
        return expressionOptimizer.expression(expression, symbols);
    }

    /**
     * Optimizes the given expressions.
     */
    public List<Expression> expressions(final List<Expression> expressions) {
        return expressionOptimizer.expressions(expressions, symbols);
    }

    /**
     * A statement optimizer, and the type of statement it optimizes.
     */
    public record StatementComponent<T extends Statement>(Class<T> type, StatementOptimizer<T> optimizer) {

        public StatementComponent {
            requireNonNull(type);
            requireNonNull(optimizer);
        }

        private Statement apply(final Statement statement, final DefaultAstOptimizer astOptimizer) {
            return optimizer.optimize(type.cast(statement), astOptimizer);
        }
    }

    /**
     * Returns true if optimization level is at least 1.
     */
    private static boolean isLevel1() {
        return OptimizationOptions.INSTANCE.getLevel() >= 1;
    }
}
