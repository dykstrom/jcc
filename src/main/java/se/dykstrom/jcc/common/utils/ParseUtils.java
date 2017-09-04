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

package se.dykstrom.jcc.common.utils;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;

/**
 * Contains static utility methods related to parsing.
 *
 * @author Johan Dykstrom
 */
public final class ParseUtils {

    private ParseUtils() { }

    /**
     * Checks that the parsing has completed, and that the next token is EOF.
     * If this is not the case, a syntax error is generated.
     */
    public static void checkParsingComplete(Parser parser) {
        if (parser.getCurrentToken().getType() != Token.EOF) {
            parser.notifyErrorListeners("Syntax error at EOF.");
        }
    }
}
