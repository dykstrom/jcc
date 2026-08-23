/*
 * Copyright (C) 2026 Johan Dykstrom
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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for BASIC, specifically for arrays.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunArrayIT : AbstractIntegrationTests() {

    @Test
    fun shouldDefineIntegerArray() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10) as integer",
                "print a%(0)"
            ),
            listOf("0")
        )
    }

    @Test
    fun shouldDefineArrayWithoutType() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10)",
                "dim b$(10)",
                "dim c(10)",
                "a%(0) = 17",
                "b$(0) = \"foo\"",
                "c(0) = 1.5",
                "print a%(0) ; \" \" ; b$(0) ; \" \" ; c(0)"
            ),
            listOf("17 foo 1.500000")
        )
    }

    @Test
    fun shouldDefineArrayWithoutTypeAfterDefInt() {
        compileAndRun(
            BASIC,
            listOf(
                "defint a-c",
                "dim b(10)",
                "b(3) = 7 \\ 2",
                "print b(3)"
            ),
            listOf("3")
        )
    }

    /**
     * An undefined array is defined implicitly, with the inclusive upper bound 10
     * in every dimension, just like in QuickBASIC.
     */
    @Test
    fun shouldDefineUndefinedArrayImplicitly() {
        compileAndRun(
            BASIC,
            listOf(
                "a%(3) = 7",
                "print a%(3)"
            ),
            listOf("7")
        )
    }

    @Test
    fun shouldReadUndefinedArrayBeforeWriting() {
        compileAndRun(
            BASIC,
            listOf("print a%(3)"),
            listOf("0")
        )
    }

    @Test
    fun shouldUseAllElementsOfImplicitArray() {
        compileAndRun(
            BASIC,
            listOf(
                "a%(0) = 1",
                "a%(10) = 2",
                "print a%(0) + a%(10)"
            ),
            listOf("3")
        )
    }

    @Test
    fun shouldDefineUndefinedMultiDimensionalArrayImplicitly() {
        compileAndRun(
            BASIC,
            listOf(
                "b%(2, 3) = 5",
                "b%(10, 10) = 6",
                "print b%(2, 3) ; \" \" ; b%(10, 10)"
            ),
            listOf("5 6")
        )
    }

    /**
     * An implicitly defined array gets its element type from the type specifier,
     * from a DEFtype statement, or from the default type, which is double.
     */
    @Test
    fun shouldDefineUndefinedArrayWithDefaultType() {
        compileAndRun(
            BASIC,
            listOf(
                "a(3) = 1.5",
                "b$(3) = \"foo\"",
                "print a(3) ; \" \" ; b$(3)"
            ),
            listOf("1.500000 foo")
        )
    }

    /**
     * The implicit declaration is added to the start of the program, before any OPTION BASE,
     * so verify that the base still applies to the implicitly defined array.
     */
    @Test
    fun shouldDefineUndefinedArrayImplicitlyWithOptionBase1() {
        compileAndRun(
            BASIC,
            listOf(
                "option base 1",
                "a%(3) = 7",
                "print a%(3) ; \" \" ; lbound(a%) ; \" \" ; ubound(a%)"
            ),
            listOf("7 1 10")
        )
    }

    /**
     * A floating point subscript is rounded (half-to-even) to an integer, on both sides
     * of an assignment.
     */
    @Test
    fun shouldRoundFloatSubscript() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10) as integer",
                "a%(1.7) = 7",
                "print a%(2) ; \" \" ; a%(2.4)"
            ),
            listOf("7 7")
        )
    }

    @Test
    fun shouldDefineArrayUsingConstant() {
        compileAndRun(
            BASIC,
            listOf(
                "const MAX = 100",
                "dim a%(MAX) as integer",
                // This is ok, because the upper bound is inclusive
                "print a%(MAX)"
            ),
            listOf("0")
        )
    }

    @Test
    fun shouldDefineMultiDimensionalArray() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10, 5, 2) as integer",
                "print a%(3, 2, 1)"
            ),
            listOf("0")
        )
    }

    @Test
    fun shouldDefineThreeArrays() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10) as integer",
                "dim b%(5) as integer",
                "dim c%(2) as integer",
                "print a%(0) ; b%(0) ; c%(0)"
            ),
            listOf("000")
        )
    }

    @Test
    fun shouldPrintAllElementsOfIntegerArray() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(3) as integer",
                "dim index as integer",
                "while index <= 3",
                "  print a%(index)",
                "  index = index + 1",
                "wend"
            ),
            listOf("0", "0", "0", "0")
        )
    }

    @Test
    fun shouldPrintAllElementsOfFloatArray() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a#(3) as double",
                "dim index as integer",
                "while index <= 3",
                "  print a#(index)",
                "  index = index + 1",
                "wend"
            ),
            listOf("0.000000", "0.000000", "0.000000", "0.000000")
        )
    }

    @Test
    fun shouldPrintAllElementsOfStringArray() {
        // Brackets keep the printed lines non-empty (the run helper drops trailing empty lines),
        // while still verifying that each unassigned element is the empty string.
        compileAndRun(
            BASIC,
            listOf(
                "dim a$(3) as string",
                "dim index as integer",
                "while index <= 3",
                "  print \"[\"; a$(index); \"]\"",
                "  index = index + 1",
                "wend"
            ),
            listOf("[]", "[]", "[]", "[]")
        )
    }

    @Test
    fun shouldDefineLargeStringArray() {
        // String elements are emitted with an explicit per-element initializer,
        // so this proves that Clang copes with a large one.
        compileAndRun(
            BASIC,
            listOf(
                "dim a$(10000) as string",
                "a$(10000) = \"foo\"",
                "print \"[\"; a$(0); \"]\"; a$(10000)"
            ),
            listOf("[]foo")
        )
    }

    @Test
    fun arraySubscriptsCanBeExpressions() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10, 5) as integer",
                "dim b as integer",
                "let b = 1 + 4",
                "print a%(b - 1, abs(-2))"
            ),
            listOf("0")
        )
    }

    @Test
    fun arraySubscriptsCanBeFunctionCalls() {
        compileAndRun(
            BASIC,
            listOf(
                """
                dim a%(10, 5) as integer
                a%(abs(-3), cint(2.2)) = val("77")
                print a%(cint(1.7 + 1.3), cint(val("2")))
                """
            ),
            listOf("77")
        )
    }

    @Test
    fun arraySubscriptsCanBeFloats() {
        compileAndRun(
            BASIC,
            listOf(
                """
                dim a%(10, 5) as integer
                a%(abs(-3.1), 2.2) = val("77")
                print a%(1.6 + 1.3, val("2"))
                """
            ),
            listOf("77")
        )
    }

    @Test
    fun arraySubscriptsCanBeArrayExpressions() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10, 5) as integer",
                "a%(7, 1) = 7",
                "a%(7, 2) = 1",
                "a%(a%(7, 1), a%(7, 2)) = a%(a%(7, 1), 2) + 13",
                "print a%(a%(0, 0) + 7, a%(7, 2))"
            ),
            listOf("14")
        )
    }

    @Test
    fun shouldSetAndGetArrayElement() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10) as integer",
                "dim f#(10) as double",
                "dim s$(10) as string",
                "a%(3) = 9",
                "f#(4) = 9.7",
                "s$(9) = \"foo\"",
                "print a%(3)",
                "print f#(4)",
                "print s$(9)"
            ),
            listOf("9", "9.700000", "foo")
        )
    }

    @Test
    fun shouldSetAndGetAllArrayElements() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a(7) as integer",
                "dim i as integer",
                "while i <= 7",
                "  a(i) = 10 - i",
                "  i = i + 1",
                "wend",
                "i = 0",
                "while i <= 7",
                "  print i; \"=\"; a(i)",
                "  i = i + 1",
                "wend"
            ),
            listOf("0=10", "1=9", "2=8", "3=7", "4=6", "5=5", "6=4", "7=3")
        )
    }

    @Test
    fun shouldSetAndGetAllArrayElementsWithOptionBase1() {
        compileAndRun(
            BASIC,
            listOf(
                """
                option base 1
                dim a(7) as integer
                dim i as integer

                i = 1
                while i <= 7
                  a(i) = 10 - i
                  i = i + 1
                wend

                i = 1
                while i <= 7
                  print i; "="; a(i)
                  i = i + 1
                wend
                """
            ),
            listOf("1=9", "2=8", "3=7", "4=6", "5=5", "6=4", "7=3")
        )
    }

    @Test
    fun shouldSetAndGetAllArrayElementsWithLboundUbound() {
        compileAndRun(
            BASIC,
            listOf(
                """
                option base 1
                dim a(7) as integer
                dim i as integer
                dim min as integer, max as integer

                min = lbound(a)
                max = ubound(a)

                i = min
                while i <= max
                  a(i) = 10 - i
                  i = i + 1
                wend

                i = min
                while i <= max
                  print i; "="; a(i)
                  i = i + 1
                wend
                """
            ),
            listOf("1=9", "2=8", "3=7", "4=6", "5=5", "6=4", "7=3")
        )
    }

    @Test
    fun settingArrayElementShouldNotAffectAdjacentVariables() {
        compileAndRun(
            BASIC,
            listOf(
                """
                dim a1(1) as integer
                dim a2(1) as integer
                dim a3(2) as integer

                a1(0) = 10
                a1(1) = 11
                a2(0) = 20
                a2(1) = 21
                a3(0) = 30
                a3(1) = 31
                a3(2) = 32

                print a1(0); " "; a1(1); " "; a2(0); " "; a2(1); " "; a3(0); " "; a3(1); " "; a3(2)
                """
            ),
            listOf("10 11 20 21 30 31 32")
        )
    }

    @Test
    fun settingArrayElementShouldNotAffectAdjacentVariablesWithOptionBase1() {
        compileAndRun(
            BASIC,
            listOf(
                """
                option base 1

                dim a1(1) as integer
                dim a2(1) as integer
                dim a3(2) as integer

                a1(1) = 11
                a2(1) = 21
                a3(1) = 31
                a3(2) = 32

                print a1(1); " "; a2(1); " "; a3(1); " "; a3(2)
                """
            ),
            listOf("11 21 31 32")
        )
    }

    @Test
    fun shouldSetAndGetAllStringArrayElements() {
        compileAndRun(
            BASIC,
            listOf(
                "dim s(7) as string",
                "dim i as integer",
                "",
                "i = 0",
                "while i <= 7",
                "  s(i) = \"i=\" + ltrim$(str$(i))",
                "  i = i + 1",
                "wend",
                "i = 0",
                "while i <= 7",
                "  print s(i)",
                "  i = i + 1",
                "wend",
                "",
                "i = 0",
                "while i <= 7",
                "  s(i) = \"i=\" + ltrim$(str$(i * 10))",
                "  i = i + 1",
                "wend",
                "i = 0",
                "while i <= 7",
                "  print s(i)",
                "  i = i + 1",
                "wend"
            ),
            listOf(
                "i=0", "i=1", "i=2", "i=3", "i=4", "i=5", "i=6", "i=7",
                "i=0", "i=10", "i=20", "i=30", "i=40", "i=50", "i=60", "i=70"
            )
        )
    }

    @Test
    fun shouldSetAndGetAll2DArrayElements() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a(2, 3) as integer",
                "dim x as integer, y as integer",
                "",
                "x = 0",
                "while x <= 2",
                "  y = 0",
                "  while y <= 3",
                "    a(x, y) = 10 * x + y",
                "    y = y + 1",
                "  wend",
                "  x = x + 1",
                "wend",
                "",
                "x = 0",
                "while x <= 2",
                "  y = 0",
                "  while y <= 3",
                "    print a(x, y)",
                "    y = y + 1",
                "  wend",
                "  x = x + 1",
                "wend",
            ),
            listOf("0", "1", "2", "3", "10", "11", "12", "13", "20", "21", "22", "23")
        )
    }

    @Test
    fun shouldSetAndGet2DArrayElementsSimple() {
        compileAndRun(
            BASIC,
            listOf(
                "DIM a(1, 1) AS INTEGER",
                "a(0, 0) = 0",
                "a(0, 1) = 1",
                "a(1, 0) = 10",
                "a(1, 1) = 11",
                "PRINT a(0, 0)",
                "PRINT a(0, 1)",
                "PRINT a(1, 0)",
                "PRINT a(1, 1)",
            ),
            listOf("0", "1", "10", "11")
        )
    }

    @Test
    fun shouldSetAndGetAll2DArrayElementsWithOptionBase1() {
        compileAndRun(
            BASIC,
            listOf(
                """
                option base 1

                dim a(2, 3) as integer
                dim x as integer, y as integer

                x = 1
                while x <= 2
                  y = 1
                  while y <= 3
                    a(x, y) = 10 * x + y
                    y = y + 1
                  wend
                  x = x + 1
                wend

                x = 1
                while x <= 2
                  y = 1
                  while y <= 3
                    print a(x, y)
                    y = y + 1
                  wend
                  x = x + 1
                wend
                """
            ),
            listOf("11", "12", "13", "21", "22", "23")
        )
    }

    @Test
    fun shouldSwapIntegerAndArrayElement() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10) as integer",
                "dim b% as integer",
                "a%(3) = 9",
                "b% = 4",
                "print a%(3); \"-\"; b%",
                "swap a%(3), b%",
                "print a%(3); \"-\"; b%"
            ),
            listOf("9-4", "4-9")
        )
    }

    @Test
    fun shouldSwapTwoIntegerArrayElements() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10) as integer",
                "dim b%(5) as integer",
                "a%(3) = 9",
                "b%(1) = 4",
                "print a%(3); \"-\"; b%(1)",
                "swap a%(3), b%(1)",
                "print a%(3); \"-\"; b%(1)"
            ),
            listOf("9-4", "4-9")
        )
    }

    @Test
    fun shouldSwapTwoElementsInSameIntegerArray() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10) as integer",
                "a%(3) = 1",
                "a%(7) = 3",
                "print a%(3); \"-\"; a%(7)",
                "swap a%(3), a%(7)",
                "print a%(3); \"-\"; a%(7)"
            ),
            listOf("1-3", "3-1")
        )
    }

    @Test
    fun shouldSwapTwoElementsInSameIntegerArrayWithOptionBase1() {
        compileAndRun(
            BASIC,
            listOf(
                """
                option base 1
                dim a%(10) as integer
                a%(1) = 1
                a%(10) = 3
                print a%(1); "-"; a%(10)
                swap a%(1), a%(10)
                print a%(1); "-"; a%(10)
                """
            ),
            listOf("1-3", "3-1")
        )
    }

    @Test
    fun shouldSwapIntegerAndFloatArrayElements() {
        compileAndRun(
            BASIC,
            listOf(
                "dim a%(10) as integer",
                "dim f#(5) as double",
                "a%(3) = 9",
                "f#(1) = 3.14",
                "print a%(3); \"-\"; f#(1)",
                "swap a%(3), f#(1)",
                "print a%(3); \"-\"; f#(1)"
            ),
            listOf("9-3.140000", "3-9.000000")
        )
    }

    @Test
    fun shouldSwapTwoElementsInSameStringArray() {
        compileAndRun(
            BASIC,
            listOf(
                "dim arr(2) as string",
                "arr(0) = \"foo\"",
                "arr(1) = \"bar\"",
                "print arr(0); \"-\"; arr(1)",
                "swap arr(0), arr(1)",
                "print arr(0); \"-\"; arr(1)"
            ),
            listOf("foo-bar", "bar-foo")
        )
    }
}
