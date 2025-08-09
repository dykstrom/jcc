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
import se.dykstrom.jcc.common.utils.OptimizationOptions;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO;
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
        this(new DefaultAstExpressionOptimizer(typeManager), symbolTable);
    }

    public DefaultAstOptimizer(final AstExpressionOptimizer expressionOptimizer, final SymbolTable symbolTable) {
        this.expressionOptimizer = requireNonNull(expressionOptimizer);
        this.symbols = requireNonNull(symbolTable);
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
     * Optimizes statements.
     */
    protected Statement statement(final Statement statement) {
        if (statement instanceof AssignStatement assignStatement) {
            return assignStatement(assignStatement);
        } else if (statement instanceof FunctionDefinitionStatement functionDefinitionStatement) {
            return functionDefinitionStatement(functionDefinitionStatement);
        } else if (statement instanceof IfStatement ifStatement) {
            return ifStatement(ifStatement);
        } else if (statement instanceof LabelledStatement ls) {
            return labelledStatement(ls);
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
        final Expression expression = expression(statement.getRhsExpression());
        statement = statement.withRhsExpression(expression);

        if (expression instanceof AddExpression addExpression) {
            Expression left = addExpression.getLeft();
            Expression right = addExpression.getRight();

            if ((left instanceof IdentifierDerefExpression ide) && (right instanceof LiteralExpression le)) {
                return assignStatementAddExpression(statement, ide, le);
            } else if ((left instanceof LiteralExpression le) && (right instanceof IdentifierDerefExpression ide)) {
                return assignStatementAddExpression(statement, ide, le);
            }
        } else if (expression instanceof IDivExpression iDivExpression) {
            final var left = iDivExpression.getLeft();
            final var right = iDivExpression.getRight();

            if ((left instanceof IdentifierDerefExpression ide) && (right instanceof LiteralExpression le)) {
                return assignStatementIDivExpression(statement, ide, le);
            }
        } else if (expression instanceof MulExpression mulExpression) {
            final var left = mulExpression.getLeft();
            final var right = mulExpression.getRight();

            if ((left instanceof IdentifierDerefExpression ide) && (right instanceof LiteralExpression le)) {
                return assignStatementMulExpression(statement, ide, le);
            } else if ((left instanceof LiteralExpression le) && (right instanceof IdentifierDerefExpression ide)) {
                return assignStatementMulExpression(statement, ide, le);
            }
        } else if (expression instanceof SubExpression subExpression) {
            Expression left = subExpression.getLeft();
            Expression right = subExpression.getRight();
            if ((left instanceof IdentifierDerefExpression ide) && (right instanceof LiteralExpression le)) {
                return assignStatementSubExpression(statement, ide, le);
            }
        }
        return statement;
    }

    private Statement assignStatementAddExpression(final AssignStatement statement,
                                                   final IdentifierDerefExpression ide,
                                                   final LiteralExpression le) {
        final Identifier identifier = ide.getIdentifier();

        if ((identifier.type() instanceof I64) && (le.getType() instanceof I64)) {
            if (statement.getLhsExpression() instanceof IdentifierNameExpression ine) {
                if (identifier.equals(ine.getIdentifier())) {
                    if (le.getValue().equals("1")) {
                        return IncStatement.from(statement);
                    } else {
                        return AddAssignStatement.from(statement, le);
                    }
                }
            }
        }

        return statement;
    }

    private Statement assignStatementIDivExpression(final AssignStatement statement,
                                                    final IdentifierDerefExpression ide,
                                                    final LiteralExpression le) {
        final Identifier identifier = ide.getIdentifier();

        if ((identifier.type() instanceof I64) && (le.getType() instanceof I64)) {
            if (statement.getLhsExpression() instanceof IdentifierNameExpression ine) {
                if (identifier.equals(ine.getIdentifier())) {
                    return IDivAssignStatement.from(statement, le);
                }
            }
        }

        return statement;
    }

    private Statement assignStatementMulExpression(final AssignStatement statement,
                                                   final IdentifierDerefExpression ide,
                                                   final LiteralExpression le) {
        final Identifier identifier = ide.getIdentifier();

        if ((identifier.type() instanceof I64) && (le.getType() instanceof I64)) {
            if (statement.getLhsExpression() instanceof IdentifierNameExpression ine) {
                if (identifier.equals(ine.getIdentifier())) {
                    return MulAssignStatement.from(statement, le);
                }
            }
        }

        return statement;
    }

    private Statement assignStatementSubExpression(final AssignStatement statement,
                                                   final IdentifierDerefExpression ide,
                                                   final LiteralExpression le) {
        final Identifier identifier = ide.getIdentifier();

        if ((identifier.type() instanceof I64) && (le.getType() instanceof I64)) {
            if (statement.getLhsExpression() instanceof IdentifierNameExpression ine) {
                if (identifier.equals(ine.getIdentifier())) {
                    if (le.getValue().equals("1")) {
                        return DecStatement.from(statement);
                    } else {
                        return SubAssignStatement.from(statement, le);
                    }
                }
            }
        }

        return statement;
    }

    /**
     * Optimizes function definition statements.
     */
    private Statement functionDefinitionStatement(final FunctionDefinitionStatement statement) {
        return statement.withExpression(expression(statement.expression()));
    }

    /**
     * Optimizes IF statements.
     */
    private Statement ifStatement(final IfStatement statement) {
        final var expression = expressionInIfOrWhile(expression(statement.getExpression()));
        final var thenStatements = statement.getThenStatements().stream().map(this::statement).toList();
        final var elseStatements = statement.getElseStatements().stream().map(this::statement).toList();
        return statement.withExpression(expression).withThenStatements(thenStatements).withElseStatements(elseStatements);
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
        final var expression = expressionInIfOrWhile(expression(statement.getExpression()));
        final var statements = statement.getStatements().stream().map(this::statement).toList();
        return statement.withExpression(expression).withStatements(statements);
    }

    /**
     * Optimizes the conditional expression used in if and while statements.
     * For example, the expression 'x != 0' is replaced by just 'x'.
     */
    private Expression expressionInIfOrWhile(final Expression expression) {
        if (expression instanceof NotEqualExpression ne) {
            if (ne.getLeft().equals(ZERO)) {
                return ne.getRight();
            } else if (ne.getRight().equals(ZERO)) {
                return ne.getLeft();
            } else {
                return expression;
            }
        } else {
            return expression;
        }
    }

    /**
     * Optimizes expressions.
     */
    protected Expression expression(final Expression expression) {
        return expressionOptimizer.expression(expression, symbols);
    }

    /**
     * Returns true if optimization level is at least 1.
     */
    private static boolean isLevel1() {
        return OptimizationOptions.INSTANCE.getLevel() >= 1;
    }
}
