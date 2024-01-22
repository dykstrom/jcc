/*
 * Copyright (C) 2024 Johan Dykstrom
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_F64_TO_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_I64_F64_TO_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_I64_TO_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_TO_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IDE_I64_A
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_M_1
import se.dykstrom.jcc.common.assembly.base.Label
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg
import se.dykstrom.jcc.common.assembly.instruction.MoveMemToReg
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToReg
import se.dykstrom.jcc.common.assembly.instruction.Ret
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier

class ColCodeGeneratorUserFunctionTests : AbstractColCodeGeneratorTests() {

    @Test
    fun shouldGenerateNoArgToI64Function() {
        // Given
        val identifier = Identifier("foo", FUN_TO_I64)
        val fds = FunctionDefinitionStatement(0, 0, identifier, listOf(), IL_5)
        val udf = UserDefinedFunction(identifier.name(), listOf(), listOf(), FUN_TO_I64.returnType)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(lines.filterIsInstance<Label>().any { it.name == udf.mappedName })
        assertTrue(lines.filterIsInstance<MoveRegToReg>().any { it.destination == "rax" })
        assertEquals(1, countInstances(Ret::class, lines))
    }

    @Test
    fun shouldGenerateI64ToI64Function() {
        // Given
        val identifier = Identifier("foo", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "a", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)
        val udf = UserDefinedFunction(identifier.name(), listOf("a"), listOf(I64.INSTANCE), FUN_I64_TO_I64.returnType)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(lines.filterIsInstance<Label>().any { it.name == udf.mappedName })
        assertTrue(lines.filterIsInstance<MoveRegToReg>().any { it.destination == "rax" })
        assertEquals(1, countInstances(Ret::class, lines))
    }

    @Test
    fun shouldGenerateI64ToI64FunctionThatReturnsArg() {
        // Given
        val identifier = Identifier("foo", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "a", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IDE_I64_A)
        val udf = UserDefinedFunction(identifier.name(), listOf("a"), listOf(I64.INSTANCE), FUN_I64_TO_I64.returnType)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(lines.filterIsInstance<Label>().any { it.name == udf.mappedName })
        assertTrue(lines.filterIsInstance<MoveRegToReg>().any { it.destination == "rax" }) // Set return value
        assertTrue(lines.filterIsInstance<MoveMemToReg>().any { it.source == "[rbp+10h]" }) // Access argument
        assertEquals(1, countInstances(Ret::class, lines))
    }

    @Test
    fun shouldCallUserDefinedI64ToI64Function() {
        // Given
        val identifier = Identifier("foo", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_17)

        val fce = FunctionCallExpression(0, 0, identifier, listOf(IL_M_1))
        val ps = PrintlnStatement(0, 0, fce)

        // When
        val result = assembleProgram(listOf(fds, ps))
        val lines = result.lines()

        // Then
        val definedFunction = symbols.getFunction(identifier.name(), FUN_I64_TO_I64.argTypes)
        assertTrue(hasDirectCallTo(lines, definedFunction.mappedName))
        assertTrue(lines.filterIsInstance<MoveImmToReg>().any { it.source == IL_17.value })
    }

    @Test
    fun shouldGenerateFunctionWithFunctionTypeArg() {
        // Given
        val identifier = Identifier("foo", Fun.from(listOf(FUN_I64_TO_I64), I64.INSTANCE))
        val declarations = listOf(Declaration(0, 0, "a", FUN_I64_TO_I64))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)
        val udf = UserDefinedFunction(identifier.name(), listOf("a"), listOf(FUN_I64_TO_I64), I64.INSTANCE)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(lines.filterIsInstance<Label>().any { it.name == udf.mappedName })
        assertTrue(lines.filterIsInstance<Label>().any { it.name == "_foo_FunL\$I64\$RToI64" })
    }

    @Test
    fun shouldGenerateFunctionWithFunction2TypeArg() {
        // Given
        val identifier = Identifier("foo", Fun.from(listOf(FUN_I64_F64_TO_I64), I64.INSTANCE))
        val declarations = listOf(Declaration(0, 0, "a", FUN_I64_F64_TO_I64))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)
        val udf = UserDefinedFunction(identifier.name(), listOf("a"), listOf(FUN_I64_F64_TO_I64), I64.INSTANCE)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(lines.filterIsInstance<Label>().any { it.name == udf.mappedName })
        assertTrue(lines.filterIsInstance<Label>().any { it.name == "_foo_FunL\$I64\$F64\$RToI64" })
    }

    @Test
    fun shouldGenerateFunctionWithTwoFunctionTypeArgs() {
        // Given
        val argTypes = listOf(FUN_I64_F64_TO_I64, FUN_I64_TO_I64)
        val identifier = Identifier("foo", Fun.from(argTypes, I64.INSTANCE))
        val declarations = listOf(
            Declaration(0, 0, "a", FUN_I64_F64_TO_I64),
            Declaration(0, 0, "b", FUN_I64_TO_I64)
        )
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)
        val udf = UserDefinedFunction(identifier.name(), listOf("a", "b"), argTypes, I64.INSTANCE)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(lines.filterIsInstance<Label>().any { it.name == udf.mappedName })
        assertTrue(lines.filterIsInstance<Label>().any { it.name == "_foo_FunL\$I64\$F64\$RToI64_FunL\$I64\$RToI64" })
    }

    @Test
    fun shouldGenerateFunctionWithFunctionFunctionTypeArg() {
        // Given
        val functionType = Fun.from(listOf(FUN_F64_TO_I64), I64.INSTANCE)
        val identifier = Identifier("foo", Fun.from(listOf(functionType), I64.INSTANCE))
        val declarations = listOf(Declaration(0, 0, "a", functionType))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)
        val udf = UserDefinedFunction(identifier.name(), listOf("a"), listOf(functionType), I64.INSTANCE)

        // When
        val result = assembleProgram(listOf(fds))
        val lines = result.lines()

        // Then
        assertTrue(lines.filterIsInstance<Label>().any { it.name == udf.mappedName })
        assertTrue(lines.filterIsInstance<Label>().any { it.name == "_foo_FunL\$FunL\$F64\$RToI64\$RToI64" })
    }
}
