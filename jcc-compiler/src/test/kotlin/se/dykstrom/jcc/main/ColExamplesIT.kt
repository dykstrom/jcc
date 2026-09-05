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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.COL
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

/**
 * Compile-and-run integration tests for the example programs shipped in `src/examples`.
 *
 * The examples are packaged into the distribution but nothing else in the build compiles them,
 * so they can rot without any build failing - see "Examples are packaged, never compiled" in
 * `docs/system/build.md`. This class closes that gap for the examples it covers.
 *
 * The source is copied into `target` rather than compiled where it lies, because the compiler
 * writes the `.ll` file next to its input, and `src/examples` is not gitignored.
 */
class ColExamplesIT : AbstractIntegrationTests() {

    @Test
    fun shouldCountLetters() {
        compileExample(LETTER_FREQUENCY)
        runAndAssertSuccess(SAMPLE_INPUT, SAMPLE_REPORT)
    }

    @Test
    fun shouldCountLettersWhenFinalLineIsUnterminated() {
        // Real end of input, where the last line has no trailing newline. runAndAssertSuccess
        // terminates every line it is given, and so never reaches this case.
        compileExample(LETTER_FREQUENCY)
        runAndAssertSuccessWithRawInput(SAMPLE_INPUT.joinToString("\n"), SAMPLE_REPORT)
    }

    @Test
    fun shouldReportZeroesForEmptyInput() {
        // No letters at all means no denominator, and dividing an integer by zero is undefined
        compileExample(LETTER_FREQUENCY)
        runAndAssertSuccess(listOf(), EMPTY_REPORT)
    }

    companion object {
        private val LETTER_FREQUENCY = Path.of("src", "examples", "col", "letter_frequency.col")

        private val SAMPLE_INPUT = listOf(
            "Hello, World!",
            "The quick brown fox jumps over the lazy dog.",
        )

        private val SAMPLE_REPORT = listOf(
            "Letters: 45",
            "a:     1  (  2.2%)",
            "b:     1  (  2.2%)",
            "c:     1  (  2.2%)",
            "d:     2  (  4.4%)",
            "e:     4  (  8.9%)",
            "f:     1  (  2.2%)",
            "g:     1  (  2.2%)",
            "h:     3  (  6.7%)",
            "i:     1  (  2.2%)",
            "j:     1  (  2.2%)",
            "k:     1  (  2.2%)",
            "l:     4  (  8.9%)",
            "m:     1  (  2.2%)",
            "n:     1  (  2.2%)",
            "o:     6  ( 13.3%)",
            "p:     1  (  2.2%)",
            "q:     1  (  2.2%)",
            "r:     3  (  6.7%)",
            "s:     1  (  2.2%)",
            "t:     2  (  4.4%)",
            "u:     2  (  4.4%)",
            "v:     1  (  2.2%)",
            "w:     2  (  4.4%)",
            "x:     1  (  2.2%)",
            "y:     1  (  2.2%)",
            "z:     1  (  2.2%)",
        )

        private val EMPTY_REPORT = listOf("Letters: 0") + ('a'..'z').map { "$it:     0  (  0.0%)" }

        /**
         * Compiles a shipped example, from a copy in `target` so that the generated `.ll` file
         * does not land in the source tree.
         */
        private fun compileExample(examplePath: Path) {
            val sourcePath = createSourceFile(Files.readAllLines(examplePath, UTF_8), COL)
            compileAndAssertSuccess(sourcePath, language = COL)
        }
    }
}
