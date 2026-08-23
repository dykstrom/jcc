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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.utils.FileUtils
import se.dykstrom.jcc.main.Language.ASSEMBUNNY
import se.dykstrom.jcc.main.Language.TINY
import java.io.File
import java.nio.file.Files

class JccIT : AbstractIntegrationTests() {

    @BeforeEach
    fun checkWorkingDir() {
        val workingDir = File(".").absolutePath
        assertTrue(
            workingDir.contains("jcc-compiler"),
            "Expected working dir 'jcc-compiler', but was '$workingDir'"
        )
    }

    @Test
    fun compileSyntaxErrorAssembunny() {
        compileAndAssertFail(createSourceFile(listOf("inc"), ASSEMBUNNY))
    }

    @Test
    fun compileSyntaxErrorBasic() {
        compileAndAssertFail(createSourceFile(listOf("10 GOTO"), Language.BASIC))
    }

    @Test
    fun compileSemanticsErrorBasic() {
        compileAndAssertFail(createSourceFile(listOf("10 GOTO 20"), Language.BASIC))
    }

    @Test
    fun compileSyntaxErrorTiny() {
        compileAndAssertFail(createSourceFile(listOf("BEGUN END"), TINY))
    }

    @Test
    fun compileSemanticsErrorTiny() {
        compileAndAssertFail(createSourceFile(listOf("BEGIN WRITE undefined END"), TINY))
    }

    @Test
    fun compileButNotAssemble() {
        // Given: -S, so the compiler stops after emitting the .ll file
        val sourcePath = createSourceFile(listOf("10 PRINT"), Language.BASIC)
        val llvmPath = FileUtils.withExtension(sourcePath, LL)
        llvmPath.toFile().deleteOnExit()
        val args = arrayOf("-S", sourcePath.toString())

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(0, returnCode, "Compiler exit value non-zero,")
        assertTrue(Files.exists(llvmPath), "LLVM IR file not found: $llvmPath")
    }

    @Tag("LLVM")
    @Test
    fun optionOutputFilename() {
        val sourcePath = createSourceFile(listOf("BEGIN WRITE 1 END"), TINY)
        val llvmPath = FileUtils.withExtension(sourcePath, LL)
        val outputPath = FileUtils.withExtension(sourcePath, "foo")

        outputPath.toFile().deleteOnExit()

        val jcc = Jcc(arrayOf("-o", outputPath.toString(), sourcePath.toString()))
        assertSuccessfulCompilation(jcc, llvmPath, outputPath)
    }
}
