/*
 * Copyright (C) 2019 Johan Dykstrom
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

import se.dykstrom.jcc.basic.ast.LineInputStatement;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

/**
 * Contains function to help with type management.
 */
public final class BasicTypeHelper {

    private BasicTypeHelper() { }

    /**
     * Returns a copy of the given statement, with the types updated.
     */
    public static LineInputStatement updateTypes(LineInputStatement statement, SymbolTable symbols, BasicTypeManager types) {
        Identifier identifier = updateType(statement.identifier(), symbols, types);
        return statement.withIdentifier(identifier);
    }

    private static Identifier updateType(Identifier identifier, SymbolTable symbols, BasicTypeManager types) {
        String name = identifier.getName();

        // If the identifier was already defined, use the old definition
        if (symbols.contains(name)) {
            identifier = symbols.getIdentifier(name);
        }

        // If the identifier has no type, look it up using type manager
        Type type = identifier.getType();
        if (type instanceof Unknown) {
            type = types.getTypeByName(name);
        }

        // Return updated identifier with possibly new type
        return identifier.withType(type);
    }
}
