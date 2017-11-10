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

import se.dykstrom.jcc.common.assembly.base.Register;

/**
 * An abstract base class for all storage locations. This class provides some methods 
 * that are common to all storage locations.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractStorageLocation implements StorageLocation {

    protected final RegisterManager registerManager;

    public AbstractStorageLocation(RegisterManager registerManager) {
        this.registerManager = registerManager;
    }

    /**
     * Allocates the temporary register in the register manager, executes {@code runnable},
     * and safely de-allocates the register again.
     */
    protected void withTemporaryRegister(Register register, Runnable runnable) {
        Register r = registerManager.allocate(register);
        try {
            if (r == null) throw new IllegalStateException("register " + register + " not available");
            runnable.run();
        } finally {
            if (r != null) registerManager.free(register);
        }
    }

    /**
     * Allocates the temporary registers in the register manager, executes {@code runnable},
     * and safely de-allocates the registers again.
     */
    protected void withTemporaryRegisters(Register register1, Register register2, Runnable runnable) {
        Register r1 = registerManager.allocate(register1);
        Register r2 = registerManager.allocate(register2);
        try {
            if (r1 == null) throw new IllegalStateException("register " + register1 + " not available");
            if (r2 == null) throw new IllegalStateException("register " + register2 + " not available");
            runnable.run();
        } finally {
            if (r1 != null) registerManager.free(r1);
            if (r2 != null) registerManager.free(r2);
        }
    }
}
