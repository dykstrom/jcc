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

package se.dykstrom.jcc.antlr4;

import org.antlr.v4.runtime.*;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.JccException;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Contains static utility methods related to ANTLR4.
 *
 * @author Johan Dykstrom
 */
public final class Antlr4Utils {

    private Antlr4Utils() { }

    /**
     * Checks that the parsing has completed, and that the next token is EOF.
     * If this is not the case, a syntax error is generated.
     */
    public static void checkParsingComplete(final Parser parser) {
        if (parser.getCurrentToken().getType() != Token.EOF) {
            parser.notifyErrorListeners("Syntax error at EOF.");
        }
    }

    /**
     * Converts an InputStream to an ANTLR4 specific CharStream.
     */
    public static CharStream toCharStream(final InputStream inputStream) {
        try {
            return CharStreams.fromStream(inputStream, UTF_8);
        } catch (IOException e) {
            throw new JccException("Cannot read source file");
        }
    }

    /**
     * Returns a view of the given CompilationErrorListener
     * in the form of an ANTLR4 specific BaseErrorListener.
     */
    public static BaseErrorListener asBaseErrorListener(final CompilationErrorListener compilationErrorListener) {
        return new BaseErrorListener() {
            @Override
            public void syntaxError(final Recognizer<?, ?> recognizer,
                                    final Object offendingSymbol,
                                    final int line,
                                    final int charPositionInLine,
                                    final String msg,
                                    final RecognitionException exception) {
                compilationErrorListener.syntaxError(line, charPositionInLine, msg, exception);
            }
        };
    }
}
