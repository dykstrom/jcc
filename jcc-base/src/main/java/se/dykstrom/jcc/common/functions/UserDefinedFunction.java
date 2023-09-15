/*
 * Copyright (C) 2019 Johan Dykstrom
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

/**
 * Represents a user-defined function.
 */
public class UserDefinedFunction extends Function {

    public UserDefinedFunction(final String name, final List<Type> argTypes, final Type returnType) {
        super(name, false, argTypes, returnType, Map.of());
    }

    @Override
    public String getMappedName() {
        return mapName(getName());
    }

    /**
     * Maps the given function name to the name to use in code generation.
     */
    public static String mapName(final String functionName) {
        return "_" + functionName;
    }
}
