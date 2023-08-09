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

package se.dykstrom.jcc.assembunny.compiler;

import org.antlr.v4.runtime.*;
import org.junit.Test;
import se.dykstrom.jcc.antlr4.Antlr4Utils;

public class AssembunnyParserTest {

    @Test
    public void shouldParseEmptyProgram() {
        parse("");
    }

    @Test
    public void shouldParseSingleStatement() {
        parse("inc a");
        parse("dec b");
        parse("cpy a b");
        parse("cpy 1 c");
        parse("jnz d 0");
        parse("jnz a -1");
        parse("outn a");
    }

    @Test
    public void shouldParseMultipleStatements() {
        parse("inc a dec a cpy c d jnz b -1 outn b");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotParseMissingRegister() {
        parse("inc");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotParseInvalidRegister() {
        parse("inc e");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotParseCopyToInteger() {
        parse("cpy a 7");
    }

    /**
     * Parses the given program text.
     */
    private void parse(String text) {
        AssembunnyLexer lexer = new AssembunnyLexer(CharStreams.fromString(text));
        lexer.addErrorListener(ERROR_LISTENER);
        AssembunnyParser parser = new AssembunnyParser(new CommonTokenStream(lexer));
        parser.addErrorListener(ERROR_LISTENER);
        parser.program();
        Antlr4Utils.checkParsingComplete(parser);
    }

    private static final BaseErrorListener ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalStateException("Syntax error at " + line + ":" + charPositionInLine + ": " + msg, e);
        }
    };
}
