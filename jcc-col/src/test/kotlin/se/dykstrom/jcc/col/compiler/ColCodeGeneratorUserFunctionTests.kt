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
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.FL_2_0
import se.dykstrom.jcc.col.ColTests.Companion.FUN_F64_TO_I64
import se.dykstrom.jcc.col.ColTests.Companion.FUN_I64_F64_TO_I64
import se.dykstrom.jcc.col.ColTests.Companion.FUN_I64_TO_I64
import se.dykstrom.jcc.col.ColTests.Companion.FUN_TO_I64
import se.dykstrom.jcc.col.ColTests.Companion.IDE_I64_A
import se.dykstrom.jcc.col.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.ColTests.Companion.IL_M_1
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_I64
import se.dykstrom.jcc.common.ast.CastToFloatExpression
import se.dykstrom.jcc.common.ast.CastToIntExpression
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.functions.ReferenceFunction
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.types.F32
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier

internal class ColCodeGeneratorUserFunctionTests : AbstractColCodeGeneratorTests() {

    @Test
    fun shouldGenerateNoArgToI64Function() {
        // Given
        val identifier = Identifier("foo", FUN_TO_I64)
        val fds = FunctionDefinitionStatement(0, 0, identifier, listOf(), IL_5)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then
        assertContains(result, listOf(
            "define tailcc i64 @foo()",
            "ret i64 5",
        ))
    }

    @Test
    fun shouldGenerateI64ToI64Function() {
        // Given
        val identifier = Identifier("foo", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "a", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then
        assertContains(result, listOf(
            "define tailcc i64 @foo_I64(i64 %0)",
            "%_a = alloca i64",
            "store i64 %0, ptr %_a",
            "ret i64 5",
        ))
    }

    @Test
    fun shouldGenerateI64ToI64FunctionThatReturnsArg() {
        // Given
        val identifier = Identifier("foo", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "a", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IDE_I64_A)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then
        assertContains(result, listOf(
            "store i64 %0, ptr %_a",
            "%1 = load i64, ptr %_a",
            "ret i64 %1",
        ))
    }

    @Test
    fun shouldGenerateTwoArgFunction() {
        // Given
        val identifier = Identifier("foo", FUN_I64_F64_TO_I64)
        val declarations = listOf(
            Declaration(0, 0, "a", I64.INSTANCE),
            Declaration(0, 0, "b", F64.INSTANCE),
        )
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then
        assertContains(result, listOf(
            "define tailcc i64 @foo_I64_F64(i64 %0, double %1)",
            "%_a = alloca i64",
            "%_b = alloca double",
            "store i64 %0, ptr %_a",
            "store double %1, ptr %_b",
        ))
        // Stack space should be allocated only once
        assertEquals(1, result.lines().map { it.toText() }.count { it.contains("%_a = alloca i64") })
    }

    @Test
    fun shouldCallUserDefinedI64ToI64Function() {
        // Given
        val identifier = Identifier("foo", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_17)
        val udf = UserDefinedFunction("foo", listOf("x"), listOf(I64.INSTANCE), I64.INSTANCE)

        val fce = FunctionCallExpression(0, 0, identifier, listOf(IL_M_1), udf)
        val ps = funCall(BF_PRINTLN_I64, fce)

        // When
        val result = assembleProgram(cg, listOf(fds, ps))

        // Then
        assertContains(result, listOf(
            "define tailcc i64 @foo_I64(i64 %0)",
            "%0 = call tailcc i64 @foo_I64(i64 -1)",
        ))
    }

    @Test
    fun shouldGenerateFunctionWithFunctionTypeArg() {
        // Given
        val identifier = Identifier("foo", Fun.from(listOf(FUN_I64_TO_I64), I64.INSTANCE))
        val declarations = listOf(Declaration(0, 0, "a", FUN_I64_TO_I64))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then
        assertContains(result, listOf(
            $$"define tailcc i64 @foo_FunL$I64$R.toI64(ptr %0)",
            "%_a = alloca ptr",
            "store ptr %0, ptr %_a",
        ))
    }

    @Test
    fun shouldGenerateFunctionWithFunction2TypeArg() {
        // Given
        val identifier = Identifier("foo", Fun.from(listOf(FUN_I64_F64_TO_I64), I64.INSTANCE))
        val declarations = listOf(Declaration(0, 0, "a", FUN_I64_F64_TO_I64))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then
        assertContains(result, listOf(
            $$"define tailcc i64 @foo_FunL$I64.F64$R.toI64(ptr %0)", // All function pointers are the same, regardless of their type
            "%_a = alloca ptr",
            "store ptr %0, ptr %_a",
        ))
    }

    @Test
    fun shouldGenerateFunctionWithTwoFunctionTypeArgs() {
        // Given
        val argTypes = listOf(FUN_I64_F64_TO_I64, FUN_I64_TO_I64)
        val identifier = Identifier("foo", Fun.from(argTypes, I64.INSTANCE))
        val declarations = listOf(
            Declaration(0, 0, "a", FUN_I64_F64_TO_I64),
            Declaration(0, 0, "b", FUN_I64_TO_I64),
        )
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then
        assertContains(result, listOf(
            $$"define tailcc i64 @foo_FunL$I64.F64$R.toI64_FunL$I64$R.toI64(ptr %0, ptr %1)",
        ))
    }

    @Test
    fun shouldGenerateFunctionWithFunctionFunctionTypeArg() {
        // Given
        val functionType = Fun.from(listOf(FUN_F64_TO_I64), I64.INSTANCE)
        val identifier = Identifier("foo", Fun.from(listOf(functionType), I64.INSTANCE))
        val declarations = listOf(Declaration(0, 0, "a", functionType))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, IL_5)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then
        assertContains(result, listOf(
            $$"define tailcc i64 @foo_FunL$FunL$F64$R.toI64$R.toI64(ptr %0)", // All function pointers are the same, regardless of their type
        ))
    }

