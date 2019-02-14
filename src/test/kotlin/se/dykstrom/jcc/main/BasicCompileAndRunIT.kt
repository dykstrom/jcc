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

import org.junit.Test
import java.util.Arrays.asList

/**
 * Compile-and-run integration tests for Basic.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunIT : AbstractIntegrationTest() {

    @Test
    fun shouldPrintExpressions() {
        val source = asList(
                "10 PRINT 5 + 2 * 7",
                "20 PRINT 8 \\ 1",
                "30 PRINT 1 + 2 + 3 + 4 + 5",
                "40 PRINT &HFE + &H01",
                "50 PRINT &O10 - &O5",
                "60 PRINT &B10010 + &B101"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "19\n8\n15\n255\n3\n23\n", 0)
    }

    @Test
    fun shouldPrintLongerExpressions() {
        val source = asList(
                "10 PRINT 20 - 3 * 5 + 1 * 8 \\ 2",
                "20 PRINT 5 - 3 + 7 * 2 - 10 * 20 \\ 5",
                "30 PRINT 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "9\n-24\n55\n")
    }

    @Test
    fun shouldPrintStringExpressions() {
        val source = asList(
                "PRINT \"A\" + \"B\"",
                "PRINT \"one\" + \"two\" + \"three\"",
                "PRINT \"12345\" + \"\" + \"67890\" + \"\" + \"abcde\""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "AB\nonetwothree\n1234567890abcde\n")
    }

    @Test
    fun shouldPrintGroupedExpressions() {
        val source = asList(
                "10 PRINT (1 + 2) * (3 - 4)",
                "20 PRINT (99 + 1)",
                "30 PRINT 2 * (90 \\ (5 + 5))",
                "40 PRINT (7 - 2) * 1 + 2 * (90 \\ (5 + 5)) - (8 - (2 * 2))"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-3\n100\n18\n19\n")
    }

    @Test
    fun shouldVerifyIntegerDivision() {
        val source = asList(
                "10 PRINT 7\\2; -7\\2; 7\\-2; -7\\-2",
                "20 PRINT 10\\5; -10\\5; 10\\-5; -10\\-5",
                "30 PRINT 27\\5; -27\\5; 27\\-5; -27\\-5"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3-3-33\n2-2-22\n5-5-55\n")
    }

    @Test
    fun shouldVerifyIntegerModulo() {
        val source = asList(
                "10 PRINT 7 MOD 2; -7 MOD 2; 7 MOD -2; -7 MOD -2",
                "20 PRINT 10 MOD 5; -10 MOD 5; 10 MOD -5; -10 MOD -5",
                "30 PRINT 27 MOD 5; -27 MOD 5; 27 MOD -5; -27 MOD -5"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1-11-1\n0000\n2-22-2\n")
    }

    @Test
    fun shouldPrintMultipleArgs() {
        val source = asList(
                "10 PRINT \"good \"; 2; \" go\"",
                "20 PRINT \"(1 + 2) * 3\"; \" = \"; (1 + 2) * 3",
                "30 PRINT 1, 2, 3, 4, 5, 6, 7, 8, 9, 10"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "good 2 go\n(1 + 2) * 3 = 9\n12345678910\n")
    }

    @Test
    fun shouldPrintArgsPushedOnStack() {
        val source = asList(
                "PRINT 1, 2, 3, \" first on stack \", 4, 5.6, \" last on stack \""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "123 first on stack 45.600000 last on stack \n")
    }

    @Test
    fun shouldPrintAndGoto() {
        val source = asList(
                "10 print \"A\"",
                "20 goto 40",
                "30 print \"B\"",
                "40 print \"C\"",
                "50 end"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "A\nC\n")
    }

    @Test
    fun shouldLoopAndOnGosub() {
        val source = asList(
                "10 a = 1",
                "20 while a < 4",
                "30   on a gosub 100, 200, 300",
                "40   a = a + 1",
                "50 wend",
                "60 end",
                "100 print \"one\"",
                "110 return",
                "200 print \"two\"",
                "210 return",
                "300 print \"three\"",
                "310 return"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "one\ntwo\nthree\n")
    }

    @Test
    fun shouldLoopAndOnGoto() {
        val source = asList(
                "10 a = 0",
                "20 a = a + 1",
                "30 on a goto 100, 200, 300",
                "40 end",
                "100 print \"one\"",
                "110 goto 20",
                "200 print \"two\"",
                "210 goto 20",
                "300 print \"three\"",
                "310 goto 20"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "one\ntwo\nthree\n")
    }

    @Test
    fun shouldGotoRem() {
        val source = asList(
                "10 goto 30",
                "20 print \"A\"",
                "30 rem hi!",
                "40 print \"B\""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "B\n")
    }

    @Test
    fun shouldGotoAssignment() {
        val source = asList(
                "10 goto 30",
                "20 print \"A\"",
                "30 x = 10",
                "40 print x"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "10\n")
    }

    @Test
    fun shouldGosubAssignment() {
        val source = asList(
                "10 gosub 40",
                "20 print x; y$",
                "30 end",
                "40 x = 10",
                "50 y$ = \"Hello!\"",
                "60 return"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "10Hello!\n")
    }

    @Test
    fun shouldExitAfterGosub() {
        val source = asList(
                "10 gosub 20",
                "20 print 17",
                "30 end"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "17\n")
    }

    @Test
    fun shouldExitAfterGosubWithRWGB() {
        val source = asList(
                "10 gosub 20",
                "15 return",
                "20 print 17",
                "30 end"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "17\n")
    }

    @Test
    fun shouldReturnWithoutGosub() {
        val source = asList(
                "10 print 1",
                "20 return" // RETURN without GOSUB
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1\nError: RETURN without GOSUB\n")
    }

    @Test
    fun shouldAssignIntegers() {
        val source = asList(
                "10 let a% = 5 + 7",
                "20 let b = 0 - &H09",
                "30 print a% ; \" \" ; b",
                "40 let c = a% * b + 1 + &O10",
                "50 print c"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "12 -9\n-99\n")
    }

    @Test
    fun shouldAssignStrings() {
        val source = asList(
                "let a$ = \"A\"",
                "let b = \"B\"",
                "print a$ ; b",
                "defstr x,y,z",
                "let x1 = z1",
                "print x1 ; \".\" ; z1"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "AB\n.\n")
    }

    @Test
    fun shouldAssignBooleans() {
        val source = asList(
                "10 let a = TRUE",
                "20 let b = FALSE",
                "30 let c = a OR b",
                "40 print a ; \" \" ; b ; \" \" ; c"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-1 0 -1\n")
    }

    @Test
    fun shouldReassignNewValues() {
        val source = asList(
                "10 let str = \"A\" : let int = 0",
                "20 let str = \"B\" : let int = 1",
                "30 print str ; int",
                "40 let str = \"\" : let int = -17",
                "50 print str ; int"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "B1\n-17\n")
    }

    @Test
    fun shouldPrintVariables() {
        val source = asList(
                "10 let value.1 = 9 : value.2 = -1",
                "20 print value.1 * value.2",
                "30 print value.1 \\ value.2",
                "40 print value.1 + value.2 * value.2"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-9\n-9\n10\n")
    }

    @Test
    fun shouldPrintUndefined() {
        val source = asList(
                "print x",
                "print x + 7",
                "print y#",
                "print z$",
                "print a%"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n7\n0.000000\n\n0\n")
    }

    @Test
    fun shouldPrintDefined() {
        val source = asList(
                "defbool a-c",
                "defdbl d-f",
                "defint g-i",
                "defstr j-l",
                "print a;b;c",
                "print d",
                "print e + f",
                "print g",
                "print h - 7; i + i",
                "print j;\"-\";k"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "000\n0.000000\n0.000000\n0\n-70\n-\n")
    }

    @Test
    fun shouldPrintDimmed() {
        val source = asList(
                "dim dig as boolean",
                "dim err as integer",
                "dim foo as string",
                "defdbl d-f",
                "print dig",
                "print err",
                "print foo",
                "print d; \"+\"; e; \"+\"; f",
                "foo = \"foo\"",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n0\n\n0.000000+0.000000+0.000000\nfoo\n")
    }

    @Test
    fun shouldPrintAndReassign() {
        val source = asList(
                "10 let a = 7",
                "20 print \"a=\"; a",
                "30 let a = a + 1",
                "40 print \"a=\"; a"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "a=7\na=8\n")
    }

    @Test
    fun shouldPrintBooleanExpressions() {
        val source = asList(
                "10 let a = 7 : print a",
                "20 let b = 5 : print b",
                "30 let eq = a = b : print eq",
                "40 let ne = a <> b : print ne",
                "50 let gt = a > b : print gt",
                "60 let ge = a >= b : print ge",
                "70 let lt = a < b : print lt",
                "80 let le = a <= b : print le"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "7\n5\n0\n-1\n-1\n-1\n0\n0\n")
    }

    @Test
    fun shouldPrintBooleanExpressionsWithStrings() {
        val source = asList(
                "x = \"aa\"",
                "print \"ab\" = x; \"ab\" = \"ab\"; \"ab\" = \"ac\"",
                "print \"ab\" <> x; \"ab\" <> \"ab\"; \"ab\" <> \"ac\"",
                "print \"ab\" < x; \"ab\" < \"ab\"; \"ab\" < \"ac\"",
                "print \"ab\" <= x; \"ab\" <= \"ab\"; \"ab\" <= \"ac\"",
                "print \"ab\" > x; \"ab\" > \"ab\"; \"ab\" > \"ac\"",
                "print \"ab\" >= x; \"ab\" >= \"ab\"; \"ab\" >= \"ac\""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0-10\n-10-1\n00-1\n0-1-1\n-100\n-1-10\n")
    }

    @Test
    fun shouldPrintConditionalExpressions() {
        val source = asList(
                "10 let a = 7 + 8: print a",
                "20 let b = 5 - 2: print b",
                "30 let eq = a = 15 : print eq",
                "40 let ne = a <> 30 : print ne",
                "60 print eq and b > a",
                "70 print eq or a > b",
                "80 print (ne or b = 0) and (ne or true)",
                "90 print ne xor not ne"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "15\n3\n-1\n-1\n0\n-1\n-1\n-1\n")
    }

    @Test
    fun shouldPrintTruthTable() {
        val source = asList(
                "10 PRINT \"T AND T = \"; TRUE AND TRUE",
                "20 PRINT \"T AND F = \"; TRUE AND FALSE",
                "30 PRINT \"F AND T = \"; FALSE AND TRUE",
                "40 PRINT \"F AND F = \"; FALSE AND FALSE",
                "50 PRINT \"T OR T = \"; TRUE OR TRUE",
                "60 PRINT \"T OR F = \"; TRUE OR FALSE",
                "70 PRINT \"F OR T = \"; FALSE OR TRUE",
                "80 PRINT \"F OR F = \"; FALSE OR FALSE",
                "90 PRINT \"T XOR T = \"; TRUE XOR TRUE",
                "100 PRINT \"T XOR F = \"; TRUE XOR FALSE",
                "110 PRINT \"F XOR T = \"; FALSE XOR TRUE",
                "120 PRINT \"F XOR F = \"; FALSE XOR FALSE",
                "130 PRINT \"NOT F = \"; NOT FALSE",
                "140 PRINT \"NOT T = \"; NOT TRUE"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile,
                "T AND T = -1\nT AND F = 0\nF AND T = 0\nF AND F = 0\n" +
                        "T OR T = -1\nT OR F = -1\nF OR T = -1\nF OR F = 0\n" +
                        "T XOR T = 0\nT XOR F = -1\nF XOR T = -1\nF XOR F = 0\n" +
                        "NOT F = -1\nNOT T = 0\n")
    }

    @Test
    fun shouldPrintFromIfClause() {
        val source = asList(
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
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "x>5\nx<10\n7\n")
    }

    @Test
    fun shouldPrintFromElseIfClause() {
        val source = asList(
                "x = 7",
                "if x < 5 then",
                "  print 5",
                "elseif x < 10 then",
                "  print 10",
                "  print 10",
                "else",
                "  print \"else\"",
                "endif"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "10\n10\n")
    }

    @Test
    fun shouldRunOneLineIfs() {
        val source = asList(
                "10 x% = 7",
                "20 if x% = 5 then 30 else print 20 : goto 40",
                "30 print 30",
                "40 print 40",
                "50 if x% <> 5 goto 60 else 70",
                "60 print 60",
                "70 print 70",
                "80 end"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "20\n40\n60\n70\n")
    }

    @Test
    fun shouldCalculateFaculty() {
        val source = asList(
                "10 n = 5",
                "20 result = 1",
                "30 i = n",
                "40 if i = 0 goto 100",
                "50 result = result * i",
                "60 i = i - 1",
                "70 goto 40",
                "100 print \"fac(\"; n; \")=\"; result"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "fac(5)=120\n")
    }

    @Test
    fun shouldPrintInWhile() {
        val source = asList(
                "a = &B00",
                "while a < &B11",
                "  b = &B00",
                "  while b < &B11",
                "    print a;\",\";b",
                "    b = b + &B01",
                "  wend",
                "  a = a + &B01",
                "wend"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0,0\n0,1\n0,2\n1,0\n1,1\n1,2\n2,0\n2,1\n2,2\n")
    }
}
