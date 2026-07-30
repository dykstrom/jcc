/*
 * Copyright (C) 2026 Johan Dykstrom
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

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

import java.util.Set;

/**
 * An error strategy that replaces ANTLR's token dump with a message naming the block that
 * was left open, when a WHILE or a block IF is missing its terminator. Everything else,
 * including error recovery, is left to {@link DefaultErrorStrategy}.
 *
 * @author Johan Dykstrom
 */
public class BasicErrorStrategy extends DefaultErrorStrategy {

    /**
     * Tokens that can legitimately appear where a block terminator was expected. Requiring one
     * of these keeps an ordinary error inside a block body from being reported as a missing
     * terminator, since recovery from such an error can leave the parser in the block's context.
     */
    private static final Set<Integer> BOUNDARY_TOKENS = Set.of(
            Token.EOF,
            BasicParser.ELSE,
            BasicParser.ELSEIF,
            BasicParser.END,
            BasicParser.WEND
    );

    @Override
    public void reportError(final Parser recognizer, final RecognitionException e) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }
        if (!reportUnterminatedBlock(recognizer, e.getOffendingToken(), e)) {
            super.reportError(recognizer, e);
        }
    }

    @Override
    protected void reportMissingToken(final Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }
        if (!reportUnterminatedBlock(recognizer, recognizer.getCurrentToken(), new InputMismatchException(recognizer))) {
            super.reportMissingToken(recognizer);
        }
    }

    /**
     * This is the path a missing block terminator actually takes: {@code sync} sees a token that
     * cannot continue the block and reports it as unwanted, before {@code reportError} is reached.
     */
    @Override
    protected void reportUnwantedToken(final Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }
        if (!reportUnterminatedBlock(recognizer, recognizer.getCurrentToken(), new InputMismatchException(recognizer))) {
            super.reportUnwantedToken(recognizer);
        }
    }

    /**
     * Renders NEWLINE as "end of line" rather than as an escaped line break, so that an
     * expression running off the end of its line reads as the mistake it is.
     */
    @Override
    protected String getTokenErrorDisplay(final Token t) {
        if (t != null && t.getType() == BasicParser.NEWLINE) {
            return "'end of line'";
        }
        return super.getTokenErrorDisplay(t);
    }

    /**
     * Reports the error as an unterminated block if the parser failed while matching the
     * structure of a WHILE or a block IF. Returns {@code true} if it did report.
     */
    private boolean reportUnterminatedBlock(final Parser recognizer,
                                            final Token offendingToken,
                                            final RecognitionException e) {
        if (offendingToken == null || !BOUNDARY_TOKENS.contains(offendingToken.getType())) {
            return false;
        }
        final String message = unterminatedBlockMessage(recognizer.getContext());
        if (message == null) {
            return false;
        }
        beginErrorCondition(recognizer);
        recognizer.notifyErrorListeners(offendingToken, message, e);
        return true;
    }

    /**
     * Returns a message naming the block the given context left open, or {@code null} if the
     * context is not a block whose terminator is missing. Only the innermost context is
     * considered: an error deeper inside the block body belongs to the statement that caused it.
     */
    private static String unterminatedBlockMessage(final ParserRuleContext ctx) {
        return switch (ctx) {
            case BasicParser.IfThenBlockContext c -> unterminatedIf(c);
            case BasicParser.EndIfContext c -> unterminatedIf(c.getParent());
            case BasicParser.WhileStmtContext c -> unterminatedWhile(c);
            case null, default -> null;
        };
    }

    private static String unterminatedIf(final ParserRuleContext ifThenBlockCtx) {
        if (ifThenBlockCtx == null) {
            return null;
        }
        return "IF without matching END IF, IF at line " + ifThenBlockCtx.getStart().getLine();
    }

    private static String unterminatedWhile(final BasicParser.WhileStmtContext whileStmtCtx) {
        return "WHILE without matching WEND, WHILE at line " + whileStmtCtx.getStart().getLine();
    }
}
