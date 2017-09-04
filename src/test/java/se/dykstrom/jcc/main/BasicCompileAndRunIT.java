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

/**
 * Compile-and-run integration tests for Basic.
 *
 * @author Johan Dykstrom
 */
public class BasicCompileAndRunIT extends AbstractIntegrationTest {

    @Test
    public void printExpression() throws Exception {
        List<String> source = asList(
                "10 PRINT 5 + 2 * 7",
                "20 PRINT 8 / 1",
                "30 PRINT 1 + 2 + 3 + 4 + 5"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "19\n8\n15\n");
    }

    @Test
    public void printLongerExpression() throws Exception {
        List<String> source = asList(
                "10 PRINT 20 - 3 * 5 + 1 * 8 / 2",
                "20 PRINT 5 - 3 + 7 * 2 - 10 * 20 / 5",
                "30 PRINT 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "9\n-24\n55\n");
    }

    @Test
    public void printGroupedExpressions() throws Exception {
        List<String> source = asList(
                "10 PRINT (1 + 2) * (3 - 4)",
                "20 PRINT (99 + 1)",
                "30 PRINT 2 * (90 / (5 + 5))",
                "40 PRINT (7 - 2) * 1 + 2 * (90 / (5 + 5)) - (8 - (2 * 2))"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "-3\n100\n18\n19\n");
    }

    @Test
    public void printMultipleArgs() throws Exception {
        List<String> source = asList(
                "10 PRINT \"good \"; 2; \" go\"",
                "20 PRINT \"(1 + 2) * 3\"; \" = \"; (1 + 2) * 3",
                "30 PRINT 1, 2, 3, 4, 5, 6, 7, 8, 9, 10"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "good 2 go\n(1 + 2) * 3 = 9\n12345678910\n");
    }

    @Test
    public void printAndGoto() throws Exception {
        List<String> source = asList(
                "10 print \"A\"", 
                "20 goto 40", 
                "30 print \"B\"", 
                "40 print \"C\"", 
                "50 end"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "A\nC\n");
    }

    @Test
    public void gotoRem() throws Exception {
        List<String> source = asList(
                "10 goto 30", 
                "20 print \"A\"", 
                "30 rem hi!", 
                "40 print \"B\""
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "B\n");
    }

    @Test
    public void gotoAssignment() throws Exception {
        List<String> source = asList(
                "10 goto 30", 
                "20 print \"A\"", 
                "30 x = 10", 
                "40 print x"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "10\n");
    }

    @Test
    public void assignmentsWithIntegers() throws Exception {
        List<String> source = asList(
                "10 let a% = 5 + 7",
                "20 let b = 0 - 9",
                "30 print a% ; \" \" ; b",
                "40 let c = a% * b + 1",
                "50 print c"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "12 -9\n-107\n");
    }

    @Test
    public void assignmentsWithStrings() throws Exception {
        List<String> source = asList(
                "10 let a$ = \"A\"",
                "20 let b = \"B\"",
                "30 print a$ ; b"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "AB\n");
    }

    @Test
    public void assignmentsWithBooleans() throws Exception {
        List<String> source = asList(
                "10 let a = TRUE",
                "20 let b = FALSE",
                "30 let c = a OR b",
                "40 print a ; \" \" ; b ; \" \" ; c"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "-1 0 -1\n");
    }

    @Test
    public void reassignment() throws Exception {
        List<String> source = asList(
                "10 let str = \"A\" : let int = 0",
                "20 let str = \"B\" : let int = 1",
                "30 print str ; int",
                "40 let str = \"\" : let int = -17",
                "50 print str ; int"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "B1\n-17\n");
    }

    @Test
    public void printExpressionWithVariables() throws Exception {
        List<String> source = asList(
                "10 let value.1 = 9 : value.2 = -1",
                "20 print value.1 * value.2",
                "30 print value.1 / value.2",
                "40 print value.1 + value.2 * value.2"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "-9\n-9\n10\n");
    }

    @Test
    public void printWithReassignment() throws Exception {
        List<String> source = asList(
                "10 let a = 7",
                "20 print \"a=\"; a",
                "30 let a = a + 1",
                "40 print \"a=\"; a"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "a=7\na=8\n");
    }

    @Test
    public void printBooleanExpressions() throws Exception {
        List<String> source = asList(
                "10 let a = 7 : print a",
                "20 let b = 5 : print b",
                "30 let eq = a = b : print eq",
                "40 let ne = a <> b : print ne",
                "50 let gt = a > b : print gt",
                "60 let ge = a >= b : print ge",
                "70 let lt = a < b : print lt",
                "80 let le = a <= b : print le"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "7\n5\n0\n-1\n-1\n-1\n0\n0\n");
    }

    @Test
    public void printConditionalExpressions() throws Exception {
        List<String> source = asList(
                "10 let a = 7 + 8: print a",
                "20 let b = 5 - 2: print b",
                "30 let eq = a = 15 : print eq",
                "40 let ne = a <> 30 : print ne",
                "60 print eq and b > a",
                "70 print eq or a > b",
                "80 print (ne or b = 0) and (ne or true)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "15\n3\n-1\n-1\n0\n-1\n-1\n");
    }

    @Test
    public void printTruthTable() throws Exception {
        List<String> source = asList(
                "10 PRINT \"T AND T = \"; TRUE AND TRUE",
                "20 PRINT \"T AND F = \"; TRUE AND FALSE",
                "30 PRINT \"F AND T = \"; FALSE AND TRUE",
                "40 PRINT \"F AND F = \"; FALSE AND FALSE",
                "50 PRINT \"T OR T = \"; TRUE OR TRUE",
                "60 PRINT \"T OR F = \"; TRUE OR FALSE",
                "70 PRINT \"F OR T = \"; FALSE OR TRUE",
                "80 PRINT \"F OR F = \"; FALSE OR FALSE"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, 
                "T AND T = -1\nT AND F = 0\nF AND T = 0\nF AND F = 0\nT OR T = -1\nT OR F = -1\nF OR T = -1\nF OR F = 0\n");
    }
}
