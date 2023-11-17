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

package se.dykstrom.jcc.common.utils;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.optimization.AstExpressionOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Contains static utility methods related to expressions.
 *
 * @author Johan Dykstrom
 */
public final class ExpressionUtils {

    private ExpressionUtils() { }

    /**
     * Evaluates the given list of (integer) expressions, and returns a list of {@code Long} values.
     *
     * @param expressions A list of expressions to evaluate.
     * @param symbols     The symbol table, used to look up constant values.
     * @param optimizer   The expression optimizer that is used to evaluate the expressions.
     * @return A list of Long values corresponding to the input expressions.
     */
    public static List<Long> evaluateIntegerExpressions(final List<Expression> expressions,
                                                        final SymbolTable symbols,
                                                        final AstExpressionOptimizer optimizer) {
        return expressions.stream()
                .map(expression -> evaluateExpression(expression, symbols, optimizer, e -> ((IntegerLiteral) e).asLong()))
                .toList();
    }

    /**
     * Evaluates the given constant expression, and returns a value extracted from the result
     * using the extractor function.
     */
    public static <T> T evaluateExpression(final Expression expression,
                                           final SymbolTable symbols,
                                           final AstExpressionOptimizer optimizer,
                                           final Function<Expression, T> extractor) {
        Expression optimizedExpression = optimizer.expression(expression, symbols);
        try {
            return extractor.apply(optimizedExpression);
        } catch (Exception e) {
            throw new IllegalArgumentException("could not evaluate expression: " + expression);
        }
    }

    /**
     * Returns {@code true} if all expressions in the given collection are of type integer.
     */
    public static boolean areAllIntegerExpressions(Collection<Expression> expressions, TypeManager types) {
        return expressions.stream().map(types::getType).allMatch(type -> type instanceof I64);
    }

    /**
     * Returns {@code true} if all expressions in the given collection are constant expressions.
     */
    public static boolean areAllConstantExpressions(final Collection<Expression> expressions,
                                                    final SymbolTable symbolTable) {
        return expressions.stream().allMatch(expression -> isConstantExpression(expression, symbolTable));
    }

    /**
     * Returns {@code true} if the given expression is a constant expression.
     * In this context, a constant expression is either a literal expression,
     * a constant, or an expression that is composed of other constant expressions,
     * that makes it possible to evaluate the expression in compile-time.
     */
    public static boolean isConstantExpression(final Expression expression, final SymbolTable symbolTable) {
        if (expression instanceof LiteralExpression) {
            return true;
        }
        if (expression instanceof UnaryExpression unaryExpression) {
            return isConstantExpression(unaryExpression.getExpression(), symbolTable);
        }
        if (expression instanceof BinaryExpression binaryExpression) {
            return isConstantExpression(binaryExpression.getLeft(), symbolTable) &&
                    isConstantExpression(binaryExpression.getRight(), symbolTable);
        }
        if (expression instanceof IdentifierDerefExpression ide) {
            return symbolTable.isConstant(ide.getIdentifier().name());
        }
        return false;
    }

    /**
     * Checks if the given expression is a division by zero, and throws an exception if that is the case.
     * Returns the expression if everything is ok. This method does not check expressions recursively.
     * Checking recursively must be done by the semantics parser.
     */
    public static BinaryExpression checkDivisionByZero(final BinaryExpression expression) {
        final Expression right = expression.getRight();
        if (right instanceof LiteralExpression literal) {
            final String value = literal.getValue();
            if (isZero(value)) {
                throw new InvalidValueException("division by zero: " + value, value);
            }
        }
        return expression;
    }

    /**
     * Returns {@code true} if the string {@code value} represents a zero value.
     */
    private static boolean isZero(final String value) {
        final Pattern zeroPattern = Pattern.compile("0(\\.0*)?");
        return zeroPattern.matcher(value).matches();
    }
}
