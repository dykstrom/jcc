/*
 * Copyright (C) 2023 Johan Dykstrom
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

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.utils.FileUtils
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class JccTests {

    @Test
    fun shouldPrintVersion() {
        // Given
        val args = arrayOf("--version")

        // When
        val output = tapSystemOut {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertTrue(output.startsWith("jcc"))
    }

    @Test
    fun shouldPrintHelp() {
        // Given
        val args = arrayOf("--help")

        // When
        val output = tapSystemOut {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.startsWith("Usage: jcc"))
    }

    @Test
    fun shouldPrintHelpIfNoArgs() {
        // Given
        val args: Array<String> = arrayOf()

        // When
        val output = tapSystemOut {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.startsWith("Usage: jcc"))
    }

    @Test
    fun shouldReportInvalidBackend() {
        // Given
        val args = arrayOf("--backend", "FOO")

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("jcc: Invalid value for --backend parameter"))
    }

    @Test
    fun shouldReportNoFileType() {
        // Given
        val path = Files.createTempFile("ut_", "")
        val args = arrayOf(path.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("Cannot determine file type"))
    }

    @Test
    fun shouldReportInvalidFileType() {
        // Given
        val path = Files.createTempFile("ut_", ".invalid")
        val args = arrayOf(path.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("Invalid file type"))
    }

    @Test
    fun shouldReportFileNotFound() {
        // Given
        val args = arrayOf("does_not_exist.tiny")

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.startsWith("jcc: error: does_not_exist.tiny: No such file or directory"))
    }

    @Test
    fun shouldReportUndefinedFunctionError() {
        // Given
        val (sourcePath, _) = createSourceFile("PRINT foo(17)")
        val args = arrayOf("-S", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("error: undefined function: foo"))
    }

    @Test
    fun shouldReportUndefinedVariableWarning() {
        // Given
        val (sourcePath, _) = createSourceFile("PRINT foo")
        val args = arrayOf("-S", "-Wundefined-variable", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("warning: undefined variable: foo"))
    }

    @Test
    fun shouldNotReportUndefinedVariableWarning() {
        // Given
        val (sourcePath, _) = createSourceFile("PRINT foo")
        val args = arrayOf("-S", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertFalse(output.contains("warning"))
    }

    @Test
    fun shouldReportFloatConversionWarning() {
        // Given
        val (sourcePath, _) = createSourceFile("PRINT hex$(27.5)")
        val args = arrayOf("-S", "-Wfloat-conversion", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("warning: implicit conversion turns floating-point number into integer"))
    }

    @Test
    fun shouldCompileButNotAssemble() {
        // Given
        val (sourcePath, asmPath) = createSourceFile("PRINT")
        val args = arrayOf("-S", sourcePath.toString())

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(0, returnCode)
        assertTrue(Files.exists(asmPath), "asm file not found: $asmPath")
    }

    @Tag("LLVM")
    @Test
    fun shouldCompileButNotAssembleLlvm() {
        // Given
        val (sourcePath, asmPath) = createSourceFile("BEGIN WRITE 0 END", "tiny", "ll")
        val args = arrayOf("--backend", "LLVM", "-S", sourcePath.toString())

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(0, returnCode)
        assertTrue(Files.exists(asmPath), "LLVM IR file not found: $asmPath")
    }

    private fun createSourceFile(text: String, sourceExt: String = "bas", outputExt: String = "asm"): Pair<Path, Path> {
        val sourcePath = Files.createTempFile("ut_", ".$sourceExt")
        sourcePath.toFile().deleteOnExit()
        Files.write(sourcePath, listOf(text), UTF_8)
        val outputPath = FileUtils.withExtension(sourcePath, outputExt)
        outputPath.toFile().deleteOnExit()

        // Remove temporary file created by clang when LLVM backend is used
        val clangAssemblyPath = FileUtils.withExtension(sourcePath.fileName, "s")
        clangAssemblyPath.toFile().deleteOnExit()

        return Pair(sourcePath, outputPath)
    }
}
