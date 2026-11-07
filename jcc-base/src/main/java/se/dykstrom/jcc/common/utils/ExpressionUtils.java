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
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Contains static utility methods related to expressions.
 *
 * @author Johan Dykstrom
 */
public final class ExpressionUtils {

    private ExpressionUtils() { }

    /**
     * Returns {@code true} if all expressions in the given collection are of type integer.
     */
    public static boolean areAllIntegerExpressions(Collection<Expression> expressions, TypeManager types) {
        return expressions.stream().map(types::getType).allMatch(type -> type instanceof I64);
    }

    /**
     * Checks if the given expression is a division by zero, and throws an exception if that is the case.
     * Returns the expression if everything is ok. This method does not check expressions recursively.
     * Checking recursively must be done by the semantics parser.
     */
    public static BinaryExpression checkDivisionByZero(final BinaryExpression expression) {
        if (isZeroDivisor(expression)) {
            final String value = ((LiteralExpression) expression.getRight()).getValue();
            throw new InvalidValueException("division by zero: " + value, value);
        }
        return expression;
    }

    /**
     * Returns {@code true} if the right hand side of the given expression is a literal zero. This
     * method does not check expressions recursively.
     */
    public static boolean isZeroDivisor(final BinaryExpression expression) {
        return expression.getRight() instanceof LiteralExpression literal && isZero(literal.getValue());
    }

    /**
     * Returns {@code true} if the string {@code value} represents a zero value.
     */
    private static boolean isZero(final String value) {
        final Pattern zeroPattern = Pattern.compile("0(\\.0*)?");
        return zeroPattern.matcher(value).matches();
    }

    /**
     * Returns {@code true} if the given expression does not contain any function calls.
     */
    public static boolean hasNoFunctionCall(final Expression expression) {
        if (expression instanceof FunctionCallExpression) {
            return false;
        }
        if (expression instanceof UnaryExpression unaryExpression) {
            return hasNoFunctionCall(unaryExpression.getExpression());
        }
        if (expression instanceof BinaryExpression binaryExpression) {
            return hasNoFunctionCall(binaryExpression.getLeft()) && hasNoFunctionCall(binaryExpression.getRight());
        }
        if (expression instanceof ArrayAccessExpression arrayExpression) {
            return arrayExpression.getSubscripts().stream().allMatch(ExpressionUtils::hasNoFunctionCall);
        }
        return true;
    }

    /**
     * Returns {@code true} if the given expression does not contain any function calls
     * to a user-defined function.
     */
    public static boolean hasNoUdfFunctionCall(final Expression expression, final SymbolTable symbolTable) {
        if (expression instanceof FunctionCallExpression fce) {
            final var identifier = fce.getIdentifier();
            final var function = symbolTable.getFunction(identifier);
            if (function instanceof UserDefinedFunction) {
                return false;
            }
            return fce.getArgs().stream().allMatch(e -> hasNoUdfFunctionCall(e, symbolTable));
        }
        if (expression instanceof UnaryExpression unaryExpression) {
            return hasNoUdfFunctionCall(unaryExpression.getExpression(), symbolTable);
        }
        if (expression instanceof BinaryExpression binaryExpression) {
            return hasNoUdfFunctionCall(binaryExpression.getLeft(), symbolTable) &&
                   hasNoUdfFunctionCall(binaryExpression.getRight(), symbolTable);
        }
        if (expression instanceof ArrayAccessExpression arrayExpression) {
            return arrayExpression.getSubscripts().stream().allMatch(e -> hasNoUdfFunctionCall(e, symbolTable));
        }
        return true;
    }
}
