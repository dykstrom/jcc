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

import java.util.List;
import java.util.Map;
import java.util.Set;

import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a function that is defined in a dynamic library. These functions can be implemented by a single call 
 * to a single library function, and thus, the call to this function is replaced by a call to the library function.
 *
 * @author Johan Dykstrom
 */
public class LibraryFunction extends Function {

    private final String functionName;

    /**
     * Creates a new library function.
     * 
     * @param name The name of the function.
     * @param returnType The function return type.
     * @param argTypes The types of the formal arguments.
     * @param dependencies The dependencies the function has on libraries and library functions.
     * @param functionName The name of the function to call in the dynamic library.
     */
    public LibraryFunction(String name, Type returnType, List<Type> argTypes, Map<String, Set<String>> dependencies, String functionName) {
        super(name, returnType, argTypes, dependencies);
        this.functionName = functionName;
    }

    /**
     * Returns the name of the function in the dynamic library, that is, the name that has been exported from the dynamic library.
     */
    public String getFunctionName() {
        return functionName;
    }
}
