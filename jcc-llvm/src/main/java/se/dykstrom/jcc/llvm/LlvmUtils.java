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

import java.util.List;

import static java.util.stream.Collectors.joining;

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
        return getCreateFormatIdentifier(List.of(type), symbolTable);
    }

    /**
     * Adds a printf format string to the symbol table for the given types,
     * and returns an identifier to identify the global variable that will
     * be the result.
     */
    public static Identifier getCreateFormatIdentifier(final List<Type> types, final SymbolTable symbolTable) {
        final var formatStr = types.stream().map(Type::getFormat).collect(joining()) + "\n\0";
        final var formatName = clean(".printf.fmt." + types.stream().map(Type::toString).collect(joining(".")));
        final var identifier = new Identifier("@" + formatName, Str.INSTANCE);
        if (!symbolTable.contains(identifier.name())) {
            symbolTable.addConstant(new Constant(identifier, formatStr));
        }
        return identifier;
    }

    private static String clean(final String s) {
        return s.replace("(", "lp.")
                .replace(")", ".rp.")
                .replace("->", "to.");
    }
}
