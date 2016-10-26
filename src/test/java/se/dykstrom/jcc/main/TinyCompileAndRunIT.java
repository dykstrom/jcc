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

import static java.util.Collections.singletonList;

/**
 * Compile-and-run integration tests for Tiny.
 *
 * @author Johan Dykstrom
 */
public class TinyCompileAndRunIT extends AbstractIntegrationTest {

    @Test
    public void writeExpression() throws Exception {
        Path sourceFile = createSourceFile(singletonList("BEGIN WRITE 1 + 2 - 3 END"), TINY);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0\n");
    }
}
