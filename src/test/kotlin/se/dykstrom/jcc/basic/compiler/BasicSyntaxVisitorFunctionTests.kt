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
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import java.util.Collections.emptyList

/**
 * Tests class `BasicSyntaxVisitor`, especially functionality related to function calls.
 *
 * @author Johan Dykstrom
 * @see BasicSyntaxVisitor
 */
class BasicSyntaxVisitorFunctionTests : AbstractBasicSyntaxVisitorTest() {

    @Test
    fun shouldParseCall() {
        val fe = FunctionCallExpression(0, 0, IDENT_UNK_FOO, emptyList())
        val ps = PrintStatement(0, 0, listOf(fe))

        parseAndAssert("print foo()", listOf(ps))
    }

    @Test
    fun shouldParseCallWithTypedFunc() {
        val fe = FunctionCallExpression(0, 0, IDENT_STR_COMMAND, emptyList())
        val ps = PrintStatement(0, 0, listOf(fe))

        parseAndAssert("print command$()", listOf(ps))
    }

    @Test
    fun shouldParseCallWithArg() {
        val fe = FunctionCallExpression(0, 0, IDENT_UNK_FOO, listOf(IL_1))
        val ps = PrintStatement(0, 0, listOf(fe))

        parseAndAssert("print foo(1)", listOf(ps))
    }

    @Test
    fun shouldParseCallWithSeveralArgs() {
        val expressions = listOf<Expression>(IL_1, SL_A, BL_FALSE)
        val fe = FunctionCallExpression(0, 0, IDENT_UNK_FOO, expressions)
        val ps = PrintStatement(0, 0, listOf(fe))

        parseAndAssert("print foo(1, \"A\", false)", listOf(ps))
    }

    @Test
    fun shouldParseCallWithFunCallArgs() {
        val feBar12 = FunctionCallExpression(0, 0, IDENT_UNK_BAR, listOf(IL_1, IL_2))
        val feBar34 = FunctionCallExpression(0, 0, IDENT_UNK_BAR, listOf(IL_3, IL_4))
        val feFoo = FunctionCallExpression(0, 0, IDENT_UNK_FOO, listOf(feBar12, feBar34))
        val ps = PrintStatement(0, 0, listOf(feFoo))

        parseAndAssert("print foo(bar(1, 2), bar(3, 4))", listOf(ps))
    }
}
