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

package se.dykstrom.jcc.common.assembly.other;

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a single data definition in the data section.
 *
 * @author Johan Dykstrom
 */
public class DataDefinition implements Code {

    private final Identifier identifier;
    private final Type type;
    private final String value;

    public DataDefinition(Identifier identifier, Type type, String value) {
        this.identifier = identifier;
        this.type = type;
        this.value = value;
    }

    @Override
    public String toAsm() {
        return identifier.getMappedName() + " " + toAsm(type) + " " + value;
    }

    private String toAsm(Type type) {
        if (type instanceof I64) {
            return "dq";
        } else if (type instanceof Str) {
            return "db";
        } else {
            throw new IllegalArgumentException("unknown type: " + type.getClass().getSimpleName());
        }
    }
}
