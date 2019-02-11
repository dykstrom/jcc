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
import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Contains a number of built-in functions for the Basic language.
 *
 * @author Johan Dykstrom
 */
public final class BasicBuiltInFunctions {

    public static final LibraryFunction FUN_ABS  = new LibraryFunction("abs", singletonList(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("_abs64"));
    public static final LibraryFunction FUN_ATN  = new LibraryFunction("atn", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("atan"));
    public static final LibraryFunction FUN_COS  = new LibraryFunction("cos", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("cos"));
    public static final LibraryFunction FUN_EXP  = new LibraryFunction("exp", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("exp"));
    public static final LibraryFunction FUN_FABS = new LibraryFunction("abs", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("fabs"));
    public static final LibraryFunction FUN_FMOD = new LibraryFunction("fmod", asList(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("fmod"));
    public static final LibraryFunction FUN_LEN  = new LibraryFunction("len", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strlen"));
    public static final LibraryFunction FUN_LOG  = new LibraryFunction("log", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("log"));
    public static final LibraryFunction FUN_SIN  = new LibraryFunction("sin", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("sin"));
    public static final LibraryFunction FUN_SQR  = new LibraryFunction("sqr", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("sqrt"));
    public static final LibraryFunction FUN_TAN  = new LibraryFunction("tan", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("tan"));
    public static final LibraryFunction FUN_VAL  = new LibraryFunction("val", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("_atoi64"));

    public static final AssemblyFunction FUN_ASC    = new BasicAscFunction();
    public static final AssemblyFunction FUN_CDBL   = new BasicCdblFunction();
    public static final AssemblyFunction FUN_CINT   = new BasicCintFunction();
    public static final AssemblyFunction FUN_CHR    = new BasicChrFunction();
    public static final AssemblyFunction FUN_FIX    = new BasicFixFunction();
    public static final AssemblyFunction FUN_HEX    = new BasicHexFunction();
    public static final AssemblyFunction FUN_INSTR2 = new BasicInstr2Function();
    public static final AssemblyFunction FUN_INSTR3 = new BasicInstr3Function();
    public static final AssemblyFunction FUN_INT    = new BasicIntFunction();
    public static final AssemblyFunction FUN_LCASE  = new BasicLcaseFunction();
    public static final AssemblyFunction FUN_OCT    = new BasicOctFunction();
    public static final AssemblyFunction FUN_SGN    = new BasicSgnFunction();
    public static final AssemblyFunction FUN_SPACE  = new BasicSpaceFunction();
    public static final AssemblyFunction FUN_UCASE  = new BasicUcaseFunction();

    private BasicBuiltInFunctions() { }
}
