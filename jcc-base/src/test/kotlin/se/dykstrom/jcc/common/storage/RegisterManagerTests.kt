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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.assembly.base.Register

/**
 * Tests class `RegisterManager`.
 *
 * @author Johan Dykstrom
 */
class RegisterManagerTests {

    private val registerManager = RegisterManager()

    @Test
    fun shouldRunWithTemporaryRegister() {
        // Given
        val registers = HashSet<Register>()

        // When
        registerManager.withTemporaryRegister { registers.add(it) }

        // Then
        assertEquals(1, registers.size)
        val register = registers.iterator().next()
        assertTrue(register.isVolatile)
    }
}
