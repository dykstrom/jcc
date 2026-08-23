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

package se.dykstrom.jcc.common.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests the order [CompilationMessage] sorts in, which is the order the compiler prints
 * diagnostics in.
 *
 * @author Johan Dykstrom
 */
class CompilationMessageTests {

    @Test
    fun shouldSortByLine() {
        assertOrder(error(3, 0), error(1, 0), error(2, 0), expected = listOf("1:0", "2:0", "3:0"))
    }

    @Test
    fun shouldSortByColumnWithinALine() {
        // Two messages about the same line used to be printed in the order they were reported,
        // which for a syntax error is the order the parser backtracked in
        assertOrder(error(1, 9), error(1, 6), error(1, 0), expected = listOf("1:0", "1:6", "1:9"))
    }

    @Test
    fun shouldSortWarningsAndErrorsTogether() {
        assertOrder(error(2, 4), warning(2, 1), error(1, 8), expected = listOf("1:8", "2:1", "2:4"))
    }

    private fun assertOrder(vararg messages: CompilationMessage, expected: List<String>) {
        val sorted = messages.toMutableList()
        sorted.sort()
        assertEquals(expected, sorted.map { "${it.line()}:${it.column()}" })
    }

    private fun error(line: Int, column: Int) = CompilationError(line, column, "error", null)

    private fun warning(line: Int, column: Int) = CompilationWarning(line, column, "warning", null)
}
