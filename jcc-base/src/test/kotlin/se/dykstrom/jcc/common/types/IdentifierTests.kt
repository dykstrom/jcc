/*
 * Copyright (C) 2022 Johan Dykstrom
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

package se.dykstrom.jcc.common.types

import org.junit.Test
import kotlin.test.assertEquals

class IdentifierTests {

    @Test
    fun shouldGetMappedName() {
        assertEquals("_i%", Identifier("i%", I64.INSTANCE).mappedName)
        assertEquals("_f_hash", Identifier("f#", F64.INSTANCE).mappedName)
        assertEquals("_s$", Identifier("s$", Str.INSTANCE).mappedName)
        assertEquals("_i%_arr", Identifier("i%", Arr.INSTANCE).mappedName)
    }

    @Test
    fun shouldGetIdentifierWithType() {
        assertEquals(Identifier("foo", Str.INSTANCE), Identifier("foo", I64.INSTANCE).withType(Str.INSTANCE))
        assertEquals(Identifier("foo", I64.INSTANCE), Identifier("foo", F64.INSTANCE).withType(I64.INSTANCE))
    }
}
