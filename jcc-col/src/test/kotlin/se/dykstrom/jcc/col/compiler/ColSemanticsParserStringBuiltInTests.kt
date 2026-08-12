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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.FL_1_5_F32
import se.dykstrom.jcc.col.ColTests.Companion.IL_17_I32
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_STR
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_STRING_F64
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_STRING_I64
import se.dykstrom.jcc.common.ast.CastToF64Expression
import se.dykstrom.jcc.common.ast.CastToI64Expression
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.types.Bool
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str

/**
 * Semantic analysis of the string built-ins: which signatures resolve, how a narrower numeric
 * argument reaches the `string` conversion, and which calls stay errors. Code generation and the
 * emitted symbols are covered by [ColLlvmCodeGeneratorStringBuiltInTests].
 */
class ColSemanticsParserStringBuiltInTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldResolveStringBuiltIns() {
        assertEquals(I64.INSTANCE, bodyType("""fun a() -> i64 := len("hello")"""))
        assertEquals(I64.INSTANCE, bodyType("""fun b() -> i64 := indexof("hello", "ll")"""))
        assertEquals(Str.INSTANCE, bodyType("""fun c() -> string := substr("hello", 0, 2)"""))
        assertEquals(Str.INSTANCE, bodyType("fun d() -> string := readln()"))
        assertEquals(Bool.INSTANCE, bodyType("fun e() -> bool := eof()"))
    }

    @Test
    fun shouldResolveStringConversionOverloads() {
        assertEquals(Str.INSTANCE, bodyType("fun a() -> string := string(17)"))
        assertEquals(Str.INSTANCE, bodyType("fun b() -> string := string(1.5)"))
        assertEquals(Str.INSTANCE, bodyType("fun c() -> string := string(true)"))
    }

    @Test
    fun shouldPromoteNarrowerNumberInStringConversion() {
        // libjcccol exports one conversion per widest numeric type, so a narrower argument reaches
        // it through the same implicit widening an arithmetic operand gets. The f32 case is what
        // the float branch of ColTypeManager.isAssignableFrom exists for: overload resolution goes
        // through isAssignableFrom, so without it string(1.5f32) would not resolve at all.
        verify(
            parse("call println(string(17i32))"),
            funCall(BF_PRINTLN_STR, call(BF_STRING_I64, CastToI64Expression(IL_17_I32)))
        )
        verify(
            parse("call println(string(1.5f32))"),
            funCall(BF_PRINTLN_STR, call(BF_STRING_F64, CastToF64Expression(FL_1_5_F32)))
        )
    }

    @Test
    fun shouldTakeStringBuiltInsAsOperands() {
        assertEquals(I64.INSTANCE, bodyType("""fun a() -> i64 := len("a") + indexof("ab", "b")"""))
        assertEquals(Str.INSTANCE, bodyType("""fun b() -> string := "len=" + string(len("abc"))"""))
        assertEquals(Bool.INSTANCE, bodyType("""fun c() -> bool := substr("abc", 0, 1) == "a""""))
    }

    @Test
    fun shouldNotConvertStringToString() {
        // string() converts *to* text; there is no overload taking one, and string is never
        // assignable from anything but a string
        parseAndExpectError("""call println(string("x"))""", "found no match for function call: string(string)")
    }

    @Test
    fun shouldNotApplyStringBuiltInToWrongType() {
        parseAndExpectError("call println(len(1))", "found no match for function call: len(i64)")
        parseAndExpectError("""call println(substr(1, 0, 1))""", "found no match for function call: substr(i64, i64, i64)")
        parseAndExpectError("""call println(indexof("a", 1))""", "found no match for function call: indexof(string, i64)")
        parseAndExpectError("""call println(substr("a", "b", 1))""", "found no match for function call: substr(string, string, i64)")
    }

    @Test
    fun shouldNotApplyStringBuiltInWithWrongArity() {
        parseAndExpectError("""call println(len("a", "b"))""", "found no match for function call: len(string, string)")
        parseAndExpectError("""call println(substr("a", 1))""", "found no match for function call: substr(string, i64)")
        parseAndExpectError("""call println(indexof("a"))""", "found no match for function call: indexof(string)")
        parseAndExpectError("""call println(readln("a"))""", "found no match for function call: readln(string)")
        parseAndExpectError("call println(eof(1))", "found no match for function call: eof(i64)")
    }

    @Test
    fun shouldNotOrderResultsOfStringBuiltIns() {
        // Ordering is still undefined for strings, whoever produced them
        parseAndExpectError(
            """fun f() -> bool := substr("abc", 0, 1) < readln()""",
            "cannot order strings: only == and != are defined for string"
        )
    }

    @Test
    fun shouldNotUseStringBuiltInAsFunctionValue() {
        // Only user-defined functions are emitted as addressable globals
        parseAndExpectError(
            "val f as (string) -> i64 := len",
            "cannot use 'len' as a function reference"
        )
    }

    @Test
    fun shouldNotRedefineStringBuiltIn() {
        // A user-defined function with the same signature collides, as it does for any built-in
        parseAndExpectError(
            """fun len(s as string) -> i64 := 0""",
            "function 'len(string) -> i64' has already been defined"
        )
    }

    @Test
    fun shouldOverloadStringBuiltInWithDifferentSignature() {
        // A different signature is a new overload, not a collision
        val program = parse(
            """
            fun len(s as string, t as string) -> i64 := len(s) + len(t)
            call println(len("a", "bc"))
            """.trimIndent()
        )
        val statement = program.statements[0] as FunctionDefinitionStatement
        assertEquals(I64.INSTANCE, typeManager.getType(statement.expression()))
    }

    /** Returns the type of the body of a program consisting of a single function definition. */
    private fun bodyType(text: String) =
        typeManager.getType((parse(text).statements[0] as FunctionDefinitionStatement).expression())

    private fun call(function: Function, vararg args: Expression) =
        FunctionCallExpression(function.identifier, args.toList())
}
