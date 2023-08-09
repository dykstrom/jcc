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

package se.dykstrom.jcc.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.dykstrom.jcc.common.utils.FileUtils.*
import java.nio.file.Path

class FileUtilsTests {
    @Test
    fun shouldGetBasename() {
        assertEquals("C:\\Temp\\msvcrt", getBasename("C:\\Temp\\msvcrt.dll"))
        assertEquals("msvcrt", getBasename("msvcrt.dll"))
        assertEquals("msvcrt.v2", getBasename("msvcrt.v2.dll"))
        assertEquals("msvcrt", getBasename("msvcrt"))
    }

    @Test
    fun shouldGetExtension() {
        assertEquals("dll", getExtension("C:\\Temp\\msvcrt.dll"))
        assertEquals("dll", getExtension("msvcrt.dll"))
        assertEquals("dll", getExtension("msvcrt.v2.dll"))
        assertNull(getExtension("msvcrt"))
    }

    @Test
    fun shouldChangeExtension() {
        assertEquals(Path.of("C:\\Temp\\file.exe"), withExtension(Path.of("C:\\Temp\\file.bas"), "exe"))
        assertEquals(Path.of("file.asm"), withExtension(Path.of("file.c"), "asm"))
        assertEquals(Path.of("file.asm"), withExtension(Path.of("file"), "asm"))
    }
}
