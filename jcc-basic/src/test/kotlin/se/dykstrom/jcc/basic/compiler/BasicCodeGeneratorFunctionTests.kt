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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_17_E4
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_3_14
import se.dykstrom.jcc.basic.BasicTests.Companion.FUN_FLO
import se.dykstrom.jcc.basic.BasicTests.Companion.FUN_FOO
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_FUN_FLO
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_FUN_FOO
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_STR_B
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_STR_B
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_2
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_4
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_H
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_ONE
import se.dykstrom.jcc.basic.BasicTests.Companion.hasIndirectCallTo
import se.dykstrom.jcc.basic.ast.DefStrStatement
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.*
import se.dykstrom.jcc.common.assembly.directive.DataDefinition
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg
import se.dykstrom.jcc.common.assembly.instruction.MoveMemToReg
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToMem
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToReg
import se.dykstrom.jcc.common.assembly.instruction.floating.*
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement
import se.dykstrom.jcc.common.code.Comment
import se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_PRINTF_STR_VAR
import se.dykstrom.jcc.common.types.Str

class BasicCodeGeneratorFunctionTests : AbstractBasicCodeGeneratorTests() {

    @BeforeEach
    fun setUp() {
        // Define some functions for testing
        symbols.addFunction(FUN_ABS)
        symbols.addFunction(FUN_CHR)
        symbols.addFunction(FUN_CINT)
        symbols.addFunction(FUN_FLO)
        symbols.addFunction(FUN_FOO)
        symbols.addFunction(FUN_LEN)
        symbols.addFunction(FUN_LBOUND)
        symbols.addFunction(FUN_LBOUND_I64)
        symbols.addFunction(FUN_SIN)
    }

