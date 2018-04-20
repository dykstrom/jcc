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

package se.dykstrom.jcc.common.storage;

import se.dykstrom.jcc.common.assembly.base.FloatRegister;
import se.dykstrom.jcc.common.assembly.base.Register;

import java.util.function.Consumer;

/**
 * An abstract base class for all storage locations. This class provides some methods 
 * that are common to all storage locations.
 *
 * @author Johan Dykstrom
 */
abstract class AbstractStorageLocation implements StorageLocation {

    final RegisterManager registerManager;
    final FloatRegisterManager floatRegisterManager;
    final MemoryManager memoryManager;

    AbstractStorageLocation(RegisterManager registerManager, FloatRegisterManager floatRegisterManager, MemoryManager memoryManager) {
        this.registerManager = registerManager;
        this.floatRegisterManager = floatRegisterManager;
        this.memoryManager = memoryManager;
    }

    /**
     * Allocates temporary memory in the memory manager, executes {@code consumer},
     * and safely de-allocates the memory again.
     */
    protected void withTemporaryMemory(Consumer<String> consumer) {
        String memory = memoryManager.allocate();
        try {
            consumer.accept(memory);
        } finally {
            memoryManager.free(memory);
        }
    }

    /**
     * Allocates a temporary register in the floating point register manager, executes {@code consumer},
     * and safely de-allocates the register again.
     *
     * @param consumer The consumer represents the code to run with the temporary register being allocated.
     */
    protected void withTemporaryFloatRegister(Consumer<FloatRegister> consumer) {
        FloatRegister r = floatRegisterManager.allocateVolatile();
        if (r == null) throw new IllegalStateException("no volatile floating point register available");
        try {
            consumer.accept(r);
        } finally {
            floatRegisterManager.free(r);
        }
    }

    /**
     * Allocates a temporary register in the register manager, executes {@code consumer},
     * and safely de-allocates the register again.
     */
    protected void withTemporaryRegister(Consumer<Register> consumer) {
        Register r = registerManager.allocateVolatile();
        if (r == null) throw new IllegalStateException("no volatile g.p. register available");
        try {
            consumer.accept(r);
        } finally {
            registerManager.free(r);
        }
    }
}
