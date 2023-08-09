/*
 * Copyright (C) 2021 Johan Dykstrom
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

package se.dykstrom.jcc.common.storage;

import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a storage location that stores floating point data in a memory location.
 *
 * This class is just a thin wrapper around class {@link MemoryStorageLocation} and is
 * provided just to override the {@link #stores(Type)} method.
 *
 * @author Johan Dykstrom
 */
public class FloatMemoryStorageLocation extends MemoryStorageLocation {

    FloatMemoryStorageLocation(String memory, MemoryManager memoryManager, RegisterManager registerManager) {
        super(memory, memoryManager, registerManager);
    }

    @Override
    public boolean stores(Type type) {
        return type instanceof F64;
    }
}
