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

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Manages the types in a programming language.
 *
 * @author Johan Dykstrom
 */
public interface TypeManager {
    /**
     * Returns the name of {@code type} in this language.
     */
    String getTypeName(Type type);

    /**
     * Returns the type of {@code expression}. The type of the expression may be determined
     * by the operator, by the operands, or by a combination of both. For example, relational
     * expressions always have type {@code Bool}. For arithmetic expressions, the type is
     * usually derived from the operand types.
     *
     * @param expression The expression to find the type of.
     * @return The type of the expression.
     */
    Type getType(Expression expression);

    /**
     * Returns the list of types matching the given list of expressions.
     *
     * @param expressions The list of expressions to find the types of.
     * @return The types of the expressions.
     */
    default List<Type> getTypes(List<Expression> expressions) {
        return expressions.stream().map(this::getType).collect(toList());
    }

    /**
     * Returns {@code true} if {@code thisType} is assignable from {@code thatType}.
     * This type is assignable from that type if the types are the same, or if values
     * of that type can somehow be converted to values of this type.
     *
     * @param thisType The type of the identifier to assign a value to.
     * @param thatType The type of the value to assign to the identifier.
     * @return True if this type is assignable from that type.
     */
    boolean isAssignableFrom(Type thisType, Type thatType);

    /**
     * Resolves the function with the given name and arguments. If an exact match can be found,
     * that function is returned. Otherwise, this method tries to cast the actual arguments, and
     * finds a match this way. How types can be cast is language dependent.
     *
     * @param name The name of the function.
     * @param actualArgTypes The types of the actual arguments.
     * @param symbols The symbol table to lookup defined functions in.
     * @return The function found.
     * @throws SemanticsException If no matching function was found, or if several matching functions were found.
     */
    Function resolveFunction(String name, List<Type> actualArgTypes, SymbolTable symbols);
}
