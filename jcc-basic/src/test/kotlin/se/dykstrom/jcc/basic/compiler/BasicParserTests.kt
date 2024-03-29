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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import se.dykstrom.jcc.common.utils.FormatUtils.EOL

class BasicParserTests : AbstractBasicParserTests() {

    @Test
    fun shouldParseEmptyProgram() {
        parse("")
    }

    @Test
    fun shouldParsePrintStatements() {
        parse("10 print")
        parse("10 print \"Hello, world!\"")
        parse("10 print \"Hello, \";\"world!\"")
        parse("10 print \"One\",\"Two\",\"Three\"")
        parse("10 print 17")
        parse("10 print -1")
    }

    @Test
    fun shouldParseLabels() {
        parse("foo: gosub bar")
        parse("bar: print 17")
        parse("fifteen.5: rem")
    }

    @Test
    fun shouldParseGosubStatements() {
        parse("10 gosub 10")
        parse("10 gosub 1000")
        parse("10 gosub label")
    }

    @Test
    fun shouldParseReturnStatements() {
        parse("10 return")
    }

    @Test
    fun shouldParseRandomizeStatements() {
        parse("10 randomize")
        parse("10 randomize 0.37 * 17")
        parse("10 randomize -1")
        parse("10 randomize seed")
    }

    @Test
    fun shouldParseSwapStatements() {
        parse("10 swap a, b")
        parse("10 swap foo$, bar$")
        parse("10 swap f%, g#")
    }

    @Test
    fun shouldParseGotoStatements() {
        parse("10 goto 10")
        parse("10 goto 123456789")
        parse("10 goto foo")
    }

    @Test
    fun shouldParseOnGotoStatements() {
        parse("10 on x goto 10")
        parse("10 on 3 goto 10, 20, 30")
        parse("10 on y goto foo, bar")
        parse("10 on 3 goto 10, foo, 30, bar")
    }

    @Test
    fun shouldParseOnGosubStatements() {
        parse("10 on x gosub 10")
        parse("10 on 3 gosub 10, 20, 30")
        parse("10 on 1 gosub foo, bar, axe")
    }

    @Test
    fun shouldParseDefStatements() {
        parse("10 defdbl a-b,c,d-e")
        parse("10 defint a-b,c,d-e")
        parse("10 defstr a-b,c,d-e")
        parse("10 DEFSTR a-b,c,d-e")
        parse("10 Defstr a-b,c,d-e")
    }

    @Test
    fun shouldParseDimStatements() {
        parse("dim a as integer")
        parse("dim aa as DOUBLE, bb as STRING, dd as DOUBLE")
        parse("DIM count AS INTEGER, name AS STRING")
        parse("Dim count As Integer, name As String")
    }

    @Test
    fun shouldParseConstStatements() {
        parse("const a = 5")
        parse("CONST FOO = 2.56")
        parse("CONST FOO = 2.56 + 20 / (9 + 0.1)")
        parse("CONST STR = \"S\"")
        parse("CONST STR = \"P\" + \"S\"")
        parse("const one = 1, two = 2, three = 3")
        parse("const foo = 1 * 7, bar = foo / 7")
        parse("CONST i% = 7, f# = 1.0, s$ = \"\"")
    }

    @Test
    fun testAssignment() {
        parse("10 let a = 5")
        parse("10 let abc123 = 123")
        parse("10 LET LIMIT% = 1")
        parse("10 LET NAME$ = \"Foo\"")
        parse("10 abc123 = 123")
        parse("10 MAX.FILES% = 50")
        parse("20 this.var = that.var")
        parse("20 s$ = t$")
        parse("30 f1 = 3.3E10")
        parse("30 f2 = .0001")
        parse("30 f# = 0.0")
    }

    @Test
    fun testEnd() {
        parse("10 end")
    }

    @Test
    fun testRem() {
        parse("10 rem")
        parse("10 rem 1")
        parse("10 '")
        parse("10 'Comment")
        parse("10 print:rem")
        parse("10 goto 10 : rem Endless loop...")
    }

    @Test
    fun testTwoPrintsOneLine() {
        parse("10 print : print")
        parse("10 print 1 : print 2")
        parse("10 print\"Hi\" : print \"there!\"")
        parse("10 print 0 : print -1")
    }

