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

package se.dykstrom.jcc.common.assembly.directive;

import se.dykstrom.jcc.common.types.*;

import static java.util.Objects.requireNonNull;

/**
 * Represents a single data definition in the data section.
 *
 * @author Johan Dykstrom
 */
public record DataDefinition(Identifier identifier, String value, boolean constant) implements Directive {

    public DataDefinition(final Identifier identifier, final String value, final boolean constant) {
        this.identifier = requireNonNull(identifier);
        this.value = requireNonNull(value);
        this.constant = constant;
    }

    /**
     * Returns the type of the identifier of this data definition.
     */
    public Type type() {
        return identifier.type();
    }

    @Override
    public String toText() {
        return identifier.getMappedName() + " " + toText(type(), constant) + " " + value;
    }

    private String toText(final Type type, final boolean constant) {
        return switch (type) {
            case F64 ignored -> "dq";
            case I64 ignored -> "dq";
            // String constants have data type db, because they are arrays of characters
            // String variables have data type dq, because they contain an address to an array of characters,
            // and addresses are quad-word in 64 bit
            case Str ignored -> constant ? "db" : "dq";
            default -> throw new IllegalArgumentException("unknown type: " + type.getClass().getSimpleName());
        };
    }
}
