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

package se.dykstrom.jcc.common.assembly.other;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LibraryTest {

    private static final String LIBRARY_NAME_1 = "foo";
    private static final String LIBRARY_NAME_2 = "bar";

    private static final String LIBRARY_FILE_1 = LIBRARY_NAME_1 + ".dll";
    private static final String LIBRARY_FILE_2 = LIBRARY_NAME_2 + ".dll";

    @Test
    public void testOne() {
        Library library = new Library(singletonList(LIBRARY_FILE_1));
        String asm = library.toAsm();
        assertTrue(asm.contains(LIBRARY_NAME_1 + ","));
        assertTrue(asm.contains(LIBRARY_FILE_1));
        assertFalse(asm.contains("\\"));
    }

    @Test
    public void testTwo() {
        Library library = new Library(asList(LIBRARY_FILE_1, LIBRARY_FILE_2));
        String asm = library.toAsm();
        assertTrue(asm.contains(LIBRARY_NAME_1 + ","));
        assertTrue(asm.contains(LIBRARY_FILE_1));
        assertTrue(asm.contains(LIBRARY_NAME_2 + ","));
        assertTrue(asm.contains(LIBRARY_FILE_2));
        assertTrue(asm.contains("\\"));
    }
}
