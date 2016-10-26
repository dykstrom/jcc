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

import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents an expression that references an identifier. In this case we are interested in the value
 * stored in the memory location that is identified by the identifier. For example, this may be
 * "WRITE foo" in Tiny or "10 PRINT foo" in Basic.
 *
 * @author Johan Dykstrom
 */
public class IdentifierReferenceExpression extends IdentifierExpression {

    public IdentifierReferenceExpression(int line, int column, Identifier identifier) {
        super(line, column, identifier);
    }

    @Override
    public Type getType() {
        return getIdentifier().getType();
    }
}
