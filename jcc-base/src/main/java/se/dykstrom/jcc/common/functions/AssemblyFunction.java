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

import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;

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
     * @param argTypes The types of the formal arguments.
     * @param returnType The function return type.
     * @param dependencies The dependencies the function has on libraries and library functions.
     */
    protected AssemblyFunction(String name, List<Type> argTypes, Type returnType, Map<String, Set<Function>> dependencies) {
        super(name, argTypes, returnType, dependencies);
    }

    /**
     * Creates a new assembly function.
     *
     * @param name The name of the function.
     * @param argTypes The types of the formal arguments.
     * @param returnType The function return type.
     * @param dependencies The dependencies the function has on libraries and library functions.
     * @param constants The dependencies the function has on global constants.
     */
    protected AssemblyFunction(String name, List<Type> argTypes, Type returnType, Map<String, Set<Function>> dependencies, Set<Constant> constants) {
        super(name, argTypes, returnType, dependencies, constants);
    }

    @Override
    public String getMappedName() {
        return "_" + getName() + "_" + getMappedArgTypes();
    }

    private String getMappedArgTypes() {
        return getArgTypes().stream().map(Type::toString).collect(joining("_"));
    }

    /**
     * Returns the assembly code lines that implement this function.
     */
    public abstract List<Line> lines();
}
