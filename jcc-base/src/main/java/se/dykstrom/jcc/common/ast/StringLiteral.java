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

/**
 * Represents a literal string such as '"foo"'.
 * 
 * @author Johan Dykstrom
 */
public class StringLiteral extends AbstractLiteralExpression {

    public StringLiteral(int line, int column, String value) {
        super(line, column, value, Str.INSTANCE);
    }

    @Override
    public String toString() {
        return "\"" + getValue() + "\"";
    }

    /**
     * Creates a {@link StringLiteral} from the given node and string.
     */
    public static StringLiteral from(Node node, String value) {
        return new StringLiteral(node.line(), node.column(), value);
    }
}
