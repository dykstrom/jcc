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

import org.junit.Assert.assertThrows
import org.junit.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FUN_TO_F64
import se.dykstrom.jcc.basic.BasicTests.Companion.FUN_TO_STR
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_FUN_BAR_I64
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_FUN_COMMAND_STR
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_FUN_FNFOO_F64
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_FUN_FOO_F64
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_2
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_4
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_A
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import java.util.Collections.emptyList

/**
 * Tests class `BasicSyntaxVisitor`, especially functionality related to functions.
 *
 * @author Johan Dykstrom
 * @see BasicSyntaxVisitor
 */
class BasicSyntaxVisitorFunctionTests : AbstractBasicSyntaxVisitorTests() {

    @Test
    fun shouldParseCall() {
        val fe = FunctionCallExpression(0, 0, IDENT_FUN_FOO_F64, emptyList())
        val ps = PrintStatement(0, 0, listOf(fe))

        parseAndAssert("print foo()", listOf(ps))
    }

    @Test
    fun shouldParseCallWithTypedFunc() {
        val fe = FunctionCallExpression(0, 0, IDENT_FUN_COMMAND_STR, emptyList())
        val ps = PrintStatement(0, 0, listOf(fe))

        parseAndAssert("print command$()", listOf(ps))
    }

    @Test
    fun shouldParseCallWithArg() {
        val fe = FunctionCallExpression(0, 0, IDENT_FUN_FOO_F64, listOf(IL_1))
        val ps = PrintStatement(0, 0, listOf(fe))

        parseAndAssert("print foo(1)", listOf(ps))
    }

    @Test
    fun shouldParseCallWithSeveralArgs() {
        val expressions = listOf(IL_1, SL_A, IL_0)
        val fe = FunctionCallExpression(0, 0, IDENT_FUN_FOO_F64, expressions)
        val ps = PrintStatement(0, 0, listOf(fe))

        parseAndAssert("print foo(1, \"A\", 0)", listOf(ps))
    }

    @Test
    fun shouldParseCallWithFunCallArgs() {
        val feBar12 = FunctionCallExpression(0, 0, IDENT_FUN_BAR_I64, listOf(IL_1, IL_2))
        val feBar34 = FunctionCallExpression(0, 0, IDENT_FUN_BAR_I64, listOf(IL_3, IL_4))
        val feFoo = FunctionCallExpression(0, 0, IDENT_FUN_FOO_F64, listOf(feBar12, feBar34))
        val ps = PrintStatement(0, 0, listOf(feFoo))

        parseAndAssert("print foo(bar%(1, 2), bar%(3, 4))", listOf(ps))
    }

    @Test
    fun shouldParseNoArgDefFnExpression() {
        val ident = Identifier("FNbar", FUN_TO_F64)
        val fds = FunctionDefinitionStatement(0, 0, ident, listOf(), IL_1)

        parseAndAssert("DEF FNbar() = 1", listOf(fds))
    }

    @Test
    fun shouldParseNoArgDefFnExpressionWithTypeSpecifier() {
        val ident = Identifier("FNbar$", FUN_TO_STR)
        val fds = FunctionDefinitionStatement(0, 0, ident, listOf(), SL_A)

        parseAndAssert("DEF FNbar$() = \"A\"", listOf(fds))
    }

    @Test
    fun shouldParseOneArgDefaultDefFnExpression() {
        val args = listOf(Declaration(0, 0, "f", F64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, IDENT_FUN_FNFOO_F64, args, IL_1)

        parseAndAssert("DEF FNfoo(f) = 1", listOf(fds))
    }

    @Test
    fun shouldParseOneArgTypeSpecifierDefFnExpression() {
        val args = listOf(Declaration(0, 0, "f#", F64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, IDENT_FUN_FNFOO_F64, args, IDE_F64_F)

        parseAndAssert("DEF FNfoo(f#) = f#", listOf(fds))
    }

    @Test
    fun shouldParseOneArgWithAsFloatDefFnExpression() {
        val args = listOf(Declaration(0, 0, "f", F64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, IDENT_FUN_FNFOO_F64, args, IL_1)

        parseAndAssert("DEF FNfoo(f AS DOUBLE) = 1", listOf(fds))
    }

    @Test
    fun shouldParseOneArgWithAsIntegerDefFnExpression() {
        val type = Fun.from(listOf(I64.INSTANCE), F64.INSTANCE)
        val ident = Identifier("FNfoo", type)
        val args = listOf(Declaration(0, 0, "b", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, ident, args, IL_1)

        parseAndAssert("DEF FNfoo(b AS INTEGER) = 1", listOf(fds))
    }

    @Test
    fun shouldParseTwoArgDefFnExpression() {
        val type = Fun.from(listOf(F64.INSTANCE, I64.INSTANCE), I64.INSTANCE)
        val ident = Identifier("FNbar%", type)
        val args = listOf(
            Declaration(0, 0, "f", F64.INSTANCE),
            Declaration(0, 0, "a%", I64.INSTANCE)
        )
        val fds = FunctionDefinitionStatement(0, 0, ident, args, IDE_I64_A)

        parseAndAssert("DEF FNbar%(f AS DOUBLE, a%) = a%", listOf(fds))
    }

    @Test
    fun shouldNotParseDefFoo() {
        assertThrows(IllegalStateException::class.java) { parseAndAssert("DEF FOOfoo() = 1", listOf()) }
    }
}
