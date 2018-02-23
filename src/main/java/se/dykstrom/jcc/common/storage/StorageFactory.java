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
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Type;

/**
 * A factory class for creating temporary storage. The temporary storage returned may be a register,
 * or a local variable (a memory address), or something else.
 *
 * @author Johan Dykstrom
 */
@SuppressWarnings("unused")
public class StorageFactory {

    private final RegisterManager registerManager = new RegisterManager();
    private final FloatRegisterManager floatRegisterManager = new FloatRegisterManager();
    private final MemoryManager memoryManager = new MemoryManager();

    /**
     * Allocates storage in the form of a volatile register.
     *
     * @return The allocated storage.
     * @throws IllegalStateException If there are no volatile registers available.
     */
    public StorageLocation allocateVolatile() {
        Register register = registerManager.allocateVolatile();
        if (register != null) {
            return new RegisterStorageLocation(register, registerManager);
        }
        throw new IllegalStateException("no volatile register available");
    }

    /**
     * Allocates non-volatile storage, either in the form of a non-volatile register,
     * or in the form of a memory address. The parameter {@code type} specifies what
     * type of data to store, as different registers are used for integers and floats.
     *
     * @param type The type of data to store.
     * @return The allocated storage.
     */
    public StorageLocation allocateNonVolatile(Type type) {
        if (type instanceof F64) {
            FloatRegister register = floatRegisterManager.allocate();
            if (register != null) {
                return new FloatRegisterStorageLocation(register, floatRegisterManager, registerManager, memoryManager);
            } else {
                // TODO: We will probably need a FloatMemoryStorageLocation here.
                return new MemoryStorageLocation(memoryManager.allocate(), memoryManager, registerManager);
            }
        } else {
            Register register = registerManager.allocateNonVolatile();
            if (register != null) {
                return new RegisterStorageLocation(register, registerManager);
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
     * Allocates storage in the form of the given general purpose register.
     *
     * @param register The register to allocate.
     * @return The allocated storage.
     * @throws IllegalStateException If it was not possible to allocate the register.
     */
    public StorageLocation allocate(Register register) {
        Register allocatedRegister = registerManager.allocate(register);
        if (allocatedRegister != null) {
            return new RegisterStorageLocation(allocatedRegister, registerManager);
        }
        throw new IllegalStateException("register " + register.toString() + " not available");
    }

    /**
     * Allocates storage in the form of the given floating point register.
     *
     * @param register The register to allocate.
     * @return The allocated storage.
     * @throws IllegalStateException If it was not possible to allocate the register.
     */
    public StorageLocation allocate(FloatRegister register) {
        FloatRegister allocatedRegister = floatRegisterManager.allocate(register);
        if (allocatedRegister != null) {
            return new FloatRegisterStorageLocation(allocatedRegister, floatRegisterManager, registerManager, memoryManager);
        }
        throw new IllegalStateException("register " + register.toString() + " not available");
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
