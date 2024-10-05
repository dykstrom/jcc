/*
 * Copyright (C) 2016 Johan Dykstrom
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

import org.junit.jupiter.api.Assertions.*
import se.dykstrom.jcc.common.utils.FileUtils
import se.dykstrom.jcc.common.utils.ProcessUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Abstract base class for integration tests.
 *
 * @author Johan Dykstrom
 */
abstract class AbstractIntegrationTests {

    companion object {

        const val ASM = "asm"
        const val EXE = "exe"

        private const val ASM_OPTION = "-assembler"
        private const val ASM_VALUE = "../fasm/FASM.EXE"
        private const val ASM_INC_OPTION = "-assembler-include"
        private const val ASM_INC_VALUE = "../fasm/INCLUDE"

        /**
         * Creates a temporary file, whose contents will be `source` and with an extension
         * matching `language`. The file will be encoded in UTF-8. The file will be created
         * in the project's target directory. This directory can be excluded from virus scanning
         * to improve test performance.
         */
        fun createSourceFile(source: List<String>, language: Language): Path {
            val path = Files.createTempFile(Path.of("target"), "it_", "." + language.extension())
            path.toFile().deleteOnExit()
            Files.write(path, source, StandardCharsets.UTF_8)
            return path
        }

        /**
         * Builds a command line for running the flat assembler.
         */
        fun buildCommandLine(sourceFilename: String, vararg otherArgs: String): Array<String> {
            val args = ArrayList<String>()
            args.add(ASM_OPTION)
            args.add(ASM_VALUE)
            args.add(ASM_INC_OPTION)
            args.add(ASM_INC_VALUE)
            args.addAll(listOf(*otherArgs))
            args.add(sourceFilename)
            return args.toTypedArray()
        }

        /**
         * Asserts that the compilation finished successfully, and that the asm and exe files exist.
         */
        fun assertSuccessfulCompilation(jcc: Jcc, asmPath: Path, exePath: Path) {
            assertEquals(0, jcc.run(), "Compiler exit value non-zero,")
            assertTrue(Files.exists(asmPath), "asm file not found: $asmPath")
            assertTrue(Files.exists(exePath), "exe file not found: $exePath")
        }

        /**
         * Compiles the given source file, and asserts that the compilation failed.
         */
        fun compileAndAssertFail(sourcePath: Path) {
            val jcc = Jcc(buildCommandLine(sourcePath.toString()))
            assertEquals(1, jcc.run())
        }

        /**
         * Compiles the given source file, and asserts that the compilation is successful.
         */
        fun compileAndAssertSuccess(sourcePath: Path, extraArg: String) {
            compileAndAssertSuccess(sourcePath, false, 100, extraArg)
        }

        /**
         * Compiles the given source file, and asserts that the compilation is successful.
         *
         * @param sourcePath The path to the source file to compile.
         * @param printGc Enable GC debug information if true.
         * @param initialGcThreshold Number of memory allocations before first GC.
         * @param extraArg An extra argument, e.g. an optimization flag, or `null` if no extra argument.
         */
        fun compileAndAssertSuccess(
            sourcePath: Path,
            printGc: Boolean = false,
            initialGcThreshold: Int = 100,
            extraArg: String? = null
        ) {
            val asmPath = FileUtils.withExtension(sourcePath, ASM)
            val exePath = FileUtils.withExtension(sourcePath, EXE)
            exePath.toFile().deleteOnExit()
            val args = ArrayList<String>()
            if (printGc) {
                args.add("-print-gc")
                args.add("-initial-gc-threshold")
                args.add(initialGcThreshold.toString())
            }
            if (extraArg != null) {
                args.add(extraArg)
            }
            val jcc = Jcc(buildCommandLine(sourcePath.toString(), *args.toTypedArray()))
            assertSuccessfulCompilation(jcc, asmPath, exePath)
        }

        /**
         * Runs the program that results from compiling the given source file,
         * and compares the output and exit value of the program with the expected
         * output and exit value.
         *
         * @param sourcePath A source file that has previously been compiled to an executable program.
         * @param expectedOutput The expected output of the program.
         * @param expectedExitValue The expected exit value, or `null` if exit value does not matter.
         */
        fun runAndAssertSuccess(sourcePath: Path, expectedOutput: String, expectedExitValue: Int? = null) {
            val exePath = FileUtils.withExtension(sourcePath, EXE)
            var process: Process? = null
            try {
                process = ProcessUtils.setUpProcess(listOf(exePath.toString()), emptyMap())
                assertFalse(process.isAlive, "Process is still alive")
                if (expectedExitValue != null) {
                    assertEquals(expectedExitValue, process.exitValue(), "Exit value differs:")
                }
                val actualOutput = ProcessUtils.readOutput(process)
                assertEquals(expectedOutput, actualOutput, "Program output differs:")
            } finally {
                if (process != null) {
                    ProcessUtils.tearDownProcess(process)
                }
            }
        }

        /**
         * Runs the program that results from compiling the given source file,
         * and compares the output and exit value of the program with the expected
         * output and exit value.
         *
         * @param sourcePath A source file that has previously been compiled to an executable program.
         * @param input Text to provide as input to the program.
         * @param expectedOutput The expected output of the program.
         */
        fun runAndAssertSuccess(sourcePath: Path, input: List<String>, expectedOutput: List<String>) {
            val exePath = FileUtils.withExtension(sourcePath, EXE)

            // Write input to a temporary file
            val inputPath = Files.createTempFile(null, null)
            Files.write(inputPath, input, StandardCharsets.UTF_8)
            val inputFile = inputPath.toFile()
            inputFile.deleteOnExit()
            var process: Process? = null
            try {
                process = ProcessUtils.setUpProcess(listOf(exePath.toString()), inputFile, emptyMap())
                assertFalse(process.isAlive, "Process is still alive")
                assertEquals(0, process.exitValue(), "Exit value differs:")
                val actualOutput = ProcessUtils.readOutput(process)
                assertOutput(expectedOutput, actualOutput)
            } finally {
                if (process != null) {
                    ProcessUtils.tearDownProcess(process)
                }
            }
        }

        /**
         * Runs the program that results from compiling the given source file,
         * and compares the output and exit value of the program with the expected
         * output and exit value. This method splits the actual output into lines,
         * and then compares with the expected output line by line. However, only
         * the beginning of the lines are compared. This can be used when you know
         * what will be printed at the beginning of each line, but not the complete
         * line, which may contain some dynamic data.
         *
         * @param sourcePath A source file that has previously been compiled to an executable program.
         * @param expectedOutput The expected output of the program.
         */
        fun runAndAssertSuccess(sourcePath: Path, expectedOutput: List<String>) {
            val exePath = FileUtils.withExtension(sourcePath, EXE)
            var process: Process? = null
            try {
                process = ProcessUtils.setUpProcess(listOf(exePath.toString()), emptyMap())
                assertFalse(process.isAlive, "Process is still alive")
                assertEquals(0, process.exitValue(), "Exit value differs:")
                val actualOutput = ProcessUtils.readOutput(process)
                assertOutput(expectedOutput, actualOutput)
            } finally {
                if (process != null) {
                    ProcessUtils.tearDownProcess(process)
                }
            }
        }

        /**
         * Asserts that the actual output equals the expected output, after first splitting
         * the actual output into several lines.
         */
        fun assertOutput(expectedOutput: List<String>, actualOutput: String) {
            val actualLines = actualOutput.split("\n").dropLastWhile { it.isEmpty() }.toTypedArray()
            assertEquals(expectedOutput.size, actualLines.size, "Number of lines differ:")
            for (i in expectedOutput.indices) {
                assertTrue(
                    actualLines[i].startsWith(expectedOutput[i]),
                    "Output differs on line " + i + ": " + "expected:<" + expectedOutput[i] + "> but was:<" + actualLines[i] + ">"
                )
            }
        }
    }
}
