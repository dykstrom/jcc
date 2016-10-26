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

package se.dykstrom.jcc.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FileUtilsTest {
    @Test
    public void testGetBasename() {
        assertEquals("C:\\Temp\\msvcrt", FileUtils.getBasename("C:\\Temp\\msvcrt.dll"));
        assertEquals("msvcrt", FileUtils.getBasename("msvcrt.dll"));
        assertEquals("msvcrt.v2", FileUtils.getBasename("msvcrt.v2.dll"));
        assertEquals("msvcrt", FileUtils.getBasename("msvcrt"));
    }

    @Test
    public void testGetExtension() {
        assertEquals("dll", FileUtils.getExtension("C:\\Temp\\msvcrt.dll"));
        assertEquals("dll", FileUtils.getExtension("msvcrt.dll"));
        assertEquals("dll", FileUtils.getExtension("msvcrt.v2.dll"));
        assertNull(FileUtils.getExtension("msvcrt"));
    }
}
