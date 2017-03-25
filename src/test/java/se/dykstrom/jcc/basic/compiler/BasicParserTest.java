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

import org.antlr.v4.runtime.*;
import org.junit.Test;

import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

public class BasicParserTest {

    @Test
    public void testOnePrint() throws Exception {
        parse("10 print");
        parse("10 print \"Hello, world!\"");
        parse("10 print \"Hello, \";\"world!\"");
        parse("10 print \"One\",\"Two\",\"Three\"");
        parse("10 print 17");
    }

    @Test
    public void testOneGoto() throws Exception {
        parse("10 goto 10");
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
    }

    @Test
    public void testPrintExpressions() throws Exception {
        parse("10 print 1 + 2 + 3");
        parse("10 print 1 * (2 + 3)");
        parse("10 print (1-2)/(2-1)*(1+2)/(2+1)");
        parse("10 print ((1 + 2) - 3) * 4");
    }

    @Test
    public void testTwoPrintsTwoLines() throws Exception {
        parse("10 print" + EOL + "20 print");
        parse("10 print \"Hi\"" + EOL + "20 print \"there!\"");
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
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingLineNumber() throws Exception {
        parse("goto 10");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingGotoLine() throws Exception {
        parse("10 goto");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingStatementAfterColon() throws Exception {
        parse("10 print :");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingQuotationMark() throws Exception {
        parse("10 print \"Unfinished string");
    }

    private void parse(String text) {
        BasicLexer lexer = new BasicLexer(new ANTLRInputStream(text));
        lexer.addErrorListener(ERROR_LISTENER);
        BasicParser parser = new BasicParser(new CommonTokenStream(lexer));
        parser.addErrorListener(ERROR_LISTENER);
        parser.program();
    }

    private static final BaseErrorListener ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalStateException("Syntax error at " + line + ":" + charPositionInLine + ": " + msg, e);
        }
    };
}
