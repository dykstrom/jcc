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

package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.BooleanLiteral.TRUE
import se.dykstrom.jcc.common.ast.WhileStatement
import se.dykstrom.jcc.common.error.SyntaxException

class ColSyntaxParserWhileTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParseWhileWithCallBody() {
        val statement = WhileStatement(TRUE, listOf(printlnCall()))
        verify(parse("while true do call println() end"), statement)
    }

    @Test
    fun shouldParseWhileWithEmptyBody() {
        val statement = WhileStatement(TRUE, listOf())
        verify(parse("while true do end"), statement)
    }

    @Test
    fun shouldParseWhileWithMultipleBodyStatements() {
        val statement = WhileStatement(TRUE, listOf(printlnCall(), printlnCall()))
        verify(parse("while true do call println() call println() end"), statement)
    }

    @Test
    fun shouldParseNestedWhile() {
        val inner = WhileStatement(TRUE, listOf(printlnCall()))
        val outer = WhileStatement(TRUE, listOf(inner))
        verify(parse("while true do while true do call println() end end"), outer)
    }

    @Test
    fun shouldNotParseWhileWithoutDo() {
        assertThrows<SyntaxException> { parse("while true call println() end") }
    }

    @Test
    fun shouldNotParseWhileWithoutEnd() {
        assertThrows<SyntaxException> { parse("while true do call println()") }
    }

    @Test
    fun shouldNotParseWhileWithoutCondition() {
        assertThrows<SyntaxException> { parse("while do call println() end") }
    }
}
