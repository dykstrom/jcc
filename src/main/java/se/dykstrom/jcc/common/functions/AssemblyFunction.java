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

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a built-in function that is defined in assembly code, and statically linked into the main program.
 *
 * @author Johan Dykstrom
 */
public abstract class AssemblyFunction extends Function {

    /**
     * Creates a new assembly function.
     * 
     * @param name The name of the function.
     * @param returnType The function return type.
     * @param argTypes The types of the formal arguments.
     * @param dependencies The dependencies the function has on libraries and library functions.
     */
    public AssemblyFunction(String name, Type returnType, List<Type> argTypes, Map<String, Set<String>> dependencies) {
        super(name, returnType, argTypes, dependencies);
    }
    
    /**
     * Returns the mapped name of this function, that is, the name that should be used in code generation
     * to avoid any clashes with the backend assembler reserved words.
     */
    public String getMappedName() {
        return "_" + getName();
    }

    /**
     * Returns the assembly code lines that implement this function.
     */
    public abstract List<Code> codes();
}
