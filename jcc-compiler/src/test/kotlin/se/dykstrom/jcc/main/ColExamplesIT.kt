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
 * writes the `.ll` file next to its input, and `src/examples` is not gitignored. Each example
 * is compiled and run at the default -O0 and again at -O1, because the AST optimizer only runs
 * at -O1 and above.
 */
class ColExamplesIT : AbstractIntegrationTests() {

    @Test
    fun shouldCountLetters() = forEachLevel { level ->
        compileExample(LETTER_FREQUENCY, level)
        runAndAssertSuccess(SAMPLE_INPUT, SAMPLE_REPORT)
    }

    @Test
    fun shouldCountLettersWhenFinalLineIsUnterminated() = forEachLevel { level ->
        // Real end of input, where the last line has no trailing newline. runAndAssertSuccess
        // terminates every line it is given, and so never reaches this case.
        compileExample(LETTER_FREQUENCY, level)
        runAndAssertSuccessWithRawInput(SAMPLE_INPUT.joinToString("\n"), SAMPLE_REPORT)
    }

    @Test
    fun shouldReportZeroesForEmptyInput() = forEachLevel { level ->
        // No letters at all means no denominator, and dividing an integer by zero is undefined
        compileExample(LETTER_FREQUENCY, level)
        runAndAssertSuccess(listOf(), EMPTY_REPORT)
    }

    @Test
    fun shouldConvertUnits() = forEachLevel { level ->
        compileExample(UNITS, level)
        runAndAssertSuccess(listOf(), listOf("115.240000", "80.000000", "true", "200.000000"))
    }

    @Test
    fun shouldConvertBetweenUnits() = forEachLevel { level ->
        compileExample(OTHER_UNITS, level)
        runAndAssertSuccess(listOf(), listOf("100.000000 km/h", "10.000000 s", "0.277778 km", "277.777778 m"))
    }

    @Test
    fun shouldUseConstants() = forEachLevel { level ->
        compileExample(CONSTANTS, level)
        runAndAssertSuccess(listOf(), listOf("Constants", "50.000000", "6.283185", "1200.000000", "large"))
    }

    @Test
    fun shouldComputeWithBigIntegers() = forEachLevel { level ->
        compileExample(BIGINT, level)
        runAndAssertSuccess(listOf(), BIGINT_OUTPUT)
    }

    companion object {
        private val LETTER_FREQUENCY = Path.of("src", "examples", "col", "letter_frequency.col")
        private val UNITS = Path.of("src", "examples", "col", "units.col")
        private val CONSTANTS = Path.of("src", "examples", "col", "constants.col")
        private val OTHER_UNITS = Path.of("src", "examples", "col", "other_units.col")
        private val BIGINT = Path.of("src", "examples", "col", "bigint.col")

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

        private val BIGINT_OUTPUT = listOf(
            "123456789012345678900246913569",
            "123456789012345678902222222211",
            "-123456789012345678902222222211",
            "-121932631124828532112482853211126352690",
            "9223372036854775808",
            "999",
            "0",
            "15511210043330985984000000",
            "93326215443944152681699238856266700490715968264381621468592963895217599993229915608941463976156518286253697920827223758251185210916864000000000000000000000000",
        )

        private val EMPTY_REPORT = listOf("Letters: 0") + ('a'..'z').map { "$it:     0  (  0.0%)" }

        /**
         * Compiles a shipped example, from a copy in `target` so that the generated `.ll` file
         * does not land in the source tree.
         */
        /** The compiler arguments for each optimization level the examples are tested at: the default -O0, and -O1. */
        private val LEVELS = listOf(listOf(), listOf("-O1"))

        private fun forEachLevel(test: (List<String>) -> Unit) = LEVELS.forEach(test)

        private fun compileExample(examplePath: Path, level: List<String>) {
            val sourcePath = createSourceFile(Files.readAllLines(examplePath, UTF_8), COL)
            compileAndAssertSuccess(sourcePath, COL, *level.toTypedArray())
        }
    }
}
