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

import org.hamcrest.CoreMatchers.hasItem
import org.junit.Assert.assertThat
import org.junit.Test
import se.dykstrom.jcc.common.assembly.base.FloatRegister.XMM6
import se.dykstrom.jcc.common.assembly.base.Register
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests class `AbstractStorageLocation`.
 *
 * @author Johan Dykstrom
 */
class AbstractStorageLocationTests {

    private val memoryManager = MemoryManager()
    private val registerManager = RegisterManager()
    private val floatRegisterManager = FloatRegisterManager()

    private val location = FloatRegisterStorageLocation(XMM6, floatRegisterManager, registerManager, memoryManager)

    @Test
    fun shouldRunWithTemporaryRegister() {
        // Given
        val registers = HashSet<Register>()

        // When
        location.withTemporaryRegister { registers.add(it) }

        // Then
        assertEquals(1, registers.size)
        val register = registers.iterator().next()
        assertTrue(register.isVolatile)
    }

    @Test
    fun shouldRunWithTemporaryMemory() {
        // Given
        val memories = HashSet<String>()

        // When
        location.withTemporaryMemory { memories.add(it) }

        // Then
        assertEquals(1, memories.size)
        assertEquals(1, memoryManager.usedMemoryAddresses.size)
        val temporaryMemory = memories.iterator().next()
        assertThat(memoryManager.usedMemoryAddresses, hasItem(getUnmappedName(temporaryMemory)))
        val allocatedMemory = memoryManager.allocate()
        assertEquals(temporaryMemory, allocatedMemory)
        assertEquals(1, memoryManager.usedMemoryAddresses.size)
    }

    private fun getUnmappedName(name: String) = name.substring(1)
}
