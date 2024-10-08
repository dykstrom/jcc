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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import se.dykstrom.jcc.common.utils.FileUtils
import se.dykstrom.jcc.main.Language.ASSEMBUNNY
import se.dykstrom.jcc.main.Language.TINY
import java.io.File

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

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun compileSuccessAssembunny() {
        compileAndAssertSuccess(createSourceFile(listOf("inc a"), ASSEMBUNNY))
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun compileSuccessBasic() {
        compileAndAssertSuccess(createSourceFile(listOf("10 PRINT"), Language.BASIC))
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun compileSuccessTiny() {
        compileAndAssertSuccess(createSourceFile(listOf("BEGIN WRITE 1 END"), TINY))
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun compileSuccessFasmReservedWord() {
        compileAndAssertSuccess(createSourceFile(listOf("BEGIN READ section, db, format END"), TINY))
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun optionOutputFilename() {
        val sourcePath = createSourceFile(listOf("BEGIN WRITE 1 END"), TINY)
        val asmPath = FileUtils.withExtension(sourcePath, ASM)
        val exePath = FileUtils.withExtension(sourcePath, "foo")

        exePath.toFile().deleteOnExit()

        val jcc = Jcc(buildCommandLine(sourcePath.toString(), "-o", exePath.toString()))
        assertSuccessfulCompilation(jcc, asmPath, exePath)
    }
}
