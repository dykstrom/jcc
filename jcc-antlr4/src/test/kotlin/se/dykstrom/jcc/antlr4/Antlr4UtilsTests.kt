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

package se.dykstrom.jcc.antlr4

import org.antlr.v4.runtime.RecognitionException
import org.junit.Test
import se.dykstrom.jcc.common.error.CompilationErrorListener
import kotlin.test.assertEquals

class Antlr4UtilsTests {
    @Test
    fun shouldViewAsBaseErrorListener() {
        // Given
        val compilationErrorListener = CompilationErrorListener()

        // When
        val baseErrorListener = Antlr4Utils.asBaseErrorListener(compilationErrorListener)
        baseErrorListener.syntaxError(
            null, null, 3, 6, "msg", RecognitionException(null, null, null)
        )

        // Then
        assertEquals(1, compilationErrorListener.errors.size)
        with (compilationErrorListener) {
            assertEquals(3, errors[0].line)
            assertEquals(6, errors[0].column)
        }
    }
}
