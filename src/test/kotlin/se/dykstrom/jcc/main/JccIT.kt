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

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Paths

class JccIT : AbstractIntegrationTest() {

    @Test
    fun noArguments() {
        val jcc = Jcc(arrayOf())
        assertEquals(1, jcc.run())
    }

    @Test
    fun invalidFileType() {
        val jcc = Jcc(buildCommandLine("""C:\Temp\any_file.invalid"""))
        assertEquals(1, jcc.run())
    }

    @Test
    fun fileNotFound() {
        val jcc = Jcc(buildCommandLine("""C:\Temp\does_not_exist.tiny"""))
        assertEquals(1, jcc.run())
    }

    @Test
    fun compileSyntaxErrorAssembunny() {
        compileAndAssertFail(createSourceFile(listOf("inc"), ASSEMBUNNY))
    }

    @Test
    fun compileSyntaxErrorBasic() {
        compileAndAssertFail(createSourceFile(listOf("10 GOTO"), BASIC))
    }

    @Test
    fun compileSemanticsErrorBasic() {
        compileAndAssertFail(createSourceFile(listOf("10 GOTO 20"), BASIC))
    }

    @Test
    fun compileSyntaxErrorTiny() {
        compileAndAssertFail(createSourceFile(listOf("BEGIN END"), TINY))
    }

    @Test
    fun compileSemanticsErrorTiny() {
        compileAndAssertFail(createSourceFile(listOf("BEGIN WRITE undefined END"), TINY))
    }

    @Test
    fun compileSuccessAssembunny() {
        compileAndAssertSuccess(createSourceFile(listOf("inc a"), ASSEMBUNNY))
    }

    @Test
    fun compileSuccessBasic() {
        compileAndAssertSuccess(createSourceFile(listOf("10 PRINT"), BASIC))
    }

    @Test
    fun compileSuccessTiny() {
        compileAndAssertSuccess(createSourceFile(listOf("BEGIN WRITE 1 END"), TINY))
    }

    @Test
    fun compileSuccessFasmReservedWord() {
        compileAndAssertSuccess(createSourceFile(listOf("BEGIN READ section, db, format END"), TINY))
    }

    @Test
    fun optionOutputFilename() {
        val path = createSourceFile(listOf("BEGIN WRITE 1 END"), TINY)

        val sourceFilename = path.toString()
        val asmFilename = convertFilename(sourceFilename, ASM)
        val exeFilename = convertFilename(sourceFilename, "foo")

        Paths.get(exeFilename).toFile().deleteOnExit()

        val jcc = Jcc(buildCommandLine(sourceFilename, "-o", exeFilename))
        assertSuccessfulCompilation(jcc, asmFilename, exeFilename)
    }
}
