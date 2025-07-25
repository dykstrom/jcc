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
import se.dykstrom.jcc.common.error.AmbiguousException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.ReferenceFunction;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

import java.util.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Abstract base class for all type managers.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractTypeManager implements TypeManager {

    protected final Map<Type, String> typeToName = new HashMap<>();
    protected final Map<String, Type> nameToType = new HashMap<>();

    /**
     * Returns true if actualType can be promoted to expectedType without any data loss.
     */
    public static boolean canBePromoted(final Type actualType, final Type expectedType) {
        if ((actualType instanceof IntegerType at) && (expectedType instanceof IntegerType et)) {
            return at.bits() < et.bits();
        }
        if ((actualType instanceof FloatType at) && (expectedType instanceof FloatType et)) {
            return at.bits() < et.bits();
        }
        return false;
    }

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
        } else if (expression instanceof UnaryExpression unaryExpression) {
            return getType(unaryExpression.getExpression());
        }
        throw new IllegalArgumentException("unknown expression: " + expression.getClass().getSimpleName());
    }

    private Type binaryExpression(BinaryExpression expression) {
        Type left = getType(expression.getLeft());
        Type right = getType(expression.getRight());

        // If expression is a (legal) floating point division, the result is a floating point value
        if (expression instanceof DivExpression) {
            if ((isInteger(left) || left instanceof F64) && (isInteger(right) || right instanceof F64)) {
                return F64.INSTANCE;
            }
        }
        // If both subexpressions are integers, the result is an integer of the biggest type
        if ((left instanceof IntegerType leftIt) && (right instanceof IntegerType rightIt)) {
        	return promoteInteger(leftIt, rightIt);
        }
        // If both subexpressions are floats, the result is a float of the biggest type
        if ((left instanceof FloatType leftFt) && (right instanceof FloatType rightFt)) {
            return promoteFloat(leftFt, rightFt);
        }
        // If one of the subexpressions is a float, and the other is an integer, the result is a float
        if ((left instanceof F64 || right instanceof F64) && (isInteger(left) || isInteger(right))) {
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

    private Type promoteFloat(final FloatType left, final FloatType right) {
        return (left.bits() >= right.bits()) ? left : right;
    }

    private Type promoteInteger(final IntegerType left, final IntegerType right) {
        return (left.bits() >= right.bits()) ? left : right;
    }

    @SuppressWarnings("java:S6204")
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
                .collect(toList());
        // Possibly add variable of function type with the right number of arguments
        final var optionalFunction = getReferenceFunction(name, actualArgTypes, symbols);
        optionalFunction.ifPresent(functions::add);

        // For each function, count the number of casts required to make it fit the actual args
        Map<Integer, List<Function>> functionsByNumberOfCasts = mapFunctionsByNumberOfCasts(functions, actualArgTypes);

        // If we found no matching functions after casting args
        if (functionsByNumberOfCasts.isEmpty()) {
            throw new UndefinedException(detailMessage("found no match for function call: ", name, actualArgTypes, symbols), name);
        }

        // Find the function that required the fewest number of casts
        int minNumberOfCasts = Collections.min(functionsByNumberOfCasts.keySet());
        List<Function> matchingFunctions = functionsByNumberOfCasts.get(minNumberOfCasts);

        // If there are more than one of these, we cannot choose between them
        if (matchingFunctions.size() > 1) {
            throw new AmbiguousException(detailMessage("ambiguous function call: ", name, actualArgTypes, symbols), name);
        }

        return matchingFunctions.getFirst();
    }

    /**
     * Returns an optional {@link ReferenceFunction} that represents a variable with the given
     * name, and a type that is a function of the same number of arguments as actualArgsTypes.
     */
    private Optional<Function> getReferenceFunction(final String name,
                                                    final List<Type> actualArgTypes,
                                                    final SymbolTable symbols) {
        if (symbols.contains(name) &&
                (symbols.getIdentifier(name).type() instanceof Fun funType) &&
                (funType.getArgTypes().size() == actualArgTypes.size())) {
            return Optional.of(new ReferenceFunction(name, funType.getArgTypes(), funType.getReturnType()));
        }
        return Optional.empty();
    }

    /**
     * Maps all functions in {@code functions} by the number of arguments that must be cast
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

                // Use type inference to find the actual argument type
                if ((actualArgType instanceof AmbiguousType at) && at.contains(formalArgType)) {
                    actualArgType = formalArgType;
                }

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

    @Override
    public List<Expression> resolveArgs(final List<Expression> actualArgs, final List<Type> formalArgTypes) {
        final List<Expression> resolvedArgs = new ArrayList<>();

        for (int i = 0; i < actualArgs.size(); i++) {
            final var actualArg = actualArgs.get(i);
            final var actualArgType = getType(actualArg);
            final var formalArgType = formalArgTypes.get(i);

            if (actualArg instanceof IdentifierDerefExpression ide) {
                if (actualArgType instanceof AmbiguousType at) {
                    if (at.contains(formalArgType)) {
                        final var identifier = ide.getIdentifier();
                        resolvedArgs.add(ide.withIdentifier(identifier.withType(formalArgType)));
                        continue;
                    } else {
                        throw new SemanticsException("cannot resolve arguments, ambiguous argument " + i +
                                                     " cannot be matched to formal argument " + i +
                                                     " of type '" + formalArgType + "'");
                    }
                }
            }

            if (!actualArgType.equals(formalArgType)) {
                if (canBePromoted(actualArgType, formalArgType)) {
                    // At the moment, we can only promote i32 to i64 and f32 to f64
                    if (isInteger(formalArgType)) {
                        resolvedArgs.add(new CastToI64Expression(actualArg.line(), actualArg.column(), actualArg));
                    } else if (isFloat(formalArgType)) {
                        resolvedArgs.add(new CastToF64Expression(actualArg.line(), actualArg.column(), actualArg));
                    }
                    continue;
                } else {
                    throw new SemanticsException("cannot cast actual argument " + i + " of type " + actualArgType +
                                                 " to match formal argument " + i + " of type " + formalArgType);
                }
            }

            resolvedArgs.add(actualArg);
        }

        return resolvedArgs;
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
}
