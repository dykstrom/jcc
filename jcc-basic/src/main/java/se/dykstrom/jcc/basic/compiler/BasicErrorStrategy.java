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
import org.antlr.v4.runtime.RuleContext;
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
 * <p>It also replaces ANTLR's token dump in the six cases where the parser has enough context to
 * name the mistake: a block left without its terminator, a terminator with no block open for it to
 * close, END used as a variable name, a statement a programmer expected to continue onto the next
 * line, an expression that runs off the end of its line, and a string literal whose closing quote
 * is missing.
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
     *
     * <p>The soft keywords are here because they are identifiers too, and so begin an expression
     * as readily as ID does. Keep them in step with the softKeyword rule in Basic.g4.
     */
    private static final Set<Integer> EXPRESSION_START_TOKENS = Set.of(
            BasicParser.BINNUMBER,
            BasicParser.FLOATNUMBER,
            BasicParser.HEXNUMBER,
            BasicParser.ID,
            BasicParser.MALFORMED_RADIXNUMBER,
            BasicParser.MINUS,
            BasicParser.NOT,
            BasicParser.NUMBER,
            BasicParser.OCTNUMBER,
            BasicParser.LPAREN,
            BasicParser.STRING,
            BasicParser.UNTERMINATED_STRING,
            BasicParser.CASE,
            BasicParser.CLOSE,
            BasicParser.COLOR,
            BasicParser.DATA,
            BasicParser.DO,
            BasicParser.ERASE,
            BasicParser.EXIT,
            BasicParser.FOR,
            BasicParser.FUNCTION,
            BasicParser.LOCATE,
            BasicParser.LOOP,
            BasicParser.NEXT,
            BasicParser.OPEN,
            BasicParser.READ,
            BasicParser.REDIM,
            BasicParser.RESTORE,
            BasicParser.SELECT,
            BasicParser.STEP,
            BasicParser.SUB,
            BasicParser.TO,
            BasicParser.TYPE,
            BasicParser.USING
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
        if (isOnASpokenForLine(recognizer, offendingToken)) {
            return true;
        }
        final int previousReportedLine = lastReportedLine;
        lastReportedLine = offendingToken.getLine();
        return reportUnterminatedString(recognizer, offendingToken, e)
                || reportContinuedStatement(recognizer, offendingToken, e)
                || reportOrphanTerminator(recognizer, offendingToken, e)
                || reportReservedWordAsVariable(recognizer, offendingToken, e)
                || reportUnterminatedBlock(recognizer, offendingToken, e, previousReportedLine)
                || reportExpressionRunOffLine(recognizer, offendingToken, e)
                || swallowedAtEndOfFile(recognizer, offendingToken, previousReportedLine);
    }

    /**
     * Returns whether the offending token is on a line nothing more can usefully be said about:
     * one that already carries an error, or one that holds the rest of an expression reported as
     * running off the line before it. One mistake per line is all a reader can act on, and
     * everything after the first error on a line is a guess about text the parser has already
     * lost track of.
     */
    private boolean isOnASpokenForLine(final Parser recognizer, final Token offendingToken) {
        if (offendingToken.getLine() != lastReportedLine && offendingToken.getLine() != continuationLine) {
            return false;
        }
        beginErrorCondition(recognizer);
        return true;
    }

    /**
     * Reports a string literal whose closing quote is missing, in the one place the grammar's own
     * alternative for it cannot reach: the prompt of a LINE INPUT, where the missing quote
     * swallows the separator the rule needs after the string. Returns {@code true} if it did
     * report.
     */
    private boolean reportUnterminatedString(final Parser recognizer,
                                             final Token offendingToken,
                                             final RecognitionException e) {
        if (offendingToken.getType() != BasicParser.UNTERMINATED_STRING) {
            return false;
        }
        final String message = "unterminated string literal; add the closing '\"'";
        beginErrorCondition(recognizer);
        recognizer.notifyErrorListeners(offendingToken, message, e);
        return true;
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
     * Reports a reserved word used as a variable name, in the two places the grammar's reservedWord
     * alternative cannot reach. Returns {@code true} if it did report.
     *
     * <p>Most reserved words are named by {@code BasicSyntaxVisitor}, from the alternative the
     * grammar gives an assignment and a DIM. Two cases are left over, and both are caught here.
     */
    private boolean reportReservedWordAsVariable(final Parser recognizer,
                                                 final Token offendingToken,
                                                 final RecognitionException e) {
        final Token keyword = reservedWordUsedAsVariable(recognizer, offendingToken);
        if (keyword == null) {
            return false;
        }
        final String message = "'" + keyword.getText() + "' is a reserved word and cannot be used as a variable name";
        beginErrorCondition(recognizer);
        recognizer.notifyErrorListeners(keyword, message, e);
        return true;
    }

    /**
     * Returns the reserved word the offending token shows was used as a variable name, or
     * {@code null} if it shows nothing of the kind.
     *
     * <p>END is found by the '=' after it. It cannot join the grammar's reservedWord rule, since
     * making it start a statement changes what the parser expects at a block boundary, where an
     * unterminated block and an orphaned terminator are diagnosed. No statement beginning with END
     * can be followed by '=', so the pair identifies the mistake on its own.
     *
     * <p>AS, BASE, INPUT and LINE are found wherever they turn up out of place. Each is a keyword
     * in one fixed position only — AS in a type clause, BASE after OPTION, LINE with the INPUT
     * after it — so anywhere else the programmer can only have meant a name. This is what reaches
     * a reserved word read as an operand, which the grammar cannot accept without letting an
     * unfinished expression swallow the keyword after it.
     */
    private static Token reservedWordUsedAsVariable(final Parser recognizer, final Token offendingToken) {
        if (offendingToken.getType() == BasicParser.EQ) {
            final int index = offendingToken.getTokenIndex() - 1;
            if (index < 0) {
                return null;
            }
            final Token previous = recognizer.getInputStream().get(index);
            return previous.getType() == BasicParser.END ? previous : null;
        }
        return isOutOfPlace(recognizer, offendingToken) ? offendingToken : null;
    }

    private static boolean isOutOfPlace(final Parser recognizer, final Token token) {
        return switch (token.getType()) {
            case BasicParser.AS, BasicParser.BASE, BasicParser.INPUT -> true;
            // LINE is a statement of its own, but only with the INPUT after it
            case BasicParser.LINE ->
                    tokenAfter(recognizer.getInputStream(), token.getTokenIndex()).getType() != BasicParser.INPUT;
            default -> false;
        };
    }

    /**
     * A block terminator with no opener: the token the message points at, the terminator's own
     * name, and the keyword that opens it.
     */
    private record OrphanTerminator(Token token, String name, String opener) {}

    /**
     * Reports the error as a block terminator with nothing open for it to close. Returns
     * {@code true} if it did report.
     *
     * <p>This runs before the unterminated-block check, because a terminator whose own opener is
     * not open describes the mistake better than the block the parser happens to be inside does. A
     * WEND in the body of a block IF that is properly terminated used to be reported as <em>IF
     * without matching END IF</em>, naming an END IF the reader can see is there.
     */
    private boolean reportOrphanTerminator(final Parser recognizer,
                                           final Token offendingToken,
                                           final RecognitionException e) {
        final OrphanTerminator orphan = orphanTerminator(recognizer, offendingToken);
        if (orphan == null) {
            return false;
        }
        final String message = orphan.name() + " without matching " + orphan.opener();
        beginErrorCondition(recognizer);
        recognizer.notifyErrorListeners(orphan.token(), message, e);
        return true;
    }

    /**
     * Returns the orphaned terminator the offending token belongs to, or {@code null} if the token
     * is not a terminator, or if the block it terminates is open.
     *
     * <p>END IF is found through its IF: END on its own is a statement, so the parser matches it
     * and then finds the IF unwanted. The message points at the END all the same.
     */
    private static OrphanTerminator orphanTerminator(final Parser recognizer, final Token offendingToken) {
        return switch (offendingToken.getType()) {
            case BasicParser.WEND -> isOpen(recognizer, BasicParser.WhileStmtContext.class)
                    ? null : new OrphanTerminator(offendingToken, "WEND", "WHILE");
            case BasicParser.ELSE -> isOpen(recognizer, BasicParser.IfThenBlockContext.class)
                    ? null : new OrphanTerminator(offendingToken, "ELSE", "IF");
            case BasicParser.ELSEIF -> isOpen(recognizer, BasicParser.IfThenBlockContext.class)
                    ? null : new OrphanTerminator(offendingToken, "ELSEIF", "IF");
            case BasicParser.IF -> orphanEndIf(recognizer, offendingToken);
            default -> null;
        };
    }

    private static OrphanTerminator orphanEndIf(final Parser recognizer, final Token offendingToken) {
        if (isOpen(recognizer, BasicParser.IfThenBlockContext.class)) {
            return null;
        }
        final int index = offendingToken.getTokenIndex() - 1;
        if (index < 0) {
            return null;
        }
        final Token end = recognizer.getInputStream().get(index);
        return end.getType() == BasicParser.END ? new OrphanTerminator(end, "END IF", "IF") : null;
    }

    /** Returns {@code true} if the parser is somewhere inside a context of the given block rule. */
    private static boolean isOpen(final Parser recognizer, final Class<? extends ParserRuleContext> blockRule) {
        for (RuleContext ctx = recognizer.getContext(); ctx != null; ctx = ctx.getParent()) {
            if (blockRule.isInstance(ctx)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reports the error as an unterminated block if the parser failed while matching the
     * structure of a WHILE or a block IF. Returns {@code true} if it did report.
     *
     * <p>The message points at the block's opening keyword rather than at the token the parser
     * failed on, which is usually the end of the file: the line that needs the terminator is the
     * one the reader has to edit, and a message at EOF has no source line to quote.
     */
    private boolean reportUnterminatedBlock(final Parser recognizer,
                                            final Token offendingToken,
                                            final RecognitionException e,
                                            final int previousReportedLine) {
        if (!BOUNDARY_TOKENS.contains(offendingToken.getType())) {
            return false;
        }
        final UnterminatedBlock block = unterminatedBlock(recognizer.getContext(), previousReportedLine);
        if (block == null) {
            return false;
        }
        beginErrorCondition(recognizer);
        recognizer.notifyErrorListeners(block.opener(), block.message(), e);
        return true;
    }

    /**
     * Returns the block the given context left open, or {@code null} if the context is not a
     * block whose terminator is missing. Only the innermost context is considered: an error
     * deeper inside the block body belongs to the statement that caused it.
     */
    private static UnterminatedBlock unterminatedBlock(final ParserRuleContext ctx, final int previousReportedLine) {
        return switch (ctx) {
            case BasicParser.IfThenBlockContext c -> unterminatedIf(c, previousReportedLine);
            case BasicParser.EndIfContext c -> unterminatedIf(c.getParent(), previousReportedLine);
            case BasicParser.WhileStmtContext c -> unterminatedWhile(c, previousReportedLine);
            case null, default -> null;
        };
    }

    private static UnterminatedBlock unterminatedIf(final ParserRuleContext ifThenBlockCtx,
                                                    final int previousReportedLine) {
        if (!(ifThenBlockCtx instanceof BasicParser.IfThenBlockContext)
                || isRecoveredFrom(ifThenBlockCtx, previousReportedLine)) {
            return null;
        }
        return new UnterminatedBlock(blockToBlame(ifThenBlockCtx).getStart(), "IF without matching END IF");
    }

    private static UnterminatedBlock unterminatedWhile(final BasicParser.WhileStmtContext whileStmtCtx,
                                                       final int previousReportedLine) {
        if (isRecoveredFrom(whileStmtCtx, previousReportedLine)) {
            return null;
        }
        return new UnterminatedBlock(blockToBlame(whileStmtCtx).getStart(), "WHILE without matching WEND");
    }

    /**
     * Returns the block whose terminator the programmer left out, which is not always the block
     * the parser found open. A nested block takes the first terminator it meets, so deleting the
     * inner WEND of two nested loops leaves the *outer* one open - and naming it points the reader
     * at a line that is fine.
     *
     * <p>Indentation is what tells the two apart: a block closed by a terminator that is indented
     * like the block around it, rather than like itself, was closed by that block's terminator.
     * The innermost such block is the one missing its own. Both conditions are required, so source
     * that is not indented keeps the block the parser found.
     */
    private static ParserRuleContext blockToBlame(final ParserRuleContext openBlock) {
        final ParserRuleContext inner = blockClosedByOuterTerminator(openBlock, openBlock);
        return (inner != null) ? inner : openBlock;
    }

    private static ParserRuleContext blockClosedByOuterTerminator(final ParserRuleContext ctx,
                                                                  final ParserRuleContext openBlock) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof ParserRuleContext child) {
                // Deepest first: the innermost block that took a terminator is the one to blame
                final ParserRuleContext deeper = blockClosedByOuterTerminator(child, openBlock);
                if (deeper != null) {
                    return deeper;
                }
                if (child.getClass() == openBlock.getClass() && tookTerminatorOf(child, openBlock)) {
                    return child;
                }
            }
        }
        return null;
    }

    /** Returns whether the given block is closed by a terminator indented like the block around it. */
    private static boolean tookTerminatorOf(final ParserRuleContext block, final ParserRuleContext openBlock) {
        final Token terminator = terminatorOf(block);
        if (terminator == null) {
            return false;
        }
        final int column = terminator.getCharPositionInLine();
        return column != block.getStart().getCharPositionInLine()
                && column == openBlock.getStart().getCharPositionInLine();
    }

    /** Returns the keyword that closes the given block, or {@code null} if it has none. */
    private static Token terminatorOf(final ParserRuleContext block) {
        return switch (block) {
            case BasicParser.WhileStmtContext c -> (c.WEND() != null) ? c.WEND().getSymbol() : null;
            case BasicParser.IfThenBlockContext c -> (c.endIf() != null && c.endIf().END() != null)
                    ? c.endIf().END().getSymbol() : null;
            case null, default -> null;
        };
    }

    /**
     * Swallows an error at the end of the file once something has been reported, and returns
     * {@code true} if it did. The file ending while the parser is still inside something is the
     * mistake already reported travelling outwards, and ANTLR's word for it is a token dump at
     * {@code <EOF>}, on a line the reader cannot act on. An unterminated block is reported before
     * this, so the messages worth having at the end of the file are not lost.
     */
    private boolean swallowedAtEndOfFile(final Parser recognizer,
                                         final Token offendingToken,
                                         final int previousReportedLine) {
        if (offendingToken.getType() != Token.EOF || previousReportedLine == 0) {
            return false;
        }
        beginErrorCondition(recognizer);
        return true;
    }

    /** A block left without its terminator: the keyword that opened it, and what to say about it. */
    private record UnterminatedBlock(Token opener, String message) { }

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
            if (token.getType() == BasicParser.LPAREN) {
                unclosed.push(token);
            } else if (token.getType() == BasicParser.RPAREN && !unclosed.isEmpty()) {
                unclosed.pop();
            }
        }
        return unclosed.peek();
    }

    private static String unfinishedExpressionMessage(final Token culprit) {
        if (culprit.getType() == BasicParser.LPAREN) {
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
                || tokenType == BasicParser.RPAREN
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
     * A block terminator is excluded unless it is orphaned: one that has an opener means a block
     * was left open, which {@link #reportUnterminatedBlock} says better. An orphaned one is junk,
     * and skipping its line keeps the enclosing block's own terminator matching further down. A
     * WEND in the body of a block IF used to make the IF rule fail on it, so that the IF's own
     * END IF was then reported as orphaned too.
     */
    private static boolean startsUnparsableLine(final Parser recognizer) {
        final TokenStream tokens = recognizer.getInputStream();
        final int type = tokens.LA(1);
        if (type == Token.EOF || type == BasicParser.NEWLINE) {
            return false;
        }
        if (BOUNDARY_TOKENS.contains(type) && orphanTerminator(recognizer, tokens.LT(1)) == null) {
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
