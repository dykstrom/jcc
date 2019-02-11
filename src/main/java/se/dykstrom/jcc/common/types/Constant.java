/*
 * Copyright (C) 2018 Johan Dykstrom
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

import java.util.function.Supplier;

/**
 * Represents a constant value. All constants have a type and a value.
 * The value may however be determined at a later stage in the process
 * by getting it from a supplier. Note that this supplier may be called
 * more than once if the constant is referred to more than once. It is
 * critical that the same value is returned on every invocation.
 *
 * @author Johan Dykstrom
 */
public class Constant {

    private final Identifier identifier;
    private final Supplier<String> supplier;

    public Constant(Identifier identifier, String value) {
        this.identifier = identifier;
        this.supplier = () -> value;
    }

    public Constant(Identifier identifier, Supplier<String> supplier) {
        this.identifier = identifier;
        this.supplier = supplier;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public String getValue() {
        return supplier.get();
    }
}
