/*
 * Copyright (C) 2018 Johan Dykstrom
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

package se.dykstrom.jcc.common.storage

import org.junit.Test
import se.dykstrom.jcc.common.assembly.base.FloatRegister
import se.dykstrom.jcc.common.assembly.base.FloatRegister.*
import se.dykstrom.jcc.common.assembly.base.Register
import se.dykstrom.jcc.common.assembly.base.Register.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class StorageFactoryTests {

    private val storageFactory = StorageFactory()

    @Test
    fun shouldAllocateAllRegisters() {
        allocateAndAssert(RAX, RCX, RDX, R8, R9, R10, R11, RBX, RDI, RSI, R12, R13, R14, R15)
        allocateAndFail(RCX)
    }

    @Test
    fun shouldAllocateAndFreeRegisters() {
        allocateAndAssert(RAX, RCX, RDX, R8, R9, R10, R11, RBX, RDI)

        // Free some registers and allocate again
        with (storageFactory) {
            RegisterStorageLocation(RCX, registerManager).close()
            RegisterStorageLocation(RDX, registerManager).close()
        }

        allocateAndAssert(RCX, RDX, RSI, R12, R13, R14, R15)
        allocateAndFail(RCX)
    }

    @Test
    fun shouldTryWithResources() {
        // Allocate and free automatically
        storageFactory.allocate(RAX).use { location ->
            assertTrue(location is RegisterStorageLocation)
            val rsl = location as RegisterStorageLocation
            assertEquals(RAX, rsl.register)
        }

        // And again
        storageFactory.allocate(RAX).use { location ->
            assertTrue(location is RegisterStorageLocation)
            val rsl = location as RegisterStorageLocation
            assertEquals(RAX, rsl.register)
        }

        // Allocate and free floating point register automatically
        storageFactory.allocate(XMM0).use { location ->
            assertTrue(location is FloatRegisterStorageLocation)
            val rsl = location as FloatRegisterStorageLocation
            assertEquals(XMM0, rsl.register)
        }

        // And again
        storageFactory.allocate(XMM0).use { location ->
            assertTrue(location is FloatRegisterStorageLocation)
            val rsl = location as FloatRegisterStorageLocation
            assertEquals(XMM0, rsl.register)
        }
    }

    @Test
    fun shouldAllocateAllFloatRegisters() {
        allocateAndAssert(XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7, XMM8, XMM9, XMM10, XMM11, XMM12, XMM13, XMM14, XMM15)
        allocateAndFail(XMM0)
    }

    @Test
    fun shouldAllocateAndFreeFloatRegisters() {
        allocateAndAssert(XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7, XMM8, XMM9)

        // Free some registers and allocate again
        with (storageFactory) {
            FloatRegisterStorageLocation(XMM0, floatRegisterManager, registerManager, memoryManager).close()
            FloatRegisterStorageLocation(XMM1, floatRegisterManager, registerManager, memoryManager).close()
        }

        allocateAndAssert(XMM0, XMM1, XMM10, XMM11, XMM12, XMM13, XMM14, XMM15)
        allocateAndFail(XMM0)
    }

    // TODO: Add memory allocation tests for FloatMemoryStorageLocation.

    @Test
    fun shouldAllocateMemory() {
        // Allocate the seven non-volatile registers
        for (i in 0..6) {
            assertTrue(storageFactory.allocateNonVolatile() is RegisterStorageLocation)
        }

        // Allocate one non-volatile memory location
        assertTrue(storageFactory.allocateNonVolatile() is MemoryStorageLocation)
    }

    @Test
    fun shouldAllocateDifferentMemory() {
        // Allocate the seven non-volatile registers
        for (i in 0..6) {
            assertTrue(storageFactory.allocateNonVolatile() is RegisterStorageLocation)
        }

        // Allocate one non-volatile memory location
        val location0 = storageFactory.allocateNonVolatile() as MemoryStorageLocation
        // Allocate another
        val location1 = storageFactory.allocateNonVolatile() as MemoryStorageLocation
        // The memory addresses should be different
        assertNotEquals(location0.memory, location1.memory, "Memory addresses equal")
    }

    @Test
    fun shouldAllocateAndFreeMemory() {
        // Allocate the seven non-volatile registers
        for (i in 0..6) {
            assertTrue(storageFactory.allocateNonVolatile() is RegisterStorageLocation)
        }

        // Allocate one non-volatile memory location
        var location = storageFactory.allocateNonVolatile() as MemoryStorageLocation
        val memory0 = location.memory

        // Free memory location, and allocate it again
        location.close()
        location = storageFactory.allocateNonVolatile() as MemoryStorageLocation
        val memory1 = location.memory

        // The memory addresses should be the same
        assertEquals(memory0, memory1, "Memory addresses differ")
    }

    // -----------------------------------------------------------------------

    private fun allocateAndAssert(vararg expectedRegisters: Register) {
        expectedRegisters.forEach {
            val location = storageFactory.allocate(it)
            assertTrue(location is RegisterStorageLocation)
            val rsl = location as RegisterStorageLocation
            assertEquals(it, rsl.register)
        }
    }

    private fun allocateAndAssert(vararg expectedRegisters: FloatRegister) {
        expectedRegisters.forEach {
            val location = storageFactory.allocate(it)
            assertTrue(location is FloatRegisterStorageLocation)
            val rsl = location as FloatRegisterStorageLocation
            assertEquals(it, rsl.register)
        }
    }

    private fun allocateAndFail(register: Register) {
        try {
            storageFactory.allocate(register)
            fail("Expected exception")
        } catch (ignore: IllegalStateException) {
            // OK
        }
    }

    private fun allocateAndFail(register: FloatRegister) {
        try {
            storageFactory.allocate(register)
            fail("Expected exception")
        } catch (ignore: IllegalStateException) {
            // OK
        }
    }
}
