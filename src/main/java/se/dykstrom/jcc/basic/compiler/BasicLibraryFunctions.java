/*
 * Copyright (C) 2017 Johan Dykstrom
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

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.compiler.CompilerUtils.LIB_LIBC;

import java.util.List;

import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

/**
 * Contains a number of pre-defined library functions for the Basic language.
 *
 * @author Johan Dykstrom
 */
public class BasicLibraryFunctions {

    public static final Identifier IDENT_FUN_ABS = new Identifier("abs", Fun.from(I64.INSTANCE));
    public static final Identifier IDENT_FUN_LEN = new Identifier("len", Fun.from(I64.INSTANCE));

    public static final LibraryFunction FUN_ABS = createFunction(IDENT_FUN_ABS, singletonList(I64.INSTANCE), LIB_LIBC, "_abs64");
    public static final LibraryFunction FUN_LEN = createFunction(IDENT_FUN_LEN, singletonList(Str.INSTANCE), LIB_LIBC, "strlen");
    
    private BasicLibraryFunctions() { }

    /**
     * Creates a new library function.
     * 
     * @param identifier The function identifier in the symbol table.
     * @param args The functions arguments.
     * @param libraryFileName The file name of the library.
     * @param functionName The function name in the library.
     * @return The created library function.
     */
    private static LibraryFunction createFunction(Identifier identifier, List<Type> args, String libraryFileName, String functionName) {
        return new LibraryFunction(
                identifier.getName(), 
                ((Fun) identifier.getType()).getReturnType(), 
                args, 
                MapUtils.of(libraryFileName, SetUtils.of(functionName)), 
                functionName
        );
    }
}
