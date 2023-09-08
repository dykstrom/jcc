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

package se.dykstrom.jcc.col.types

import org.junit.Test
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColTypeManagerTests {

    private val typeManager = ColTypeManager()

    @Test
    fun shouldGetTypeNameOfScalarTypes() {
        assertEquals("f64", typeManager.getTypeName(F64.INSTANCE))
        assertEquals("i64", typeManager.getTypeName(I64.INSTANCE))
        assertEquals("string", typeManager.getTypeName(Str.INSTANCE))
    }

    @Test
    fun shouldFindPredefinedTypes() {
        assertEquals(F64.INSTANCE, typeManager.getTypeFromName("f64").get())
        assertEquals(I64.INSTANCE, typeManager.getTypeFromName("i64").get())
        assertEquals(Str.INSTANCE, typeManager.getTypeFromName("string").get())
    }

    @Test
    fun shouldDefineType() {
        // Given
        assertTrue { typeManager.getTypeFromName("foo").isEmpty }

        // When
        typeManager.defineTypeName("foo", I64.INSTANCE)

        // Then
        assertEquals(I64.INSTANCE, typeManager.getTypeFromName("foo").get())
    }
}
