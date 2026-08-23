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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.IDENT_STR_S
import se.dykstrom.jcc.col.ColTests.Companion.IDE_STR_S
import se.dykstrom.jcc.col.ColTests.Companion.SL_BAR
import se.dykstrom.jcc.col.ColTests.Companion.SL_FOO
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_BOOL
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_STR
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.DeclarationAssignment
import se.dykstrom.jcc.common.ast.EqualExpression
import se.dykstrom.jcc.common.ast.NotEqualExpression
import se.dykstrom.jcc.common.types.Bool

/**
 * Tests code generation of COL strings on the LLVM backend: literals, concatenation through
 * libjcccol, equality through strcmp, printing, and string-typed vals. The garbage-collector
 * plumbing these programs depend on is tested in [ColLlvmCodeGeneratorGcTests].
 */
internal class ColLlvmCodeGeneratorStringTests : AbstractColCodeGeneratorTests() {

    @Test
    fun shouldPrintStringLiteral() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_STR, SL_FOO)))
        assertContains(result, listOf(
            // The literal is a private constant holding its UTF-8 bytes plus a trailing NUL
            """@_.str.0 = private constant [4 x i8] c"foo\00"""",
            """@_.printf.fmt.Str.nl = private constant [4 x i8] c"%s\0A\00"""",
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr @_.str.0)",
        ))
        // A literal is not GC-owned: the collector ignores slot values it does not own
        assertNotContains(result, listOf("@jcc_gc_register(ptr @_.str.0)"))
    }

    @Test
    fun shouldPrintNonAsciiStringLiteralAsUtf8Bytes() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_STR, se.dykstrom.jcc.common.ast.StringLiteral(0, 0, "höst"))))
        // "höst" is five UTF-8 bytes; the constant is byte-transparent, not re-encoded
        assertContains(result, listOf("""@_.str.0 = private constant [6 x i8] c"höst\00""""))
    }

    @Test
    fun shouldConcatenateStringsAndRegisterTheResult() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_STR, AddExpression(SL_FOO, SL_BAR))))
        assertContains(result, listOf(
            "declare ptr @col_concat_str_str(ptr, ptr)",
            "%0 = call ptr @col_concat_str_str(ptr @_.str.0, ptr @_.str.1)",
            // Register, then store into a rooted slot before anything else can collect
            "%1 = call ptr @jcc_gc_register(ptr %0)",
            "store ptr %1, ptr %_.gc.slot.0",
            "%2 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr %1)",
        ))
        val text = result.toText()
        assertTrue(
            text.indexOf("@jcc_gc_register(ptr %0)") < text.indexOf("store ptr %1, ptr %_.gc.slot.0"),
            "register must precede the store into the rooted slot"
        )
    }

    @Test
    fun shouldCompareStringsForEquality() {
        // The comparison is bound to a val rather than printed: println(bool) converts through
        // col_string_bool, whose registration would drown out what this test is about
        val statements = listOf(
            ValDeclarationStatement(DeclarationAssignment("same", Bool.INSTANCE, EqualExpression(SL_FOO, SL_BAR)))
        )
        val result = assembleProgram(cg, statements)
        assertContains(result, listOf(
            "declare i32 @strcmp(ptr, ptr)",
            "%0 = call i32 @strcmp(ptr @_.str.0, ptr @_.str.1)",
            "%1 = icmp eq i32 %0, 0",
        ))
        // strcmp allocates nothing and returns no view of its operands, so nothing is registered
        assertNotContains(result, listOf("jcc_gc_register"))
    }

    @Test
    fun shouldCompareStringsForInequality() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, NotEqualExpression(SL_FOO, SL_BAR))))
        assertContains(result, listOf(
            "%0 = call i32 @strcmp(ptr @_.str.0, ptr @_.str.1)",
            "%1 = icmp ne i32 %0, 0",
        ))
    }

    @Test
    fun shouldDeclareStringVal() {
        val statements = listOf(
            ValDeclarationStatement(DeclarationAssignment(IDENT_STR_S.name(), IDENT_STR_S.type(), SL_FOO)),
            funCall(BF_PRINTLN_STR, IDE_STR_S),
        )
        val result = assembleProgram(cg, statements)
        assertContains(result, listOf(
            // A string val is a local of main, null-initialized and rooted in main's frame
            "%_s = alloca ptr",
            "store ptr null, ptr %_s",
            "call void @jcc_gc_add_root(ptr %_s)",
            "store ptr @_.str.0, ptr %_s",
            "%0 = load ptr, ptr %_s",
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr %0)",
        ))
    }

    @Test
    fun shouldConcatenateValAndLiteral() {
        val statements = listOf(
            ValDeclarationStatement(DeclarationAssignment(IDENT_STR_S.name(), IDENT_STR_S.type(), SL_FOO)),
            funCall(BF_PRINTLN_STR, AddExpression(IDE_STR_S, SL_BAR)),
        )
        val result = assembleProgram(cg, statements)
        assertContains(result, listOf(
            "%0 = load ptr, ptr %_s",
            "%1 = call ptr @col_concat_str_str(ptr %0, ptr @_.str.1)",
            "%2 = call ptr @jcc_gc_register(ptr %1)",
            "store ptr %2, ptr %_.gc.slot.0",
        ))
    }
}
