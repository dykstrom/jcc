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

package se.dykstrom.jcc.llvm;

import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

public final class LlvmUtils {

    private LlvmUtils() { }

    public static LlvmOperator typeToOperator(final Type type,
                                              final LlvmOperator fOperator,
                                              final LlvmOperator iOperator) {
        if (type instanceof F32 || type instanceof F64) {
            return fOperator;
        } else if (type instanceof I32 || type instanceof I64) {
            return iOperator;
        } else {
            throw new IllegalArgumentException("unknown type: " + type.getName());
        }
    }

    /**
     * Adds a printf format string to the symbol table for the given type,
     * and returns an identifier to identify the global variable that will
     * be the result.
     */
    public static Identifier getCreateFormatIdentifier(final Type type, final SymbolTable symbolTable) {
        final var formatStr = type.getFormat() + "\n\0";
        final var formatName = clean(".printf.fmt." + type);
        final var identifier = new Identifier(formatName, Str.INSTANCE);
        if (!symbolTable.contains(formatName)) {
            final var globalIdentifier = new Identifier("@" + formatName, Str.INSTANCE);
            // A global string constant is represented by two entries in the symbol table.
            // The first links the identifier to the global "address" of the constant.
            symbolTable.addConstant(new Constant(identifier, globalIdentifier.name()));
            // The second links the global address to the actual string value.
            symbolTable.addConstant(new Constant(globalIdentifier, formatStr));
        }
        return identifier;
    }

    private static String clean(String s) {
        return s.replace("(", "lp.")
                .replace(")", ".rp.")
                .replace("->", "to.");
    }
}
