/*
 * Copyright (C) 2024 Johan Dykstrom
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

import se.dykstrom.jcc.common.types.Bool;

/**
 * Represents a boolean literal such as 'true'.
 *
 * @author Johan Dykstrom
 */
public class BooleanLiteral extends AbstractLiteralExpression {

    public static final BooleanLiteral TRUE = new BooleanLiteral(0, 0, "-1");
    public static final BooleanLiteral FALSE = new BooleanLiteral(0, 0, "0");

    private BooleanLiteral(final int line, final int column, final String value) {
        super(line, column, value, Bool.INSTANCE);
    }

    public static BooleanLiteral from(final int line, final int column, final String text) {
        return switch (text) {
            case "true" -> new BooleanLiteral(line, column, TRUE.getValue());
            case "false" -> new BooleanLiteral(line, column, FALSE.getValue());
            default -> throw new IllegalStateException("Unexpected value: " + text);
        };
    }
}
