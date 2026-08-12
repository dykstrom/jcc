/*
 * Copyright (C) 2026 Johan Dykstrom
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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.FL_1_5
import se.dykstrom.jcc.col.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.ColTests.Companion.SL_BAR
import se.dykstrom.jcc.col.ColTests.Companion.SL_FOO
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_EOF
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_INDEXOF_STR_STR
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_LEN_STR
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_BOOL
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_I64
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_STR
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_READLN
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_STRING_BOOL
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_STRING_F64
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_STRING_I64
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_SUBSTR_STR_I64_I64
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.BooleanLiteral
import se.dykstrom.jcc.common.ast.DeclarationAssignment
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.types.Bool

/**
 * Tests code generation of the COL string built-ins on the LLVM backend. What matters here is the
 * exact exported symbol of each call — jcc resolves purely by symbol name, so a typo in the tables
 * surfaces as an undefined symbol at link time rather than as a compilation error — and which
 * results are handed to the collector.
 */
internal class ColLlvmCodeGeneratorStringBuiltInTests : AbstractColCodeGeneratorTests() {

    private val cg = ColLlvmCodeGenerator(typeManager, symbols, optimizer)

    @Test
    fun shouldCallStrlenForLen() {
        // len goes straight to libc: no libjcccol symbol, and nothing is allocated
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, call(BF_LEN_STR, SL_FOO))))
        assertContains(result, listOf(
            "declare i64 @strlen(ptr)",
            "%0 = call i64 @strlen(ptr @_.str.0)",
        ))
        assertNotContains(result, listOf("jcc_gc_register"))
    }

    @Test
    fun shouldCallIndexofWithoutRegistering() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, call(BF_INDEXOF_STR_STR, SL_FOO, SL_BAR))))
        assertContains(result, listOf(
            "declare i64 @col_indexof_str_str(ptr, ptr)",
            "%0 = call i64 @col_indexof_str_str(ptr @_.str.0, ptr @_.str.1)",
        ))
        // An integer result owns no memory
        assertNotContains(result, listOf("jcc_gc_register"))
    }

    @Test
    fun shouldCallEofWithoutRegistering() {
        // The result is bound to a val rather than printed, because println(bool) converts through
        // col_string_bool and would contribute a registration of its own - which would say nothing
        // about eof
        val result = assembleProgram(cg, listOf(boolVal("at_end", call(BF_EOF))))
        assertContains(result, listOf(
            "declare i1 @col_eof()",
            "%0 = call i1 @col_eof()",
        ))
        assertNotContains(result, listOf("jcc_gc_register"))
    }

    @Test
    fun shouldRegisterSubstrResult() {
        val statements = listOf(funCall(BF_PRINTLN_STR, call(BF_SUBSTR_STR_I64_I64, SL_FOO, ZERO, IL_17)))
        val result = assembleProgram(cg, statements)
        assertContains(result, listOf(
            "declare ptr @col_substr_str_i64_i64(ptr, i64, i64)",
            "%0 = call ptr @col_substr_str_i64_i64(ptr @_.str.0, i64 0, i64 17)",
            // A fresh block from libjcccol: registered, then stored into a rooted slot
            "%1 = call ptr @jcc_gc_register(ptr %0)",
            "store ptr %1, ptr %_.gc.slot.0",
        ))
    }

    @Test
    fun shouldRegisterReadlnResult() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_STR, call(BF_READLN))))
        assertContains(result, listOf(
            "declare ptr @col_readln()",
            "%0 = call ptr @col_readln()",
            "%1 = call ptr @jcc_gc_register(ptr %0)",
            "store ptr %1, ptr %_.gc.slot.0",
        ))
    }

    @Test
    fun shouldRegisterStringConversionResults() {
        val statements = listOf(
            funCall(BF_PRINTLN_STR, call(BF_STRING_I64, IL_17)),
            funCall(BF_PRINTLN_STR, call(BF_STRING_F64, FL_1_5)),
            funCall(BF_PRINTLN_STR, call(BF_STRING_BOOL, BooleanLiteral.TRUE)),
        )
        val result = assembleProgram(cg, statements)
        assertContains(result, listOf(
            "declare ptr @col_string_i64(i64)",
            "declare ptr @col_string_f64(double)",
            "declare ptr @col_string_bool(i1)",
            "call ptr @col_string_i64(i64 17)",
            "call ptr @col_string_f64(double 1.5)",
            // LLVM's true is 1; the FASM backend's is -1, which is why LiteralCodeGenerator maps it
            "call ptr @col_string_bool(i1 1)",
        ))
        // One registration and one rooted slot per conversion
        assertContains(result, listOf("%_.gc.slot.0", "%_.gc.slot.1", "%_.gc.slot.2"))
    }

    @Test
    fun shouldPrintBooleanAsTextThroughStringConversion() {
        // println(bool) routes through col_string_bool so that println(b) and println(string(b))
        // print the same thing. The printf format is therefore the string one, and the i1 is no
        // longer zero-extended for printing.
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, BooleanLiteral.TRUE)))
        assertContains(result, listOf(
            "%0 = call ptr @col_string_bool(i1 1)",
            "%1 = call ptr @jcc_gc_register(ptr %0)",
            "store ptr %1, ptr %_.gc.slot.0",
            "%2 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr %1)",
        ))
        assertNotContains(result, listOf("@_.printf.fmt.Bool.nl", "zext i1"))
    }

    @Test
    fun shouldConcatenateABuiltInResult() {
        // A registered built-in result is an ordinary string operand: the concatenation registers
        // its own result, and both stay rooted across the second registration
        // The left operand is evaluated first, so SL_BAR is the first literal global
        val len = call(BF_STRING_I64, call(BF_LEN_STR, SL_FOO))
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_STR, AddExpression(SL_BAR, len))))
        assertContains(result, listOf(
            "call i64 @strlen(ptr @_.str.1)",
            "call ptr @col_string_i64(i64 %0)",
            "call ptr @col_concat_str_str(ptr @_.str.0, ptr %2)",
        ))
    }

    private fun call(function: Function, vararg args: Expression) =
        FunctionCallExpression(function.identifier, args.toList(), function)

    private fun boolVal(name: String, expression: Expression) =
        ValDeclarationStatement(DeclarationAssignment(name, Bool.INSTANCE, expression))
}
