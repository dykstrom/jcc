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

import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents an expression that references the name of an identifier. In this case we are interested in
 * the identifier itself, or the address of the identifier. For example, this may be "READ foo" in Tiny.
 *
 * @author Johan Dykstrom
 */
public class IdentifierNameExpression extends IdentifierExpression implements TypedExpression {

    private IdentifierNameExpression(int line, int column, Identifier identifier) {
        super(line, column, identifier);
    }

    /**
     * Creates an {@link IdentifierNameExpression} from the given node and identifier.
     */
    public static IdentifierNameExpression from(Node node, Identifier identifier) {
        return new IdentifierNameExpression(node.getLine(), node.getColumn(), identifier);
    }

    @Override
    public Type getType() {
        return Str.INSTANCE;
    }
}
