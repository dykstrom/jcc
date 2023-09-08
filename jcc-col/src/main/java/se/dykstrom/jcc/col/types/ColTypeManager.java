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

package se.dykstrom.jcc.col.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.DivExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.NegateExpression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.compiler.AbstractTypeManager;
import se.dykstrom.jcc.common.error.AmbiguousException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Arr;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.NumericType;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.utils.SetUtils;

import static java.util.stream.Collectors.joining;

/**
 * Manages the types in the COL language.
 *
 * @author Johan Dykstrom
 */
public class ColTypeManager extends AbstractTypeManager {

    private static final Map<Type, String> TYPE_TO_NAME = new HashMap<>();

    private final Map<String, Type> nameToType = new HashMap<>();

    static {
        TYPE_TO_NAME.put(F64.INSTANCE, "f64");
        TYPE_TO_NAME.put(I64.INSTANCE, "i64");
        TYPE_TO_NAME.put(Str.INSTANCE, "string");
    }

    public ColTypeManager() {
        TYPE_TO_NAME.forEach((key, value) -> nameToType.put(value, key));
    }
    
    @Override
    public String getTypeName(Type type) {
        if (TYPE_TO_NAME.containsKey(type)) {
            return TYPE_TO_NAME.get(type);
        } else if (type instanceof Arr array) {
            if (array == Arr.INSTANCE) {
                return "T[]";
            } else {
                return getTypeName(array.getElementType()) + getBrackets(array.getDimensions());
            }
        } else if (type instanceof Fun function) {
            return "function(" + getArgTypeNames(function.getArgTypes()) + ")->" + getTypeName(function.getReturnType());
        }
        throw new IllegalArgumentException("unknown type: " + type.getName());
    }

    private String getBrackets(int dimensions) {
        return IntStream.range(0, dimensions).mapToObj(i -> "[]").collect(joining());
    }

    private String getArgTypeNames(List<Type> argTypes) {
        return argTypes.stream().map(this::getTypeName).collect(joining(", "));
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

    @Override
    public boolean isAssignableFrom(final Type thisType, final Type thatType) {
        if (thatType instanceof Fun) {
            return false;
        } else if (thisType == Arr.INSTANCE && thatType instanceof Arr) {
            // All arrays are assignable to an array of the generic array type
            return true;
        }
        return thisType.equals(thatType) || thisType instanceof NumericType && thatType instanceof NumericType;
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
        if (left instanceof F64 || right instanceof F64) {
            if (left instanceof I64 || right instanceof I64) {
                return F64.INSTANCE;
            }
        }
        // If expression is a string concatenation, the result is a string
        if (expression instanceof AddExpression) {
            if (left instanceof Str && right instanceof Str) {
                return Str.INSTANCE;
            }
        }

        throw new SemanticsException("illegal expression: " + expression);
    }

    @Override
    public Function resolveFunction(String name, List<Type> actualArgTypes, SymbolTable symbols) {
        // First, we try to find an exact match
        try {
            return symbols.getFunction(name, actualArgTypes);
        } catch (IllegalArgumentException ignore) {
            // Ignore
        }

        // Find all functions with the right name and number of arguments
        List<Function> functions = symbols.getFunctions(name).stream()
                .filter(f -> f.getArgTypes().size() == actualArgTypes.size())
                .toList();

        // For each function, count the number of casts required to make it fit the actual args
        Map<Integer, List<Function>> functionsByNumberOfCasts = mapFunctionsByNumberOfCasts(functions, actualArgTypes);

        // If we found no matching functions after casting args
        if (functionsByNumberOfCasts.isEmpty()) {
            throw new UndefinedException(detailMessage("found no match for function call: ", name, actualArgTypes, symbols), name);
        }

        // Find the function that required the fewest number of casts
        int minNumberOfCasts = SetUtils.min(functionsByNumberOfCasts.keySet());
        List<Function> matchingFunctions = functionsByNumberOfCasts.get(minNumberOfCasts);

        // If there are more than one of these, we cannot choose between them
        if (matchingFunctions.size() > 1) {
            throw new AmbiguousException(detailMessage("ambiguous function call: ", name, actualArgTypes, symbols), name);
        }

        return matchingFunctions.get(0);
    }

    /**
     * Maps all functions in {@code functions} by the number of arguments the must be cast
     * to make the function match. Functions that are not applicable to the given arguments
     * are not included in the map.
     *
     * @param functions List of functions to map.
     * @param actualArgTypes List of actual argument types that we want to apply the function to.
     * @return A map of number of casts to lists of functions.
     */
    private Map<Integer, List<Function>> mapFunctionsByNumberOfCasts(List<Function> functions, List<Type> actualArgTypes) {
        Map<Integer, List<Function>> map = new HashMap<>();

        // For each function
        for (Function function : functions) {
            boolean canBeApplied = true;
            int numberOfCasts = 0;

            // For each argument
            List<Type> formalArgTypes = function.getArgTypes();
            for (int i = 0; i < formalArgTypes.size(); i++) {
                Type formalArgType = formalArgTypes.get(i);
                Type actualArgType = actualArgTypes.get(i);

                // If the formal and actual argument types are not the same
                if (!formalArgType.equals(actualArgType)) {
                    if (isAssignableFrom(formalArgType, actualArgType)) {
                        // If the actual argument can be cast to the formal argument
                        numberOfCasts++;
                    } else {
                        // If the actual argument cannot be cast, this function cannot be applied to these arguments
                        canBeApplied = false;
                    }
                }
            }

            // If the function can be applied (after casts), add it to the map
            if (canBeApplied) {
                map.computeIfAbsent(numberOfCasts, k -> new ArrayList<>()).add(function);
            }
        }

        return map;
    }

    /**
     * Returns a detail message for an exception, including the invalid function call, and possible
     * function call matches found.
     */
    private String detailMessage(String prefix, String name, List<Type> argTypes, SymbolTable symbols) {
        StringBuilder msg = new StringBuilder(prefix + name + toString(argTypes) + "\npossible matches:");
        symbols.getFunctions(name).stream()
                .map(f -> "\n  -> " + f.getName() + toString(f.getArgTypes()))
                .sorted()
                .forEach(msg::append);
        return msg.toString();
    }

    private String toString(List<Type> argTypes) {
        return argTypes.stream().map(this::getTypeName).collect(joining(", ", "(", ")"));
    }

    /**
     * Returns the type corresponding to the given type name,
     * or an empty optional if the type name is undefined.
     */
    public Optional<Type> getTypeFromName(final String typeName) {
        return Optional.ofNullable(nameToType.get(typeName));
    }

    /**
     * Defines the given type name to refer to the given type.
     */
    public void defineTypeName(final String typeName, final Type type) {
        nameToType.put(typeName, type);
    }
}
