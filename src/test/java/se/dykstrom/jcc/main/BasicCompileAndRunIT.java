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
    public void printLongerExpression() throws Exception {
        List<String> source = singletonList("10 PRINT 20 - 3 * 5 + 1 * 8 / 2");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "9\n");
    }

    @Test
    public void printGroupedExpressions() throws Exception {
        List<String> source = asList(
                "10 PRINT (1 + 2) * (3 - 4)",
                "20 PRINT (99 + 1)",
                "30 PRINT 2 * (90 / (5 + 5))"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "-3\n100\n18\n");
    }

    @Test
    public void printMultipleArgs() throws Exception {
        List<String> source = asList(
                "10 PRINT \"good \"; 2; \" go\"",
                "20 PRINT \"(1 + 2) * 3\"; \" = \"; (1 + 2) * 3"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "good 2 go\n(1 + 2) * 3 = 9\n");
    }

    @Test
    public void printAndGoto() throws Exception {
        List<String> source = asList("10 print \"A\"", "20 goto 40", "30 print \"B\"", "40 print \"C\"", "50 end");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "A\nC\n");
    }

    @Test
    public void gotoRem() throws Exception {
        List<String> source = asList("10 goto 30", "20 print \"A\"", "30 rem hi!", "40 print \"B\"");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "B\n");
    }

    @Test
    public void assignmentsWithIntegers() throws Exception {
        List<String> source = asList(
                "10 let a% = 5 + 7",
                "20 let b = 0 - 9",
                "30 print a% ; \" \" ; b");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "12 -9\n");
    }

    @Test
    public void assignmentsWithStrings() throws Exception {
        List<String> source = asList(
                "10 let a$ = \"A\"",
                "20 let b = \"B\"",
                "30 print a$ ; b");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "AB\n");
    }

    @Test
    public void reassignment() throws Exception {
        List<String> source = asList(
                "10 let str = \"A\" : let int = 0",
                "20 let str = \"B\" : let int = 1",
                "30 print str ; int");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "B1\n");
    }

    @Test
    public void printExpressionWithVariables() throws Exception {
        List<String> source = asList(
                "10 let value.1 = 9 : value.2 = -1",
                "20 print value.1 * value.2");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "-9\n");
    }

    @Test
    public void printWithReassignment() throws Exception {
        List<String> source = asList(
                "10 let a = 7",
                "20 print \"a=\"; a",
                "30 let a = a + 1",
                "40 print \"a=\"; a");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "a=7\na=8\n");
    }
}
