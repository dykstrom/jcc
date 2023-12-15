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

package se.dykstrom.jcc.common.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompilationErrorListenerTests {

    private val errorListener = CompilationErrorListener()

    @Test
    fun shouldListenToSemanticsError() {
        // Given
        val line = 1
        val column = 2
        val msg = "error message"

        // When
        errorListener.semanticsError(line, column, msg, SemanticsException(msg))

        // Then
        with (errorListener) {
            assertEquals(1, errors.size)
            assertEquals(line, errors[0].line())
            assertEquals(column, errors[0].column())
            assertEquals(msg, errors[0].msg())
        }
    }
}
