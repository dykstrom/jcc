/*
 * Copyright (C) 2017 Johan Dykstrom
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.utils.FileUtils
import se.dykstrom.jcc.common.utils.ProcessUtils
import se.dykstrom.jcc.main.Language.ASSEMBUNNY
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Compile-and-run integration tests for the Assembunny LLVM backend.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class AssembunnyLlvmCompileAndRunIT : AbstractIntegrationTests() {

    @Test
    fun shouldExitWith5() {
        val source = listOf(
                "cpy 4 c",
                "cpy c a",
                "inc a",
                "dec a",
                "jnz a 2",
                "cpy 0 a",
                "inc a",
                "outn a"
        )
        val sourceFile = createSourceFile(source, ASSEMBUNNY)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(listOf(), listOf("5"), 5)
    }

    @Test
    fun shouldExitWith12() {
        val source = listOf(
                "cpy 3 b",
                "cpy 4 c",
                "inc a",
                "dec c",
                "jnz c -2",
                "dec b",
                "jnz b -5",
                "outn a"
        )
        val sourceFile = createSourceFile(source, ASSEMBUNNY)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(listOf(), listOf("12"), 12)
    }

    fun compileAndAssertSuccess(sourcePath: Path, extraArg: String? = null) {
        val llvmPath = FileUtils.withExtension(sourcePath, "ll")
        val outputPath = Path.of("target", "a.out")
        outputPath.toFile().deleteOnExit()
        val args = ArrayList<String>()
        args.add("--backend")
        args.add("LLVM")
        if (extraArg != null) {
            args.add(extraArg)
        }
        args.add("-o")
        args.add(outputPath.toString())
        args.add(sourcePath.toString())
        val jcc = Jcc(args.toTypedArray())
        assertSuccessfulCompilation(jcc, llvmPath, outputPath)
    }

    fun runAndAssertSuccess(input: List<String>, expectedOutput: List<String>, expectedExitValue: Int = 0) {
        val outputPath = Path.of("target", "a.out")

        // Write input to a temporary file
        val inputPath = Files.createTempFile(null, null)
        Files.write(inputPath, input, StandardCharsets.UTF_8)
        val inputFile = inputPath.toFile()
        inputFile.deleteOnExit()

        var process: Process? = null
        try {
            process = ProcessUtils.setUpProcess(listOf(outputPath.toString()), inputFile, emptyMap())
            assertFalse(process.isAlive, "Process is still alive")
            assertEquals(expectedExitValue, process.exitValue(), "Exit value differs:")
            val actualOutput = ProcessUtils.readOutput(process)
            assertOutput(expectedOutput, actualOutput)
        } finally {
            if (process != null) {
                ProcessUtils.tearDownProcess(process)
            }
        }
    }
}
