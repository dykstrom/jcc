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

import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.compiler.CompilerUtils.LIB_LIBC;

/**
 * Contains a number of built-in functions for the Basic language.
 *
 * @author Johan Dykstrom
 */
public final class BasicBuiltInFunctions {

    public static final LibraryFunction FUN_ABS = new LibraryFunction("abs", singletonList(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, "_abs64");
    public static final LibraryFunction FUN_FABS = new LibraryFunction("abs", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "fabs");
    public static final LibraryFunction FUN_FMOD = new LibraryFunction("fmod", asList(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "fmod");
    public static final LibraryFunction FUN_LEN = new LibraryFunction("len", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, "strlen");
    public static final LibraryFunction FUN_SIN = new LibraryFunction("sin", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "sin");
    public static final LibraryFunction FUN_VAL = new LibraryFunction("val", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, "_atoi64");

    public static final AssemblyFunction FUN_ASC    = new BasicAscFunction();
    public static final AssemblyFunction FUN_INSTR2 = new BasicInstr2Function();
    public static final AssemblyFunction FUN_INSTR3 = new BasicInstr3Function();
    public static final AssemblyFunction FUN_SGN    = new BasicSgnFunction();

    public static final Identifier IDENT_FUN_ABS    = FUN_ABS.getIdentifier();
    public static final Identifier IDENT_FUN_ASC    = FUN_ASC.getIdentifier();
    public static final Identifier IDENT_FUN_FABS   = FUN_FABS.getIdentifier();
    public static final Identifier IDENT_FUN_FMOD   = FUN_FMOD.getIdentifier();
    public static final Identifier IDENT_FUN_INSTR2 = FUN_INSTR2.getIdentifier();
    public static final Identifier IDENT_FUN_INSTR3 = FUN_INSTR3.getIdentifier();
    public static final Identifier IDENT_FUN_LEN    = FUN_LEN.getIdentifier();
    public static final Identifier IDENT_FUN_SGN    = FUN_SGN.getIdentifier();
    public static final Identifier IDENT_FUN_VAL    = FUN_VAL.getIdentifier();
    public static final Identifier IDENT_FUN_SIN    = FUN_SIN.getIdentifier();
    
    private BasicBuiltInFunctions() { }
}
