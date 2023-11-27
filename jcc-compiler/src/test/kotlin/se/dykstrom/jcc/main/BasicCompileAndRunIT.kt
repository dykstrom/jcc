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
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for Basic.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunIT : AbstractIntegrationTest() {

    @Test
    fun shouldPrintExpressions() {
        val source = listOf(
            "10 PRINT 5 + 2 * 7",
            "20 PRINT 8 \\ 1",
            "30 PRINT 1 + 2 + 3 + 4 + 5",
            "40 PRINT &HFE + &H01",
            "50 PRINT &O10 - &O5",
            "60 PRINT &B10010 + &B101",
            "FOO: PRINT 20 - 3 * 5 + 1 * 8 \\ 2",
            "BAR: PRINT 5 - 3 + 7 * 2 - 10 * 20 \\ 5",
            "AXE: PRINT 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10",
            "PRINT \"A\" + \"B\"",
            "PRINT \"one\" + \"two\" + \"three\"",
            "PRINT \"12345\" + \"\" + \"67890\" + \"\" + \"abcde\""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(
            sourceFile,
            "19\n8\n15\n255\n3\n23\n9\n-24\n55\nAB\nonetwothree\n1234567890abcde\n",
            0
        )
    }

    @Test
    fun shouldPrintGroupedExpressions() {
        val source = listOf(
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
    fun shouldPrintNegatedExpressions() {
        val source = listOf(
            "PRINT -(1 + 3)",
            "PRINT -(5 - 1)",
            "PRINT -5 - 3",
            "PRINT -1000",
            "PRINT -abs(-3)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-4\n-4\n-8\n-1000\n-3\n", 0)
    }

    @Test
    fun shouldVerifyIntegerDivisionAndModulo() {
        val source = listOf(
            "PRINT 7\\2; -7\\2; 7\\-2; -7\\-2",
            "PRINT 10\\5; -10\\5; 10\\-5; -10\\-5",
            "PRINT 27\\5; -27\\5; 27\\-5; -27\\-5",
            "PRINT 7 MOD 2; -7 MOD 2; 7 MOD -2; -7 MOD -2",
            "PRINT 10 MOD 5; -10 MOD 5; 10 MOD -5; -10 MOD -5",
            "PRINT 27 MOD 5; -27 MOD 5; 27 MOD -5; -27 MOD -5"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3-3-33\n2-2-22\n5-5-55\n1-11-1\n0000\n2-22-2\n")
    }

    @Test
    fun shouldPrintMultipleArgs() {
        val source = listOf(
            "PRINT \"good \"; 2; \" go\"",
            "PRINT \"(1 + 2) * 3\"; \" = \"; (1 + 2) * 3",
            "PRINT 1, 2, 3, 4, 5, 6, 7, 8, 9, 10",
            "PRINT 1, 2, 3, \" first on stack \", 4, 5.6, \" last on stack \""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(
            sourceFile,
            "good 2 go\n(1 + 2) * 3 = 9\n12345678910\n123 first on stack 45.600000 last on stack \n"
        )
    }

    @Test
    fun shouldPrintAndGoto() {
        val source = listOf(
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
    fun shouldPrintAndGotoLabel() {
        val source = listOf(
                "        print \"A\"",
                "        goto line.c",
                "        print \"B\"",
                "line.c: print \"C\"",
                "        end"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "A\nC\n")
    }

    @Test
    fun shouldLoopAndOnGosub() {
        val source = listOf(
                "10 a% = 1",
                "20 while a% < 4",
                "30   on a% gosub 100, 200, 300",
                "40   a% = a% + 1",
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
        val source = listOf(
                "10 a% = 0",
                "20 a% = a% + 1",
                "30 on a% goto 100, 200, 300",
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
        val source = listOf(
                "one:   goto three",
                "two:   print \"A\"",
                "three: rem hi!",
                "four:  print \"B\""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "B\n")
    }

    @Test
    fun shouldGotoAssignment() {
        val source = listOf(
                "10 goto 30",
                "20 print \"A\"",
                "30 x% = 10",
                "40 print x%"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "10\n")
    }

    @Test
    fun shouldGosubAssignment() {
        val source = listOf(
                "10 gosub 40",
                "20 print x%; y$",
                "30 end",
                "40 x% = 10",
                "50 y$ = \"Hello!\"",
                "60 return"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "10Hello!\n")
    }

    @Test
    fun shouldExitAfterGosub() {
        val source = listOf(
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
        val source = listOf(
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
        val source = listOf(
                "10 print 1",
                "20 return" // RETURN without GOSUB
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1\nError: RETURN without GOSUB\n")
    }

    @Test
    fun shouldAssignExpressions() {
        val source = listOf(
            "let a% = 5 + 7",
            "let b% = 0 - &H09",
            "print a% ; \" \" ; b%",
            "let c% = a% * b% + 1 + &O10",
            "print c%",
            "let d$ = \"A\"",
            "let e$ = \"B\"",
            "print d$ ; e$",
            "defstr x,y,z",
            "let x1 = z1",
            "print x1 ; \".\" ; z1"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "12 -9\n-99\nAB\n.\n")
    }

    @Test
    fun shouldAssignConditionalExpression() {
        val source = listOf(
                "00 dim a as integer, b as integer, c as integer",
                "10 let a = NOT 0",
                "20 let b = NOT -1",
                "30 let c = a OR b",
                "40 print a ; \" \" ; b ; \" \" ; c"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-1 0 -1\n")
    }

    @Test
    fun shouldAssignFromConstants() {
        val source = listOf(
            "CONST A = 17, B = 99.9, C = \"C\"",
            "LET a% = 5 + A",
            "LET b# = B - 9.9",
            "LET c$ = \"=\" + C + \"=\"",
            "PRINT a%",
            "PRINT b#",
            "PRINT c$"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-save-temps")
        runAndAssertSuccess(sourceFile, "22\n90.000000\n=C=\n")
    }

    @Test
    fun shouldReassignNewValues() {
        val source = listOf(
                "10 let str$ = \"A\" : let int% = 0",
                "20 let str$ = \"B\" : let int% = 1",
                "30 print str$ ; int%",
                "40 let str$ = \"\" : let int% = -17",
                "50 print str$ ; int%"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "B1\n-17\n")
    }

    @Test
    fun shouldAssignToUntypedVariable() {
        val source = listOf(
            "const FOO = 9",
            "let a = 5 + 7",
            "let b = abs(-5.5)",
            "print a ; \" \" ; b",
            "let c = a + b",
            "print c",
            "let d = FOO MOD 2",
            "print d"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "12.000000 5.500000\n17.500000\n1.000000\n")
    }

    @Test
    fun shouldPrintVariables() {
        val source = listOf(
                "00 dim value.1 as integer, value.2 as integer",
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
    fun shouldPrintUndefinedVariable() {
        val source = listOf(
                "print x",
                "print x + 7",
                "print y#",
                "print z$",
                "print a%"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0.000000\n7.000000\n0.000000\n\n0\n")
    }

    @Test
    fun shouldPrintDefinedVariable() {
        val source = listOf(
                "defdbl d-f",
                "defint g-i",
                "defstr j-l",
                "print d",
                "print e + f",
                "print g",
                "print h - 7; i + i",
                "print j;\"-\";k"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0.000000\n0.000000\n0\n-70\n-\n")
    }

    @Test
    fun shouldPrintDimmedVariables() {
        val source = listOf(
            "dim value as integer",
            "let value = 9",
            "print value",
            // Print default values
            "dim dig as double",
            "dim err as integer",
            "dim foo as string",
            "defdbl d-f",
            "print dig",
            "print err",
            "print foo",
            "print d; \"+\"; e; \"+\"; f"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "9\n0.000000\n0\n\n0.000000+0.000000+0.000000\n")
    }

    @Test
    fun shouldPrintConstants() {
        val source = listOf(
            "CONST FOO = 77",
            "CONST BAR = 0.99",
            "CONST TEE = \"TEE\"",
            "CONST FALSE = 0, TRUE = NOT FALSE",
            "print FOO",
            "print BAR",
            "print TEE",
            "print FALSE",
            "print TRUE"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "77\n0.990000\nTEE\n0\n-1\n")
    }

    @Test
    fun shouldPrintRelationalExpressions() {
        val source = listOf(
                "10 dim a as integer : let a = 7 : print a",
                "20 dim b as integer : let b = 5 : print b",
                "25 dim result as integer",
                "30 let result = a = b : print result",
                "40 let result = a <> b : print result",
                "50 let result = a > b : print result",
                "60 let result = a >= b : print result",
                "70 let result = a < b : print result",
                "80 let result = a <= b : print result"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "7\n5\n0\n-1\n-1\n-1\n0\n0\n")
    }

    @Test
    fun shouldPrintRelationalExpressionsWithStrings() {
        val source = listOf(
                "x$ = \"aa\"",
                "print \"ab\" = x$; \"ab\" = \"ab\"; \"ab\" = \"ac\"",
                "print \"ab\" <> x$; \"ab\" <> \"ab\"; \"ab\" <> \"ac\"",
                "print \"ab\" < x$; \"ab\" < \"ab\"; \"ab\" < \"ac\"",
                "print \"ab\" <= x$; \"ab\" <= \"ab\"; \"ab\" <= \"ac\"",
                "print \"ab\" > x$; \"ab\" > \"ab\"; \"ab\" > \"ac\"",
                "print \"ab\" >= x$; \"ab\" >= \"ab\"; \"ab\" >= \"ac\""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0-10\n-10-1\n00-1\n0-1-1\n-100\n-1-10\n")
    }

    @Test
    fun shouldPrintRelationalExpressionsWithStringExpressions() {
        val source = listOf(
                // Compare result of add expression with static string
                "print \"a\" + \"b\" = \"ab\"; \"a\" + \"b\" <> \"ab\"",
                // Compare static string with result of add expressions
                "print \"ab\" = \"a\" + \"b\"; \"ab\" <> \"a\" + \"b\"",
                "print \"ab\" > \"a\" + \"b\"; \"ab\" <= \"a\" + \"b\"",
                // Compare result of function calls
                "print ucase$(\"ab\") = ucase$(\"a\") + ucase$(\"b\"); ltrim$(\"ab\") < rtrim$(\"a\" + \"c\")"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-10\n-10\n0-1\n-1-1\n")
    }

    @Test
    fun shouldPrintConditionalExpressions() {
        val source = listOf(
                "10 let a = 7 + 8: print a",
                "20 let b = 5 - 2: print b",
                "30 dim eq as integer : let eq = a = 15 : print eq",
                "40 dim ne as integer : let ne = a <> 30 : print ne",
                "60 print eq and b > a",
                "70 print eq or a > b",
                "80 print (ne or b = 0) and (ne or -1)",
                "90 print ne xor not ne"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "15.000000\n3.000000\n-1\n-1\n0\n-1\n-1\n-1\n")
    }

    @Test
    fun shouldPrintBitwiseAndExpressions() {
        val source = listOf(
            "PRINT &B10010 AND &B10",
            "PRINT &B10010 AND &B101",
            "PRINT &B10010 AND &B10010",
            "PRINT &B10010 AND -1",
            "PRINT &B10010 AND 0",
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "2\n0\n18\n18\n0\n")
    }

    @Test
    fun shouldPrintBitwiseOrExpressions() {
        val source = listOf(
            "PRINT &B10010 OR &B10",
            "PRINT &B10010 OR &B101",
            "PRINT &B10010 OR &B10010",
            "PRINT &B10010 OR -1",
            "PRINT &B10010 OR 0",
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "18\n23\n18\n-1\n18\n")
    }

    @Test
    fun shouldPrintBitwiseXorExpressions() {
        val source = listOf(
            "PRINT &B10010 XOR &B10",
            "PRINT &B10010 XOR &B101",
            "PRINT &B10010 XOR &B10010",
            "PRINT &B10010 XOR 0",
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "16\n23\n0\n18\n")
    }

    @Test
    fun shouldPrintBitwiseNotExpressions() {
        val source = listOf(
            "PRINT NOT &B10010",
            "PRINT NOT -1",
            "PRINT NOT 0",
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-19\n0\n-1\n")
    }

    @Test
    fun shouldPrintTruthTable() {
        val source = listOf(
            "00 DIM F AS INTEGER, T AS INTEGER",
            "05 LET F = 0 : LET T = NOT(F)",
            "10 PRINT \"T AND T = \"; T AND T",
            "20 PRINT \"T AND F = \"; T AND F",
            "30 PRINT \"F AND T = \"; F AND T",
            "40 PRINT \"F AND F = \"; F AND F",
            "50 PRINT \"T OR T = \"; T OR T",
            "60 PRINT \"T OR F = \"; T OR F",
            "70 PRINT \"F OR T = \"; F OR T",
            "80 PRINT \"F OR F = \"; F OR F",
            "90 PRINT \"T XOR T = \"; T XOR T",
            "100 PRINT \"T XOR F = \"; T XOR F",
            "110 PRINT \"F XOR T = \"; F XOR T",
            "120 PRINT \"F XOR F = \"; F XOR F",
            "130 PRINT \"NOT F = \"; NOT F",
            "140 PRINT \"NOT T = \"; NOT T"
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
        val source = listOf(
                "10 x = 7",
                "20 if x > 5 then",
                "30   print \"x>5\"",
                "40 end if",
                "50 if x < 10 then",
                "60   print \"x<10\"",
                "70 else",
                "80   print \"else\"",
                "90 end if",
                "100 print x"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "x>5\nx<10\n7.000000\n")
    }

    @Test
    fun shouldPrintFromElseIfClause() {
        val source = listOf(
                "x = 7",
                "if x < 5 then",
                "  print 5",
                "elseif x < 10 then",
                "  print 10",
                "  print 10",
                "else",
                "  print \"else\"",
                "end if"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "10\n10\n")
    }

    @Test
    fun shouldEndInThenClause() {
        val source = listOf("""
            x = 3
            print "before"
            if x < 5 then
              end
            end if
            print "after"
            """
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "before\n")
    }

    @Test
    fun shouldRunOneLineIfs() {
        val source = listOf(
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
        val source = listOf(
                "      n% = 5",
                "      result% = 1",
                "      i% = n%",
                "loop: if i% = 0 goto done",
                "      result% = result% * i%",
                "      i% = i% - 1",
                "      goto loop",
                "done: print \"fac(\"; n%; \")=\"; result%"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "fac(5)=120\n")
    }

    @Test
    fun shouldPrintInWhile() {
        val source = listOf(
                "dim a as integer, b as integer",
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

    @Test
    fun shouldRandomizeWithExpression() {
        val source = listOf(
                "randomize 27",
                "print rnd()",
                "print rnd()",
                "randomize 27",
                "print rnd()",
                "print rnd()"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0.271149\n0.036865\n0.271149\n0.036865\n")
    }

    @Test
    fun shouldRandomizeWithoutExpression() {
        val source = listOf(
                "randomize",
                "print rnd",
                "randomize",
                "print rnd",
                "randomize",
                "print rnd"
        )
        val expected = listOf(
                "Random Number Seed (-32768 to 32767)? 0.237946",
                "Random Number Seed (-32768 to 32767)? 0.237946",
                "Random Number Seed (-32768 to 32767)? 0.895538"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, listOf("1000", "1000", "2000"), expected)
    }

    @Test
    fun shouldSwapValues() {
        val source = listOf(
                "let a% = 17",
                "let b% = 4711",
                "let f# = 8.7",
                "let g# = 0",
                "let s$ = \"s\"",
                "let t$ = \"t\"",

                "print a%; \":\"; b%",
                "swap a%, b%",
                "print a%; \":\"; b%",

                "print f#; \":\"; g#",
                "swap f#,  g#",
                "print f#; \":\"; g#",

                "print g#; \":\"; b%",
                "swap g#,  b%",
                "print g#; \":\"; b%",

                "print s$; \":\"; t$",
                "swap s$,  t$",
                "print s$; \":\"; t$"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "17:4711\n4711:17\n8.700000:0.000000\n0.000000:8.700000\n8.700000:17\n17.000000:9\ns:t\nt:s\n")
    }
}
