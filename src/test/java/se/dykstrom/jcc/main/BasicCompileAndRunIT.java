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

import static java.util.Arrays.asList;

import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

/**
 * Compile-and-run integration tests for Basic.
 *
 * @author Johan Dykstrom
 */
public class BasicCompileAndRunIT extends AbstractIntegrationTest {

    @Test
    public void shouldPrintExpressions() throws Exception {
        List<String> source = asList(
                "10 PRINT 5 + 2 * 7",
                "20 PRINT 8 / 1",
                "30 PRINT 1 + 2 + 3 + 4 + 5"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "19\n8\n15\n", 0);
    }

    @Test
    public void shouldPrintLongerExpressions() throws Exception {
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
    public void shouldPrintGroupedExpressions() throws Exception {
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
    public void shouldVerifyIntegerDivision() throws Exception {
        List<String> source = asList(
                "10 PRINT 7/2; -7/2; 7/-2; -7/-2",
                "20 PRINT 10/5; -10/5; 10/-5; -10/-5",
                "30 PRINT 27/5; -27/5; 27/-5; -27/-5"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "3-3-33\n2-2-22\n5-5-55\n");
    }
    
    @Test
    public void shouldVerifyIntegerModulo() throws Exception {
        List<String> source = asList(
                "10 PRINT 7 MOD 2; -7 MOD 2; 7 MOD -2; -7 MOD -2",
                "20 PRINT 10 MOD 5; -10 MOD 5; 10 MOD -5; -10 MOD -5",
                "30 PRINT 27 MOD 5; -27 MOD 5; 27 MOD -5; -27 MOD -5"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1-11-1\n0000\n2-22-2\n");
    }

    @Test
    public void shouldPrintMultipleArgs() throws Exception {
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
    public void shouldPrintAndGoto() throws Exception {
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
    public void shouldGotoRem() throws Exception {
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
    public void shouldGotoAssignment() throws Exception {
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
    public void shouldAssignIntegers() throws Exception {
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
    public void shouldAssignStrings() throws Exception {
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
    public void shouldAssignBooleans() throws Exception {
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
    public void shouldReassignNewValues() throws Exception {
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
    public void shouldPrintVariables() throws Exception {
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
    public void shouldPrintAndReassign() throws Exception {
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
    public void shouldPrintBooleanExpressions() throws Exception {
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
    public void shouldPrintBooleanExpressionsWithStrings() throws Exception {
        List<String> source = asList(
                "x = \"aa\"",
                "print \"ab\" = x; \"ab\" = \"ab\"; \"ab\" = \"ac\"",
                "print \"ab\" <> x; \"ab\" <> \"ab\"; \"ab\" <> \"ac\"",
                "print \"ab\" < x; \"ab\" < \"ab\"; \"ab\" < \"ac\"",
                "print \"ab\" <= x; \"ab\" <= \"ab\"; \"ab\" <= \"ac\"",
                "print \"ab\" > x; \"ab\" > \"ab\"; \"ab\" > \"ac\"",
                "print \"ab\" >= x; \"ab\" >= \"ab\"; \"ab\" >= \"ac\""
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0-10\n-10-1\n00-1\n0-1-1\n-100\n-1-10\n");
    }

    @Test
    public void shouldPrintConditionalExpressions() throws Exception {
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
    public void shouldPrintTruthTable() throws Exception {
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

    @Test
    public void shouldPrintFromIfClause() throws Exception {
        List<String> source = asList(
                "10 x = 7",
                "20 if x > 5 then",
                "30   print \"x>5\"",
                "40 end if",
                "50 if x < 10 then",
                "60   print \"x<10\"",
                "70 else",
                "80   print \"else\"",
                "90 endif",
                "100 print x"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "x>5\nx<10\n7\n");
    }

    @Test
    public void shouldPrintFromElseIfClause() throws Exception {
        List<String> source = asList(
                "x = 7",
                "if x < 5 then",
                "  print 5",
                "elseif x < 10 then",
                "  print 10",
                "  print 10",
                "else",
                "  print \"else\"",
                "endif"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "10\n10\n");
    }

    @Test
    public void shouldRunOneLineIfs() throws Exception {
        List<String> source = asList(
                "10 x% = 7",
                "20 if x% = 5 then 30 else print 20 : goto 40",
                "30 print 30",
                "40 print 40",
                "50 if x% <> 5 goto 60 else 70",
                "60 print 60",
                "70 print 70",
                "80 end"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "20\n40\n60\n70\n");
    }

    @Test
    public void shouldCalculateFaculty() throws Exception {
        List<String> source = asList(
                "10 n = 5",
                "20 result = 1",
                "30 i = n",
                "40 if i = 0 goto 100",
                "50 result = result * i",
                "60 i = i - 1",
                "70 goto 40",
                "100 print \"fac(\"; n; \")=\"; result"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "fac(5)=120\n");
    }

    @Test
    public void shouldPrintInWhile() throws Exception {
        List<String> source = asList(
                "a = 0",
                "while a < 3",
                "  b = 0",
                "  while b < 3",
                "    print a;\",\";b",
                "    b = b + 1",
                "  wend",
                "  a = a + 1",
                "wend"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0,0\n0,1\n0,2\n1,0\n1,1\n1,2\n2,0\n2,1\n2,2\n");
    }
}
