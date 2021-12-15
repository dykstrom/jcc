/*
 * Copyright (C) 2021 Johan Dykstrom
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

package se.dykstrom.jcc.basic.code.statement

import org.junit.Test
import se.dykstrom.jcc.basic.ast.ReturnStatement
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.common.assembly.base.Label
import se.dykstrom.jcc.common.assembly.instruction.Ret
import kotlin.test.assertEquals

class ReturnCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = ReturnCodeGenerator(context)

    @Test
    fun generateReturnWithoutLabel() {
        // Given
        val returnStatement = ReturnStatement(0, 0)

        // When
        val lines = generator.generate(returnStatement)

        // Then
        assertEquals(listOf(Ret()), lines)
    }

    @Test
    fun generateReturnWithLabel() {
        // Given
        val returnStatement = ReturnStatement(0, 0, "foo")

        // When
        val lines = generator.generate(returnStatement)

        // Then
        assertEquals(listOf(Label("_line_foo"), Ret()), lines)
    }
}