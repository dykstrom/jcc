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
    public static final LibraryFunction FUN_ATN = new LibraryFunction("atn", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "atan");
    public static final LibraryFunction FUN_COS = new LibraryFunction("cos", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "cos");
    public static final LibraryFunction FUN_EXP = new LibraryFunction("exp", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "exp");
    public static final LibraryFunction FUN_FABS = new LibraryFunction("abs", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "fabs");
    public static final LibraryFunction FUN_FMOD = new LibraryFunction("fmod", asList(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "fmod");
    public static final LibraryFunction FUN_LEN = new LibraryFunction("len", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, "strlen");
    public static final LibraryFunction FUN_LOG = new LibraryFunction("log", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "log");
    public static final LibraryFunction FUN_SIN = new LibraryFunction("sin", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "sin");
    public static final LibraryFunction FUN_SQR = new LibraryFunction("sqr", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "sqrt");
    public static final LibraryFunction FUN_TAN = new LibraryFunction("tan", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, "tan");
    public static final LibraryFunction FUN_VAL = new LibraryFunction("val", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, "_atoi64");

    public static final AssemblyFunction FUN_ASC    = new BasicAscFunction();
    public static final AssemblyFunction FUN_CDBL   = new BasicCdblFunction();
    public static final AssemblyFunction FUN_CINT   = new BasicCintFunction();
    public static final AssemblyFunction FUN_FIX    = new BasicFixFunction();
    public static final AssemblyFunction FUN_INSTR2 = new BasicInstr2Function();
    public static final AssemblyFunction FUN_INSTR3 = new BasicInstr3Function();
    public static final AssemblyFunction FUN_INT    = new BasicIntFunction();
    public static final AssemblyFunction FUN_SGN    = new BasicSgnFunction();

    private BasicBuiltInFunctions() { }
}
