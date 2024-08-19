/*
 * Copyright (C) 2016 Johan Dykstrom
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

package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base class for all type managers.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractTypeManager implements TypeManager {

    protected final Map<Type, String> typeToName = new HashMap<>();
    protected final Map<String, Type> nameToType = new HashMap<>();

    @Override
    public Optional<Type> getTypeFromName(final String typeName) {
        return Optional.ofNullable(nameToType.get(typeName));
    }

    @Override
    public void defineTypeName(final String typeName, final Type type) {
        nameToType.put(typeName, type);
    }

    @Override
    public Type getType(Expression expression) {
        if (expression instanceof TypedExpression typedExpression) {
            return typedExpression.getType();
        } else if (expression instanceof BinaryExpression binaryExpression) {
            return binaryExpression(binaryExpression);
        } else if (expression instanceof NegateExpression negateExpression) {
            return getType(negateExpression.getExpression());
        }
        throw new IllegalArgumentException("unknown expression: " + expression.getClass().getSimpleName());
    }

    private Type binaryExpression(BinaryExpression expression) {
        Type left = getType(expression.getLeft());
        Type right = getType(expression.getRight());

        // If expression is a (legal) floating point division, the result is a floating point value
        if (expression instanceof DivExpression) {
            if ((left instanceof I64 || left instanceof F64) && (right instanceof I64 || right instanceof F64)) {
                return F64.INSTANCE;
            }
        }
        // If both subexpressions are integers, the result is an integer
        if (left instanceof I64 && right instanceof I64) {
        	return I64.INSTANCE;
        }
        // If both subexpressions are floats, the result is a float
        if (left instanceof F64 && right instanceof F64) {
            return F64.INSTANCE;
        }
        // If one of the subexpressions is a float, and the other is an integer, the result is a float
        if ((left instanceof F64 || right instanceof F64) && (left instanceof I64 || right instanceof I64)) {
            return F64.INSTANCE;
        }
        // If expression is a string concatenation, the result is a string
        if (expression instanceof AddExpression) {
            if (left instanceof Str && right instanceof Str) {
                return Str.INSTANCE;
            }
        }

        throw new SemanticsException("illegal expression: " + expression);
    }
}
