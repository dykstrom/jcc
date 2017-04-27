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
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the types in the Basic language.
 *
 * @author Johan Dykstrom
 */
class BasicTypeManager extends AbstractTypeManager {

    private static final Map<Type, String> TYPE_NAMES = new HashMap<>();

    static {
        TYPE_NAMES.put(I64.INSTANCE, "integer");
        TYPE_NAMES.put(Str.INSTANCE, "string");
    }

    @Override
    public String getTypeName(Type type) {
        if (TYPE_NAMES.containsKey(type)) {
            return TYPE_NAMES.get(type);
        }
        throw new IllegalArgumentException("unknown type: " + type.getName());
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
        return (thisType == Unknown.INSTANCE || thisType == thatType) && (thatType != Unknown.INSTANCE);
    }

    private Type binaryExpression(BinaryExpression expression) {
        Type left = getType(expression.getLeft());
        Type right = getType(expression.getRight());

        if (left instanceof I64 && right instanceof I64) {
            return I64.INSTANCE;
        }

        throw new SemanticsException("illegal expression: " + expression);
    }
}
