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

package se.dykstrom.jcc.main

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for the COL string built-ins: `len`,
 * `substr`, `indexof`, `string`, `readln` and `eof`. Everything here is byte oriented; there are no
 * codepoint semantics to test because there are none in the language.
 *
 * These tests are what prove the exported symbols in [se.dykstrom.jcc.col.compiler.LibJccColBuiltIns]
 * actually resolve: jcc emits calls by symbol name, so a wrong name is a link error, not a
 * compilation error, and only a linked-and-run program catches it. They also exercise the C `_Bool`
 * boundary that `eof` and `string(bool)` cross, where jcc emits a bare `i1` rather than clang's
 * `i1 zeroext` — see "Parameter attributes" in `docs/system/code-generation.md` for why that is
 * safe, so it does not get re-derived from the IR.
 *
 * The string type itself is covered by [ColCompileAndRunStringIT].
 */
@Tag("LLVM")
class ColCompileAndRunStringBuiltInIT : AbstractIntegrationTests() {

    @Test
    fun shouldEchoStdinUntilEof() {
        // The filter idiom the epic exists for. Input ends with a newline here.
        val sourcePath = createSourceFile(ECHO_FILTER, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf("first", "second", "third"), listOf("first", "second", "third"))
    }

    @Test
    fun shouldEchoFinalLineWithoutTrailingNewline() {
        // A final line with no terminator must still be read, and eof must report end of input
        // immediately afterwards rather than yielding a phantom empty line
        val sourcePath = createSourceFile(ECHO_FILTER, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccessWithRawInput("alpha\nbeta", listOf("alpha", "beta"))
    }

    @Test
    fun shouldEchoNothingForEmptyInput() {
        // eof is true before the first read, so the loop body never runs
        val sourcePath = createSourceFile(ECHO_FILTER, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccessWithRawInput("", listOf())
    }

    @Test
    fun shouldPassNonAsciiInputThroughByteExact() {
        val sourcePath = createSourceFile(ECHO_FILTER, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccessWithRawInput("höstlöv\nκαλημέρα\n😀", listOf("höstlöv", "καλημέρα", "😀"))
    }

    @Test
    fun shouldCountBytesNotCharacters() {
        // The epic's UTF-8 model, made executable: len is a byte count. "höstlöv" is seven
        // characters but nine bytes, and a single emoji is four.
        val source = listOf(
            """call println(len("hello"))""",
            """call println(len(""))""",
            """call println(len("höstlöv"))""",
            """call println(len("\u{1F600}"))""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf("5", "0", "9", "4"))
    }

    @Test
    fun shouldClampSubstrAtBothEnds() {
        // Out-of-range arguments clamp rather than failing; the empty results are printed first
        // because assertOutput drops trailing empty lines
        val source = listOf(
            """call println(substr("hello", -1, 3))""",
            """call println(substr("hello", 10, 3))""",
            """call println(substr("hello", 0, 0))""",
            """call println(substr("hello", 1, -1))""",
            """call println(substr("hello", 0, 2))""",
            """call println(substr("hello", 3, 99))""",
            """call println(substr("hello", 0, 5))""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf("", "", "", "", "he", "lo", "hello"))
    }

    @Test
    fun shouldFindIndexOfNeedle() {
        val source = listOf(
            """call println(indexof("hello", "h"))""",
            """call println(indexof("hello", "ll"))""",
            """call println(indexof("hello", ""))""",
            """call println(indexof("hello", "z"))""",
            """call println(indexof("hello", "hello!"))""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf("0", "2", "0", "-1", "-1"))
    }

    @Test
    fun shouldComposeIndexofWithSubstr() {
        // The composition the C header documents: a -1 from indexof fed into substr yields the empty
        // string rather than the first bytes, which is why a negative start clamps to empty
        val source = listOf(
            """val s := "key=value"""",
            """call println(substr(s, indexof(s, "z"), 3))""",
            """call println(substr(s, indexof(s, "=") + 1, len(s)))""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf("", "value"))
    }

    @Test
    fun shouldConvertNumbersAndBooleansToStrings() {
        // string(f32) resolves through the f64 overload by implicit widening - the case the float
        // branch of ColTypeManager.isAssignableFrom exists for. The last two lines pin that
        // println(b) and println(string(b)) agree: println(bool) routes through the same
        // col_string_bool, so neither prints 1/0 any more.
        val source = listOf(
            "call println(string(0))",
            "call println(string(-42))",
            "call println(string(17i32))",
            "call println(string(3.14))",
            "call println(string(1.5f32))",
            "call println(string(true))",
            "call println(string(false))",
            "call println(true)",
            "call println(false)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(
            listOf(),
            listOf("0", "-42", "17", "3.140000", "1.500000", "true", "false", "true", "false")
        )
    }

    @Test
    fun shouldConcatenateBuiltInResults() {
        // A registered built-in result is an ordinary string value: safe as a concat operand, as a
        // val initializer, and as an argument to a user-defined function
        val source = listOf(
            """fun describe(s as string) -> string := s + " (" + string(len(s)) + " bytes)"""",
            """val word := substr("hello, world", 7, 5)""",
            "call println(describe(word))",
            """call println("at " + string(indexof("hello", "ll")))""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf("world (5 bytes)", "at 2"))
    }

    @Test
    fun shouldReadStdinIntoStringOperations() {
        // readln's result flows into the other built-ins, and the loop reuses one rooted slot per
        // call site, so a long input does not grow the shadow stack
        val source = listOf(
            "while not eof() do",
            "    val line := readln()",
            """    call println(string(len(line)) + ":" + substr(line, 0, 3))""",
            "end",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf("abcdef", "xy", ""), listOf("6:abc", "2:xy", "0:"))
    }

    @Test
    fun shouldWidenReturnValueOfUserFunction() {
        // A body that only widens to the declared return type compiles and returns the wide value;
        // without the promoting cast this module would not survive clang at all
        val source = listOf(
            "fun asI64(x as i32) -> i64 := x",
            "fun asF64(x as f32) -> f64 := x",
            "call println(asI64(17i32))",
            "call println(asF64(1.5f32))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf("17", "1.500000"))
    }

    @Test
    fun shouldTreatSameTypeCastAsIdentity() {
        val source = listOf(
            "call println(i64(17))",
            "call println(f64(1.5))",
            "call println(i32(i32(3)))",
            "call println(f32(f32(2.5)))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf("17", "1.500000", "3", "2.500000"))
    }

    companion object {
        /** `while not eof() do call println(readln()) end` — the epic's filter program. */
        private val ECHO_FILTER = listOf(
            "while not eof() do",
            "    call println(readln())",
            "end",
        )
    }
}
