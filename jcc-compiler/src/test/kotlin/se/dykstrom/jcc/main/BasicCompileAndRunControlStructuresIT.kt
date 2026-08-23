/*
 * Copyright (C) 2025 Johan Dykstrom
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

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for BASIC, focusing on control structures.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class BasicCompileAndRunControlStructuresIT : AbstractIntegrationTests() {

    @Test
    fun whileLoop() {
        val source = listOf(
            "DIM a AS INTEGER",
            "a = 0",
            "WHILE a < 5",
            "  PRINT a",
            "  a = a + 1",
            "WEND"
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "0",
                "1",
                "2",
                "3",
                "4",
            ),
        )
    }

    @Test
    fun nestedWhileLoop() {
        val source = listOf(
            "DIM a AS INTEGER, b AS INTEGER",
            "a = 0",
            "WHILE a < 3",
            "  b = 0",
            "  WHILE b < 3",
            "    PRINT a; b",
            "    b = b + 1",
            "  WEND",
            "  a = a + 1",
            "WEND"
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "00",
                "01",
                "02",
                "10",
                "11",
                "12",
                "20",
                "21",
                "22",
            ),
        )
    }

    @Test
    fun ifInt() {
        val source = listOf(
            "DIM a AS INTEGER",
            "a = 0",
            "WHILE a < 5",
            "  IF a MOD 2 = 0 THEN",
            "    PRINT a",
            "  END IF",
            "  a = a + 1",
            "WEND"
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "0",
                "2",
                "4",
            ),
        )
    }

    @Test
    fun simpleNestedIf() {
        val source = listOf(
            "IF 0 THEN",
            "  IF 1 THEN",
            "    PRINT 1",
            "  ELSE",
            "    PRINT 2",
            "  END IF",
            "ELSE",
            "  IF 3 THEN",
            "    PRINT 3",
            "  ELSE",
            "    PRINT 4",
            "  END IF",
            "END IF",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "3",
            ),
        )
    }

    @Test
    fun nestedIfAndMore() {
        val source = listOf(
            "f# = 3.5",
            "IF f# > 0.0 AND f# < 10.0 THEN",
            "  PRINT \"between\"",
            "END IF",
            "IF f3 > 10.0 THEN",
            "  PRINT \"greater\"",
            "END IF",
            "IF f3 > 10.0 THEN",
            "  PRINT \"greater\"",
            "ELSE",
            "  PRINT \"not greater\"",
            "END IF",
            "IF f# <= 10.0 THEN",
            "  IF 7.0 > f# THEN",
            "    PRINT \"less than 7\"",
            "  ELSE",
            "    PRINT \"greater than 7\"",
            "  END IF",
            "END IF",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "between",
                "not greater",
                "less than 7",
            ),
        )
    }

    @Test
    fun shouldGotoLabel() {
        val source = listOf(
            "10 PRINT 10",
            "20 GOTO 40",
            "30 PRINT 30",
            "40 PRINT 40",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "10",
                "40",
            ),
        )
    }

    @Test
    fun shouldGosubLabel() {
        val source = listOf(
            "10 PRINT 10",
            "20 GOSUB 100",
            "30 PRINT 30",
            "40 GOSUB 200",
            "50 PRINT 50",
            "60 END",
            "",
            "100 PRINT 100",
            "110 RETURN",
            "200 PRINT 200",
            "210 RETURN",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "10",
                "100",
                "30",
                "200",
                "50",
            ),
        )
    }

    @Test
    fun shouldGosubInWhile() {
        val source = listOf(
            "10 DIM a AS INTEGER",
            "20 WHILE a < 3",
            "30   GOSUB 100",
            "40   GOSUB 100",
            "50   LET a = a + 1",
            "60 WEND",
            "70 END",
            "",
            "100 PRINT 7",
            "110 RETURN",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "7",
                "7",
                "7",
                "7",
                "7",
                "7",
            ),
        )
    }

    @Test
    fun shouldGosubInWhileAndIf() {
        val source = listOf(
            "DIM a AS INTEGER",
            "WHILE a < 4",
            "  IF a MOD 2 <> 0 THEN",
            "    GOSUB 100",
            "  ELSE",
            "    GOSUB 200",
            "  END IF",
            "  LET a = a + 1",
            "WEND",
            "END",
            "",
            "100 PRINT 1",
            "110 RETURN",
            "200 PRINT 2",
            "210 RETURN",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "2",
                "1",
                "2",
                "1",
            ),
        )
    }

    @Test
    fun shouldOnGoto() {
        val source = listOf(
            "10 DIM a AS INTEGER",
            "20 a = 0",
            "30 ON a GOTO 100, 200, 300",
            "40 PRINT 40",
            "50 a = a + 1",
            "60 ON a GOTO 100, 200, 300",
            "70 PRINT 70",
            "80 END",
            "",
            "100 PRINT 100",
            "110 GOTO 50",
            "200 PRINT 200",
            "210 GOTO 50",
            "300 PRINT 300",
            "310 GOTO 50",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "40",
                "100",
                "200",
                "300",
                "70",
            ),
        )
    }

    @Test
    fun shouldOnGosub() {
        val source = listOf(
            "10 DIM a AS INTEGER",
            "20 a = 0",
            "30 ON a GOSUB 100, 200, 300",
            "40 PRINT 40",
            "50 a = a + 1",
            "60 ON a GOSUB 100, 200, 300",
            "61 a = a + 1",
            "62 ON a GOSUB 100, 200, 300",
            "70 PRINT 70",
            "80 END",
            "",
            "100 PRINT 100",
            "110 RETURN",
            "200 PRINT 200",
            "210 RETURN",
            "300 PRINT 300",
            "310 RETURN",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "40",
                "100",
                "200",
                "70",
            ),
        )
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
            "end if",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("10", "10"))
    }

    @Test
    fun shouldEndInThenClause() {
        val source = listOf(
            "x = 3",
            "print \"before\"",
            "if x < 5 then",
            "  end",
            "end if",
            "print \"after\"",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("before"))
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
            "80 end",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("20", "40", "60", "70"))
    }

    @Test
    fun shouldGotoRem() {
        val source = listOf(
            "one:   goto three",
            "two:   print \"A\"",
            "three: rem hi!",
            "four:  print \"B\"",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("B"))
    }

    @Test
    fun shouldGotoAssignment() {
        val source = listOf(
            "10 goto 30",
            "20 print \"A\"",
            "30 x% = 10",
            "40 print x%",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("10"))
    }

    @Test
    fun shouldGosubAssignment() {
        val source = listOf(
            "10 gosub 40",
            "20 print x%; y$",
            "30 end",
            "40 x% = 10",
            "50 y$ = \"Hello!\"",
            "60 return",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("10Hello!"))
    }

    @Test
    fun shouldExitAfterGosub() {
        val source = listOf(
            "10 gosub 20",
            "20 print 17",
            "30 end",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("17"))
    }

    @Test
    fun shouldExitAfterGosubWithRWGB() {
        val source = listOf(
            "10 gosub 20",
            "15 return",
            "20 print 17",
            "30 end",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("17"))
    }

    @Test
    fun shouldReturnWithoutGosub() {
        val source = listOf(
            "10 print 1",
            "20 return", // RETURN without GOSUB
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        // The error message goes to stderr, which is read before the buffered stdout.
        // Note that both message and exit value differ from QuickBASIC 4.5, which
        // prints 'Error: RETURN without GOSUB' to stdout and exits with 0.
        runAndAssertSuccess(
            listOf(),
            listOf("Error: GOSUB stack underflow (RETURN without GOSUB)", "1"),
            1,
        )
    }
}
