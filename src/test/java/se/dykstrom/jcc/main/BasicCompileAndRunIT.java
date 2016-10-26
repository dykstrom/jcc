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
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * Compile-and-run integration tests for Basic.
 *
 * @author Johan Dykstrom
 */
public class BasicCompileAndRunIT extends AbstractIntegrationTest {

    @Test
    public void printExpression() throws Exception {
        List<String> source = singletonList("10 PRINT 5 + 2 * 7");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "19\n");
    }

    @Test
    public void printMultipleArgs() throws Exception {
        List<String> source = singletonList("10 PRINT \"good \"; 2; \" go\"");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "good 2 go\n");
    }

    @Test
    public void printAndGoto() throws Exception {
        List<String> source = asList("10 print \"A\"", "20 goto 40", "30 print \"B\"", "40 print \"C\"", "50 end");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "A\nC\n");
    }
}
