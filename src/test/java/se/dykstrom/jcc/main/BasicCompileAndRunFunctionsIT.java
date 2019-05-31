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
    public void shouldCallInstr2() throws Exception {
        List<String> source = asList(
                "print instr(\"fooboo\", \"foo\")",
                "print instr(\"fooboo\", \"boo\")",
                "print instr(\"fooboo\", \"zoo\")",
                "print instr(\"fooboo\", \"\")",
                "print instr(\"\", \"foo\")"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1\n4\n0\n1\n0\n", 0);
    }

    @Test
    public void shouldCallInstr3() throws Exception {
        List<String> source = asList(
                "print instr(1, \"fooboo\", \"foo\")",
                "print instr(2, \"fooboo\", \"foo\")",
                "print instr(1, \"fooboo\", \"boo\")",
                "print instr(4, \"fooboo\", \"boo\")",
                "print instr(5, \"fooboo\", \"boo\")",
                "print instr(5, \"fooboo\", \"o\")",
                "print instr(1, \"fooboo\", \"zoo\")",
                "print instr(1, \"fooboo\", \"\")",
                "print instr(1, \"\", \"foo\")",
                "print instr(0, \"fooboo\", \"o\")",  // Start index too low
                "print instr(10, \"fooboo\", \"o\")"  // Start index too high
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1\n0\n4\n4\n0\n5\n0\n1\n0\n0\n0\n", 0);
    }

    @Test
    public void shouldCallInstr2AndInstr3() throws Exception {
        List<String> source = asList(
                "print instr(\"fooboo\", \"foo\"); \" \"; instr(1, \"fooboo\", \"foo\")",
                "print instr(\"fooboo\", \"boo\"); \" \"; instr(7, \"fooboo\", \"boo\")"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1 1\n4 0\n", 0);
    }

    @Test
    public void shouldCallLcase() throws Exception {
        List<String> source = asList(
                "print lcase$(\"\")",
                "print lcase$(\"A\")",
                "print lcase$(\"ABC\")",
                "print lcase$(\"Hello, World!\")"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "\na\nabc\nhello, world!\n", 0);
    }

    @Test
    public void shouldCallLeft() throws Exception {
        List<String> source = asList(
                "print left$(\"\", 0)",
                "print left$(\"\", 5)",
                "print left$(\"ABC\", 0)",
                "print left$(\"ABC\", 1)",
                "print left$(\"ABC\", 3)",
                "print left$(\"ABC\", 5)",
                "print left$(\"Hello, world!\", 5)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "\n\n\nA\nABC\nABC\nHello\n", 0);
    }

    @Test
    public void shouldMakeIllegalCallToLeft() throws Exception {
        List<String> source = singletonList("print left$(\"\", -1)");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: left$\n", 1);
    }

    @Test
    public void shouldCallLen() throws Exception {
        List<String> source = asList(
                "print len(\"\")",
                "print len(\"a\")",
                "print len(\"abc\")",
                "print len(\"12345678901234567890123456789012345678901234567890\")"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0\n1\n3\n50\n", 0);
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
    public void shouldCallRight() throws Exception {
        List<String> source = asList(
                "print right$(\"\", 0)",
                "print right$(\"\", 5)",
                "print right$(\"ABC\", 0)",
                "print right$(\"ABC\", 1)",
                "print right$(\"ABC\", 3)",
                "print right$(\"ABC\", 5)",
                "print right$(\"Hello, world!\", 6)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "\n\n\nC\nABC\nABC\nworld!\n", 0);
    }

    @Test
    public void shouldMakeIllegalCallToRight() throws Exception {
        List<String> source = singletonList("print right$(\"\", -1)");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: right$\n", 1);
    }

    @Test
    public void shouldCallSgn() throws Exception {
        List<String> source = asList(
                "print sgn(0)",
                "print sgn(1)",
                "print sgn(-1)",
                "print sgn(-1) * 55",
                "print sgn(1000 \\ 37)",
                "print sgn(-1000 + 50 * 10)",
                "let a = 17 + 0 : print sgn(a)",
                "let b = -17 - 0 : print sgn(b)",
                "print sgn(2147483649)",   // Does not fit in a signed 32-bit integer
                "print sgn(-2147483649)",  // Does not fit in a signed 32-bit integer
                "print sgn(sgn(sgn(-5)))"  // Nested calls
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0\n1\n-1\n-55\n1\n-1\n1\n-1\n1\n-1\n-1\n", 0);
    }

    @Test
    public void shouldCallSpace() throws Exception {
        List<String> source = asList(
                "print \"X\"; space$(-1); \"X\"",
                "print \"X\"; space$(0); \"X\"",
                "print \"X\"; space$(1); \"X\"",
                "print \"X\"; space$(3); \"X\"",
                "print \"X\"; space$(10); \"X\""
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "XX\nXX\nX X\nX   X\nX          X\n", 0);
    }

    @Test
    public void shouldCallStringInt() throws Exception {
        List<String> source = asList(
                "print \"X\"; string$(0, 32); \"X\"",
                "print \"X\"; string$(1, 48); \"X\"",
                "print \"X\"; string$(3, 49); \"X\"",
                "print \"X\"; string$(10, 32); \"X\""
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "XX\nX0X\nX111X\nX          X\n", 0);
    }

    @Test
    public void shouldMakeIllegalCallToStringInt() throws Exception {
        List<String> source = singletonList("print string$(-1, 32)");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1);

        source = singletonList("print string$(5, -1)");
        sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1);

        source = singletonList("print string$(5, 256)");
        sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1);
    }

    @Test
    public void shouldCallStringStr() throws Exception {
        List<String> source = asList(
                "print \"X\"; string$(0, \"*\"); \"X\"",
                "print \"X\"; string$(1, \"+++\"); \"X\"",
                "print \"X\"; string$(3, \"abcde\"); \"X\"",
                "print \"X\"; string$(10, \"-\"); \"X\""
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "XX\nX+X\nXaaaX\nX----------X\n", 0);
    }

    @Test
    public void shouldMakeIllegalCallToStringStr() throws Exception {
        List<String> source = singletonList("print string$(-1, \"-\")");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1);

        source = singletonList("print string$(5, \"\")");
        sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1);
    }

    @Test
    public void shouldCallUcase() throws Exception {
        List<String> source = asList(
                "print ucase$(\"\")",
                "print ucase$(\"a\")",
                "print ucase$(\"abc\")",
                "print ucase$(\"Hello, World!\")"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "\nA\nABC\nHELLO, WORLD!\n", 0);
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
