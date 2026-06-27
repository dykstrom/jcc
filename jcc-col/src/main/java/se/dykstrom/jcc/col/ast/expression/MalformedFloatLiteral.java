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
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Marks a float literal written with a decimal point on only one side, such as '.99' or '17.'.
 * COL requires digits on both sides of the point; this is rejected during semantic analysis.
 *
 * @author Johan Dykstrom
 */
public class MalformedFloatLiteral extends AbstractNode implements TypedExpression {

    private final String value;

    public MalformedFloatLiteral(final int line, final int column, final String value) {
        super(line, column);
        this.value = requireNonNull(value);
    }

    public MalformedFloatLiteral(final String value) {
        this(0, 0, value);
    }

    /**
     * A float literal would be of type f64, were it well-formed.
     */
    @Override
    public Type type() {
        return F64.INSTANCE;
    }

    /**
     * Returns the malformed literal text as written in the source.
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MalformedFloatLiteral that = (MalformedFloatLiteral) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
