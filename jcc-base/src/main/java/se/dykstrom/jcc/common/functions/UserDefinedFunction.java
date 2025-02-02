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

import java.util.List;
import java.util.Map;

import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Type;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Represents a user-defined function.
 */
public class UserDefinedFunction extends Function {

    private final List<String> argNames;

    public UserDefinedFunction(final String name,
                               final List<String> argNames,
                               final List<Type> argTypes,
                               final Type returnType) {
        super(name, argTypes, returnType, Map.of());
        this.argNames = requireNonNull(argNames);
    }

    public List<String> argNames() {
        return argNames;
    }

    @Override
    public String getMappedName() {
        return mapName(getName());
    }

    /**
     * Maps the given function name to the name to use in code generation.
     */
    private String mapName(final String functionName) {
        // Flat assembler does not allow # in identifiers
        return "_" + functionName.replace("#", "_hash") +
                getArgTypes().stream().map(this::mapName).collect(joining("_", "_", ""));
    }

    /**
     * Maps the given type to a name to use in code generation. Simple types are mapped to their name.
     * Complex types, like function types, are mapped in a type specific way.
     */
    private String mapName(final Type type) {
        if (type instanceof Fun funType) {
            final var argTypeNames = funType.getArgTypes().stream()
                                            .map(this::mapName)
                                            .collect(joining("$"));
            final var returnTypeName = mapName(funType.getReturnType());
            // Flat assembler does not allow ( and ) in identifiers, so we use L$ and $R instead
            return "FunL$" + argTypeNames + "$RTo" + returnTypeName;
        } else {
            return type.getName();
        }
    }
}
