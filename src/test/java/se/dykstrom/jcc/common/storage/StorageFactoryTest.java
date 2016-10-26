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

import org.junit.Test;
import se.dykstrom.jcc.common.assembly.base.Register;

import static org.junit.Assert.*;
import static se.dykstrom.jcc.common.assembly.base.Register.*;

public class StorageFactoryTest {

    private final StorageFactory testee = new StorageFactory();

    @Test
    public void allocateAllRegisters() {
        allocateAndAssert(RAX, RCX, RDX, R8, R9, R10, R11, RBX, RDI, RSI, R12, R13, R14, R15);
        allocateAndFail(RCX);
    }

    @Test
    public void allocateAndFree() {
        allocateAndAssert(RAX, RCX, RDX, R8, R9, R10, R11, RBX, RDI);

        // Free some registers and allocate again
        new RegisterStorageLocation(RCX, testee.getRegisterManager()).close();
        new RegisterStorageLocation(RDX, testee.getRegisterManager()).close();

        allocateAndAssert(RCX, RDX, RSI, R12, R13, R14, R15);
        allocateAndFail(RCX);
    }

    @Test
    public void tryWithResources() {
        // Allocate and free automatically
        try (StorageLocation location = testee.allocate(RAX)) {
            assertTrue(location instanceof RegisterStorageLocation);
            RegisterStorageLocation rsl = (RegisterStorageLocation) location;
            Register actualRegister = rsl.getRegister();
            assertEquals(RAX, actualRegister);
        }

        // And again
        try (StorageLocation location = testee.allocate(RAX)) {
            assertTrue(location instanceof RegisterStorageLocation);
            RegisterStorageLocation rsl = (RegisterStorageLocation) location;
            Register actualRegister = rsl.getRegister();
            assertEquals(RAX, actualRegister);
        }
    }

    // -----------------------------------------------------------------------

    private void allocateAndAssert(Register... expectedRegisters) {
        for (Register expectedRegister : expectedRegisters) {
            StorageLocation location = testee.allocate(expectedRegister);
            assertTrue(location instanceof RegisterStorageLocation);
            RegisterStorageLocation rsl = (RegisterStorageLocation) location;
            Register actualRegister = rsl.getRegister();
            assertEquals(expectedRegister, actualRegister);
        }
    }

    private void allocateAndFail(Register register) {
        try {
            testee.allocate(register);
            fail("Expected exception");
        } catch (IllegalStateException ignore) {
            // OK
        }
    }
}
