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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.dykstrom.jcc.common.assembly.base.Register.*;

import org.junit.Test;

import se.dykstrom.jcc.common.assembly.base.Register;

public class StorageFactoryTest {

    private final StorageFactory testee = new StorageFactory();

    @Test
    public void shouldAllocateAllRegisters() {
        allocateAndAssert(RAX, RCX, RDX, R8, R9, R10, R11, RBX, RDI, RSI, R12, R13, R14, R15);
        allocateAndFail(RCX);
    }

    @Test
    public void shouldAllocateAndFreeRegisters() {
        allocateAndAssert(RAX, RCX, RDX, R8, R9, R10, R11, RBX, RDI);

        // Free some registers and allocate again
        new RegisterStorageLocation(RCX, testee.getRegisterManager()).close();
        new RegisterStorageLocation(RDX, testee.getRegisterManager()).close();

        allocateAndAssert(RCX, RDX, RSI, R12, R13, R14, R15);
        allocateAndFail(RCX);
    }

    @Test
    public void shouldTryWithResources() {
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

    @Test
    public void shouldAllocateMemory() {
        // Allocate the seven non-volatile registers
        for (int i = 0; i < 7; i++) {
            assertTrue(testee.allocateNonVolatile() instanceof RegisterStorageLocation);
        }
        
        // Allocate one non-volatile memory location
        assertTrue(testee.allocateNonVolatile() instanceof MemoryStorageLocation);
    }

    @Test
    public void shouldAllocateDifferentMemory() {
        // Allocate the seven non-volatile registers
        for (int i = 0; i < 7; i++) {
            assertTrue(testee.allocateNonVolatile() instanceof RegisterStorageLocation);
        }
        
        // Allocate one non-volatile memory location
        MemoryStorageLocation location0 = (MemoryStorageLocation) testee.allocateNonVolatile();
        // Allocate another
        MemoryStorageLocation location1 = (MemoryStorageLocation) testee.allocateNonVolatile();
        // The memory addresses should be different
        assertNotEquals("Memory addresses equal", location0.getMemory(), location1.getMemory());
    }

    @Test
    public void shouldAllocateAndFreeMemory() {
        // Allocate the seven non-volatile registers
        for (int i = 0; i < 7; i++) {
            assertTrue(testee.allocateNonVolatile() instanceof RegisterStorageLocation);
        }
        
        // Allocate one non-volatile memory location
        MemoryStorageLocation location = (MemoryStorageLocation) testee.allocateNonVolatile();
        String memory0 = location.getMemory();
        
        // Free memory location, and allocate it again
        location.close();
        location = (MemoryStorageLocation) testee.allocateNonVolatile();
        String memory1 = location.getMemory();
        
        // The memory addresses should be the same
        assertEquals("Memory addresses differ", memory0, memory1);
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
