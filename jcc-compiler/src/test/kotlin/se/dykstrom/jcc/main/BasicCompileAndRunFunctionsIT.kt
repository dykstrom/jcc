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

package se.dykstrom.jcc.main

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Compile-and-run integration tests for BASIC, specifically for testing functions.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunFunctionsIT : AbstractIntegrationTests() {

    @Test
    fun shouldCallAbs() {
        val source = listOf(
            "print abs(1)",
            "print abs(-1)",
            "print abs(4 * 1000 + 7 * 100 + 11)",
            "print abs(-(4 * 1000 + 7 * 100 + 11))",
            "let a% = 17 : print abs(a%)",
            "let b% = -17 : print abs(b%)",
            "print abs(2147483649)",  // Does not fit in a signed 32-bit integer
            "print abs(-2147483649)",  // Does not fit in a signed 32-bit integer
            "print abs(abs(abs(-5)))" // Nested calls
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1\n1\n4711\n4711\n17\n17\n2147483649\n2147483649\n5\n", 0)
    }

    @Test
    fun shouldCallAsc() {
        val source = listOf(
            "print asc(\"\")",
            "print asc(\"a\")",
            "print asc(\"ABC\")",
            "print asc(\"Z\")",
            "print asc(\"+\")",
            "print asc(\"12345678901234567890123456789012345678901234567890\")"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n97\n65\n90\n43\n49\n", 0)
    }

    @Test
    fun shouldCallChr() {
        val source = listOf(
            "print chr$(65)",
            "print chr$(97)",
            "print chr$(48)"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "A\na\n0\n", 0)
    }

    @Test
    fun shouldMakeIllegalCallToChr() {
        var source = listOf("print chr$(-1)")
        var sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: chr$\n", 1)
        source = listOf("print chr$(256)")
        sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: chr$\n", 1)
    }

    @Test
    fun shouldCallCint() {
        val source = listOf(
            "print cint(99.3)",
            "print cint(99.5)",
            "print cint(99.7)",
            "print cint(-99.3)",
            "print cint(-99.5)",
            "print cint(-99.7)",
            "print cint(100)"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "99\n100\n100\n-99\n-100\n-100\n100\n", 0)
    }

    @Test
    fun shouldCallCdbl() {
        val source = listOf(
            "print cdbl(1)",
            "print cdbl(1.0)",
            "print cdbl(-1)"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1.000000\n1.000000\n-1.000000\n", 0)
    }

    @Test
    fun shouldCallDate() {
        val source = listOf(
            "print date$()"
        )
        val expectedDate = DateTimeFormatter.ofPattern("MM-dd-yyyy").format(LocalDate.now())
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, expectedDate + "\n", 0)
    }

    @Test
    fun shouldCallHex() {
        val source = listOf(
            "print hex$(-1)",
            "print hex$(0)",
            "print hex$(255)",
            "print hex$(254.9)",
            "print hex$(65536)",
            "print hex$(&HABCDEF)"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "FFFFFFFFFFFFFFFF\n0\nFF\nFF\n10000\nABCDEF\n", 0)
    }

    @Test
    fun shouldCallInt() {
        val source = listOf(
            "print int(99.3)",
            "print int(99.5)",
            "print int(99.7)",
            "print int(-99.3)",
            "print int(-99.5)",
            "print int(-99.7)"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "99\n99\n99\n-100\n-100\n-100\n", 0)
    }

    @Test
    fun shouldCallLboundUboundOnArrayWithOneDimension() {
        val source = listOf(
            "dim x%(5) as integer",
            "print lbound(x%); \"-\"; ubound(x%)"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0-5\n", 0)
    }

    @Test
    fun shouldCallLboundUboundOnArrayWithThreeDimensions() {
        val source = listOf(
            "dim x$(2, 4, 6) as string",
            "print lbound(x$, 1); \"-\"; ubound(x$, 1)",
            "print lbound(x$, 2); \"-\"; ubound(x$, 2)",
            "print lbound(x$, 3); \"-\"; ubound(x$, 3)"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0-2\n0-4\n0-6\n", 0)
    }

    @Test
    fun shouldCallLboundUboundOnArrayWithOptionBase1() {
        val source = listOf(
            "option base 1",
            "dim x#(7, 14) as double",
            "print lbound(x#, 1); \"-\"; ubound(x#, 1)",
            "print lbound(x#, 2); \"-\"; ubound(x#, 2)"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1-7\n1-14\n", 0)
    }

    @Test
    fun shouldMakeIllegalCallToUbound() {
        var source = listOf(
            "dim x%(5) as integer",
            "print ubound(x%, 0)"
        )
        var sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: ubound\n", 1)
        source = listOf(
            "dim x%(5) as integer",
            "print ubound(x%, 2)"
        )
        sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: ubound\n", 1)
    }

    @Test
    fun shouldCallMkdCvd() {
        val source = listOf(
            "print cvd(mkd$(-1.0))",
            "print cvd(mkd$(0.0))",
            "print cvd(mkd$(3.14))",
            "print cvd(mkd$(12345.67890))"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-1.000000\n0.000000\n3.140000\n12345.678900\n", 0)
    }

    @Test
    fun shouldCallMkiCvi() {
        val source = listOf(
            "print cvi(mki$(-1))",
            "print cvi(mki$(0))",
            "print cvi(mki$(1))",
            "print cvi(mki$(2147483649))",  // Does not fit in a signed 32-bit integer
            "print cvi(mki$(-2147483649))" // Does not fit in a signed 32-bit integer
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-1\n0\n1\n2147483649\n-2147483649\n", 0)
    }

    @Test
    fun shouldCallOct() {
        val source = listOf(
            "print oct$(-1)",
            "print oct$(0)",
            "print oct$(255)",
            "print oct$(255.1)",
            "print oct$(65536)",
            "print oct$(&O123)"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1777777777777777777777\n0\n377\n377\n200000\n123\n", 0)
    }

    @Test
    fun shouldCallRnd() {
        val source = listOf(
            "print rnd(-1.0)",
            "print rnd(0.0)",
            "print rnd()"
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0.480743\n0.480743\n0.607574\n", 0)
    }

    @Test
    fun shouldCallSgn() {
        val source = listOf(
            "print sgn(0)",
            "print sgn(0.0)",
            "print sgn(1)",
            "print sgn(1.0)",
            "print sgn(-1)",
            "print sgn(-1.0)",
            "print sgn(-1) * 55",
            "print sgn(1000 \\ 37)",
            "print sgn(1000 / 37)",
            "print sgn(-1000 + 50 * 10)",
            "let a = 17 + 0 : print sgn(a)",
            "let b = -17 - 0 : print sgn(b)",
            "print sgn(2147483649)",  // Does not fit in a signed 32-bit integer
            "print sgn(-2147483649)",  // Does not fit in a signed 32-bit integer
            "print sgn(sgn(sgn(-5)))" // Nested calls
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n0\n1\n1\n-1\n-1\n-55\n1\n1\n-1\n1\n-1\n1\n-1\n-1\n", 0)
    }

    @Test
    fun shouldCallTime() {
        val source = listOf(
            "print time$"
        )
        val expected = listOf(DateTimeFormatter.ofPattern("HH").format(LocalTime.now()))
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, expected)
    }

    @Test
    fun shouldCallTimer() {
        val source = listOf(
            "print int(timer / 3600)"
        )
        val expectedHour = LocalTime.now().hour.toString()
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, expectedHour + "\n", 0)
    }

    @Test
    fun shouldCallVal() {
        val source = listOf(
            """
                print val("")
                print val("a")
                print val("0")
                print val("0.0")
                print val("1")
                print val("3.141592")
                print val("4th")
                print val("12.34")
                print val("-12.34")
                print val("2147483649")            ' Does not fit in a signed 32-bit integer
                print val("-2147483649")           ' Does not fit in a signed 32-bit integer
                """
        )
        val sourceFile = createSourceFile(source, Language.BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(
            sourceFile,
            "0.000000\n0.000000\n0.000000\n0.000000\n1.000000\n3.141592\n4.000000\n12.340000\n-12.340000\n2147483649.000000\n-2147483649.000000\n",
            0
        )
    }

    @Test
    fun shouldNotCompileAssignmentFromFunctionWithUnknownReturnValue() {
        val source = listOf(
            "foo = instr(x, y)"
        )
        val sourcePath = createSourceFile(source, Language.BASIC)
        compileAndAssertFail(sourcePath)
    }
}
