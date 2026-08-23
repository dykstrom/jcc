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

import java.util.ArrayDeque;
import java.util.Deque;
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
 * <p>It also replaces ANTLR's token dump in the three cases where the parser has enough context to
 * name the mistake: a block left without its terminator, a statement a programmer expected to
 * continue onto the next line, and an expression that runs off the end of its line.
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

    /**
     * Operators that need an operand after them. A line ending with one of these is an expression
     * that runs off the end of the line.
     */
    private static final Set<Integer> OPERATOR_TOKENS = Set.of(
            BasicParser.AND,
            BasicParser.BACKSLASH,
            BasicParser.CIRCUMFLEX,
            BasicParser.EQ,
            BasicParser.EQV,
            BasicParser.GE,
            BasicParser.GT,
            BasicParser.IMP,
            BasicParser.LE,
            BasicParser.LT,
            BasicParser.MINUS,
            BasicParser.MOD,
            BasicParser.NE,
            BasicParser.NOT,
            BasicParser.OR,
            BasicParser.PLUS,
            BasicParser.SLASH,
            BasicParser.STAR,
            BasicParser.XOR
    );

    /** The line the last error was reported on, or 0 before the first error. */
    private int lastReportedLine;

    /**
     * The line that carries the rest of an expression reported as running off the end of the line
     * before it, or 0 if there is no such line. Nothing on it is reported: it is the second half of
     * a mistake that has already been named.
     */
    private int continuationLine;

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
        if (offendingToken.getLine() == continuationLine) {
            beginErrorCondition(recognizer);
            return true;
        }
        final int previousReportedLine = lastReportedLine;
        lastReportedLine = offendingToken.getLine();
        return reportContinuedStatement(recognizer, offendingToken, e)
                || reportUnterminatedBlock(recognizer, offendingToken, e, previousReportedLine)
                || reportExpressionRunOffLine(recognizer, offendingToken, e);
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
     * Reports the error as an expression that runs off the end of its line, if the parser failed on
     * the line break itself and the line holds an expression it could not have finished reading.
     * Returns {@code true} if it did report.
     *
     * <p>BASIC has no implicit continuation: a line break ends the statement wherever it falls, and
     * only a trailing {@code '_'} joins two physical lines. A programmer who splits a long
     * expression the way most other languages allow gets nothing but the token set the parser wanted
     * next, which never mentions the underscore that would have made the program legal.
     */
    private boolean reportExpressionRunOffLine(final Parser recognizer,
                                               final Token offendingToken,
                                               final RecognitionException e) {
        if (offendingToken.getType() != BasicParser.NEWLINE && offendingToken.getType() != Token.EOF) {
            return false;
        }
        final TokenStream tokens = recognizer.getInputStream();
        final int end = offendingToken.getTokenIndex();
        final int start = startOfLine(tokens, end);
        if (start == end) {
            // The line holds no tokens, so there is no expression on it to have run off its end
            return false;
        }
        final Token culprit = unfinishedExpressionToken(tokens, start, end);
        if (culprit == null) {
            return false;
        }
        final Token continuation = continuationToken(tokens, end);
        final String suggestion = continuation == null ? ""
                : "; end the line with '_' to continue the statement onto the next line";
        beginErrorCondition(recognizer);
        recognizer.notifyErrorListeners(culprit, unfinishedExpressionMessage(culprit) + suggestion, e);
        if (continuation != null) {
            // The rest of the expression is the same mistake, and cannot be parsed on its own
            continuationLine = continuation.getLine();
        }
        return true;
    }

    /**
     * Returns the index of the first token on the offending token's line. Equal to the offending
     * token's own index if the line is empty, or if the offending token is the first in the file.
     */
    private static int startOfLine(final TokenStream tokens, final int offendingIndex) {
        int index = offendingIndex;
        while (index > 0 && tokens.get(index - 1).getType() != BasicParser.NEWLINE) {
            index--;
        }
        return index;
    }

    /**
     * Returns the token that leaves an expression on the given line unfinished: an operator with no
     * operand after it, or the innermost '(' that is never closed. Returns {@code null} if every
     * expression on the line is complete, in which case the parser failed on something else and
     * {@link DefaultErrorStrategy} has as much to say about it as we do.
     */
    private static Token unfinishedExpressionToken(final TokenStream tokens, final int start, final int end) {
        final Token lastToken = tokens.get(end - 1);
        if (OPERATOR_TOKENS.contains(lastToken.getType())) {
            return lastToken;
        }
        final Deque<Token> unclosed = new ArrayDeque<>();
        for (int index = start; index < end; index++) {
            final Token token = tokens.get(index);
            if (token.getType() == BasicParser.OPEN) {
                unclosed.push(token);
            } else if (token.getType() == BasicParser.CLOSE && !unclosed.isEmpty()) {
                unclosed.pop();
            }
        }
        return unclosed.peek();
    }

    private static String unfinishedExpressionMessage(final Token culprit) {
        if (culprit.getType() == BasicParser.OPEN) {
            return "'(' is not closed before the end of the line";
        }
        return "expression expected after '" + culprit.getText() + "'";
    }

    /**
     * Returns the first token of the line after the offending token's, if that line begins with
     * something the unfinished expression could have continued with, and {@code null} otherwise. A
     * line beginning with a statement keyword is a statement of its own, and there is no line at all
     * after the last one in the file.
     */
    private static Token continuationToken(final TokenStream tokens, final int offendingIndex) {
        if (tokens.get(offendingIndex).getType() == Token.EOF) {
            return null;
        }
        final Token token = tokenAfter(tokens, offendingIndex);
        return continuesExpression(token.getType()) ? token : null;
    }

    /**
     * Returns the token following the one at the given index. The parser has usually not reached it,
     * and {@link TokenStream#get} throws on a token the stream has not buffered yet, so it has to be
     * looked ahead to from the stream's own position instead.
     */
    private static Token tokenAfter(final TokenStream tokens, final int index) {
        final int lookahead = index + 2 - tokens.index();
        return lookahead > 0 ? tokens.LT(lookahead) : tokens.get(index + 1);
    }

    private static boolean continuesExpression(final int tokenType) {
        return EXPRESSION_START_TOKENS.contains(tokenType)
                || OPERATOR_TOKENS.contains(tokenType)
                || tokenType == BasicParser.CLOSE
                || tokenType == BasicParser.COMMA
                || tokenType == BasicParser.SEMICOLON;
    }

    /**
     * Returns the ';' or ',' that ends the line in front of the offending token's line, or
     * {@code null} if that line does not end with one, or if the offending token's line begins
     * with a statement keyword rather than with something an expression could continue with.
     */
    private static Token trailingSeparator(final Parser recognizer, final Token offendingToken) {
        final TokenStream tokens = recognizer.getInputStream();
        final int index = startOfLine(tokens, offendingToken.getTokenIndex());
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
