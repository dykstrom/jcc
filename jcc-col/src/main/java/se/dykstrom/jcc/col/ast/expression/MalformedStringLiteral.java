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

package se.dykstrom.jcc.col.ast.expression;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Marks a string literal that cannot be decoded, such as one containing an unknown escape.
 * The literal text is kept as written in the source, together with the reason it was rejected;
 * the error itself is reported during semantic analysis.
 *
 * @author Johan Dykstrom
 */
public class MalformedStringLiteral extends AbstractNode implements TypedExpression {

    private final String value;
    private final String reason;

    public MalformedStringLiteral(final int line, final int column, final String value, final String reason) {
        super(line, column);
        this.value = requireNonNull(value);
        this.reason = requireNonNull(reason);
    }

    public MalformedStringLiteral(final String value, final String reason) {
        this(0, 0, value, reason);
    }

    /**
     * A string literal would be of type string, were it well-formed.
     */
    @Override
    public Type type() {
        return Str.INSTANCE;
    }

    /**
     * Returns the malformed literal text as written in the source, quotes included.
     */
    public String value() {
        return value;
    }

    /**
     * Returns the reason this literal was rejected, phrased as a complete error message.
     */
    public String reason() {
        return reason;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var that = (MalformedStringLiteral) o;
        return Objects.equals(value, that.value) && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, reason);
    }
}
