/*
 * Copyright (C) 2021 Johan Dykstrom
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

package se.dykstrom.jcc.basic.code.expression;

import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.code.expression.IdentifierDerefCodeGenerator;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.List;

/**
 * Basic extension of {@link IdentifierDerefCodeGenerator} that also adds unknown identifiers to the symbol table.
 */
public class BasicIdentifierDerefCodeGenerator extends IdentifierDerefCodeGenerator {

    public BasicIdentifierDerefCodeGenerator(Context context) { super(context); }

    @Override
    public List<Line> generate(IdentifierDerefExpression expression, StorageLocation location) {
        Identifier identifier = expression.getIdentifier();
        // If the identifier is undefined, add it to the symbol table now
        if (!symbols.contains(identifier.name())) {
            symbols.addVariable(identifier);
        }
        return super.generate(expression, location);
    }
}
