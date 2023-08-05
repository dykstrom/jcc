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

import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemErrRule
import org.junit.contrib.java.lang.system.SystemOutRule
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JccTests {

    @JvmField
    @Rule
    val systemOutRule: SystemOutRule = SystemOutRule().enableLog()

    @JvmField
    @Rule
    val systemErrRule: SystemErrRule = SystemErrRule().enableLog()

    @Test
    fun shouldPrintVersion() {
        // Given
        val args = arrayOf("--version")

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(0, returnCode)
        assertTrue(systemOutRule.log.startsWith("jcc"))
    }

    @Test
    fun shouldPrintHelp() {
        // Given
        val args = arrayOf("--help")

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(1, returnCode)
        assertTrue(systemOutRule.log.startsWith("Usage: jcc"))
    }

    @Test
    fun shouldPrintHelpIfNoArgs() {
        // Given
        val args: Array<String> = arrayOf()

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(1, returnCode)
        assertTrue(systemOutRule.log.startsWith("Usage: jcc"))
    }

    @Test
    fun shouldReportNoFileType() {
        // Given
        val path = Files.createTempFile("ut_", "")
        val args = arrayOf(path.toString())

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(1, returnCode)
        assertTrue(systemErrRule.log.contains("Cannot determine file type"))
    }

    @Test
    fun shouldReportInvalidFileType() {
        // Given
        val path = Files.createTempFile("ut_", ".invalid")
        val args = arrayOf(path.toString())

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(1, returnCode)
        assertTrue(systemErrRule.log.contains("Invalid file type"))
    }

    @Test
    fun shouldReportFileNotFound() {
        // Given
        val args = arrayOf("does_not_exist.tiny")

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(1, returnCode)
        assertTrue(systemErrRule.log.startsWith("jcc: error: does_not_exist.tiny: No such file or directory"))
    }

    @Test
    fun shouldCompileButNotAssemble() {
        // Given
        val sourcePath = Files.createTempFile(Path.of("target"), "ut_", ".bas")
        sourcePath.toFile().deleteOnExit()
        Files.write(sourcePath, listOf("10 PRINT"), UTF_8)
        val sourceFilename = sourcePath.toString()

        val asmFilename = sourceFilename.replace(".bas", ".asm")
        val asmPath = Path.of(asmFilename)
        asmPath.toFile().deleteOnExit()

        val args = arrayOf("-S", sourceFilename)

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(0, returnCode)
        assertTrue(Files.exists(asmPath), "asm file not found: $asmFilename")
    }
}
