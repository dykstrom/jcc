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

package se.dykstrom.jcc.basic.compiler;

import org.junit.Test;

import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

public class BasicParserTest extends AbstractBasicParserTest {

    @Test
    public void shouldParseEmptyProgram() {
        parse("");
    }

    @Test
    public void testPrint() {
        parse("10 print");
        parse("10 print \"Hello, world!\"");
        parse("10 print \"Hello, \";\"world!\"");
        parse("10 print \"One\",\"Two\",\"Three\"");
        parse("10 print 17");
        parse("10 print TRUE");
    }

    @Test
    public void testGoto() {
        parse("10 goto 10");
        parse("10 goto 123456789");
    }

    @Test
    public void testOnGoto() {
        parse("10 on x goto 10");
        parse("10 on 3 goto 10, 20, 30");
    }

    @Test
    public void testAssignment() {
        parse("10 let a = 5");
        parse("10 let abc123 = 123");
        parse("10 LET LIMIT% = 1");
        parse("10 LET NAME$ = \"Foo\"");
        parse("10 abc123 = 123");
        parse("10 MAX.FILES% = 50");
        parse("20 this.var = that.var");
        parse("20 s$ = t$");
        parse("20 bool = FALSE");
        parse("30 f1 = 3.3E10");
        parse("30 f2 = .0001");
        parse("30 f# = 0.0");
    }

    @Test
    public void testEnd() {
        parse("10 end");
    }

    @Test
    public void testRem() {
        parse("10 rem");
        parse("10 rem 1");
        parse("10 '");
        parse("10 'Comment");
        parse("10 print:rem");
        parse("10 goto 10 : rem Endless loop...");
    }

    @Test
    public void testTwoPrintsOneLine() {
        parse("10 print : print");
        parse("10 print 1 : print 2");
        parse("10 print\"Hi\" : print \"there!\"");
        parse("10 print false : print true");
    }

    @Test
    public void testPrintExpressions() {
        parse("10 print -5");
        parse("10 print -(5)");
        parse("10 print -a%");
        parse("10 print 1 + 2 + 3");
        parse("10 print 3 mod 2");
        parse("10 print 1 * (2 + 3) / 4");
        parse("10 print 5 \\ 2 + 9 \\ 4 MOD 2");
        parse("10 print (1-2)/(2-1)*(1+2)/(2+1)MOD(2+1)\\(1+2)");
        parse("10 print (((1 + 2) - 3) * 4) \\ 5");
        parse("10 print name$; age%");
        // Relational and conditional operators
        parse("10 print \"A\" <> \"B\"");
        parse("10 print 1 > 2; not true and false");
        parse("10 print 1 > 2 or  1 < 2 and (0 = 0 or 0 <> 0)");
        parse("10 print 1 > 2 or 1 < 2 xor 1 = 1 and false");
        parse("10 print 5 + 3 <> 10 xor not 7 > 5");
        // Hexadecimal, octal, and binary numbers
        parse("10 print &HFF; &H0");
        parse("10 print &HFACE - &HFACE");
        parse("10 print &O10; &O77");
        parse("10 print &B10; &B10010");
        // Floating point numbers
        parse("10 print 1.0; .1; 1.");
        parse("10 print 3.14#; 3.14e10; 3.14e+10; 3.14e-3");
        parse("10 print .123E+300; 1.D-10; 5.5d+13#");
        parse("10 print 1.e10 + 7 - .123# * 10.10d10");
    }

    @Test
    public void testTwoPrintsTwoLines() {
        parse("10 print" + EOL + "20 print");
        parse("10 print \"Hi\"" + EOL + "20 print \"there!\"");
    }

    @Test
    public void testLetAndPrintOneLine() {
        parse("10 LET A$=\"foo\" : PRINT \"bar\"");
        parse("10 number = 5 : print");
        parse("10 value% = 17 : print \"value = \"; value%");
        parse("10 bool = 5 = 1 : print \"5 = 1: \"; bool");
        parse("10 float# = 17# : print \"17: \"; float#");
    }

    @Test
    public void testPrintAndGotoOneLine() {
        parse("10 print : goto 10");
        parse("10 print \"1\" : print \"2\" : print \"3\" : goto 10");
    }

    @Test
    public void testPrintAndGotoTwoLines() {
        parse("10 print" + EOL + "20 goto 10");
        parse("10 print \"20\"" + EOL + "20 goto 10");
    }

    @Test
    public void testCapitalLetters() {
        parse("10 PRINT \"CAPITAL\"");
        parse("10 PRINT" + EOL + "20 GOTO 10");
        parse("10 LET A% = 0" + EOL + "20 PRINT A%");
    }

    // Negative tests:
    
    @Test(expected = IllegalStateException.class)
    public void testMissingGotoLine() {
        parse("10 goto");
    }

    @Test(expected = IllegalStateException.class)
    public void testGotoSymbol() {
        parse("10 goto ?");
    }

    @Test(expected = IllegalStateException.class)
    public void testGotoWord() {
        parse("10 goto ten");
    }
    
    @Test(expected = IllegalStateException.class)
    public void testMissingOnGotoExpression() {
        parse("10 on goto 10");
    }
    
    @Test(expected = IllegalStateException.class)
    public void testMissingOnGotoLine() {
        parse("10 on x goto");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingStatementAfterColon() {
        parse("10 print :");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingExpressionInAssignment() {
        parse("10 let value =");
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidVariableName() {
        parse("10 let foo_bar = 17");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingQuotationMark() {
        parse("10 print \"Unfinished string");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingConditionAfterAnd() {
        parse("10 print 1 <> 0 and");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingHexNumber() {
        parse("10 print &H");
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidHexNumber() {
        parse("10 print &HGG");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingOctNumber() {
        parse("10 print &O");
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidOctNumber() {
        parse("10 print &O88");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingBinNumber() {
        parse("10 print &B");
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidBinNumber() {
        parse("10 print &B123");
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidFloatNumber() {
        parse("10 print 12.34F+10#");
    }
}
