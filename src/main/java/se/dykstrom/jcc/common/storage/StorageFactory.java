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

import se.dykstrom.jcc.common.assembly.base.Register;

/**
 * A factory class for creating temporary storage. The temporary storage returned may be a register,
 * or a local variable, or something else.
 *
 * @author Johan Dykstrom
 */
@SuppressWarnings("unused")
public class StorageFactory {

    private final RegisterManager registerManager = new RegisterManager();

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
     * Allocates storage in the form of a non-volatile register.
     *
     * @return The allocated storage.
     * @throws IllegalStateException If there are no non-volatile registers available.
     */
    public StorageLocation allocateNonVolatile() {
        Register register = registerManager.allocateNonVolatile();
        if (register != null) {
            return new RegisterStorageLocation(register, registerManager);
        }
        throw new IllegalStateException("no non-volatile register available");
    }

    /**
     * Allocates storage in the form of the given register.
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
     * Returns a reference to the register manager.
     */
    public RegisterManager getRegisterManager() {
        return registerManager;
    }
}
