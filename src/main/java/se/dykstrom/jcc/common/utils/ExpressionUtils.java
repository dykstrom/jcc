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

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.LiteralExpression;
import se.dykstrom.jcc.common.ast.UnaryExpression;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.I64;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains static utility methods related to expressions.
 *
 * @author Johan Dykstrom
 */
public final class ExpressionUtils {

    private ExpressionUtils() { }

    /**
     * Evaluates the given list of (integer) expressions, and returns a list of {@code Long} values.
     */
    public static List<Long> evaluateConstantIntegerExpressions(List<Expression> expressions) {
        return expressions.stream().map(ExpressionUtils::evaluateConstantIntegerExpression).collect(Collectors.toList());
    }

    /**
     * Evaluates the given (integer) expression, and returns the resulting Long.
     */
    public static Long evaluateConstantIntegerExpression(Expression expression) {
        // TODO: Evaluate!
        return 0L;
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
    public static boolean areAllConstantExpressions(Collection<Expression> expressions) {
        return expressions.stream().allMatch(ExpressionUtils::isConstantExpression);
    }

    /**
     * Returns {@code true} if the given expression is a constant expression.
     * In this context, a constant expression is either a literal expression,
     * or an expression that is composed of other constant expressions, that
     * makes it possible to evaluate the expression in compile-time.
     */
    public static boolean isConstantExpression(Expression expression) {
        if (expression instanceof LiteralExpression) {
            return true;
        }
        if (expression instanceof UnaryExpression) {
            return isConstantExpression(((UnaryExpression) expression).getExpression());
        }
        if (expression instanceof BinaryExpression) {
            return isConstantExpression(((BinaryExpression) expression).getLeft()) && isConstantExpression(((BinaryExpression) expression).getRight());
        }
        return false;
    }
}
