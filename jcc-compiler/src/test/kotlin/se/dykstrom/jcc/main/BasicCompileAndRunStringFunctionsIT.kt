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

package se.dykstrom.jcc.main

import org.junit.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for BASIC, specific for testing string manipulation functions.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunStringFunctionsIT : AbstractIntegrationTests() {

    @Test
    fun shouldCallInstr2() {
        val source = listOf(
            "print instr(\"fooboo\", \"foo\")",
            "print instr(\"fooboo\", \"boo\")",
            "print instr(\"fooboo\", \"zoo\")",
            "print instr(\"fooboo\", \"\")",
            "print instr(\"\", \"foo\")"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1\n4\n0\n1\n0\n", 0)
    }

    @Test
    fun shouldCallInstr3() {
        val source = listOf(
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
            "print instr(10, \"fooboo\", \"o\")" // Start index too high
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1\n0\n4\n4\n0\n5\n0\n1\n0\n0\n0\n", 0)
    }

    @Test
    fun shouldCallInstr2AndInstr3() {
        val source = listOf(
            "print instr(\"fooboo\", \"foo\"); \" \"; instr(1, \"fooboo\", \"foo\")",
            "print instr(\"fooboo\", \"boo\"); \" \"; instr(7, \"fooboo\", \"boo\")"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1 1\n4 0\n", 0)
    }

    @Test
    fun shouldCallLcase() {
        val source = listOf(
            "print lcase$(\"\")",
            "print lcase$(\"A\")",
            "print lcase$(\"ABC\")",
            "print lcase$(\"Hello, World!\")"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "\na\nabc\nhello, world!\n", 0)
    }

    @Test
    fun shouldCallLeft() {
        val source = listOf(
            "print left$(\"\", 0)",
            "print left$(\"\", 5)",
            "print left$(\"ABC\", 0)",
            "print left$(\"ABC\", 1)",
            "print left$(\"ABC\", 3)",
            "print left$(\"ABC\", 5)",
            "print left$(\"Hello, world!\", 5)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "\n\n\nA\nABC\nABC\nHello\n", 0)
    }

    @Test
    fun shouldMakeIllegalCallToLeft() {
        val source = listOf("print left$(\"\", -1)")
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: left$\n", 1)
    }

    @Test
    fun shouldCallLen() {
        val source = listOf(
            "print len(\"\")",
            "print len(\"a\")",
            "print len(\"abc\")",
            "print len(\"12345678901234567890123456789012345678901234567890\")"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n1\n3\n50\n", 0)
    }

    @Test
    fun shouldCallLtrim() {
        val source = listOf(
            "print ltrim$(\"\")",
            "print ltrim$(\"   \")",
            "print ltrim$(\"ABC\")",
            "print ltrim$(\"   ABC\")",
            "print ltrim$(\"ABC   \")",
            "print ltrim$(\"   ABC   \")"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "\n\nABC\nABC\nABC   \nABC   \n", 0)
    }

    @Test
    fun shouldCallMid2() {
        val source = listOf(
            "print mid$(\"\", 1)",
            "print mid$(\"\", 5)",
            "print mid$(\"ABC\", 1)",
            "print mid$(\"ABC\", 3)",
            "print mid$(\"ABC\", 4)",
            "print mid$(\"Hello, world!\", 1)",
            "print mid$(\"Hello, world!\", 8)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "\n\nABC\nC\n\nHello, world!\nworld!\n", 0)
    }

    @Test
    fun shouldMakeIllegalCallToMid2() {
        val source = listOf("print mid$(\"\", 0)")
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: mid$\n", 1)
    }

    @Test
    fun shouldCallMid3() {
        val source = listOf(
            "print mid$(\"\", 1, 5)",
            "print mid$(\"ABC\", 1, 0)",
            "print mid$(\"ABC\", 5, 0)",
            "print mid$(\"ABC\", 5, 1)",
            "print mid$(\"ABC\", 2, 1)",
            "print mid$(\"ABC\", 1, 2)",
            "print mid$(\"ABC\", 1, 3)",
            "print mid$(\"ABC\", 1, 10)",
            "print mid$(\"This is a random text\", 6, 4)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "\n\n\n\nB\nAB\nABC\nABC\nis a\n", 0)
    }

    @Test
    fun shouldMakeIllegalCallToMid3() {
        var source = listOf("print mid$(\"\", 0, 5)") // Start less than 1
        var sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: mid$\n", 1)
        source = listOf("print mid$(\"\", 1, -1)") // Number less than 0
        sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: mid$\n", 1)
    }

    @Test
    fun shouldCallMid2AndMid3() {
        val source = listOf(
            "print mid$(\"ABC\", 2, 1)",
            "print mid$(\"ABC\", 1)",
            "print mid$(mid$(\"This is a random text\", 6, 11), 6)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "B\nABC\nrandom\n", 0)
    }

    @Test
    fun shouldCallRight() {
        val source = listOf(
            "print right$(\"\", 0)",
            "print right$(\"\", 5)",
            "print right$(\"ABC\", 0)",
            "print right$(\"ABC\", 1)",
            "print right$(\"ABC\", 3)",
            "print right$(\"ABC\", 5)",
            "print right$(\"Hello, world!\", 6)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "\n\n\nC\nABC\nABC\nworld!\n", 0)
    }

    @Test
    fun shouldMakeIllegalCallToRight() {
        val source = listOf("print right$(\"\", -1)")
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: right$\n", 1)
    }

    @Test
    fun shouldCallRtrim() {
        val source = listOf(
            "print rtrim$(\"\")",
            "print rtrim$(\"   \")",
            "print rtrim$(\"ABC\")",
            "print rtrim$(\"   ABC\")",
            "print rtrim$(\"ABC   \")",
            "print rtrim$(\"   ABC   \")"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "\n\nABC\n   ABC\nABC\n   ABC\n", 0)
    }

    @Test
    fun shouldCallSpace() {
        val source = listOf(
            "print \"X\"; space$(-1); \"X\"",
            "print \"X\"; space$(0); \"X\"",
            "print \"X\"; space$(1); \"X\"",
            "print \"X\"; space$(3); \"X\"",
            "print \"X\"; space$(3.2); \"X\"",
            "print \"X\"; space$(10); \"X\""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "XX\nXX\nX X\nX   X\nX   X\nX          X\n", 0)
    }

    @Test
    fun shouldCallStr() {
        val source = listOf(
            "print str$(0)",
            "print str$(-12345)",
            "print str$(1000000)",
            "print str$(3.14)",
            "print str$(-9.999888)",
            "print str$(5E04)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, " 0\n-12345\n 1000000\n 3.140000\n-9.999888\n 50000.000000\n", 0)
    }

    @Test
    fun shouldCallStringInt() {
        val source = listOf(
            "print \"X\"; string$(0, 32); \"X\"",
            "print \"X\"; string$(1, 48); \"X\"",
            "print \"X\"; string$(3, 49); \"X\"",
            "print \"X\"; string$(10, 32); \"X\""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "XX\nX0X\nX111X\nX          X\n", 0)
    }

    @Test
    fun shouldMakeIllegalCallToStringInt() {
        var source = listOf("print string$(-1, 32)")
        var sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1)
        source = listOf("print string$(5, -1)")
        sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1)
        source = listOf("print string$(5, 256)")
        sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1)
    }

    @Test
    fun shouldCallStringStr() {
        val source = listOf(
            "print \"X\"; string$(0, \"*\"); \"X\"",
            "print \"X\"; string$(1, \"+++\"); \"X\"",
            "print \"X\"; string$(3, \"abcde\"); \"X\"",
            "print \"X\"; string$(10, \"-\"); \"X\""
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "XX\nX+X\nXaaaX\nX----------X\n", 0)
    }

    @Test
    fun shouldMakeIllegalCallToStringStr() {
        var source = listOf("print string$(-1, \"-\")")
        var sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1)
        source = listOf("print string$(5, \"\")")
        sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "Error: Illegal function call: string$\n", 1)
    }

    @Test
    fun shouldCallUcase() {
        val source = listOf(
            "print ucase$(\"\")",
            "print ucase$(\"a\")",
            "print ucase$(\"abc\")",
            "print ucase$(\"Hello, World!\")"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "\nA\nABC\nHELLO, WORLD!\n", 0)
    }
}
