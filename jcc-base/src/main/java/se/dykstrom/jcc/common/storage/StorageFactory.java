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

package se.dykstrom.jcc.common.storage;

import se.dykstrom.jcc.common.assembly.base.FloatRegister;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.types.*;

/**
 * A factory class for creating temporary storage. The temporary storage returned may be a register,
 * or a local variable (a memory address), or something else.
 *
 * @author Johan Dykstrom
 */
public class StorageFactory {

    private final RegisterManager registerManager = new RegisterManager();
    private final FloatRegisterManager floatRegisterManager = new FloatRegisterManager();
    private final MemoryManager memoryManager = new MemoryManager();

    private final StorageFactory parent;

    public StorageFactory() {
        this.parent = null;
    }

    public StorageFactory(final StorageFactory parent) {
        this.parent = parent;
    }

    public StorageFactory pop() {
        return parent;
    }

    /**
     * Returns a RegisterStorageLocation for the given general purpose register.
     */
    public RegisterStorageLocation get(final Register register) {
        return new RegisterStorageLocation(register, registerManager, memoryManager);
    }

    /**
     * Returns a FloatRegisterStorageLocation for the given floating point register.
     */
    public FloatRegisterStorageLocation get(final FloatRegister register) {
        return new FloatRegisterStorageLocation(register, floatRegisterManager, registerManager, memoryManager);
    }

    /**
     * Allocates volatile storage if possible, otherwise allocates non-volatile storage
     * by calling method {@link #allocateNonVolatile(Type)}.
     *
     * @param type The type of data to store.
     * @return The allocated storage.
     */
    public StorageLocation allocateVolatile(final Type type) {
        if (type instanceof F64) {
            final var register = floatRegisterManager.allocateVolatile();
            if (register != null) {
                return new FloatRegisterStorageLocation(register, floatRegisterManager, registerManager, memoryManager);
            }
        } else {
            final var register = registerManager.allocateVolatile();
            if (register != null) {
                return new RegisterStorageLocation(register, registerManager, memoryManager);
            }
        }
        return allocateNonVolatile(type);
    }

    /**
     * Allocates non-volatile storage, either in the form of a non-volatile register,
     * or in the form of a memory address. The parameter {@code type} specifies what
     * type of data to store, as different registers are used for integers and floats.
     *
     * @param type The type of data to store.
     * @return The allocated storage.
     */
    public StorageLocation allocateNonVolatile(final Type type) {
        if (type instanceof FloatType) {
            FloatRegister register = floatRegisterManager.allocateNonVolatile();
            if (register != null) {
                return new FloatRegisterStorageLocation(register, floatRegisterManager, registerManager, memoryManager);
            } else {
                return new FloatMemoryStorageLocation(memoryManager.allocate(), memoryManager, registerManager);
            }
        } else {
            Register register = registerManager.allocateNonVolatile();
            if (register != null) {
                return new RegisterStorageLocation(register, registerManager, memoryManager);
            } else {
                return new MemoryStorageLocation(memoryManager.allocate(), memoryManager, registerManager);
            }
        }
    }

    /**
     * Allocates non-volatile storage, either in the form of a non-volatile
     * general purpose register, or in the form of a memory address.
     *
     * @return The allocated storage.
     */
    public StorageLocation allocateNonVolatile() {
        return allocateNonVolatile(I64.INSTANCE);
    }

    /**
     * Returns a reference to the register manager.
     */
    public RegisterManager getRegisterManager() {
        return registerManager;
    }

    /**
     * Returns a reference to the floating point register manager.
     */
    public FloatRegisterManager getFloatRegisterManager() {
        return floatRegisterManager;
    }

    /**
     * Returns a reference to the memory manager.
     */
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }
}
