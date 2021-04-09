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

import static se.dykstrom.jcc.common.assembly.base.FloatRegister.*;
import static se.dykstrom.jcc.common.assembly.base.Register.*;

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

    public final RegisterStorageLocation rax = new RegisterStorageLocation(RAX, registerManager, memoryManager);
    public final RegisterStorageLocation rcx = new RegisterStorageLocation(RCX, registerManager, memoryManager);
    public final RegisterStorageLocation rdx = new RegisterStorageLocation(RDX, registerManager, memoryManager);
    public final RegisterStorageLocation r8  = new RegisterStorageLocation(R8, registerManager, memoryManager);
    public final RegisterStorageLocation r9  = new RegisterStorageLocation(R9, registerManager, memoryManager);

    public final FloatRegisterStorageLocation xmm0 = new FloatRegisterStorageLocation(XMM0, floatRegisterManager, registerManager, memoryManager);
    public final FloatRegisterStorageLocation xmm1 = new FloatRegisterStorageLocation(XMM1, floatRegisterManager, registerManager, memoryManager);
    public final FloatRegisterStorageLocation xmm2 = new FloatRegisterStorageLocation(XMM2, floatRegisterManager, registerManager, memoryManager);
    public final FloatRegisterStorageLocation xmm3 = new FloatRegisterStorageLocation(XMM3, floatRegisterManager, registerManager, memoryManager);

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
