/*
 * Copyright (C) 2023 Johan Dykstrom
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

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FL_2_0
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_F64_TO_F64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_F64_TO_I64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_I64_F64_I64_F64_I64_F64_TO_F64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_I64_F64_TO_F64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_I64_TO_I64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_I64_TO_STR
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_STR_TO_STR
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_TO_F64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_TO_I64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.SL_A
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.SL_B
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.hasDirectCallTo
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.hasIndirectCallTo
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.*
import se.dykstrom.jcc.common.assembly.base.Label
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.instruction.floating.*
import se.dykstrom.jcc.common.assembly.other.DataDefinition
import se.dykstrom.jcc.common.assembly.other.Import
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.functions.BuiltInFunctions.*
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str

class BasicCodeGeneratorUserFunctionTests : AbstractBasicCodeGeneratorTest() {

    @Before
    fun setUp() {
        // Define some functions for testing
        defineFunction(FUN_CHR)
        defineFunction(FUN_CINT)
        defineFunction(FUN_HEX)
    }

    @Test
    fun shouldGenerateNoArgToI64DefFnExpression() {
        // Given
        val ident = Identifier("FNbar", FUN_TO_I64)
        val fds = FunctionDefinitionStatement(0, 0, ident, listOf(), IL_1)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertCodeLines(lines, 1, 1, 2, 1)
        assertTrue(lines.filterIsInstance<MoveRegToReg>().any { it.destination == "rax" })
        assertEquals(1, countInstances(Ret::class.java, lines))
    }

    @Test
    fun shouldGenerateNoArgToF64DefFnExpression() {
        // Given
        val ident = Identifier("FNbar", FUN_TO_F64)
        val fds = FunctionDefinitionStatement(0, 0, ident, listOf(), FL_3_14)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertCodeLines(lines, 1, 1, 2, 1)
        assertTrue(lines.filterIsInstance<MoveFloatRegToFloatReg>().any { it.destination == "xmm0" })
        assertEquals(1, countInstances(Ret::class.java, lines))
    }

    @Test
    fun shouldGenerateI64ToI64DefFnExpression() {
        // Given
        val ident = Identifier("FNbar", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, ident, declarations, IL_1)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertFalse(symbols.contains("x"))
        assertCodeLines(lines, 1, 1, 2, 1)
        assertTrue(lines.filterIsInstance<MoveRegToReg>().any { it.destination == "rax" })
    }

    @Test
    fun shouldReturnGlobalI64Variable() {
        // Given
        val varDeclarations = listOf(Declaration(0, 0, "a%", I64.INSTANCE))
        val vds = VariableDeclarationStatement(0, 0, varDeclarations)
        val functionIdent = Identifier("FNbar", FUN_I64_TO_I64)
        val argDeclarations = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, functionIdent, argDeclarations, IDE_I64_A)

        // When
        val result = assembleProgram(listOf(vds, fds))
        val lines = result.lines()

        // Then
        assertTrue(symbols.contains("a%"))
        assertFalse(symbols.contains("x"))
        assertCodeLines(lines, 1, 1, 2, 1)
        assertTrue(lines.filterIsInstance<MoveMemToReg>().any { it.source == "[_a%]" })
    }

    @Test
    fun shouldReturnI64Argument() {
        // Given
        val functionIdent = Identifier("FNbar", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val ide = IdentifierDerefExpression(0, 0, Identifier("x", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, functionIdent, declarations, ide)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(lines.filterIsInstance<MoveMemToReg>().any { it.source == "[rbp+10h]" })
    }

    @Test
    fun shouldReturnF64Argument() {
        // Given
        val functionIdent = Identifier("FNbar", FUN_F64_TO_F64)
        val declarations = listOf(Declaration(0, 0, "x", F64.INSTANCE))
        val ide = IdentifierDerefExpression(0, 0, Identifier("x", F64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, functionIdent, declarations, ide)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(lines.filterIsInstance<MoveMemToFloatReg>().any { it.source == "[rbp+10h]" })
    }

    @Test
    fun shouldPushAndPopUsedRegisters() {
        // Given
        val functionIdent = Identifier("FNbar", FUN_I64_F64_TO_F64)
        val declarations = listOf(
            Declaration(0, 0, "x", I64.INSTANCE),
            Declaration(0, 0, "y", F64.INSTANCE)
        )
        val ae = AddExpression(0, 0, IL_1, FL_3_14)
        val fds = FunctionDefinitionStatement(0, 0, functionIdent, declarations, ae)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        // Save 2 XMM registers
        assertEquals(2, lines.filterIsInstance<MoveDquFloatRegToMem>().count { it.destination == "[rsp]" })
        // Restore 2 XMM registers
        assertEquals(2, lines.filterIsInstance<MoveDquMemToFloatReg>().count { it.source == "[rsp]" })
        // Save RBX in main and in function
        assertEquals(2, lines.filterIsInstance<PushReg>().count { it.toText() == "push rbx" })
        // Restore RBX in function
        assertEquals(1, lines.filterIsInstance<PopReg>().count { it.toText() == "pop rbx" })
        // Save float arg to home location
        assertEquals(1, lines.filterIsInstance<MoveFloatRegToMem>().count { it.source == "xmm1" })
        // Save int arg to home location
        assertEquals(1, lines.filterIsInstance<MoveRegToMem>().count { it.source == "rcx" })
    }

    @Test
    fun shouldCallUserDefinedI64Function() {
        // Given
        val identBar = Identifier("FNbar#", FUN_I64_F64_TO_F64)
        val declarationsBar = listOf(
            Declaration(0, 0, "x", I64.INSTANCE),
            Declaration(0, 0, "y", F64.INSTANCE)
        )
        val ide = IdentifierDerefExpression(0, 0, Identifier("y", F64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identBar, declarationsBar, ide)

        val fce = FunctionCallExpression(0, 0, identBar, listOf(IL_M1, FL_17_E4))
        val ps = PrintStatement(0, 0, listOf(fce))

        // When
        val result = assembleProgram(listOf(fds, ps))
        val lines = result.lines()
        val funBar = symbols.getFunction(identBar.name(), FUN_I64_F64_TO_F64.argTypes)

        // Then
        assertTrue(hasDirectCallTo(lines, funBar.mappedName))
        // Move return value to some location
        assertEquals(1, lines.filterIsInstance<MoveFloatRegToFloatReg>().count { it.source == "xmm0" })
    }

    @Test
    fun functionCallsBuiltInF64Function() {
        // Given
        val functionIdent = Identifier("FNbar", FUN_F64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "x", F64.INSTANCE))
        val ide = IdentifierDerefExpression(0, 0, Identifier("x", F64.INSTANCE))
        val calledIdent = FUN_CINT.identifier
        val fce = FunctionCallExpression(0, 0, calledIdent, listOf(ide))
        val fds = FunctionDefinitionStatement(0, 0, functionIdent, declarations, fce)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(hasIndirectCallTo(lines, FUN_CINT.mappedName))
        assertTrue(lines.filterIsInstance<Import>().any { it.functions.contains(FUN_EXIT.name) })
        assertTrue(lines.filterIsInstance<Import>().any { it.functions.contains(FUN_CINT.name) })
    }

    @Test
    fun functionCallsUserDefinedF64Function() {
        // Given
        val identBar = Identifier("FNbar", FUN_F64_TO_I64)
        val declarationsBar = listOf(Declaration(0, 0, "x", F64.INSTANCE))
        val fdsBar = FunctionDefinitionStatement(0, 0, identBar, declarationsBar, IL_1)

        val identFoo = Identifier("FNfoo", FUN_F64_TO_I64)
        val declarationsFoo = listOf(Declaration(0, 0, "x", F64.INSTANCE))
        val ide = IdentifierDerefExpression(0, 0, Identifier("x", F64.INSTANCE))
        val fce = FunctionCallExpression(0, 0, identBar, listOf(ide))
        val fdsFoo = FunctionDefinitionStatement(0, 0, identFoo, declarationsFoo, fce)

        // When
        val result = assembleProgram(listOf(fdsBar, fdsFoo))
        val lines = result.lines()
        val funBar = symbols.getFunction(identBar.name(), FUN_F64_TO_I64.argTypes)

        // Then
        assertTrue(hasDirectCallTo(lines, funBar.mappedName))
        // Save RBX in main and in two functions
        assertEquals(3, lines.filterIsInstance<PushReg>().count { it.toText() == "push rbx" })
        // Restore RBX in two functions
        assertEquals(2, lines.filterIsInstance<PopReg>().count { it.toText() == "pop rbx" })
    }

    @Test
    fun shouldCallUserDefinedFunctionWithMoreThanFourParameters() {
        // Given
        val identBar = Identifier("FNbar", FUN_I64_F64_I64_F64_I64_F64_TO_F64)
        val declarationsBar = listOf(
            Declaration(0, 0, "a", I64.INSTANCE),
            Declaration(0, 0, "b", F64.INSTANCE),
            Declaration(0, 0, "c", I64.INSTANCE),
            Declaration(0, 0, "d", F64.INSTANCE),
            Declaration(0, 0, "e", I64.INSTANCE),
            Declaration(0, 0, "f", F64.INSTANCE),
        )
        val ideA = IdentifierDerefExpression(0, 0, Identifier("a", I64.INSTANCE))
        val ideB = IdentifierDerefExpression(0, 0, Identifier("b", F64.INSTANCE))
        val ideC = IdentifierDerefExpression(0, 0, Identifier("c", I64.INSTANCE))
        val ideD = IdentifierDerefExpression(0, 0, Identifier("d", F64.INSTANCE))
        val ideE = IdentifierDerefExpression(0, 0, Identifier("e", I64.INSTANCE))
        val ideF = IdentifierDerefExpression(0, 0, Identifier("f", F64.INSTANCE))
        val aeAB = AddExpression(0, 0, ideA, ideB)
        val aeCD = AddExpression(0, 0, ideC, ideD)
        val aeEF = AddExpression(0, 0, ideE, ideF)
        val aeABCD = AddExpression(0, 0, aeAB, aeCD)
        val aeABCDEF = AddExpression(0, 0, aeABCD, aeEF)
        val fds = FunctionDefinitionStatement(0, 0, identBar, declarationsBar, aeABCDEF)

        val fce = FunctionCallExpression(0, 0, identBar, listOf(IL_0, FL_3_14, IL_1, FL_3_14, IL_2, FL_3_14))
        val ps = PrintStatement(0, 0, listOf(fce))

        // When
        val result = assembleProgram(listOf(fds, ps))
        val lines = result.lines()
        val funBar = symbols.getFunction(identBar.name(), FUN_I64_F64_I64_F64_I64_F64_TO_F64.argTypes)

        // Then
        assertTrue(hasDirectCallTo(lines, funBar.mappedName))
        // Move return value to some location
        assertEquals(1, lines.filterIsInstance<MoveFloatRegToFloatReg>().count { it.source == "xmm0" })
        // Save arguments in home locations
        assertTrue(lines.filterIsInstance<MoveRegToMem>().any { it.destination == "[rbp+10h]" })
        assertTrue(lines.filterIsInstance<MoveFloatRegToMem>().any { it.destination == "[rbp+18h]" })
        assertTrue(lines.filterIsInstance<MoveRegToMem>().any { it.destination == "[rbp+20h]" })
        assertTrue(lines.filterIsInstance<MoveFloatRegToMem>().any { it.destination == "[rbp+28h]" })
        // Access arguments
        assertTrue(lines.filterIsInstance<MoveMemToReg>().any { it.source == "[rbp+10h]" })
        assertTrue(lines.filterIsInstance<MoveMemToFloatReg>().any { it.source == "[rbp+18h]" })
        assertTrue(lines.filterIsInstance<MoveMemToReg>().any { it.source == "[rbp+20h]" })
        assertTrue(lines.filterIsInstance<MoveMemToFloatReg>().any { it.source == "[rbp+28h]" })
        assertTrue(lines.filterIsInstance<MoveMemToReg>().any { it.source == "[rbp+30h]" })
        assertTrue(lines.filterIsInstance<MoveMemToFloatReg>().any { it.source == "[rbp+38h]" })
    }

    @Test
    fun twoFunctionsWithSameNameDifferentSignature() {
        // Given
        val ident1 = Identifier("FNbar", FUN_F64_TO_F64)
        val declarations1 = listOf(Declaration(0, 0, "x", F64.INSTANCE))
        val fds1 = FunctionDefinitionStatement(0, 0, ident1, declarations1, FL_2_0)

        val ident2 = Identifier("FNbar", FUN_I64_F64_TO_F64)
        val declarations2 = listOf(
            Declaration(0, 0, "x", I64.INSTANCE),
            Declaration(0, 0, "y", F64.INSTANCE)
        )
        val fds2 = FunctionDefinitionStatement(0, 0, ident2, declarations2, FL_3_14)

        val fce1 = FunctionCallExpression(0, 0, ident1, listOf(FL_2_0))
        val fce2 = FunctionCallExpression(0, 0, ident2, listOf(IL_M1, FL_17_E4))
        val ps1 = PrintStatement(0, 0, listOf(fce1))
        val ps2 = PrintStatement(0, 0, listOf(fce2))

        // When
        val result = assembleProgram(listOf(fds1, fds2, ps1, ps2))
        val lines = result.lines()
        val fun1 = symbols.getFunction(ident1.name(), FUN_F64_TO_F64.argTypes)
        val fun2 = symbols.getFunction(ident2.name(), FUN_I64_F64_TO_F64.argTypes)

        // Then
        // One call each to the user-defined functions
        assertTrue(hasDirectCallTo(lines, fun1.mappedName))
        assertTrue(hasDirectCallTo(lines, fun2.mappedName))
        // One label each for the user-defined functions
        assertEquals(1, lines.filterIsInstance<Label>().count { it.name == fun1.mappedName })
        assertEquals(1, lines.filterIsInstance<Label>().count { it.name == fun2.mappedName })
    }

    @Test
    fun shouldCallUserDefinedI64ToStrFunction() {
        // Given
        val identBar = Identifier("FNbar$", FUN_I64_TO_STR)
        val declarationsBar = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identBar, declarationsBar, SL_A)

        val fce = FunctionCallExpression(0, 0, identBar, listOf(IL_M1))
        val ps = PrintStatement(0, 0, listOf(fce))

        // When
        val result = assembleProgram(listOf(fds, ps))
        val lines = result.lines()
        val funBar = symbols.getFunction(identBar.name(), FUN_I64_TO_STR.argTypes)

        // Then
        assertTrue(hasDirectCallTo(lines, funBar.mappedName))
        // Allocate memory for function return value
        assertTrue(hasIndirectCallTo(lines, FUN_STRDUP.mappedName))
        // Deallocate memory after it has been used
        assertTrue(hasIndirectCallTo(lines, FUN_FREE.mappedName))
    }

    @Test
    fun shouldCallUserDefinedStrToStrFunction() {
        // Given
        val identBar = Identifier("FNbar$", FUN_STR_TO_STR)
        val declarationsBar = listOf(Declaration(0, 0, "b$", Str.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identBar, declarationsBar, IDE_STR_B)

        val fce = FunctionCallExpression(0, 0, identBar, listOf(SL_A))
        val ps = PrintStatement(0, 0, listOf(fce))

        // When
        val result = assembleProgram(listOf(fds, ps))
        val lines = result.lines()
        val funBar = symbols.getFunction(identBar.name(), FUN_STR_TO_STR.argTypes)

        // Then
        assertTrue(hasDirectCallTo(lines, funBar.mappedName))
        // Allocate memory for function return value
        assertTrue(hasIndirectCallTo(lines, FUN_STRDUP.mappedName))
        // Deallocate memory after it has been used
        assertTrue(hasIndirectCallTo(lines, FUN_FREE.mappedName))
    }

    @Test
    fun functionReturnsStringAddition() {
        // Given
        val identBar = Identifier("FNbar$", FUN_STR_TO_STR)
        val declarationsBar = listOf(Declaration(0, 0, "b$", Str.INSTANCE))
        val expressionBar = AddExpression(0, 0, SL_A, IDE_STR_B)
        val fds = FunctionDefinitionStatement(0, 0, identBar, declarationsBar, expressionBar)

        val fce = FunctionCallExpression(0, 0, identBar, listOf(SL_B))
        val ps = PrintStatement(0, 0, listOf(fce))

        // When
        val result = assembleProgram(listOf(fds, ps))
        val lines = result.lines()
        val funBar = symbols.getFunction(identBar.name(), FUN_STR_TO_STR.argTypes)

        // Then
        assertTrue(hasDirectCallTo(lines, funBar.mappedName))
        // Do not allocate memory for function return value, because it was already done by add
        assertFalse(hasIndirectCallTo(lines, FUN_STRDUP.mappedName))
        // Deallocate memory after it has been used
        assertTrue(hasIndirectCallTo(lines, FUN_FREE.mappedName))
        // Data section should have definitions for both literals "A" and "B"
        assertTrue(lines.filterIsInstance<DataDefinition>().any { it.value == "\"A\",0" })
        assertTrue(lines.filterIsInstance<DataDefinition>().any { it.value == "\"B\",0" })
    }

    @Test
    fun functionReturnsGlobalStrVariable() {
        // Given
        val varDeclaration = listOf(Declaration(0, 0, "b$", Str.INSTANCE))
        val vds = VariableDeclarationStatement(0, 0, varDeclaration)

        val identBar = Identifier("FNbar$", FUN_I64_TO_STR)
        val declarationsBar = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identBar, declarationsBar, IDE_STR_B)

        val fce = FunctionCallExpression(0, 0, identBar, listOf(IL_M1))
        val ps = PrintStatement(0, 0, listOf(fce))

        // When
        val result = assembleProgram(listOf(vds, fds, ps))
        val lines = result.lines()
        val funBar = symbols.getFunction(identBar.name(), FUN_I64_TO_STR.argTypes)

        // Then
        assertTrue(hasDirectCallTo(lines, funBar.mappedName))
        // Assign global variable to return value
        assertTrue(lines.filterIsInstance<MoveMemToReg>().any { it.source == "[_b$]" })
        // Allocate memory for function return value
        assertTrue(hasIndirectCallTo(lines, FUN_STRDUP.mappedName))
        // Deallocate memory after it has been used
        assertTrue(hasIndirectCallTo(lines, FUN_FREE.mappedName))
    }

    @Test
    fun functionCallsBuiltInStrFunction() {
        // Given
        val functionIdent = Identifier("FNbar$", FUN_I64_TO_STR)
        val declarations = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val ide = IdentifierDerefExpression(0, 0, Identifier("x", I64.INSTANCE))
        val calledIdent = FUN_HEX.identifier
        val fce = FunctionCallExpression(0, 0, calledIdent, listOf(ide))
        val fds = FunctionDefinitionStatement(0, 0, functionIdent, declarations, fce)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(hasIndirectCallTo(lines, FUN_HEX.mappedName))
        // Do not allocate memory for function return value, because it was already done by hex$
        assertFalse(hasIndirectCallTo(lines, FUN_STRDUP.mappedName))
        // Do not deallocate memory after call to hex$
        assertFalse(hasIndirectCallTo(lines, FUN_FREE.mappedName))
    }

    @Test
    fun twoFunctionsReturnSameStrLiteral() {
        // Given
        val identBar = Identifier("FNbar$", FUN_I64_TO_STR)
        val declarationsBar = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val fdsBar = FunctionDefinitionStatement(0, 0, identBar, declarationsBar, SL_A)

        val identFoo = Identifier("FNfoo$", FUN_I64_TO_STR)
        val declarationsFoo = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val fdsFoo = FunctionDefinitionStatement(0, 0, identFoo, declarationsFoo, SL_A)

        // When
        val result = assembleProgram(listOf(fdsBar, fdsFoo))
        val lines = result.lines()

        // Then
        assertNotNull(symbols.getFunction(identBar.name(), FUN_I64_TO_STR.argTypes))
        assertNotNull(symbols.getFunction(identFoo.name(), FUN_I64_TO_STR.argTypes))
        // Allocate memory for function return value in two functions
        assertEquals(2, lines.filterIsInstance<CallIndirect>().count { it.target == "[${FUN_STRDUP.mappedName}]" })
        // Data section should have one definition for literal "A"
        assertEquals(1, lines.filterIsInstance<DataDefinition>().count { it.value == "\"A\",0" })
    }
}
