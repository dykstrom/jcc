/*
 * Copyright (C) 2019 Johan Dykstrom
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
 * Compile-and-run integration tests for Basic, specific for testing string manipulation functions.
 *
 * @author Johan Dykstrom
 */
public class BasicCompileAndRunStringFunctionsIT extends AbstractIntegrationTest {

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
    public void shouldCallLtrim() throws Exception {
        List<String> source = asList(
                "print ltrim$(\"\")",
                "print ltrim$(\"   \")",
                "print ltrim$(\"ABC\")",
                "print ltrim$(\"   ABC\")",
                "print ltrim$(\"ABC   \")",
                "print ltrim$(\"   ABC   \")"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "\n\nABC\nABC\nABC   \nABC   \n", 0);
    }

    @Test
    public void shouldCallMid2() throws Exception {
        List<String> source = asList(
                "print mid$(\"\", 1)",
                "print mid$(\"\", 5)",
                "print mid$(\"ABC\", 1)",
                "print mid$(\"ABC\", 3)",
                "print mid$(\"ABC\", 4)",
                "print mid$(\"Hello, world!\", 1)",
                "print mid$(\"Hello, world!\", 8)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "\n\nABC\nC\n\nHello, world!\nworld!\n", 0);
    }

    @Test
    public void shouldMakeIllegalCallToMid2() throws Exception {
        List<String> source = singletonList("print mid$(\"\", 0)");
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: mid$\n", 1);
    }

    @Test
    public void shouldCallMid3() throws Exception {
        List<String> source = asList(
                "print mid$(\"\", 1, 5)",
                "print mid$(\"ABC\", 1, 0)",
                "print mid$(\"ABC\", 5, 0)",
                "print mid$(\"ABC\", 5, 1)",
                "print mid$(\"ABC\", 2, 1)",
                "print mid$(\"ABC\", 1, 2)",
                "print mid$(\"ABC\", 1, 3)",
                "print mid$(\"ABC\", 1, 10)",
                "print mid$(\"This is a random text\", 6, 4)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "\n\n\n\nB\nAB\nABC\nABC\nis a\n", 0);
    }

    @Test
    public void shouldMakeIllegalCallToMid3() throws Exception {
        List<String> source = singletonList("print mid$(\"\", 0, 5)"); // Start less than 1
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: mid$\n", 1);

        source = singletonList("print mid$(\"\", 1, -1)"); // Number less than 0
        sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: mid$\n", 1);
    }

    @Test
    public void shouldCallMid2AndMid3() throws Exception {
        List<String> source = asList(
                "print mid$(\"ABC\", 2, 1)",
                "print mid$(\"ABC\", 1)",
                "print mid$(mid$(\"This is a random text\", 6, 11), 6)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "B\nABC\nrandom\n", 0);
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
    public void shouldCallRtrim() throws Exception {
        List<String> source = asList(
                "print rtrim$(\"\")",
                "print rtrim$(\"   \")",
                "print rtrim$(\"ABC\")",
                "print rtrim$(\"   ABC\")",
                "print rtrim$(\"ABC   \")",
                "print rtrim$(\"   ABC   \")"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "\n\nABC\n   ABC\nABC\n   ABC\n", 0);
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
    public void shouldCallStr() throws Exception {
        List<String> source = asList(
                "print str$(0)",
                "print str$(-12345)",
                "print str$(1000000)",
                "print str$(3.14)",
                "print str$(-9.999888)",
                "print str$(5E04)"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, " 0\n-12345\n 1000000\n 3.140000\n-9.999888\n 50000.000000\n", 0);
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
}
