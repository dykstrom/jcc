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

package se.dykstrom.jcc.main;

import static java.util.Arrays.asList;

import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

/**
 * Compile-and-run integration tests for Assembunny.
 *
 * @author Johan Dykstrom
 */
public class AssembunnyCompileAndRunIT extends AbstractIntegrationTest {

    @Test
    public void shouldExitWith5() throws Exception {
        List<String> source = asList(
                "cpy 4 c",
                "cpy c a",
                "inc a",
                "dec a",
                "jnz a 2",
                "cpy 0 a",
                "inc a",
                "outn a"
        );
        Path sourceFile = createSourceFile(source, ASSEMBUNNY);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "5\n", 5);
    }

    @Test
    public void shouldExitWith12() throws Exception {
        List<String> source = asList(
                "cpy 3 b",
                "cpy 4 c",
                "inc a",
                "dec c",
                "jnz c -2",
                "dec b",
                "jnz b -5",
                "outn a"
        );
        Path sourceFile = createSourceFile(source, ASSEMBUNNY);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "12\n", 12);
    }
}
