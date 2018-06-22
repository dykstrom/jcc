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

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractTypeManager;
import se.dykstrom.jcc.common.error.AmbiguousException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Manages the types in the Basic language.
 *
 * @author Johan Dykstrom
 */
class BasicTypeManager extends AbstractTypeManager {

    private final Map<Character, Type> identifierTypes = new HashMap<>();

    private static final Map<Type, String> TYPE_NAMES = new HashMap<>();

    static {
        TYPE_NAMES.put(Bool.INSTANCE, "boolean");
        TYPE_NAMES.put(F64.INSTANCE, "double");
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
        if (expression instanceof IdentifierDerefExpression) {
            return derefExpression((IdentifierDerefExpression) expression);
        } else if (expression instanceof TypedExpression) {
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

    private Type derefExpression(IdentifierDerefExpression expression) {
        Type type = expression.getType();
        // If the identifier has no type, find out if there is a type defined for the name
        if (type instanceof Unknown) {
            type = getIdentType(expression.getIdentifier().getName());
        }
        // If the identifier still has no type, use the default type
        if (type instanceof Unknown) {
            type = I64.INSTANCE;
        }
        return type;
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
        // If one of the subexpressions is a float, the result is a float
        if (left instanceof F64 || right instanceof F64) {
            if (left instanceof I64 || right instanceof I64) {
                return F64.INSTANCE;
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

        // Find all functions with the right number of arguments
        List<Function> functions = symbols.getFunctions(name).stream()
                .filter(f -> f.getArgTypes().size() == actualArgTypes.size())
                .collect(Collectors.toList());

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
                map.computeIfAbsent(numberOfCasts, ArrayList::new).add(function);
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
     * Defines identifiers starting with one of {@code letters} to be of the given type.
     *
     * @param letters A set of letters that start identifiers of the given type.
     * @param type The type to associate with the letters.
     */
    public void defineIdentType(Set<Character> letters, Type type) {
        letters.forEach(c -> identifierTypes.put(c, type));
    }

    /**
     * Returns the type of an identifier with the given name, or {@code Unknown} if the type cannot be determined.
     */
    public Type getIdentType(String name) {
        return identifierTypes.getOrDefault(name.charAt(0), Unknown.INSTANCE);
    }
}
