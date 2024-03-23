/*
 * Copyright (C) 2024 Johan Dykstrom
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

import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a function that is only know as a reference, e.g. as a parameter to another function.
 */
public class ReferenceFunction extends Function {

    public ReferenceFunction(final String name, final List<Type> argTypes, final Type returnType) {
        super(name, false, argTypes, returnType, Map.of());
    }

    @Override
    public String getMappedName() {
        return getName();
    }
}
