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

package se.dykstrom.jcc.main;

import org.junit.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * Compile-and-run integration tests for Basic, specifically for testing functions.
 *
 * @author Johan Dykstrom
 */
public class BasicCompileAndRunFunctionsIT extends AbstractIntegrationTest {

    @Test
    public void shouldCallAbs() throws Exception {
        List<String> source = asList(
                "print abs(1)",
                "print abs(-1)",
                "print abs(4 * 1000 + 7 * 100 + 11)",
                "print abs(-(4 * 1000 + 7 * 100 + 11))",
                "let a% = 17 : print abs(a%)",
                "let b% = -17 : print abs(b%)",
                "print abs(2147483649)",   // Does not fit in a signed 32-bit integer
                "print abs(-2147483649)",  // Does not fit in a signed 32-bit integer
                "print abs(abs(abs(-5)))"  // Nested calls
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1\n1\n4711\n4711\n17\n17\n2147483649\n2147483649\n5\n", 0);
    }

    @Test
    public void shouldCallAsc() throws Exception {
        List<String> source = asList(
                "print asc(\"\")",
                "print asc(\"a\")",
                "print asc(\"ABC\")",
                "print asc(\"Z\")",
                "print asc(\"+\")",
                "print asc(\"12345678901234567890123456789012345678901234567890\")"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0\n97\n65\n90\n43\n49\n", 0);
    }

    @Test
    public void shouldCallChr() throws Exception {
        List<String> source = asList(
                "print chr$(65)",
                "print chr$(97)",
                "print chr$(48)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "A\na\n0\n", 0);
    }

    @Test
    public void shouldMakeIllegalCallToChr() throws Exception {
        List<String> source = singletonList("print chr$(-1)");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: chr$\n", 1);

        source = singletonList("print chr$(256)");
        sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: chr$\n", 1);
    }

    @Test
    public void shouldCallCint() throws Exception {
        List<String> source = asList(
                "print cint(99.3)",
                "print cint(99.5)",
                "print cint(99.7)",
                "print cint(-99.3)",
                "print cint(-99.5)",
                "print cint(-99.7)",
                "print cint(100)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "99\n100\n100\n-99\n-100\n-100\n100\n", 0);
    }

    @Test
    public void shouldCallCdbl() throws Exception {
        List<String> source = asList(
                "print cdbl(1)",
                "print cdbl(1.0)",
                "print cdbl(-1)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1.000000\n1.000000\n-1.000000\n", 0);
    }

    @Test
    public void shouldCallDate() throws Exception {
        List<String> source = singletonList(
                "print date$()"
        );
        String expectedDate = DateTimeFormatter.ofPattern("MM-dd-yyyy").format(LocalDate.now());

        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, expectedDate + "\n", 0);
    }

    @Test
    public void shouldCallHex() throws Exception {
        List<String> source = asList(
                "print hex$(-1)",
                "print hex$(0)",
                "print hex$(255)",
                "print hex$(254.9)",
                "print hex$(65536)",
                "print hex$(&HABCDEF)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "FFFFFFFFFFFFFFFF\n0\nFF\nFF\n10000\nABCDEF\n", 0);
    }

    @Test
    public void shouldCallInt() throws Exception {
        List<String> source = asList(
                "print int(99.3)",
                "print int(99.5)",
                "print int(99.7)",
                "print int(-99.3)",
                "print int(-99.5)",
                "print int(-99.7)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "99\n99\n99\n-100\n-100\n-100\n", 0);
    }

    @Test
    public void shouldCallLboundUboundOnArrayWithOneDimension() throws Exception {
        List<String> source = asList(
                "dim x%(5) as integer",
                "print lbound(x%); \"-\"; ubound(x%)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0-5\n", 0);
    }

    @Test
    public void shouldCallLboundUboundOnArrayWithThreeDimensions() throws Exception {
        List<String> source = asList(
                "dim x$(2, 4, 6) as string",
                "print lbound(x$, 1); \"-\"; ubound(x$, 1)",
                "print lbound(x$, 2); \"-\"; ubound(x$, 2)",
                "print lbound(x$, 3); \"-\"; ubound(x$, 3)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0-2\n0-4\n0-6\n", 0);
    }

    @Test
    public void shouldCallLboundUboundOnArrayWithOptionBase1() throws Exception {
        List<String> source = asList(
                "option base 1",
                "dim x#(7, 14) as double",
                "print lbound(x#, 1); \"-\"; ubound(x#, 1)",
                "print lbound(x#, 2); \"-\"; ubound(x#, 2)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1-7\n1-14\n", 0);
    }

    @Test
    public void shouldMakeIllegalCallToUbound() throws Exception {
        List<String> source = asList(
                "dim x%(5) as integer",
                "print ubound(x%, 0)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: ubound\n", 1);

        source = asList(
                "dim x%(5) as integer",
                "print ubound(x%, 2)"
        );
        sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: ubound\n", 1);
    }

    @Test
    public void shouldCallMkdCvd() throws Exception {
        List<String> source = asList(
                "print cvd(mkd$(-1.0))",
                "print cvd(mkd$(0.0))",
                "print cvd(mkd$(3.14))",
                "print cvd(mkd$(12345.67890))"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "-1.000000\n0.000000\n3.140000\n12345.678900\n", 0);
    }

    @Test
    public void shouldCallMkiCvi() throws Exception {
        List<String> source = asList(
                "print cvi(mki$(-1))",
                "print cvi(mki$(0))",
                "print cvi(mki$(1))",
                "print cvi(mki$(2147483649))",   // Does not fit in a signed 32-bit integer
                "print cvi(mki$(-2147483649))"   // Does not fit in a signed 32-bit integer
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "-1\n0\n1\n2147483649\n-2147483649\n", 0);
    }

    @Test
    public void shouldCallOct() throws Exception {
        List<String> source = asList(
                "print oct$(-1)",
                "print oct$(0)",
                "print oct$(255)",
                "print oct$(255.1)",
                "print oct$(65536)",
                "print oct$(&O123)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1777777777777777777777\n0\n377\n377\n200000\n123\n", 0);
    }

    @Test
    public void shouldCallRnd() throws Exception {
        List<String> source = asList(
                "print rnd(-1.0)",
                "print rnd(0.0)",
                "print rnd()"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0.480743\n0.480743\n0.607574\n", 0);
    }

    @Test
    public void shouldCallSgn() throws Exception {
        List<String> source = asList(
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
                "print sgn(2147483649)",   // Does not fit in a signed 32-bit integer
                "print sgn(-2147483649)",  // Does not fit in a signed 32-bit integer
                "print sgn(sgn(sgn(-5)))"  // Nested calls
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0\n0\n1\n1\n-1\n-1\n-55\n1\n1\n-1\n1\n-1\n1\n-1\n-1\n", 0);
    }

    @Test
    public void shouldCallTime() throws Exception {
        List<String> source = singletonList(
                "print time$"
        );
        List<String> expected = singletonList(DateTimeFormatter.ofPattern("HH").format(LocalTime.now()));

        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, expected);
    }

    @Test
    public void shouldCallTimer() throws Exception {
        List<String> source = singletonList(
                "print int(timer / 3600)"
        );
        String expectedHour = Integer.toString(LocalTime.now().getHour());

        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, expectedHour + "\n", 0);
    }

    @Test
    public void shouldCallVal() throws Exception {
        List<String> source = List.of(
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
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(
                sourceFile,
                """
                0.000000
                0.000000
                0.000000
                0.000000
                1.000000
                3.141592
                4.000000
                12.340000
                -12.340000
                2147483649.000000
                -2147483649.000000
                """,
                0
        );
    }

    @Test
    public void shouldNotCompileAssignmentFromFunctionWithUnknownReturnValue() throws Exception {
        List<String> source = singletonList(
                "foo = instr(x, y)"
        );
        Path sourcePath = createSourceFile(source, BASIC);
        compileAndAssertFail(sourcePath);
    }
}