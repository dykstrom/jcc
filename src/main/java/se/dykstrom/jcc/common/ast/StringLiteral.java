/*
 * Copyright (C) 2016 Johan Dykstrom
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

import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

/**
 * Represents a literal string such as '"foo"'.
 * 
 * @author Johan Dykstrom
 */
public class StringLiteral extends Expression implements LiteralExpression {

    private final String value;

    public StringLiteral(int line, int column, String value) {
        super(line, column);
        this.value = value;
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }

    @Override
    public Type getType() {
        return Str.INSTANCE;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Creates a {@link StringLiteral} from the given node and string.
     */
    public static StringLiteral from(Node node, String value) {
        return new StringLiteral(node.getLine(), node.getColumn(), value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringLiteral that = (StringLiteral) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
