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
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.utils.OptimizationOptions;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * The default optimizer that performs AST optimizations applicable for all programming languages.
 *
 * @author Johan Dykstrom
 */
public class DefaultAstOptimizer implements AstOptimizer {

    private final AstExpressionOptimizer expressionOptimizer;

    public DefaultAstOptimizer(TypeManager typeManager) {
        expressionOptimizer = new DefaultAstExpressionOptimizer(typeManager);
    }

    @Override
    public Program program(Program program) {
        List<Statement> statements = program.getStatements().stream().map(this::statement).collect(toList());
        return program.withStatements(statements);
    }

    /**
     * Optimizes statements.
     */
    public Statement statement(Statement statement) {
        if (statement instanceof AssignStatement) {
            return assignStatement((AssignStatement) statement);
        } else if (statement instanceof IfStatement) {
            return ifStatement((IfStatement) statement);
        } else if (statement instanceof WhileStatement) {
            return whileStatement((WhileStatement) statement);
        } else {
            return statement;
        }
    }

    /**
     * Optimizes assignment statements.
     */
    private Statement assignStatement(AssignStatement statement) {
        if (isLevel1()) {
            Expression expression = expression(statement.getExpression());
            statement = statement.withExpression(expression);

            if (expression instanceof AddExpression) {
                Expression left = ((AddExpression) expression).getLeft();
                Expression right = ((AddExpression) expression).getRight();

                if ((left instanceof IdentifierDerefExpression) && (right instanceof LiteralExpression)) {
                    return assignStatementAddExpression(statement, left, right);
                } else if ((left instanceof LiteralExpression) && (right instanceof IdentifierDerefExpression)) {
                    return assignStatementAddExpression(statement, right, left);
                }
            } else if (expression instanceof SubExpression) {
                Expression left = ((SubExpression) expression).getLeft();
                Expression right = ((SubExpression) expression).getRight();

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

        if (identifier.equals(statement.getIdentifier())) {
            Type type = identifier.getType();
            if ((type instanceof I64) && literal.getValue().equals("1")) {
                return IncStatement.from(statement);
            } else if (type instanceof I64) {
                return AddAssignStatement.from(statement, literal);
            }
        }
        return statement;
    }

    private Statement assignStatementSubExpression(AssignStatement statement,
                                                   Expression identifierDerefExpression,
                                                   Expression literalExpression) {
        Identifier identifier = ((IdentifierDerefExpression) identifierDerefExpression).getIdentifier();
        LiteralExpression literal = ((LiteralExpression) literalExpression);

        if (identifier.equals(statement.getIdentifier())) {
            Type type = identifier.getType();
            if ((type instanceof I64) && literal.getValue().equals("1")) {
                return DecStatement.from(statement);
            } else if (type instanceof I64) {
                return SubAssignStatement.from(statement, literal);
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
    protected Expression expression(Expression expression) {
        if (isLevel1()) {
            return expressionOptimizer.expression(expression);
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
