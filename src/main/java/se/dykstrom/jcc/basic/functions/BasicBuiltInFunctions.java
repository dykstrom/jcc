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

package se.dykstrom.jcc.basic.functions;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.compiler.CompilerUtils.LIB_LIBC;

import java.util.List;

import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

/**
 * Contains a number of built-in functions for the Basic language.
 *
 * @author Johan Dykstrom
 */
public class BasicBuiltInFunctions {

    public static final LibraryFunction FUN_ABS = createLibraryFunction("abs", singletonList(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, "_abs64");
    public static final LibraryFunction FUN_LEN = createLibraryFunction("len", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, "strlen");
    public static final LibraryFunction FUN_VAL = createLibraryFunction("val", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, "_atoi64");

    public static final AssemblyFunction FUN_ASC    = new BasicAscFunction();
    public static final AssemblyFunction FUN_INSTR2 = new BasicInstr2Function();
    public static final AssemblyFunction FUN_INSTR3 = new BasicInstr3Function();
    public static final AssemblyFunction FUN_SGN    = new BasicSgnFunction();

    public static final Identifier IDENT_FUN_ABS    = createIdentifier(FUN_ABS);
    public static final Identifier IDENT_FUN_ASC    = createIdentifier(FUN_ASC);
    public static final Identifier IDENT_FUN_INSTR2 = createIdentifier(FUN_INSTR2);
    public static final Identifier IDENT_FUN_INSTR3 = createIdentifier(FUN_INSTR3);
    public static final Identifier IDENT_FUN_LEN    = createIdentifier(FUN_LEN);
    public static final Identifier IDENT_FUN_SGN    = createIdentifier(FUN_SGN);
    public static final Identifier IDENT_FUN_VAL    = createIdentifier(FUN_VAL);

    private BasicBuiltInFunctions() { }

    /**
     * Creates an identifier form the given function. The identifier will have type {@link Fun}
     * parameterized with the argument and return types of {@code function}.
     * 
     * @param function The function to create an identifier for.
     * @return The created identifier.
     */
    private static Identifier createIdentifier(Function function) {
        return new Identifier(function.getName(), Fun.from(function.getArgTypes(), function.getReturnType()));
    }

    /**
     * Creates a new library function.
     * 
     * @param name The function name used in the symbol table.
     * @param args The function arguments.
     * @param returnType The function return type.
     * @param libraryFileName The file name of the library.
     * @param functionName The function name in the library.
     * @return The created library function.
     */
    private static LibraryFunction createLibraryFunction(String name, List<Type> args, Type returnType, String libraryFileName, String functionName) {
        return new LibraryFunction(name, args, returnType, MapUtils.of(libraryFileName, SetUtils.of(functionName)), functionName);
    }
}
