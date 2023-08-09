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
import se.dykstrom.jcc.common.assembly.base.Register
import se.dykstrom.jcc.common.types.F64
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StorageFactoryTests {

    private val storageFactory = StorageFactory()

    @Test
    fun shouldTryWithResources() {
        var savedRegister: Register?
        var savedFloatRegister: FloatRegister?

        // Allocate and free automatically
        storageFactory.allocateNonVolatile().use { location ->
            assertTrue(location is RegisterStorageLocation)
            savedRegister = location.register
        }

        // And again
        storageFactory.allocateNonVolatile().use { location ->
            assertTrue(location is RegisterStorageLocation)
            assertEquals(savedRegister, location.register)
        }

        // Allocate and free floating point register automatically
        storageFactory.allocateNonVolatile(F64.INSTANCE).use { location ->
            assertTrue(location is FloatRegisterStorageLocation)
            savedFloatRegister = location.register
        }

        // And again
        storageFactory.allocateNonVolatile(F64.INSTANCE).use { location ->
            assertTrue(location is FloatRegisterStorageLocation)
            assertEquals(savedFloatRegister, location.register)
        }
    }

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
        val location0 = storageFactory.allocateNonVolatile() as MemoryStorageLocation
        val memory0 = location0.memory
        location0.close()

        // Allocate it again after freeing it
        val location1 = storageFactory.allocateNonVolatile() as MemoryStorageLocation
        val memory1 = location1.memory
        location1.close()

        // The memory addresses should be the same
        assertEquals(memory0, memory1, "Memory addresses differ")
    }
}
