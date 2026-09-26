/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.col.type;

import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.LogicalExpression;
import se.dykstrom.jcc.common.ast.RelationalExpression;
import se.dykstrom.jcc.common.ast.SubExpression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.ast.UnaryExpression;
import se.dykstrom.jcc.common.compiler.AbstractTypeManager;
import se.dykstrom.jcc.common.types.Void;
import se.dykstrom.jcc.common.types.*;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

/**
 * Manages the types in the COL language.
 *
 * @author Johan Dykstrom
 */
public class ColTypeManager extends AbstractTypeManager {

    public ColTypeManager() {
        typeToName.put(Bool.INSTANCE, "bool");
        typeToName.put(F32.INSTANCE, "f32");
        typeToName.put(F64.INSTANCE, "f64");
        typeToName.put(I32.INSTANCE, "i32");
        typeToName.put(I64.INSTANCE, "i64");
        typeToName.put(Str.INSTANCE, "string");
        typeToName.put(Void.INSTANCE, "void");
        typeToName.forEach((key, value) -> nameToType.put(value, key));
    }

    @Override
    public String getTypeName(final Type type) {
        if (type == null) {
            throw new IllegalArgumentException("null type");
        } else if (type.isUnknown()) {
            return type.getName();
        } else if (typeToName.containsKey(type)) {
            return typeToName.get(type);
        } else if (type instanceof Arr array) {
            return getArrayTypeName(array);
        } else if (type instanceof Fun function) {
            return "function(" + getArgTypeNames(function.getArgTypes()) + ")->" + getTypeName(function.getReturnType());
        } else if (type instanceof NamedType namedType) {
            return namedType.name();
        } else if (type instanceof Opaque opaque) {
            return opaque.name();
        } else if (type instanceof AmbiguousType(Set<Type> types)) {
            return getPossibleTypeNames(types);
        }
        throw new IllegalArgumentException("unknown type: " + type.getClass().getSimpleName());
    }

    private String getArrayTypeName(final Arr array) {
        if (array == Arr.INSTANCE) {
            return "T[]";
        }
        return getTypeName(array.getElementType()) + getBrackets(array.getDimensions());
    }

    private String getBrackets(int dimensions) {
        return IntStream.range(0, dimensions).mapToObj(i -> "[]").collect(joining());
    }

    private String getArgTypeNames(List<Type> argTypes) {
        return argTypes.stream().map(this::getTypeName).collect(joining(", "));
    }

    private String getPossibleTypeNames(final Set<Type> possibleTypes) {
        return possibleTypes.stream()
                            .map(this::getTypeName)
                            .sorted()
                            .collect(joining(" | ", "[", "]"));
    }

    @Override
    protected boolean isKnownAssignableFrom(final Type thisType, final Type thatType) {
        if (thisType == Arr.INSTANCE && thatType instanceof Arr) {
            // All arrays are assignable to an array of the generic array type
            return true;
        }
        if ((thisType instanceof IntegerType thisIt) && (thatType instanceof IntegerType thatIt)) {
            // Smaller integer types can be assigned to larger integer types
            return thisIt.compareTo(thatIt) >= 0;
        }
        if ((thisType instanceof FloatType thisFt) && (thatType instanceof FloatType thatFt)) {
            // Smaller float types can be assigned to larger float types, exactly as for integers.
            return thisFt.compareTo(thatFt) >= 0;
        }
        return thisType.equals(thatType);
    }

    @Override
    public Type getType(final Expression expression) {
        return switch (expression) {
            case RelationalExpression ignored -> Bool.INSTANCE;
            case LogicalExpression ignored -> Bool.INSTANCE;
            case TypedExpression ignored -> super.getType(expression);
            case BinaryExpression be when getType(be.getLeft()) instanceof Opaque opaque -> opaqueType(be, opaque);
            // An opaque type inherits no unary operator
            case UnaryExpression ue when getType(ue.getExpression()) instanceof Opaque -> Unknown.INSTANCE;
            // An expression whose operands this operator does not accept has no type of its own,
            // and AbstractTypeManager says so - BinarySemanticsParser has already reported it
            case null, default -> super.getType(expression);
        };
    }

    /**
     * Returns the type of an arithmetic expression whose left operand is of the given opaque type:
     * the opaque type itself for an inherited {@code +} or {@code -}, and {@link Unknown} for an
     * expression the operand rules reject. The shared numeric promotion must never see an opaque type.
     */
    private Type opaqueType(final BinaryExpression expression, final Opaque opaque) {
        final var underlying = opaque.underlying();
        final var inherited = switch (expression) {
            case AddExpression ignored -> underlying.isNumber() || underlying instanceof Str;
            case SubExpression ignored -> underlying.isNumber();
            default -> false;
        };
        return inherited && opaque.equals(getType(expression.getRight())) ? opaque : Unknown.INSTANCE;
    }
}
