/*
 * Copyright (C) 2017 Johan Dykstrom
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

import org.junit.Test
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.common.ast.*
import java.util.Collections.emptyList

/**
 * Tests class `BasicSyntaxVisitor`, especially functionality related to WHILE statements.
 *
 * @author Johan Dykstrom
 * @see BasicSyntaxVisitor
 */
class BasicSyntaxVisitorWhileTests : AbstractBasicSyntaxVisitorTest() {

    @Test
    fun shouldParseEmptyWhile() {
        val ws = WhileStatement(0, 0, IL_5, emptyList())

        parseAndAssert("while 5 wend", listOf(ws))
    }

    @Test
    fun shouldParseSimpleWhile() {
        val ps = PrintStatement(0, 0, listOf(BL_TRUE))
        val ws = WhileStatement(0, 0, BL_TRUE, listOf(ps))

        parseAndAssert("while true print true wend", listOf(ws))
    }

    @Test
    fun shouldParseWhileWend() {
        val cs = LabelledStatement("30", CommentStatement(0, 0, "WEND"))
        val ps = LabelledStatement("20", PrintStatement(0, 0, listOf(BL_TRUE)))
        val ws = LabelledStatement("10", WhileStatement(0, 0, BL_TRUE, listOf(ps, cs)))

        parseAndAssert("""
            10 while true
            20   print true
            30 wend
            """,
            listOf(ws)
        )
    }

    @Test
    fun shouldParseNestedWhile() {
        val equalExpr = EqualExpression(0, 0, IDE_B, IL_3)
        val ps = PrintStatement(0, 0, listOf(BL_TRUE))
        val innerWhile = WhileStatement(0, 0, equalExpr, listOf(ps))
        val notEqualExpr = NotEqualExpression(0, 0, IDE_A, IL_4)
        val outerWhile = WhileStatement(0, 0, notEqualExpr, listOf(innerWhile))

        parseAndAssert("while a% <> 4 " +
                "  while b% = 3 " +
                "    print true " +
                "  wend " +
                "wend", listOf(outerWhile))
    }
}
