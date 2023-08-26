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
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.utils.OptimizationOptions;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateExpression;

/**
 * The default optimizer that performs AST optimizations applicable for all programming languages.
 *
 * @author Johan Dykstrom
 */
public class DefaultAstOptimizer implements AstOptimizer {

    private final AstExpressionOptimizer expressionOptimizer;
    private final SymbolTable symbols;

    public DefaultAstOptimizer(final TypeManager typeManager, final SymbolTable symbolTable) {
        this.expressionOptimizer = new DefaultAstExpressionOptimizer(typeManager);
        this.symbols = requireNonNull(symbolTable);
    }

    @Override
    public Program program(Program program) {
        List<Statement> statements = program.getStatements().stream().map(this::statement).toList();
        return program.withStatements(statements);
    }

    @Override
    public AstExpressionOptimizer expressionOptimizer() {
        return expressionOptimizer;
    }

    /**
     * Optimizes statements.
     */
    public Statement statement(Statement statement) {
        if (statement instanceof AssignStatement assignStatement) {
            return assignStatement(assignStatement);
        } else if (statement instanceof IfStatement ifStatement) {
            return ifStatement(ifStatement);
        } else if (statement instanceof ConstDeclarationStatement constDeclarationStatement) {
            return constDeclarationStatement(constDeclarationStatement);
        } else if (statement instanceof WhileStatement whileStatement) {
            return whileStatement(whileStatement);
        } else {
            return statement;
        }
    }

    private Statement constDeclarationStatement(final ConstDeclarationStatement statement) {
        final var updatedDeclarations = statement.getDeclarations().stream().map(declaration -> {
            final var name = declaration.name();
            final var type = declaration.type();
            final var expression = expression(declaration.expression());
            // Add constant to symbol table
            try {
                final String value = evaluateExpression(expression, symbols, expressionOptimizer, e -> ((LiteralExpression) e).getValue());
                symbols.addConstant(new Identifier(name, type), value);
            } catch (IllegalArgumentException e) {
                // This should never happen since we did the same thing in the semantics parser
                String msg = "cannot evaluate constant '" + name + "' expression: " + expression;
                throw new InvalidValueException(msg, expression.toString());
            }
            // Return updated declaration
            return declaration.withExpression(expression);
        })
        .toList();
        return statement.withDeclarations(updatedDeclarations);
    }

    /**
     * Optimizes assignment statements.
     */
    private Statement assignStatement(AssignStatement statement) {
        if (isLevel1()) {
            Expression expression = expression(statement.getRhsExpression());
            statement = statement.withRhsExpression(expression);

            if (expression instanceof AddExpression addExpression) {
                Expression left = addExpression.getLeft();
                Expression right = addExpression.getRight();

                if ((left instanceof IdentifierDerefExpression) && (right instanceof LiteralExpression)) {
                    return assignStatementAddExpression(statement, left, right);
                } else if ((left instanceof LiteralExpression) && (right instanceof IdentifierDerefExpression)) {
                    return assignStatementAddExpression(statement, right, left);
                }
            } else if (expression instanceof SubExpression subExpression) {
                Expression left = subExpression.getLeft();
                Expression right = subExpression.getRight();

                if ((left instanceof IdentifierDerefExpression) && (right instanceof LiteralExpression)) {
                    return assignStatementSubExpression(statement, left, right);
                } else if ((left instanceof LiteralExpression) && (right instanceof IdentifierDerefExpression)) {
                    return assignStatementSubExpression(statement, right, left);
                }
            }
        }
        return statement;
    }

    private Statement assignStatementAddExpression(AssignStatement statement,
                                                   Expression identifierDerefExpression,
                                                   Expression literalExpression) {
        Identifier identifier = ((IdentifierDerefExpression) identifierDerefExpression).getIdentifier();
        LiteralExpression literal = (LiteralExpression) literalExpression;

        if (statement.getLhsExpression() instanceof IdentifierNameExpression identifierNameExpression) {
            if (identifier.equals(identifierNameExpression.getIdentifier())) {
                Type type = identifier.type();
                if ((type instanceof I64) && literal.getValue().equals("1")) {
                    return IncStatement.from(statement);
                } else if (type instanceof I64) {
                    return AddAssignStatement.from(statement, literal);
                }
            }
        }

        return statement;
    }

    private Statement assignStatementSubExpression(AssignStatement statement,
                                                   Expression identifierDerefExpression,
                                                   Expression literalExpression) {
        Identifier identifier = ((IdentifierDerefExpression) identifierDerefExpression).getIdentifier();
        LiteralExpression literal = ((LiteralExpression) literalExpression);

        if (statement.getLhsExpression() instanceof IdentifierNameExpression identifierNameExpression) {
            if (identifier.equals(identifierNameExpression.getIdentifier())) {
                Type type = identifier.type();
                if ((type instanceof I64) && literal.getValue().equals("1")) {
                    return DecStatement.from(statement);
                } else if (type instanceof I64) {
                    return SubAssignStatement.from(statement, literal);
                }
            }
        }

        return statement;
    }

    /**
     * Optimizes IF statements.
     */
    private Statement ifStatement(IfStatement statement) {
        if (isLevel1()) {
            Expression expression = expression(statement.getExpression());
            statement = statement.withExpression(expression);
        }
        return statement;
    }

    /**
     * Optimizes WHILE statements.
     */
    private Statement whileStatement(WhileStatement statement) {
        if (isLevel1()) {
            Expression expression = expression(statement.getExpression());
            statement = statement.withExpression(expression);
        }
        return statement;
    }

    /**
     * Optimizes expressions.
     */
    protected Expression expression(final Expression expression) {
        if (isLevel1()) {
            return expressionOptimizer.expression(expression, symbols);
        } else {
            return expression;
        }
    }

    /**
     * Returns true if optimization level is at least 1.
     */
    private static boolean isLevel1() {
        return OptimizationOptions.INSTANCE.getLevel() >= 1;
    }
}
