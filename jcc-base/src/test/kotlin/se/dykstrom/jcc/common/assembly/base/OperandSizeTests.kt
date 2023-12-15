/*
 * Copyright (C) 2019 Johan Dykstrom
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

package se.dykstrom.jcc.common.assembly.base

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.assembly.base.OperandSize.BYTE

class OperandSizeTests {

    @Test
    fun shouldValidateDecimalValue() {
        BYTE.validate("0")
        BYTE.validate(Byte.MIN_VALUE.toString())
        BYTE.validate(Byte.MAX_VALUE.toString())
    }

    @Test
    fun shouldValidateHexadecimalValue() {
        BYTE.validate("0h")
        BYTE.validate("7fh")
    }

    @Test
    fun shouldNotValidateInvalidValue() {
        val exception = assertThrows<NumberFormatException> {
            BYTE.validate("128")
        }
        assertTrue { exception.message?.startsWith("Value out of range.") ?: false }
    }
}
