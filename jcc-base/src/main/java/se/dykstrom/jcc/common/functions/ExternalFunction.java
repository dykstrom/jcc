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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Represents an external function that cannot be called directly from the compiled program,
 * but is used to implement different library functions.
 *
 * @author Johan Dykstrom
 */
public class ExternalFunction extends Function {

    public ExternalFunction(final String name) {
        super(name, emptyList(), null, emptyMap());
    }

    @Override
    public String getMappedName() {
        throw new UnsupportedOperationException("unsupported for external function: " + getName());
    }

    @Override
    public String mangledName() {
        return getName();
    }

    @Override
    public String toString() {
        return "External function: " + getName();
    }
}
