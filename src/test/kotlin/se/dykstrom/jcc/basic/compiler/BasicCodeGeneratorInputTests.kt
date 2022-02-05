/*
 * Copyright (C) 2019 Johan Dykstrom
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

package se.dykstrom.jcc.basic.compiler

import org.junit.Assert.assertEquals
import org.junit.Test
import se.dykstrom.jcc.basic.ast.LineInputStatement
import se.dykstrom.jcc.common.assembly.instruction.CallDirect
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.assembly.other.DataDefinition
import se.dykstrom.jcc.common.types.Str

/**
 * Tests features related to (LINE) INPUT statements in code generation.
 *
 * @author Johan Dykstrom
 */
class BasicCodeGeneratorInputTests : AbstractBasicCodeGeneratorTest() {

    @Test
    fun lineInputShouldDefineStringVariable() {
        val statement = LineInputStatement.builder(IDENT_STR_S).build()

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Variable s$ should be defined and be a string
        assertEquals(1, lines
                .filterIsInstance(DataDefinition::class.java)
                .map { it.identifier() }
                .count { it.mappedName == IDENT_STR_S.mappedName && it.type == Str.INSTANCE })
        // There should be a call to getline
        assertEquals(1, lines
                .filterIsInstance(CallDirect::class.java)
                .count { it.target.contains("getline") })
    }

    @Test
    fun lineInputShouldPrintPrompt() {
        val prompt = "thePrompt"
        val statement = LineInputStatement.builder(IDENT_STR_S).prompt(prompt).build()

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // The prompt should be defined as a variable
        assertEquals(1, lines
                .filterIsInstance(DataDefinition::class.java)
                .count { it.value().contains(prompt) })
        // The prompt should be printed using printf
        assertEquals(1, lines
                .filterIsInstance(CallIndirect::class.java)
                .count { it.target.contains("printf") })
    }

    @Test
    fun lineInputShouldNotPrintNewline() {
        val statement = LineInputStatement.builder(IDENT_STR_S).inhibitNewline(true).build()

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // The the newline format string should not be defined
        assertEquals(0, lines
                .filterIsInstance(DataDefinition::class.java)
                .count { it.identifier().name.contains("line_input_newline") })
    }
}
