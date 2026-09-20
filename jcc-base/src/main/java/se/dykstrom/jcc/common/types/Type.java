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

package se.dykstrom.jcc.common.types;

import java.util.stream.Stream;

/**
 * Interface to be implemented by all type classes.
 *
 * @author Johan Dykstrom
 */
public interface Type {
    /**
     * Returns the internal name of this type.
     */
    String getName();

    /**
     * Returns the LLVM IR type name of this type.
     */
    String llvmName();

    /**
     * Returns the default value for this type as a string in LLVM format.
     */
    String llvmDefaultValue();

    /**
     * Returns the default value for this type as a string in assembly format.
     */
    String getDefaultValue();

    /**
     * Returns the printf format specifier for this type.
     */
    String getFormat();

    default boolean isFloat() {
        return false;
    }

    default boolean isInteger() {
        return false;
    }

    default boolean isNumber() {
        return isFloat() || isInteger();
    }

    /**
     * Returns {@code true} for the type of an expression that was already reported as an error.
     * Every check compares types only when both of them are known; see {@link Unknown}.
     */
    default boolean isUnknown() {
        return false;
    }

    /**
     * Returns whether every given type is known, that is, whether none of the expressions they
     * came from has already been reported. A check compares types only when they are all known:
     * a message about the type the compiler fell back to would name a mistake the program does
     * not contain.
     */
    static boolean isKnown(final Type... types) {
        return Stream.of(types).noneMatch(Type::isUnknown);
    }
}
