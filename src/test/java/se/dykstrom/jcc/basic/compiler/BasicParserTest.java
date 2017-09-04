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
    public void shouldParseEmptyProgram() throws Exception {
        parse("");
    }

    @Test
    public void testOnePrint() throws Exception {
        parse("10 print");
        parse("10 print \"Hello, world!\"");
        parse("10 print \"Hello, \";\"world!\"");
        parse("10 print \"One\",\"Two\",\"Three\"");
        parse("10 print 17");
        parse("10 print TRUE");
    }

    @Test
    public void testOneGoto() throws Exception {
        parse("10 goto 10");
        parse("10 goto 123456789");
    }

    @Test
    public void testOneAssignment() throws Exception {
        parse("10 let a = 5");
        parse("10 let abc123 = 123");
        parse("10 LET LIMIT% = 1");
        parse("10 LET NAME$ = \"Foo\"");
        parse("10 abc123 = 123");
        parse("10 MAX.FILES% = 50");
        parse("20 this.var = that.var");
        parse("20 s$ = t$");
        parse("20 bool = FALSE");
    }

    @Test
    public void testOneEnd() throws Exception {
        parse("10 end");
    }

    @Test
    public void testRem() throws Exception {
        parse("10 rem");
        parse("10 rem 1");
        parse("10 '");
        parse("10 'Comment");
        parse("10 print:rem");
        parse("10 goto 10 : rem Endless loop...");
    }

    @Test
    public void testTwoPrintsOneLine() throws Exception {
        parse("10 print : print");
        parse("10 print 1 : print 2");
        parse("10 print\"Hi\" : print \"there!\"");
        parse("10 print false : print true");
    }

    @Test
    public void testPrintExpressions() throws Exception {
        parse("10 print 1 + 2 + 3");
        parse("10 print 1 * (2 + 3)");
        parse("10 print (1-2)/(2-1)*(1+2)/(2+1)");
        parse("10 print ((1 + 2) - 3) * 4");
        parse("10 print name$; age%");
        parse("10 print 1 > 2; true and false");
        parse("10 print 1 > 2 or  1 < 2 and (0 = 0 or 0 <> 0)");
    }

    @Test
    public void testTwoPrintsTwoLines() throws Exception {
        parse("10 print" + EOL + "20 print");
        parse("10 print \"Hi\"" + EOL + "20 print \"there!\"");
    }

    @Test
    public void testLetAndPrintOneLine() throws Exception {
        parse("10 LET A$=\"foo\" : PRINT \"bar\"");
        parse("10 number = 5 : print");
        parse("10 value% = 17 : print \"value = \"; value%");
        parse("10 bool = 5 = 1 : print \"5 = 1: \"; bool");
    }

    @Test
    public void testPrintAndGotoOneLine() throws Exception {
        parse("10 print : goto 10");
        parse("10 print \"1\" : print \"2\" : print \"3\" : goto 10");
    }

    @Test
    public void testPrintAndGotoTwoLines() throws Exception {
        parse("10 print" + EOL + "20 goto 10");
        parse("10 print \"20\"" + EOL + "20 goto 10");
    }

    @Test
    public void testCapitalLetters() throws Exception {
        parse("10 PRINT \"CAPITAL\"");
        parse("10 PRINT" + EOL + "20 GOTO 10");
        parse("10 LET A% = 0" + EOL + "20 PRINT A%");
    }

    // Negative tests:
    
    @Test(expected = IllegalStateException.class)
    public void testMissingGotoLine() throws Exception {
        parse("10 goto");
    }

    @Test(expected = IllegalStateException.class)
    public void testGotoSymbol() throws Exception {
        parse("10 goto ?");
    }

    @Test(expected = IllegalStateException.class)
    public void testGotoWord() throws Exception {
        parse("10 goto ten");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingStatementAfterColon() throws Exception {
        parse("10 print :");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingExpressionInAssignment() throws Exception {
        parse("10 let value =");
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidVariableName() throws Exception {
        parse("10 let foo_bar = 17");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingQuotationMark() throws Exception {
        parse("10 print \"Unfinished string");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingConditionAfterAnd() throws Exception {
        parse("10 print 1 <> 0 and");
    }
}
