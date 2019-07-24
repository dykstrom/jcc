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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.basic.ast.DefStrStatement
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.*
import se.dykstrom.jcc.common.assembly.base.Code
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.instruction.floating.ConvertIntRegToFloatReg
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveFloatRegToFloatReg
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveFloatRegToMem
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveMemToFloatReg
import se.dykstrom.jcc.common.assembly.other.DataDefinition
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_PRINTF
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str

class BasicCodeGeneratorFunctionTests : AbstractBasicCodeGeneratorTest() {

    @Before
    fun setUp() {
        // Define some functions for testing
        defineFunction(FUN_ABS)
        defineFunction(FUN_CINT)
        defineFunction(FUN_FOO)
        defineFunction(FUN_FLO)
        defineFunction(FUN_LEN)
        defineFunction(FUN_SGN)
        defineFunction(FUN_SIN)
    }

    @Test
    fun shouldGenerateSingleFunctionCallWithInt() {
        val fe = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(IL_1))
        val ps = PrintStatement(0, 0, listOf(fe))

        val result = assembleProgram(listOf(ps))
        val codes = result.codes()

        // Three moves: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, codes))
        // Three calls: abs, printf, and exit
        assertCodes(codes, 1, 3, 1, 3)
        assertTrue(hasIndirectCallTo(codes, FUN_ABS.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithString() {
        val fe = FunctionCallExpression(0, 0, FUN_LEN.identifier, listOf(SL_ONE))
        val ps = PrintStatement(0, 0, listOf(fe))

        val result = assembleProgram(listOf(ps))
        val codes = result.codes()

        // Three moves: format string, string expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, codes))
        // Three calls: len, printf, and exit
        assertCodes(codes, 1, 3, 1, 3)
        assertTrue(hasIndirectCallTo(codes, FUN_LEN.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithFloat() {
        val expression = FunctionCallExpression(0, 0, FUN_SIN.identifier, listOf(FL_3_14))
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, expression)

        val result = assembleProgram(listOf(assignStatement))
        val codes = result.codes()

        // One move: exit code
        assertEquals(1, countInstances(MoveImmToReg::class.java, codes))
        // One move: float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, codes))
        // Two moves: argument to argument passing float register, and result to non-volatile float register
        assertEquals(2, countInstances(MoveFloatRegToFloatReg::class.java, codes))
        // Two calls: sin and exit
        assertCodes(codes, 1, 2, 1, 2)
        assertTrue(hasIndirectCallTo(codes, FUN_SIN.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithIntegerCastToFloat() {
        val expression = FunctionCallExpression(0, 0, FUN_SIN.identifier, listOf(IL_4))
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, expression)

        val result = assembleProgram(listOf(assignStatement))
        val codes = result.codes()

        // Two moves: exit code and integer literal
        assertEquals(2, countInstances(MoveImmToReg::class.java, codes))
        // One conversion: integer literal to float
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        // One move: result to non-volatile float register
        assertEquals(1, countInstances(MoveFloatRegToFloatReg::class.java, codes))
        // Two calls: sin and exit
        assertCodes(codes, 1, 2, 1, 2)
        assertTrue(hasIndirectCallTo(codes, FUN_SIN.mappedName))
    }

    @Test
    fun shouldGenerateCallToFloatToIntFunction() {
        val expression = FunctionCallExpression(0, 0, FUN_CINT.identifier, listOf(FL_3_14))
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, expression)

        val result = assembleProgram(listOf(assignStatement))
        val codes = result.codes()

        // One move: exit code
        assertEquals(1, countInstances(MoveImmToReg::class.java, codes))
        // One move: float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, codes))
        // One move: argument to argument passing float register
        assertEquals(1, countInstances(MoveFloatRegToFloatReg::class.java, codes))
        // Two calls: sin and exit
        assertCodes(codes, 1, 1, 2, 2)
        // CINT is an assembly function, which makes the call direct
        assertTrue(hasDirectCallTo(codes, FUN_CINT.mappedName))
    }

    @Test
    fun shouldGenerateVarargsFunctionCall() {
        // The varargs function call will be to printf
        val printStatement = PrintStatement(0, 0, listOf(FL_3_14, IL_1))

        val result = assembleProgram(listOf(printStatement))
        val codes = result.codes()

        // Three moves: format string, integer literal, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, codes))
        // One move: float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, codes))
        // One move: argument to argument passing float register
        assertEquals(1, countInstances(MoveFloatRegToFloatReg::class.java, codes))
        // Two calls: printf and exit
        assertCodes(codes, 1, 2, 1, 2)
        assertTrue(hasIndirectCallTo(codes, FUN_PRINTF.name))
    }

    @Test
    fun shouldGenerateNestedFunctionCall() {
        val fe1 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(IL_1))
        val fe2 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(fe1))
        val fe3 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(fe2))
        val ps = PrintStatement(0, 0, listOf(fe3))

        val result = assembleProgram(listOf(ps))
        val codes = result.codes()

        // Three moves: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, codes))
        // Five calls: abs*3, printf, and exit
        assertCodes(codes, 1, 3, 1, 5)
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
        val codes = result.codes()

        // Three moves: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, codes))
        // Eight calls: abs*6, printf, and exit
        assertCodes(codes, 1, 3, 1, 8)
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
        val codes = result.codes()

        // We should be able to find at least one case where an evaluated argument is moved to and from a temporary variable
        assertTrue(codes.filterIsInstance<DataDefinition>().any { it.identifier.mappedName.startsWith("__tmp") })
        assertTrue(codes.filterIsInstance<MoveRegToMem>().any { it.destination.startsWith("[__tmp") }) // Mapped name
        assertTrue(codes.filterIsInstance<MoveMemToReg>().any { it.source.startsWith("[__tmp") }) // Mapped name
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
        val codes = result.codes()

        // We should be able to find at least one case where an evaluated argument is moved to and from a temporary variable
        // This is used for parameter passing to the printf function to move values from float register to g.p. register
        assertTrue(codes.filterIsInstance<DataDefinition>().any { it.identifier.mappedName.startsWith("__tmp") })
        assertTrue(codes.filterIsInstance<MoveFloatRegToMem>().any { it.destination.startsWith("[__tmp") }) // Mapped name
        assertTrue(codes.filterIsInstance<MoveMemToReg>().any { it.source.startsWith("[__tmp") }) // Mapped name
    }

    @Test
    fun shouldGenerateFunctionCallToAssemblyFunction() {
        val fe = FunctionCallExpression(0, 0, FUN_CINT.identifier, listOf(IL_1))
        val ps = PrintStatement(0, 0, listOf(fe))

        val result = assembleProgram(listOf(ps))
        val codes = result.codes()

        // Three moves in main program: format string, integer expression, and exit code
        assertEquals(3, countInstances(MoveImmToReg::class.java, codes))
        // One return from function
        assertEquals(1, countInstances(Ret::class.java, codes))
        // Three calls: cint, printf, and exit
        // Two labels: main, cint
        assertCodes(codes, 1, 2, 2, 3)
        // cint is an assembly function, which makes the call direct
        assertTrue(hasDirectCallTo(codes, FUN_CINT.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithUndefined() {
        val fe = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(IDE_UNK_U))
        val ps = PrintStatement(0, 0, listOf(fe))

        val result = assembleProgram(listOf(ps))
        val codes = result.codes()

        // Two moves: format string and exit code
        assertEquals(2, countInstances(MoveImmToReg::class.java, codes))
        // One move: variable u
        assertEquals(1, countInstances(MoveMemToReg::class.java, codes))
        // Three calls: abs, printf, and exit
        assertCodes(codes, 1, 3, 1, 3)
        assertTrue(hasIndirectCallTo(codes, FUN_ABS.mappedName))
    }

    @Test
    fun shouldGenerateFunctionCallWithDefinedType() {
        val ds = DefStrStatement(0, 0, setOf('u'))
        val fe = FunctionCallExpression(0, 0, FUN_LEN.identifier, listOf(IDE_UNK_U))
        val ps = PrintStatement(0, 0, listOf(fe))

        val result = assembleProgram(listOf(ds, ps))
        val codes = result.codes()

        // Two moves: format string and exit code
        assertEquals(2, countInstances(MoveImmToReg::class.java, codes))
        // One move: variable u
        assertEquals(1, countInstances(MoveMemToReg::class.java, codes))
        // One data definition: variable u of type string
        assertTrue(codes.filterIsInstance<DataDefinition>().any { it.type is Str && it.identifier.mappedName == IDENT_UNK_U.mappedName })
        // Three calls: len, printf, and exit
        assertCodes(codes, 1, 3, 1, 3)
        assertTrue(hasIndirectCallTo(codes, FUN_LEN.mappedName))
    }

    private fun hasIndirectCallTo(codes: List<Code>, mappedName: String) =
        codes.filter { it is CallIndirect }.map { it as CallIndirect }.any { it.target.contains(mappedName) }

    private fun hasDirectCallTo(codes: List<Code>, mappedName: String) =
        codes.filter { it is CallDirect }.map { it as CallDirect }.any { it.target.contains(mappedName) }

    companion object {

        private val FUN_FOO = LibraryFunction("foo", listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_LIBC, ExternalFunction("fooo"))
        private val FUN_FLO = LibraryFunction("flo", listOf(F64.INSTANCE, F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, ExternalFunction("floo"))

        private val IDENT_FUN_FOO = FUN_FOO.identifier
        private val IDENT_FUN_FLO = FUN_FLO.identifier
    }
}
