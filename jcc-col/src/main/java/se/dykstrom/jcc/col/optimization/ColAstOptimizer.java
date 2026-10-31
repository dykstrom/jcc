/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.optimization;

import se.dykstrom.jcc.col.ast.expression.BecomeExpression;
import se.dykstrom.jcc.col.ast.statement.FunCallStatement;
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement;
import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.AstExpressionOptimizer;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.optimization.DefaultAstExpressionOptimizer;
import se.dykstrom.jcc.common.optimization.DefaultAstExpressionOptimizer.ExpressionComponent;
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer;
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer.StatementComponent;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The COL AST optimizer. It is a {@link DefaultAstOptimizer} whose expression optimizer does not
 * replace a reference to a constant with the constant's value. The default substitution looks a
 * name up in the global symbol table, so it would replace a function parameter that shadows a
 * constant. A constant reference instead loads from the constant's global, and LLVM folds the
 * load itself. The optimizer also optimizes val initializers, and the arguments of call
 * statements and become expressions.
 */
public class ColAstOptimizer implements AstOptimizer {

    private final DefaultAstOptimizer optimizer;

    public ColAstOptimizer(final TypeManager typeManager, final SymbolTable symbolTable) {
        final var expressionOptimizer = new DefaultAstExpressionOptimizer(
                typeManager,
                List.of(
                        new ExpressionComponent<>(IdentifierDerefExpression.class, ColAstOptimizer::identifierDerefExpression),
                        new ExpressionComponent<>(BecomeExpression.class, ColAstOptimizer::becomeExpression)
                )
        );
        this.optimizer = new DefaultAstOptimizer(
                expressionOptimizer,
                UnaryOperator.identity(),
                List.of(
                        new StatementComponent<>(ValDeclarationStatement.class, ColAstOptimizer::valStatement),
                        new StatementComponent<>(FunCallStatement.class, ColAstOptimizer::funCallStatement)
                ),
                symbolTable
        );
    }

    /**
     * Leaves an identifier reference unchanged, instead of replacing a constant with its value.
     */
    private static Expression identifierDerefExpression(final IdentifierDerefExpression ide,
                                                        final SymbolTable symbols,
                                                        final AstExpressionOptimizer optimizer) {
        return ide;
    }

    /**
     * Optimizes the arguments of the call in a become expression.
     */
    private static Expression becomeExpression(final BecomeExpression become,
                                               final SymbolTable symbols,
                                               final AstExpressionOptimizer optimizer) {
        final var fce = become.functionCall();
        return become.withFunctionCall(fce.withArgs(optimizer.expressions(fce.getArgs(), symbols)));
    }

    /**
     * Optimizes val initializers.
     */
    private static Statement valStatement(final ValDeclarationStatement statement,
                                          final DefaultAstOptimizer optimizer) {
        final var declaration = statement.declaration();
        return statement.withDeclaration(declaration.withExpression(optimizer.expression(declaration.expression())));
    }

    /**
     * Optimizes the arguments of call statements.
     */
    private static Statement funCallStatement(final FunCallStatement statement,
                                              final DefaultAstOptimizer optimizer) {
        final var fce = statement.expression();
        return statement.withExpression(fce.withArgs(optimizer.expressions(fce.getArgs())));
    }

    @Override
    public AstProgram program(final AstProgram program) {
        return optimizer.program(program);
    }

    @Override
    public AstExpressionOptimizer expressionOptimizer() {
        return optimizer.expressionOptimizer();
    }
}
