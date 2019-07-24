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
                "let a = 17 : print abs(a)",
                "let b = -17 : print abs(b)",
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
    public void shouldCallHex() throws Exception {
        List<String> source = asList(
                "print hex$(-1)",
                "print hex$(0)",
                "print hex$(255)",
                "print hex$(65536)",
                "print hex$(&HABCDEF)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "FFFFFFFFFFFFFFFF\n0\nFF\n10000\nABCDEF\n", 0);
    }

    @Test
    public void shouldCallOct() throws Exception {
        List<String> source = asList(
                "print oct$(-1)",
                "print oct$(0)",
                "print oct$(255)",
                "print oct$(65536)",
                "print oct$(&O123)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1777777777777777777777\n0\n377\n200000\n123\n", 0);
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
    public void shouldCallVal() throws Exception {
        List<String> source = asList(
                "print val(\"\")",
                "print val(\"a\")",
                "print val(\"0\")",
                "print val(\"1\")",
                "print val(\"4th\")",
                "print val(\"1234\")",
                "print val(\"-1234\")",
                "print val(\"2147483649\")",            // Does not fit in a signed 32-bit integer
                "print val(\"-2147483649\")",           // Does not fit in a signed 32-bit integer
                "print val(\"9223372036854775807\")",   // Max value of 64-bit integer
                "print val(\"-9223372036854775808\")",  // Min value of 64-bit integer
                "print val(\"9223372036854775808\")"    // Overflow
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, 
                            "0\n0\n0\n1\n4\n1234\n-1234\n2147483649\n-2147483649\n9223372036854775807\n-9223372036854775808\n9223372036854775807\n", 
                            0);
    }
}
