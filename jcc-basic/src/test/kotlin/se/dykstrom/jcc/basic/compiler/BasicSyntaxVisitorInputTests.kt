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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.ast.LineInputStatement
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_STR_S

/**
 * Tests class `BasicSyntaxVisitor`, especially functionality related to (LINE) INPUT statements.
 *
 * @author Johan Dykstrom
 * @see BasicSyntaxVisitor
 */
class BasicSyntaxVisitorInputTests : AbstractBasicSyntaxVisitorTests() {

    @Test
    fun shouldParseSimpleStatement() {
        val lis = LineInputStatement.builder(IDENT_STR_S).build()
        parseAndAssert("line input s$", listOf(lis))
    }

    @Test
    fun shouldParseOptionalSemiColon() {
        val lis = LineInputStatement.builder(IDENT_STR_S).inhibitNewline(true).build()
        parseAndAssert("line input; s$", listOf(lis))
    }

    @Test
    fun shouldParsePrompt() {
        val lis = LineInputStatement.builder(IDENT_STR_S).prompt("prompt:").build()
        parseAndAssert("line input \"prompt:\"; s$", listOf(lis))
    }

    @Test
    fun shouldParseEmptyPrompt() {
        val lis = LineInputStatement.builder(IDENT_STR_S).prompt("").build()
        parseAndAssert("line input \"\"; s$", listOf(lis))
    }

    @Test
    fun shouldParseComplexStatement() {
        val lis = LineInputStatement.builder(IDENT_STR_S).inhibitNewline(true).prompt("Enter number: ").build()
        parseAndAssert("line input; \"Enter number: \"; s$", listOf(lis))
    }
}
