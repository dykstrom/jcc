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
 * Compile-and-run integration tests for COL strings on the LLVM backend: literals, escapes,
 * concatenation, equality, and string-typed vals. The collector these programs depend on is
 * exercised in [ColLlvmGarbageCollectionIT].
 *
 * Strings are LLVM-only, so there is no FASM counterpart to this class.
 */
@Tag("LLVM")
class ColLlvmCompileAndRunStringIT : AbstractIntegrationTests() {

    @Test
    fun shouldPrintStringLiteral() {
        // The empty literal is printed first: assertOutput drops trailing empty lines, so an empty
        // line last would be invisible to it
        val source = listOf(
            """call println("")""",
            """call println("hello")""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath, language = COL)
        runLlvmAndAssertSuccess(listOf(), listOf("", "hello"))
    }

    @Test
    fun shouldPrintEscapes() {
        val source = listOf(
            """call println("a\tb")""",
            """call println("say \"hi\"")""",
            """call println("back\\slash")""",
            """call println("two\nlines")""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath, language = COL)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "a\tb",
                "say \"hi\"",
                "back\\slash",
                "two",
                "lines",
            )
        )
    }

    @Test
    fun shouldPassNonAsciiThroughByteExact() {
        // Source is read as UTF-8 and the literal is emitted as its bytes, so what goes in comes out
        val source = listOf(
            """call println("höstlöv")""",
            """call println("\u{1F600} smiley")""",
            """val greeting := "καλημέρα"""",
            "call println(greeting)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath, language = COL)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "höstlöv",
                "😀 smiley",
                "καλημέρα",
            )
        )
    }

    @Test
    fun shouldConcatenateStrings() {
        val source = listOf(
            """val hello := "hello"""",
            """call println(hello + ", world")""",
            """call println(hello + ", " + hello + "!")""",
            """val empty := """"",
            """call println(hello + empty)""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath, language = COL)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "hello, world",
                "hello, hello!",
                "hello",
            )
        )
    }

    @Test
    fun shouldCompareStringsByContent() {
        // strcmp semantics, not pointer identity: two separately built strings with the same
        // content are equal
        val source = listOf(
            """val a := "ab"""",
            """val b := "a" + "b"""",
            "call println(a == b)",
            "call println(a != b)",
            """call println(a == "ba")""",
            """call println(a != "ba")""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath, language = COL)
        runLlvmAndAssertSuccess(listOf(), listOf("true", "false", "false", "true"))
    }

    @Test
    fun shouldConcatenateInIfExpression() {
        val source = listOf(
            """val a := "yes"""",
            """val b := "no"""",
            """call println(if a == "yes" then a + "!" else b + "!")""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath, language = COL)
        runLlvmAndAssertSuccess(listOf(), listOf("yes!"))
    }

    @Test
    fun shouldPassStringsToAndFromFunctions() {
        // The three shapes a callee can return: a value it built, an argument it does not own, and
        // a literal. All three are safe because a root is a slot address whose contents are read at
        // mark time, so the caller roots whatever comes back without knowing which it is.
        val source = listOf(
            """fun greet(name as string) -> string := "Hello, " + name""",
            """fun echo(s as string) -> string := s""",
            """fun lit() -> string := "literal"""",
            """fun join(a as string, b as string, sep as string) -> string := a + sep + b""",
            """call println(greet("world"))""",
            """call println(echo("unchanged"))""",
            "call println(lit())",
            """call println(join("left", "right", " | "))""",
            """call println(echo(greet("nested")) + "!")""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath, language = COL)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "Hello, world",
                "unchanged",
                "literal",
                "left | right",
                "Hello, nested!",
            )
        )
    }

    @Test
    fun shouldPassStringsThroughFunctionValuesAndLambdas() {
        // A lambda is lifted to an ordinary top-level function, so a string parameter needs nothing
        // beyond what a named function needs - and a lambda captures nothing, so there is no
        // environment to collect either
        val source = listOf(
            """fun shout(s as string) -> string := s + "!"""",
            "fun apply(f as (string) -> string, s as string) -> string := f(s)",
            "val named as (string) -> string := shout",
            """val quoted := fun(s as string) -> string := "<" + s + ">"""",
            """call println(apply(shout, "direct"))""",
            """call println(named("by value"))""",
            """call println(quoted("lambda"))""",
            """call println(apply(fun(s as string) := s + s, "twice"))""",
            """call println(apply(quoted, "passed"))""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath, language = COL)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "direct!",
                "by value!",
                "<lambda>",
                "twicetwice",
                "<passed>",
            )
        )
    }

    @Test
    fun shouldBuildStringsWithBecome() {
        // The accumulator idiom, COL's most common become shape, with a string accumulator: the
        // argument is produced in the frame the tail call pops, and stays reachable because the
        // callee roots its parameter before anything can allocate. Mutual become crosses two
        // prototypes, which is what tailcc was adopted for.
        val source = listOf(
            "fun build(acc as string, n as i64) -> string :=",
            """    if n == 0 then acc else become build(acc + "ab", n - 1)""",
            "fun even(acc as string, n as i64) -> string :=",
            """    if n == 0 then acc else become odd(acc + "e", n - 1)""",
            "fun odd(acc as string, n as i64) -> string :=",
            """    if n == 0 then acc else become even(acc + "o", n - 1)""",
            // The zero-iteration case returns the argument untouched; printed first because
            // assertOutput drops trailing empty lines
            """call println(build("", 0))""",
            """call println(build("", 5))""",
            """call println(even("", 6))""",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath, language = COL)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "",
                "ababababab",
                "eoeoeo",
            )
        )
    }

    // A string val inside a while body is covered by ColLlvmGarbageCollectionIT, whose loop is the
    // only place a timing-bounded iteration count is worth the noise. The collector behaviour these
    // programs depend on under real memory pressure is exercised there too.
}
