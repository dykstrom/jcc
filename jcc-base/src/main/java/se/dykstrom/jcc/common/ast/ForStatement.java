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

package se.dykstrom.jcc.common.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Represents a counted loop such as:
 *
 * <pre>
 * FOR i = 1 TO 10 STEP 2
 *   PRINT i
 * NEXT i
 * </pre>
 *
 * The step is null if the loop has no explicit step. An inclusive loop runs while
 * the counter has not passed the end, and an exclusive loop stops when the counter
 * reaches the end.
 *
 * @author Johan Dykstrom
 */
public class ForStatement extends AbstractNode implements BlockStatement {

    private final IdentifierNameExpression counter;
    private final Expression start;
    private final Expression end;
    private final Expression step;
    private final boolean inclusive;
    private final List<Statement> statements;

    public ForStatement(final int line,
                        final int column,
                        final IdentifierNameExpression counter,
                        final Expression start,
                        final Expression end,
                        final Expression step,
                        final boolean inclusive,
                        final List<Statement> statements) {
        super(line, column);
        this.counter = requireNonNull(counter);
        this.start = requireNonNull(start);
        this.end = requireNonNull(end);
        this.step = step;
        this.inclusive = inclusive;
        this.statements = new ArrayList<>(statements);
    }

    public ForStatement(final int line,
                        final int column,
                        final IdentifierNameExpression counter,
                        final Expression start,
                        final Expression end,
                        final Expression step,
                        final List<Statement> statements) {
        this(line, column, counter, start, end, step, true, statements);
    }

    public ForStatement(final IdentifierNameExpression counter,
                        final Expression start,
                        final Expression end,
                        final Expression step,
                        final List<Statement> statements) {
        this(0, 0, counter, start, end, step, true, statements);
    }

    @Override
    public String toString() {
        return "FOR " + counter + " = " + start + " TO " + end +
                (inclusive ? "" : " (exclusive)") +
                (step == null ? "" : " STEP " + step) +
                " " + statements.stream().map(Statement::toString).collect(joining(" : "));
    }

    public IdentifierNameExpression getCounter() {
        return counter;
    }

    public Expression getStart() {
        return start;
    }

    public Expression getEnd() {
        return end;
    }

    /**
     * Returns the step expression, or null if the loop has no explicit step.
     */
    public Expression getStep() {
        return step;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public List<List<Statement>> blocks() {
        return List.of(statements);
    }

    @Override
    public ForStatement withBlocks(final List<List<Statement>> blocks) {
        if (blocks.size() != 1) {
            throw new IllegalArgumentException("expected 1 block, found: " + blocks.size());
        }
        return withStatements(blocks.getFirst());
    }

    @Override
    public List<Expression> expressions() {
        return (step == null) ? List.of(counter, start, end) : List.of(counter, start, end, step);
    }

    public ForStatement withCounter(final IdentifierNameExpression counter) {
        return new ForStatement(line(), column(), counter, start, end, step, inclusive, statements);
    }

    public ForStatement withStart(final Expression start) {
        return new ForStatement(line(), column(), counter, start, end, step, inclusive, statements);
    }

    public ForStatement withEnd(final Expression end) {
        return new ForStatement(line(), column(), counter, start, end, step, inclusive, statements);
    }

    public ForStatement withStep(final Expression step) {
        return new ForStatement(line(), column(), counter, start, end, step, inclusive, statements);
    }

    public ForStatement withStatements(final List<Statement> statements) {
        return new ForStatement(line(), column(), counter, start, end, step, inclusive, statements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForStatement that = (ForStatement) o;
        return inclusive == that.inclusive &&
                Objects.equals(counter, that.counter) &&
                Objects.equals(start, that.start) &&
                Objects.equals(end, that.end) &&
                Objects.equals(step, that.step) &&
                Objects.equals(statements, that.statements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counter, start, end, step, inclusive, statements);
    }
}
