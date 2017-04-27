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
import se.dykstrom.jcc.common.types.Type;

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
     * Returns the type of {@code expression}.
     */
    Type getType(Expression expression);

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
}
