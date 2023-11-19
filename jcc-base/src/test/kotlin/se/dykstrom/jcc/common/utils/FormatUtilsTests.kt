/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import se.dykstrom.jcc.common.utils.FormatUtils.*

class FormatUtilsTests {

    @Test
    fun shouldNormalizeNumber() {
        assertEquals("3.14", normalizeNumber("3.14"))
        assertEquals("0.14", normalizeNumber("0.14"))
        assertEquals("0.14", normalizeNumber(".14"))
        assertEquals("3.0", normalizeNumber("3."))
        assertEquals("3.0", normalizeNumber("3"))
    }

    @Test
    fun shouldNormalizeExponent() {
        assertEquals("", normalizeExponent(null, null, "e"))
        assertEquals("e+3", normalizeExponent("e+3", "+", "e"))
        assertEquals("e+15", normalizeExponent("d+15", "+", "e"))
        assertEquals("e-5", normalizeExponent("D-5", "-", "e"))
        assertEquals("e-1", normalizeExponent("E-1", "-", "e"))
        assertEquals("e+7", normalizeExponent("d7", null, "e"))
    }

    @Test
    fun shouldNormalizeFloatNumber() {
        assertEquals("3.14", normalizeFloatNumber("", "3.14", null, null, "e"))
        assertEquals("0.99e+2", normalizeFloatNumber(null, ".99", "E2", null, "e"))
    }
}
