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

package se.dykstrom.jcc.tiny.compiler;

import org.antlr.v4.runtime.*;
import org.junit.Test;

import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

/**
 * Tests the class {@code TinyParser} that is generated from the Tiny grammar.
 *
 * @author Johan Dykstrom
 */
public class TinyParserTest {

    @Test
    public void testWrite() throws Exception {
        parse("BEGIN WRITE 17 END");
    }

    @Test
    public void testReadWrite() throws Exception {
        parse("BEGIN" + EOL + "READ n" + EOL + "WRITE n" + EOL + "END");
    }

    @Test
    public void testAssignment() throws Exception {
        parse("BEGIN" + EOL + "a := 0" + EOL + "END");
    }

    @Test
    public void testReadAssignWrite() throws Exception {
        parse("BEGIN" + EOL + "READ a" + EOL + "b := a + 1" + EOL + "WRITE b" + EOL + "END");
    }

    @Test
    public void testMultipleArgs() throws Exception {
        parse("BEGIN" + EOL + "READ a, b" + EOL + "c := a + b" + EOL + "WRITE a, b, c" + EOL + "END");
    }

    @Test
    public void testMultipleAssignments() throws Exception {
        parse("BEGIN" + EOL
                + "READ a" + EOL
                + "b := a + 1" + EOL
                + "c := b - 1" + EOL
                + "WRITE a, b, c" + EOL
                + "END");
    }

    @Test
    public void testNegativeNumber() throws Exception {
        parse("BEGIN" + EOL + "a := -3" + EOL + "WRITE a" + EOL + "END");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoStatement() throws Exception {
        parse("BEGIN END");
    }

    private void parse(String text) {
        TinyLexer lexer = new TinyLexer(new ANTLRInputStream(text));
        lexer.addErrorListener(ERROR_LISTENER);
        TinyParser parser = new TinyParser(new CommonTokenStream(lexer));
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
