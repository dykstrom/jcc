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
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.IntervalSet;

import java.util.Set;

/**
 * An error strategy that adapts {@link DefaultErrorStrategy} to a line oriented language.
 *
 * <p>A statement ends at the end of its line, which the default strategy knows nothing about. Left
 * to itself it resumes at whatever token happens to be in the follow set, often in the middle of
 * the line it failed on, and each following line of the enclosing block then fails in turn. This
 * strategy resynchronizes on the statement terminator instead, and reports at most one error per
 * line, so one mistake produces one message.
 *
 * <p>It also replaces ANTLR's token dump in the two cases where the parser has enough context to
 * name the mistake: a block left without its terminator, and a statement a programmer expected to
 * continue onto the next line.
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

    /**
     * Tokens that can begin an expression. A line starting with one of these is the shape a
     * statement wrongly continued onto the next line takes; a line starting with a statement
     * keyword is a statement of its own, however badly the line before it ended.
     */
    private static final Set<Integer> EXPRESSION_START_TOKENS = Set.of(
            BasicParser.BINNUMBER,
            BasicParser.FLOATNUMBER,
            BasicParser.HEXNUMBER,
            BasicParser.ID,
            BasicParser.MINUS,
            BasicParser.NOT,
            BasicParser.NUMBER,
            BasicParser.OCTNUMBER,
            BasicParser.OPEN,
            BasicParser.STRING
    );

    /** The line the last error was reported on, or 0 before the first error. */
    private int lastReportedLine;

    // -----------------------------------------------------------------------------------------
    // Reporting:
    // -----------------------------------------------------------------------------------------

    @Override
    public void reportError(final Parser recognizer, final RecognitionException e) {
        if (!reported(recognizer, e.getOffendingToken(), e)) {
            super.reportError(recognizer, e);
        }
    }

    @Override
    protected void reportMissingToken(final Parser recognizer) {
        if (!reported(recognizer, recognizer.getCurrentToken(), new InputMismatchException(recognizer))) {
            super.reportMissingToken(recognizer);
        }
    }

    /**
     * This is the path a missing block terminator actually takes: {@code sync} sees a token that
     * cannot continue the block and reports it as unwanted, before {@code reportError} is reached.
     */
    @Override
    protected void reportUnwantedToken(final Parser recognizer) {
        if (!reported(recognizer, recognizer.getCurrentToken(), new InputMismatchException(recognizer))) {
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
     * Reports the error, and returns {@code true}, when this strategy has something better to say
     * than {@link DefaultErrorStrategy} — or nothing to say at all, because the line already
     * carries an error. Returns {@code false} to let the default strategy report.
     */
    private boolean reported(final Parser recognizer,
                             final Token offendingToken,
                             final RecognitionException e) {
        if (inErrorRecoveryMode(recognizer)) {
            return true;
        }
        if (offendingToken == null) {
            return false;
        }
        if (offendingToken.getLine() == lastReportedLine) {
            // One mistake per line is all a reader can act on, and everything after the first
            // error on a line is a guess about text the parser has already lost track of.
            beginErrorCondition(recognizer);
            return true;
        }
        final int previousReportedLine = lastReportedLine;
        lastReportedLine = offendingToken.getLine();
        return reportContinuedStatement(recognizer, offendingToken, e)
                || reportUnterminatedBlock(recognizer, offendingToken, e, previousReportedLine);
    }

    /**
     * Reports the error as a statement wrongly continued onto the next line, if the line before
     * the offending token's ends with a print separator. Returns {@code true} if it did report.
     */
    private boolean reportContinuedStatement(final Parser recognizer,
                                             final Token offendingToken,
                                             final RecognitionException e) {
        final Token separator = trailingSeparator(recognizer, offendingToken);
        if (separator == null) {
            return false;
        }
        final String message = "'" + separator.getText() + "' at the end of a line does not continue the statement "
                + "onto the next line; end the line with '_' to continue it";
        beginErrorCondition(recognizer);
        recognizer.notifyErrorListeners(separator, message, e);
        return true;
    }

    /**
     * Reports the error as an unterminated block if the parser failed while matching the
     * structure of a WHILE or a block IF. Returns {@code true} if it did report.
     */
    private boolean reportUnterminatedBlock(final Parser recognizer,
                                            final Token offendingToken,
                                            final RecognitionException e,
                                            final int previousReportedLine) {
        if (!BOUNDARY_TOKENS.contains(offendingToken.getType())) {
            return false;
        }
        final String message = unterminatedBlockMessage(recognizer.getContext(), previousReportedLine);
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
    private static String unterminatedBlockMessage(final ParserRuleContext ctx, final int previousReportedLine) {
        return switch (ctx) {
            case BasicParser.IfThenBlockContext c -> unterminatedIf(c, previousReportedLine);
            case BasicParser.EndIfContext c -> unterminatedIf(c.getParent(), previousReportedLine);
            case BasicParser.WhileStmtContext c -> unterminatedWhile(c, previousReportedLine);
            case null, default -> null;
        };
    }

    private static String unterminatedIf(final ParserRuleContext ifThenBlockCtx, final int previousReportedLine) {
        if (ifThenBlockCtx == null || isRecoveredFrom(ifThenBlockCtx, previousReportedLine)) {
            return null;
        }
        return "IF without matching END IF, IF at line " + ifThenBlockCtx.getStart().getLine();
    }

    private static String unterminatedWhile(final BasicParser.WhileStmtContext whileStmtCtx,
                                            final int previousReportedLine) {
        if (isRecoveredFrom(whileStmtCtx, previousReportedLine)) {
            return null;
        }
        return "WHILE without matching WEND, WHILE at line " + whileStmtCtx.getStart().getLine();
    }

    /**
     * Returns {@code true} if an error has already been reported inside the given block's body, in
     * which case the parser is here because it recovered from that error rather than because the
     * block is unterminated. The terminator may well be present further down, so claiming it is
     * missing would name a line the reader can see is fine. An error on the block's opening line
     * does not count: that line is the header, not the body, and its block still needs terminating.
     */
    private static boolean isRecoveredFrom(final ParserRuleContext blockCtx, final int previousReportedLine) {
        return previousReportedLine > blockCtx.getStart().getLine();
    }

    /**
     * Returns the ';' or ',' that ends the line in front of the offending token's line, or
     * {@code null} if that line does not end with one, or if the offending token's line begins
     * with a statement keyword rather than with something an expression could continue with.
     */
    private static Token trailingSeparator(final Parser recognizer, final Token offendingToken) {
        final TokenStream tokens = recognizer.getInputStream();
        int index = offendingToken.getTokenIndex();
        if (index < 0) {
            return null;
        }
        while (index > 0 && tokens.get(index - 1).getType() != BasicParser.NEWLINE) {
            index--;
        }
        // Below the line break in front of the offending line there must be room for a separator
        if (index < 2) {
            return null;
        }
        if (!EXPRESSION_START_TOKENS.contains(tokens.get(index).getType())) {
            return null;
        }
        final Token separator = tokens.get(index - 2);
        if (separator.getType() != BasicParser.SEMICOLON && separator.getType() != BasicParser.COMMA) {
            return null;
        }
        return separator;
    }

    // -----------------------------------------------------------------------------------------
    // Recovery:
    // -----------------------------------------------------------------------------------------

    /**
     * Skips the whole line rather than deleting a single token, when the parser is between the
     * statements of a block and the line ahead cannot be one. Deleting one token would leave the
     * rest of the line to be parsed as if it were a statement, which is how one mistake grew into
     * an error on every following line of the block.
     */
    @Override
    public void sync(final Parser recognizer) throws RecognitionException {
        if (isBlockBody(recognizer.getContext()) && startsUnparsableLine(recognizer)) {
            reportUnwantedToken(recognizer);
            consumeRestOfLine(recognizer);
            consumeTerminator(recognizer);
            return;
        }
        super.sync(recognizer);
    }

    /**
     * Resynchronizes on the statement terminator: the rest of a line the parser could not make
     * sense of is junk, and the next line is the only sound place to resume.
     */
    @Override
    public void recover(final Parser recognizer, final RecognitionException e) {
        ensureProgress(recognizer);
        consumeRestOfLine(recognizer);
        if (isBlockBody(recognizer.getContext())) {
            // The parser failed between the statements of a block, so no line rule is left to
            // match the terminator. Leaving it would make the block's own rule fail on it next,
            // abandoning the block and every statement still to come in it.
            consumeTerminator(recognizer);
        }
    }

    /**
     * Returns {@code true} if the given context is a rule whose body is a run of lines. Recovery
     * inside one of these resumes at the start of the next line.
     */
    private static boolean isBlockBody(final ParserRuleContext ctx) {
        return switch (ctx) {
            case BasicParser.ProgramContext ignored -> true;
            case BasicParser.IfThenBlockContext ignored -> true;
            case BasicParser.ElseIfBlockContext ignored -> true;
            case BasicParser.ElseBlockContext ignored -> true;
            case BasicParser.WhileStmtContext ignored -> true;
            case null, default -> false;
        };
    }

    /**
     * Returns {@code true} if the parser is at the start of a line that cannot be parsed at all.
     * A block terminator is excluded: one of those in an unexpected place means a block was left
     * open, which {@link #reportUnterminatedBlock} says better.
     */
    private static boolean startsUnparsableLine(final Parser recognizer) {
        final TokenStream tokens = recognizer.getInputStream();
        final int type = tokens.LA(1);
        if (type == Token.EOF || type == BasicParser.NEWLINE || BOUNDARY_TOKENS.contains(type)) {
            return false;
        }
        final int index = tokens.index();
        if (index > 0 && tokens.get(index - 1).getType() != BasicParser.NEWLINE) {
            return false;
        }
        return !recognizer.getExpectedTokens().contains(type);
    }

    private static void consumeRestOfLine(final Parser recognizer) {
        final TokenStream tokens = recognizer.getInputStream();
        while (tokens.LA(1) != BasicParser.NEWLINE && tokens.LA(1) != Token.EOF) {
            recognizer.consume();
        }
    }

    private static void consumeTerminator(final Parser recognizer) {
        if (recognizer.getInputStream().LA(1) == BasicParser.NEWLINE) {
            recognizer.consume();
        }
    }

    /**
     * Consumes a token if recovery has already been attempted at this token in this parser state,
     * as {@link DefaultErrorStrategy#recover} does. Without it a rule that fails on the terminator
     * consumes nothing, and the parser can loop forever.
     */
    private void ensureProgress(final Parser recognizer) {
        final TokenStream tokens = recognizer.getInputStream();
        if (lastErrorIndex == tokens.index()
                && lastErrorStates != null
                && lastErrorStates.contains(recognizer.getState())) {
            recognizer.consume();
        }
        lastErrorIndex = tokens.index();
        if (lastErrorStates == null) {
            lastErrorStates = new IntervalSet();
        }
        lastErrorStates.add(recognizer.getState());
    }
}
