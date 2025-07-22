/*
 * Copyright (C) 2025 Johan Dykstrom
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

package se.dykstrom.jcc.main

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for the COL LLVM backend, specifically for testing user-defined functions.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class ColLlvmCompileAndRunUserFunctionsIT : AbstractIntegrationTests() {

    @Test
    fun shouldCallUserDefinedFunction() {
        val source = listOf(
            "fun foo() -> i64 := 23",
            "call println(foo())",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(listOf(), listOf(
            "23",
        ))
    }

    @Test
    fun shouldCallUserDefinedFunctionWithArgs() {
        val source = listOf(
            "call println(foo(-7.0))",
            "call println(bar(5.0, 3.0))",
            "call println(bar(-1.0, 5.0))",
            "call println(tee(-2))",
            "",
            "fun foo(a as f64) -> f64 := a * a",
            "fun bar(a as f64, b as f64) -> f64 := foo(a) + tee(i64(b))",
            "fun tee(a as i64) -> f64 := -f64(a)"
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(listOf(), listOf(
            "49.000000",
            "22.000000",
            "-4.000000",
            "2.000000",
        ))
    }

    @Test
    fun shouldCallUserDefinedFunctionWithUserDefinedFunctionArg() {
        val source = listOf(
            "call println(foo(5))",
            "call println(bar(foo, 8))",
            "",
            "fun foo(a as i64) -> i64 := -a",
            "fun bar(f as (i64) -> i64, v as i64) -> i64 := f(v)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(listOf(), listOf(
            "-5",
            "-8",
        ))
    }

    @Test
    fun shouldCallUserDefinedFunctionWithUserDefinedFunctionArgReturnedFromOtherFunction() {
        val source = listOf(
            "call println(tee(foo, bar(foo), 3))",
            "",
            "fun foo(a as i64) -> i64 := a + 1",
            "fun bar(f as (i64) -> i64) -> (i64) -> i64 := f",
            "fun tee(x as (i64) -> i64, y as (i64) -> i64, z as i64) -> i64 := x(y(z))",
        )
        val sourceFile = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourceFile)
        runLlvmAndAssertSuccess(listOf(), listOf("5"))
    }

    @Test
    fun shouldPrintFunctionAddress() {
        val source = listOf(
            "call println(foo)",
            "",
            "fun foo(a as i64) -> i64 := a + 1",
        )
        val sourceFile = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourceFile)
        runLlvmAndAssertSuccess(listOf(), listOf("0x"))
    }

    @Test
    fun shouldCallUserDefinedFunctionWithIntrinsicFunctionArg() {
        // It is not possible to take a reference to an intrinsic function.
        // You get a link error similar to this:
        //
        //   Undefined symbols for architecture x86_64:
        //     "_llvm.sqrt.f64", referenced from:
        //       _main in it_13426877751931495407-b3c268.o
        //   ld: symbol(s) not found for architecture x86_64
        //
        // As a workaround you can create a function whose only purpose is
        // calling the intrinsic function.
        val source = listOf(
            "call println(bar(sqrt_, 4.0))",
            "fun sqrt_(x as f64) -> f64 := sqrt(x)",
            "fun bar(f as (f64) -> f64, v as f64) -> f64 := f(v)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(listOf(), listOf(
            "2.000000",
        ))
    }
}