    @Test
    fun testPrintExpressions() {
        parse("10 print -5")
        parse("10 print -(5)")
        parse("10 print -a%")
        parse("10 print 1 + 2 + 3")
        parse("10 print 3 mod 2")
        parse("10 print 1 * (2 + 3) / 4")
        parse("10 print 5 \\ 2 + 9 \\ 4 MOD 2")
        parse("10 print (1-2)/(2-1)*(1+2)/(2+1)MOD(2+1)\\(1+2)")
        parse("10 print (((1 + 2) - 3) * 4) \\ 5")
        parse("10 print name$; age%")
        // Relational and bitwise operators
        parse("10 print \"A\" <> \"B\"")
        parse("10 print 1 > 2; not -1 and 0")
        parse("10 print 1 > 2 or  1 < 2 and (0 = 0 or 0 <> 0)")
        parse("10 print 1 > 2 or 1 < 2 xor 1 = 1 and 0")
        parse("10 print 5 + 3 <> 10 xor not 7 > 5")
        // Hexadecimal, octal, and binary numbers
        parse("10 print &HFF; &H0")
        parse("10 print &HFACE - &HFACE")
        parse("10 print &O10; &O77")
        parse("10 print &B10; &B10010")
        // Floating point numbers
        parse("10 print 1.0; .1; 1.")
        parse("10 print 3.14#; 3.14e10; 3.14e+10; 3.14e-3")
        parse("10 print .123E+300; 1.D-10; 5.5d+13#")
        parse("10 print 1.e10 + 7 - .123# * 10.10d10")
    }

    @Test
    fun testTwoPrintsTwoLines() {
        parse("10 print" + EOL + "20 print")
        parse("10 print \"Hi\"" + EOL + "20 print \"there!\"")
    }

    @Test
    fun testLetAndPrintOneLine() {
        parse("10 LET A$=\"foo\" : PRINT \"bar\"")
        parse("10 number = 5 : print number")
        parse("10 string$ = \"s\" : print string$")
        parse("10 int% = 17 : print \"value = \"; int%")
        parse("10 float# = 17# : print \"17: \"; float#")
    }

    @Test
    fun testPrintAndGotoOneLine() {
        parse("10 print : goto 10")
        parse("10 print \"1\" : print \"2\" : print \"3\" : goto 10")
    }

    @Test
    fun testPrintAndGotoTwoLines() {
        parse("10 print" + EOL + "20 goto 10")
        parse("10 print \"20\"" + EOL + "20 goto 10")
    }

    @Test
    fun testCapitalLetters() {
        parse("10 PRINT \"CAPITAL\"")
        parse("10 PRINT" + EOL + "20 GOTO 10")
        parse("10 LET A% = 0" + EOL + "20 PRINT A%")
    }

    // Negative tests:

    @Test
    fun testInvalidLabel() {
        assertThrows<IllegalStateException> { parse("foo_bar: print") }
    }

    @Test
    fun testMissingGotoLine() {
        assertThrows<IllegalStateException> { parse("10 goto") }
    }

    @Test
    fun testMissingGosubLine() {
        assertThrows<IllegalStateException> { parse("10 gosub") }
    }

    @Test
    fun testGotoSymbol() {
        assertThrows<IllegalStateException> { parse("10 goto ?") }
    }

    @Test
    fun testGotoInvalidLabel() {
        assertThrows<IllegalStateException> { parse("10 goto one_two") }
    }

    @Test
    fun testMissingOnGotoExpression() {
        assertThrows<IllegalStateException> { parse("10 on goto 10") }
    }

    @Test
    fun testMissingOnGotoLine() {
        assertThrows<IllegalStateException> { parse("10 on x goto") }
    }

    @Test
    fun testMissingStatementAfterColon() {
        assertThrows<IllegalStateException> { parse("10 print :") }
    }

    @Test
    fun testMissingExpressionInAssignment() {
        assertThrows<IllegalStateException> { parse("10 let value =") }
    }

    @Test
    fun testInvalidVariableName() {
        assertThrows<IllegalStateException> { parse("10 let foo_bar = 17") }
    }

    @Test
    fun testMissingQuotationMark() {
        assertThrows<IllegalStateException> { parse("10 print \"Unfinished string") }
    }

    @Test
    fun testMissingConditionAfterAnd() {
        assertThrows<IllegalStateException> { parse("10 print 1 <> 0 and") }
    }

    @Test
    fun testMissingHexNumber() {
        assertThrows<IllegalStateException> { parse("10 print &H") }
    }

    @Test
    fun testInvalidHexNumber() {
        assertThrows<IllegalStateException> { parse("10 print &HGG") }
    }

    @Test
    fun testMissingOctNumber() {
        assertThrows<IllegalStateException> { parse("10 print &O") }
    }

    @Test
    fun testInvalidOctNumber() {
        assertThrows<IllegalStateException> { parse("10 print &O88") }
    }

    @Test
    fun testMissingBinNumber() {
        assertThrows<IllegalStateException> { parse("10 print &B") }
    }

    @Test
    fun testInvalidBinNumber() {
        assertThrows<IllegalStateException> { parse("10 print &B123") }
    }

    @Test
    fun testInvalidFloatNumber() {
        assertThrows<IllegalStateException> { parse("10 print 12.34F+10#") }
    }

    @Test
    fun testTwoExpressionsInRandomize() {
        assertThrows<IllegalStateException> { parse("10 randomize 5, 6") }
    }

    @Test
    fun testMissingIdentInSwap() {
        assertThrows<IllegalStateException> { parse("10 swap a, ") }
    }

    @Test
    fun testValueInSwap() {
        assertThrows<IllegalStateException> { parse("10 swap a, 5") }
    }
}
