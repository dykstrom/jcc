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
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_FUN_BAR_I64
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_B
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_5
import se.dykstrom.jcc.basic.ast.OptionBaseStatement
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.ArrayDeclaration
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str

/**
 * Tests class `BasicSyntaxVisitor`, especially functionality related to arrays.
 *
 * @author Johan Dykstrom
 * @see BasicSyntaxVisitor
 */
class BasicSyntaxVisitorArrayTests : AbstractBasicSyntaxVisitorTests() {

    @Test
    fun shouldParseSingleDimensionArrayDeclaration() {
        val declaration = ArrayDeclaration(0, 0, "arr", Arr.from(1, I64.INSTANCE), listOf(IL_5))
        val vds = VariableDeclarationStatement(0, 0, listOf(declaration))
        parseAndAssert("dim arr(5) as integer", listOf(vds))
    }

    @Test
    fun shouldParseMultiDimensionArrayDeclaration() {
        val declaration = ArrayDeclaration(0, 0, "arr", Arr.from(2, F64.INSTANCE), listOf(IDE_I64_A, IDE_I64_B))
        val vds = VariableDeclarationStatement(0, 0, listOf(declaration))
        parseAndAssert("dim arr(a, b) as double", listOf(vds))
    }

    @Test
    fun shouldParseMultipleArrayDeclarationsWithDifferentDimensions() {
        val declaration0 = ArrayDeclaration(0, 0, "arr", Arr.from(1, I64.INSTANCE), listOf(IL_5))
        val addExpression = AddExpression(0, 0, IL_1, IL_1)
        val declaration1 = ArrayDeclaration(0, 0, "foo", Arr.from(2, Str.INSTANCE), listOf(IL_3, addExpression))
        val vds = VariableDeclarationStatement(0, 0, listOf(declaration0, declaration1))
        parseAndAssert("dim arr(5) as integer, foo(3, 1 + 1) as string", listOf(vds))
    }

    /**
     * Array access looks like a function call to the syntax visitor. It will be converted
     * to an array access expression during the semantic analysis.
     */
    @Test
    fun shouldParseSingleDimensionArrayAccess() {
        val expression = FunctionCallExpression(0, 0, IDENT_FUN_BAR_I64, listOf(IL_5))
        val statement = PrintStatement(0, 0, listOf(expression))
        parseAndAssert("print bar%(5)", listOf(statement))
    }

    @Test
    fun shouldParseOptionBase1() {
        val statement = OptionBaseStatement(0, 0, 1)
        parseAndAssert("option base 1", statement)
    }

    @Test
    fun optionBaseMustHaveNumericBase() {
        assertThrows<IllegalStateException> { parse("option base X") }
    }

    @Test
    fun optionBaseMustBePositive() {
        assertThrows<IllegalStateException> { parse("option base -1") }
    }
}
