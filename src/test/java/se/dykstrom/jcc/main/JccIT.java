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

package se.dykstrom.jcc.main;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class JccIT extends AbstractIntegrationTest {

    @Test
    public void noArguments() {
        Jcc jcc = new Jcc(new String[]{});
        assertEquals(1, jcc.run());
    }

    @Test
    public void invalidFileType() {
        Jcc jcc = new Jcc(buildCommandLine("C:\\Temp\\any_file.invalid"));
        assertEquals(1, jcc.run());
    }

    @Test
    public void fileNotFound() {
        Jcc jcc = new Jcc(buildCommandLine("C:\\Temp\\does_not_exist.tiny"));
        assertEquals(1, jcc.run());
    }

    @Test
    public void compileSyntaxErrorBasic() throws Exception {
        compileAndAssertFail(createSourceFile(singletonList("10 GOTO"), BASIC));
    }

    @Test
    public void compileSemanticsErrorBasic() throws Exception {
        compileAndAssertFail(createSourceFile(singletonList("10 GOTO 20"), BASIC));
    }

    @Test
    public void compileSyntaxErrorTiny() throws Exception {
        compileAndAssertFail(createSourceFile(singletonList("BEGIN END"), TINY));
    }

    @Test
    public void compileSemanticsErrorTiny() throws Exception {
        compileAndAssertFail(createSourceFile(singletonList("BEGIN WRITE undefined END"), TINY));
    }

    @Test
    public void compileSuccessBasic() throws Exception {
        compileAndAssertSuccess(createSourceFile(singletonList("10 PRINT"), BASIC));
    }

    @Test
    public void compileSuccessTiny() throws Exception {
        compileAndAssertSuccess(createSourceFile(singletonList("BEGIN WRITE 1 END"), TINY));
    }

    @Test
    public void compileSuccessFasmReservedWord() throws Exception {
        compileAndAssertSuccess(createSourceFile(singletonList("BEGIN READ section, db, format END"), TINY));
    }

    @Test
    public void optionOutputFilename() throws Exception {
        Path path = createSourceFile(singletonList("BEGIN WRITE 1 END"), TINY);

        String sourceFilename = path.toString();
        String asmFilename = convertFilename(sourceFilename, ASM);
        String exeFilename = convertFilename(sourceFilename, "foo");

        Paths.get(exeFilename).toFile().deleteOnExit();

        Jcc jcc = new Jcc(buildCommandLine(sourceFilename, "-o", exeFilename));
        assertSuccessfulCompilation(jcc, asmFilename, exeFilename);
    }
}
