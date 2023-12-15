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

package se.dykstrom.jcc.common.assembly.other

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibraryTests {

    companion object {

        private const val LIBRARY_NAME_1 = "foo"
        private const val LIBRARY_NAME_2 = "bar"

        private const val LIBRARY_FILE_1 = "$LIBRARY_NAME_1.dll"
        private const val LIBRARY_FILE_2 = "$LIBRARY_NAME_2.dll"
    }

    @Test
    fun shouldGenerateOneLibrary() {
        val library = Library(listOf(LIBRARY_FILE_1))
        val asm = library.toText()
        assertTrue(asm.contains("$LIBRARY_NAME_1,"))
        assertTrue(asm.contains(LIBRARY_FILE_1))
        assertFalse(asm.contains("\\"))
    }

    @Test
    fun shouldGenerateTwoLibraries() {
        val library = Library(listOf(LIBRARY_FILE_1, LIBRARY_FILE_2))
        val asm = library.toText()
        assertTrue(asm.contains("$LIBRARY_NAME_1,"))
        assertTrue(asm.contains(LIBRARY_FILE_1))
        assertTrue(asm.contains("$LIBRARY_NAME_2,"))
        assertTrue(asm.contains(LIBRARY_FILE_2))
        assertTrue(asm.contains("\\"))
    }
}