    @Test
    fun shouldGenerateSingleFunctionCallWithInt() {
        val fe = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(IL_1))
        val ps = PrintStatement(0, 0, listOf(fe))

        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Three moves: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, lines))
        // Three calls: abs, printf, and exit
        assertCodeLines(lines, 1, 3, 1, 3)
        assertTrue(hasIndirectCallTo(lines, FUN_ABS.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithString() {
        val fe = FunctionCallExpression(0, 0, FUN_LEN.identifier, listOf(SL_ONE))
        val ps = PrintStatement(0, 0, listOf(fe))

        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Three moves: format string, string expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, lines))
        // Three calls: len, printf, and exit
        assertCodeLines(lines, 1, 3, 1, 3)
        assertTrue(hasIndirectCallTo(lines, FUN_LEN.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithFloat() {
        val expression = FunctionCallExpression(0, 0, FUN_SIN.identifier, listOf(FL_3_14))
        val assignStatement = AssignStatement(0, 0, INE_F64_F, expression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        // One move: exit code
        assertEquals(1, countInstances(MoveImmToReg::class.java, lines))
        // One move: float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
        // One move: result to non-volatile float register
        assertEquals(1, countInstances(MoveFloatRegToFloatReg::class.java, lines))
        // Two calls: sin and exit
        assertCodeLines(lines, 1, 2, 1, 2)
        assertTrue(hasIndirectCallTo(lines, FUN_SIN.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithIntegerCastToFloat() {
        val expression = FunctionCallExpression(0, 0, FUN_SIN.identifier, listOf(IL_4))
        val assignStatement = AssignStatement(0, 0, INE_F64_F, expression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        // Two moves: exit code and integer literal
        assertEquals(2, countInstances(MoveImmToReg::class.java, lines))
        // One conversion: integer literal to float
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        // One move: result to non-volatile float register
        assertEquals(1, countInstances(MoveFloatRegToFloatReg::class.java, lines))
        // Two calls: sin and exit
        assertCodeLines(lines, 1, 2, 1, 2)
        assertTrue(hasIndirectCallTo(lines, FUN_SIN.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithFloatCastToInteger() {
        val expression = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(FL_3_14))
        val assignStatement = AssignStatement(0, 0, INE_I64_A, expression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        // One move: float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
        // One conversion: float literal to integer
        assertEquals(1, countInstances(RoundFloatRegToIntReg::class.java, lines))
        // Two moves: save base pointer and result to non-volatile integer register
        assertEquals(2, countInstances(MoveRegToReg::class.java, lines))
        // One move: float literal
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
        // Two calls: abs and exit
        assertCodeLines(lines, 1, 2, 1, 2)
        assertTrue(hasIndirectCallTo(lines, FUN_ABS.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithArray() {
        val dimStatement = VariableDeclarationStatement(0, 0, listOf(DECL_ARR_I64_X))
        val expression = FunctionCallExpression(0, 0, FUN_LBOUND.identifier, listOf(INE_ARR_I64_X))
        val assignStatement = AssignStatement(0, 0, INE_I64_H, expression)

        val result = assembleProgram(listOf(dimStatement, assignStatement))
        val lines = result.lines()

        // Two moves: address to array and exit code
        assertEquals(2, countInstances(MoveImmToReg::class.java, lines))
        // Move address to array
        assertEquals(1, lines
            .filterIsInstance<MoveImmToReg>()
            .count { it.immediate == IDENT_ARR_I64_X.mappedName })

        // Two calls: lbound and exit
        assertCodeLines(lines, 2, 2, 1, 2)
        assertTrue(hasIndirectCallTo(lines, FUN_LBOUND.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithArrayAndInteger() {
        val dimStatement = VariableDeclarationStatement(0, 0, listOf(DECL_ARR_I64_X))
        val expression = FunctionCallExpression(0, 0, FUN_LBOUND_I64.identifier, listOf(INE_ARR_I64_X, IL_3))
        val assignStatement = AssignStatement(0, 0, INE_I64_H, expression)

        val result = assembleProgram(listOf(dimStatement, assignStatement))
        val lines = result.lines()

        // Three moves: address to array, integer argument, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, lines))
        // Move address to array
        assertEquals(1, lines
            .filterIsInstance<MoveImmToReg>()
            .count { it.immediate == IDENT_ARR_I64_X.mappedName })

        // Two calls: lbound and exit
        assertCodeLines(lines, 2, 2, 1, 2)
        assertTrue(hasIndirectCallTo(lines, FUN_LBOUND_I64.mappedName))
    }

    @Test
    fun shouldGenerateCallToFloatToIntFunction() {
        val expression = FunctionCallExpression(0, 0, FUN_CINT.identifier, listOf(FL_3_14))
        val assignStatement = AssignStatement(0, 0, INE_I64_A, expression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        // One move: exit code
        assertEquals(1, countInstances(MoveImmToReg::class.java, lines))
        // One move: float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
        // One move: assignment
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
        // Two calls: cint and exit (in different libraries)
        assertCodeLines(lines, 2, 2, 1, 2)
        assertTrue(hasIndirectCallTo(lines, FUN_CINT.mappedName))
    }

    @Test
    fun shouldGenerateVarargsFunctionCall() {
        // The varargs function call will be to printf
        val printStatement = PrintStatement(0, 0, listOf(FL_3_14, IL_1))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        // Three moves: format string, integer literal, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, lines))
        // One move: float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
        // Two calls: printf and exit
        assertCodeLines(lines, 1, 2, 1, 2)
        assertTrue(hasIndirectCallTo(lines, FUN_PRINTF_STR_VAR.mappedName))
    }

    @Test
    fun shouldGenerateNestedFunctionCall() {
        val fe1 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(IL_1))
        val fe2 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(fe1))
        val fe3 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(fe2))
        val ps = PrintStatement(0, 0, listOf(fe3))

        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Three moves: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, lines))
        // Five calls: abs*3, printf, and exit
        assertCodeLines(lines, 1, 3, 1, 5)
    }

    @Test
    fun shouldGenerateDeeplyNestedFunctionCall() {
        val fe1 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(IL_1))
        val fe2 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(fe1))
        val fe3 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(fe2))
        val fe4 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(fe3))
        val fe5 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(fe4))
        val fe6 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(fe5))
        val ps = PrintStatement(0, 0, listOf(fe6))

        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Three moves: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, lines))
        // Eight calls: abs*6, printf, and exit
        assertCodeLines(lines, 1, 3, 1, 8)
    }

    /**
     * Tests that we can encode a deeply nested function call to a function with many arguments,
     * even though we run out of registers to store evaluated arguments in. In that case, temporary
     * variables (memory addresses) will be used instead.
     */
    @Test
    fun shouldGenerateNestedFunctionCallWithManyIntArgs() {
        val fe1 = FunctionCallExpression(0, 0, IDENT_FUN_FOO, listOf(IL_1, IL_2, IL_1))
        val fe2 = FunctionCallExpression(0, 0, IDENT_FUN_FOO, listOf(IL_3, IL_4, IL_3))
        val fe3 = FunctionCallExpression(0, 0, IDENT_FUN_FOO, listOf(fe1, fe2, IL_2))
        val fe4 = FunctionCallExpression(0, 0, IDENT_FUN_FOO, listOf(fe1, fe2, IL_4))
        val fe5 = FunctionCallExpression(0, 0, IDENT_FUN_FOO, listOf(fe3, fe4, IL_1))
        val fe6 = FunctionCallExpression(0, 0, IDENT_FUN_FOO, listOf(fe5, IL_4, IL_3))
        val ps = PrintStatement(0, 0, listOf(fe6, fe6, fe6))

        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // We should be able to find at least one case where an evaluated argument is moved to and from a temporary variable
        assertTrue(lines.filterIsInstance<DataDefinition>().any { it.identifier().mappedName.startsWith("__tmp") })
        assertTrue(lines.filterIsInstance<MoveRegToMem>().any { it.destination.startsWith("[__tmp") }) // Mapped name
        assertTrue(lines.filterIsInstance<MoveMemToReg>().any { it.source.startsWith("[__tmp") }) // Mapped name
    }

    /**
     * Tests that we can encode a deeply nested function call to a function with many float arguments.
     */
    @Test
    fun shouldGenerateNestedFunctionCallWithManyFloatArgs() {
        val fe1 = FunctionCallExpression(0, 0, IDENT_FUN_FLO, listOf(FL_3_14, FL_17_E4, FL_3_14))
        val fe2 = FunctionCallExpression(0, 0, IDENT_FUN_FLO, listOf(FL_3_14, FL_17_E4, FL_3_14))
        val fe3 = FunctionCallExpression(0, 0, IDENT_FUN_FLO, listOf(fe1, fe2, FL_17_E4))
        val fe4 = FunctionCallExpression(0, 0, IDENT_FUN_FLO, listOf(fe1, fe2, FL_3_14))
        val fe5 = FunctionCallExpression(0, 0, IDENT_FUN_FLO, listOf(fe3, fe4, FL_17_E4))
        val fe6 = FunctionCallExpression(0, 0, IDENT_FUN_FLO, listOf(fe5, FL_3_14, FL_17_E4))
        val ps = PrintStatement(0, 0, listOf(fe6, fe6, fe6))

        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // We should be able to find at least one case where an evaluated argument is moved to and from a temporary variable
        // This is used for parameter passing to the printf function to move values from float register to g.p. register
        assertTrue(lines.filterIsInstance<DataDefinition>().any { it.identifier().mappedName.startsWith("__tmp") })
        assertTrue(lines.filterIsInstance<MoveFloatRegToMem>().any { it.destination.startsWith("[__tmp") }) // Mapped name
        assertTrue(lines.filterIsInstance<MoveMemToReg>().any { it.source.startsWith("[__tmp") }) // Mapped name
    }

    @Test
    fun shouldGenerateFunctionCallWithDefinedType() {
        val ds = DefStrStatement(0, 0, setOf('b'))
        val fe = FunctionCallExpression(0, 0, FUN_LEN.identifier, listOf(IDE_STR_B))
        val ps = PrintStatement(0, 0, listOf(fe))

        val result = assembleProgram(listOf(ds, ps))
        val lines = result.lines()

        // Two moves: format string and exit code
        assertEquals(2, countInstances(MoveImmToReg::class.java, lines))
        // One move: variable b
        assertEquals(1, countInstances(MoveMemToReg::class.java, lines))
        // One data definition: variable b of type string
        assertTrue(lines.filterIsInstance<DataDefinition>().any { it.type() is Str && it.identifier().mappedName == IDENT_STR_B.mappedName })
        // Three calls: len, printf, and exit
        assertCodeLines(lines, 1, 3, 1, 3)
        assertTrue(hasIndirectCallTo(lines, FUN_LEN.mappedName))
    }

    @Test
    fun printfFormatStringIsEvaluatedLater() {
        // Given
        val ps = PrintStatement(0, 0, listOf(IL_1))

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, lines.filterIsInstance<Comment>().count { it.toText().contains("Defer evaluation of argument 0: _fmt_I64") })
        assertEquals(1, lines.filterIsInstance<Comment>().count { it.toText().contains("Defer evaluation of argument 1: 1") })
    }
}