    @Test
    fun shouldCallFunctionWithUserDefinedFunctionArg() {
        // Given
        val identifierFoo = Identifier("foo", Fun.from(listOf(FUN_I64_TO_I64), I64.INSTANCE))
        val declarationsFoo = listOf(Declaration(0, 0, "a", FUN_I64_TO_I64))
        val fdsFoo = FunctionDefinitionStatement(0, 0, identifierFoo, declarationsFoo, IL_5)
        val udfFoo = UserDefinedFunction("foo", listOf("a"), listOf(FUN_I64_TO_I64), I64.INSTANCE)

        val identifierBar = Identifier("bar", FUN_I64_TO_I64)
        val declarationsBar = listOf(Declaration(0, 0, "b", I64.INSTANCE))
        val fdsBar = FunctionDefinitionStatement(0, 0, identifierBar, declarationsBar, IL_17)

        val ideBar = IdentifierDerefExpression(0, 0, identifierBar)
        val fce = FunctionCallExpression(0, 0, identifierFoo, listOf(ideBar), udfFoo)
        val ps = funCall(BF_PRINTLN_I64, fce)

        // When
        val result = assembleProgram(cg, listOf(fdsFoo, fdsBar, ps))

        // Then
        assertContains(result, listOf(
            $$"define tailcc i64 @foo_FunL$I64$R.toI64(ptr %0)",
            $$"%0 = call tailcc i64 @foo_FunL$I64$R.toI64(ptr @bar_I64)",
        ))
    }

    @Test
    fun shouldCallFunctionThatCallsFunctionArg() {
        // Given
        val identifierFoo = Identifier("foo", Fun.from(listOf(FUN_I64_TO_I64), I64.INSTANCE))
        val declarationsFoo = listOf(Declaration(0, 0, "f", FUN_I64_TO_I64))
        val identifierF = Identifier("f", FUN_I64_TO_I64)
        val rfF = ReferenceFunction("f", listOf(I64.INSTANCE), I64.INSTANCE)
        val fceF = FunctionCallExpression(0, 0, identifierF, listOf(IL_5), rfF)
        val fdsFoo = FunctionDefinitionStatement(0, 0, identifierFoo, declarationsFoo, fceF)
        val udfFoo = UserDefinedFunction("foo", listOf("f"), listOf(FUN_I64_TO_I64), I64.INSTANCE)

        val identifierBar = Identifier("bar", FUN_I64_TO_I64)
        val declarationsBar = listOf(Declaration(0, 0, "b", I64.INSTANCE))
        val fdsBar = FunctionDefinitionStatement(0, 0, identifierBar, declarationsBar, IL_17)

        val ideBar = IdentifierDerefExpression(0, 0, identifierBar)
        val fceFoo = FunctionCallExpression(0, 0, identifierFoo, listOf(ideBar), udfFoo)
        val ps = funCall(BF_PRINTLN_I64, fceFoo)

        // When
        val result = assembleProgram(cg, listOf(fdsFoo, fdsBar, ps))

        // Then
        assertContains(result, listOf(
            "%1 = load ptr, ptr %_f",
            "%2 = call tailcc i64 %1(i64 5)",
        ))
    }

