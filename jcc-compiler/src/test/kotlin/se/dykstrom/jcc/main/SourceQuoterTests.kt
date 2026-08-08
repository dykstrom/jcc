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

package se.dykstrom.jcc.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.utils.FormatUtils.EOL
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class SourceQuoterTests {

    @Test
    fun shouldQuoteLineAndPointCaretAtColumn() {
        // Given
        val quoter = SourceQuoter(createSourceFile("DIM a AS DOBLE"))

        // When
        val quote = quoter.quote(1, 9)

        // Then
        assertEquals(
            "    1 | DIM a AS DOBLE$EOL      |          ^",
            quote.orElseThrow()
        )
    }

    @Test
    fun shouldQuoteSecondLine() {
        // Given
        val quoter = SourceQuoter(createSourceFile("PRINT 1", "PRINT foo"))

        // When
        val quote = quoter.quote(2, 6)

        // Then
        assertEquals(
            "    2 | PRINT foo$EOL      |       ^",
            quote.orElseThrow()
        )
    }

    @Test
    fun shouldPointCaretAtFirstColumn() {
        // Given
        val quoter = SourceQuoter(createSourceFile("INPUT n%"))

        // When
        val quote = quoter.quote(1, 0)

        // Then
        assertEquals(
            "    1 | INPUT n%$EOL      | ^",
            quote.orElseThrow()
        )
    }

    @Test
    fun shouldKeepCaretAlignedAfterTabs() {
        // Given: two tabs, then PRINT
        val quoter = SourceQuoter(createSourceFile("\t\tPRINT foo"))

        // When: the caret points at 'foo', which is the ninth character
        val quote = quoter.quote(1, 8)

        // Then: the caret line repeats the tabs, so the caret lines up however wide they render
        assertEquals(
            "    1 | \t\tPRINT foo$EOL      | \t\t      ^",
            quote.orElseThrow()
        )
    }

    @Test
    fun shouldClampCaretToEndOfLine() {
        // Given
        val quoter = SourceQuoter(createSourceFile("PRINT"))

        // When: the column is past the end of the line
        val quote = quoter.quote(1, 20)

        // Then: the caret stops just after the last character
        assertEquals(
            "    1 | PRINT$EOL      |      ^",
            quote.orElseThrow()
        )
    }

    @Test
    fun shouldQuoteNothingForLineOutOfRange() {
        // Given
        val quoter = SourceQuoter(createSourceFile("PRINT"))

        // Then
        assertTrue(quoter.quote(2, 0).isEmpty)
        assertTrue(quoter.quote(0, 0).isEmpty)
        assertTrue(quoter.quote(-1, 0).isEmpty)
    }

    @Test
    fun shouldQuoteNothingForUnreadableFile() {
        // Given
        val quoter = SourceQuoter(Path.of("does_not_exist.bas"))

        // Then
        assertTrue(quoter.quote(1, 0).isEmpty)
    }

    private fun createSourceFile(vararg lines: String): Path {
        val sourcePath = Files.createTempFile("ut_", ".bas")
        sourcePath.toFile().deleteOnExit()
        Files.write(sourcePath, lines.toList(), UTF_8)
        return sourcePath
    }
}
