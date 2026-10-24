/*
 * Copyright (C) 2026 Johan Dykstrom
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.utils.StringUtils.distance
import se.dykstrom.jcc.common.utils.StringUtils.findSimilar
import java.util.Optional

class StringUtilsTests {

    @Test
    fun shouldCalculateDistance() {
        assertEquals(0, distance("", ""))
        assertEquals(0, distance("double", "double"))
        assertEquals(0, distance("DOUBLE", "double"))
        assertEquals(3, distance("", "abc"))
        assertEquals(3, distance("abc", ""))
        assertEquals(1, distance("doble", "double"))
        assertEquals(1, distance("doubble", "double"))
        assertEquals(1, distance("douple", "double"))
        assertEquals(2, distance("duoble", "double"))
        assertEquals(4, distance("single", "double"))
    }

    @Test
    fun shouldFindSimilar() {
        assertEquals(Optional.of("double"), findSimilar("doble", TYPE_NAMES))
        assertEquals(Optional.of("double"), findSimilar("DOBLE", TYPE_NAMES))
        assertEquals(Optional.of("integer"), findSimilar("integr", TYPE_NAMES))
        assertEquals(Optional.of("string"), findSimilar("strng", TYPE_NAMES))
    }

    @Test
    fun shouldNotFindSimilar() {
        assertTrue(findSimilar("person", TYPE_NAMES).isEmpty)
        assertTrue(findSimilar("double", listOf()).isEmpty)
    }

    /**
     * A short name must match more closely than a long one, or any two short names look similar.
     * One edit is enough for a name of three letters, but two are not.
     */
    @Test
    fun shouldNotFindSimilarShortName() {
        assertEquals(Optional.of("f64"), findSimilar("i64", listOf("f64")))
        assertTrue(findSimilar("i32", listOf("f64")).isEmpty)
        assertTrue(findSimilar("a", listOf("b")).isEmpty)
    }

    companion object {
        private val TYPE_NAMES = listOf("double", "integer", "string")
    }
}