    @Test
    fun shouldCallPrintWithFunctionArg() {
        // Given
        val identifierBar = Identifier("bar", FUN_I64_TO_I64)
        val declarationsBar = listOf(Declaration(0, 0, "b", I64.INSTANCE))
        val fdsBar = FunctionDefinitionStatement(0, 0, identifierBar, declarationsBar, IL_17)

        val ideBar = IdentifierDerefExpression(0, 0, identifierBar)
        val ps = funCall(BF_PRINTLN_I64, ideBar)

        // When
        val result = assembleProgram(cg, listOf(fdsBar, ps))

        // Then
        assertContains(result, listOf(
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.lp.I64.rp.to.I64.nl, ptr @bar_I64)",
        ))
    }

    @Test
    fun shouldGenerateAndCallOverloadedFunctions() {
        // Given
        // Define one-arg function
        val identifier1 = Identifier("foo", FUN_I64_TO_I64)
        val declarations1 = listOf(Declaration(0, 0, "a", I64.INSTANCE))
        val fds1 = FunctionDefinitionStatement(0, 0, identifier1, declarations1, IL_5)
        val udf1 = UserDefinedFunction("foo", listOf("a"), listOf(I64.INSTANCE), I64.INSTANCE)

        // Define two-arg function
        val identifier2 = Identifier("foo", FUN_I64_F64_TO_I64)
        val declarations2 = listOf(
            Declaration(0, 0, "a", I64.INSTANCE),
            Declaration(0, 0, "b", F64.INSTANCE),
        )
        val fds2 = FunctionDefinitionStatement(0, 0, identifier2, declarations2, IL_5)
        val udf2 = UserDefinedFunction("foo", listOf("a", "b"), listOf(I64.INSTANCE, F64.INSTANCE), I64.INSTANCE)

        // Call functions
        val ps1 = funCall(BF_PRINTLN_I64, FunctionCallExpression(identifier1, listOf(IL_M_1), udf1))
        val ps2 = funCall(BF_PRINTLN_I64, FunctionCallExpression(identifier2, listOf(IL_M_1, FL_2_0), udf2))

        // When
        val result = assembleProgram(cg, listOf(fds1, fds2, ps1, ps2))

        // Then
        assertContains(result, listOf(
            "define tailcc i64 @foo_I64(i64 %0)",
            "define tailcc i64 @foo_I64_F64(i64 %0, double %1)",
            "%0 = call tailcc i64 @foo_I64(i64 -1)",
            "%2 = call tailcc i64 @foo_I64_F64(i64 -1, double 2.0)",
        ))
    }

    @Test
    fun shouldWidenReturnValueBeforeReturning() {
        // The AST the semantics parser produces for `fun foo(x as i32) -> i64 := x` and
        // `fun bar(x as f32) -> f64 := x`: the widening cast is explicit, which is what makes the
        // ret carry the declared type. Without it the ret would name the parameter's narrower type
        // inside a function declared to return the wider one, and clang would reject the module.
        val intIdentifier = Identifier("foo", Fun.from(listOf(I32.INSTANCE), I64.INSTANCE))
        val intParameter = IdentifierDerefExpression(0, 0, Identifier("x", I32.INSTANCE))
        val intFds = FunctionDefinitionStatement(
            0, 0, intIdentifier,
            listOf(Declaration(0, 0, "x", I32.INSTANCE)),
            CastToIntExpression(intParameter, I64.INSTANCE)
        )

        val floatIdentifier = Identifier("bar", Fun.from(listOf(F32.INSTANCE), F64.INSTANCE))
        val floatParameter = IdentifierDerefExpression(0, 0, Identifier("x", F32.INSTANCE))
        val floatFds = FunctionDefinitionStatement(
            0, 0, floatIdentifier,
            listOf(Declaration(0, 0, "x", F32.INSTANCE)),
            CastToFloatExpression(floatParameter, F64.INSTANCE)
        )

        val result = assembleProgram(cg, listOf(intFds, floatFds))

        assertContains(result, listOf(
            "define tailcc i64 @foo_I32(i32 %0)",
            "sext i32",
            "define tailcc double @bar_F32(float %0)",
            "fpext float",
        ))
    }
}
