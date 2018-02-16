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

package se.dykstrom.jcc.basic.compiler;

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.compiler.AbstractTypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.types.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * Manages the types in the Basic language.
 *
 * @author Johan Dykstrom
 */
class BasicTypeManager extends AbstractTypeManager {

    private static final Map<Type, String> TYPE_NAMES = new HashMap<>();

    static {
        TYPE_NAMES.put(Bool.INSTANCE, "boolean");
        TYPE_NAMES.put(F64.INSTANCE, "float");
        TYPE_NAMES.put(I64.INSTANCE, "integer");
        TYPE_NAMES.put(Str.INSTANCE, "string");
        TYPE_NAMES.put(Unknown.INSTANCE, "<unknown>");
    }
    
    @Override
    public String getTypeName(Type type) {
        if (TYPE_NAMES.containsKey(type)) {
            return TYPE_NAMES.get(type);
        } else if (type instanceof Fun) {
            Fun function = (Fun) type;
            return "function(" + getArgTypeNames(function.getArgTypes()) + ")->" + getTypeName(function.getReturnType());
        }
        throw new IllegalArgumentException("unknown type: " + type.getName());
    }

    private String getArgTypeNames(List<Type> argTypes) {
        return argTypes.stream().map(this::getTypeName).collect(joining(", "));
    }

    @Override
    public Type getType(Expression expression) {
        if (expression instanceof TypedExpression) {
            return ((TypedExpression) expression).getType();
        } else if (expression instanceof BinaryExpression) {
            return binaryExpression((BinaryExpression) expression);
        }
        throw new IllegalArgumentException("unknown expression: " + expression.getClass().getSimpleName());
    }

    @Override
    public boolean isAssignableFrom(Type thisType, Type thatType) {
        if (thatType instanceof Unknown || thatType instanceof Fun) {
            return false;
        }
        return thisType instanceof Unknown || thisType.equals(thatType) || thisType instanceof F64 && thatType instanceof I64;
    }

    private Type binaryExpression(BinaryExpression expression) {
        Type left = getType(expression.getLeft());
        Type right = getType(expression.getRight());

        // If both subexpressions are integers, the result is an integer
        if (left instanceof I64 && right instanceof I64) {
        	return I64.INSTANCE;
        }
        // If both subexpressions are floats, the result is a float
        if (left instanceof F64 && right instanceof F64) {
            return F64.INSTANCE;
        }
        // If one of the subexpressions is a float, the result is a float
        if (left instanceof F64 || right instanceof F64) {
            if (left instanceof I64 || right instanceof I64) {
                return F64.INSTANCE;
            }
        }

        throw new SemanticsException("illegal expression: " + expression);
    }
}
