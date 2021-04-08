/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.common.functions;

import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a built-in function that is defined in a dynamic library. These functions can be implemented by a single call 
 * to an external library function, and thus, the call to this function is replaced by a call to the external function.
 *
 * @author Johan Dykstrom
 */
public class LibraryFunction extends Function {

    private final Function externalFunction;

    /**
     * Creates a new library function.
     *
     * @param name The function name used in the symbol table.
     * @param args The function arguments.
     * @param returnType The function return type.
     * @param libraryFileName The file name of the library.
     * @param externalFunction The external function in the library.
     */
    public LibraryFunction(String name, List<Type> args, Type returnType, String libraryFileName, Function externalFunction) {
        super(name, false, args, returnType, Map.of(libraryFileName, Set.of(externalFunction)));
        this.externalFunction = externalFunction;
    }

    /**
     * Creates a new library function.
     *
     * @param name The function name used in the symbol table.
     * @param isVarargs True if this is a varargs function.
     * @param args The function arguments.
     * @param returnType The function return type.
     * @param libraryFileName The file name of the library.
     * @param externalFunction The external function in the library.
     */
    LibraryFunction(String name, boolean isVarargs, List<Type> args, Type returnType, String libraryFileName, Function externalFunction) {
        super(name, isVarargs, args, returnType, Map.of(libraryFileName, Set.of(externalFunction)));
        this.externalFunction = externalFunction;
    }

    @Override
    public String getMappedName() {
        return mapName(externalFunction.getName());
    }

    /**
     * Maps the given function name to the name to use in code generation.
     */
    public static String mapName(String functionName) {
        return "_" + functionName + "_lib";
    }
}
