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
import se.dykstrom.jcc.common.types.Void;
import se.dykstrom.jcc.common.types.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_JCCBASIC;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Contains a number of built-in functions for the Basic language.
 *
 * @author Johan Dykstrom
 */
public final class BasicBuiltInFunctions {

    public static final LibraryFunction FUN_ABS         = new LibraryFunction("abs", singletonList(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("_abs64"));
    public static final LibraryFunction FUN_ASC         = new LibraryFunction("asc", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("asc"));
    public static final LibraryFunction FUN_ATN         = new LibraryFunction("atn", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("atan"));
    public static final LibraryFunction FUN_CDBL        = new LibraryFunction("cdbl", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("cdbl"));
    public static final LibraryFunction FUN_CINT        = new LibraryFunction("cint", singletonList(F64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("cint"));
    public static final LibraryFunction FUN_COS         = new LibraryFunction("cos", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("cos"));
    public static final LibraryFunction FUN_CVD         = new LibraryFunction("cvd", singletonList(Str.INSTANCE), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("cvd"));
    public static final LibraryFunction FUN_CVI         = new LibraryFunction("cvi", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("cvi"));
    public static final LibraryFunction FUN_DATE        = new LibraryFunction("date$", emptyList(), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("date$"));
    public static final LibraryFunction FUN_EXP         = new LibraryFunction("exp", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("exp"));
    public static final LibraryFunction FUN_FABS        = new LibraryFunction("abs", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("fabs"));
    public static final LibraryFunction FUN_FIX         = new LibraryFunction("fix", singletonList(F64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("fix"));
    public static final LibraryFunction FUN_FMOD        = new LibraryFunction("fmod", asList(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("fmod"));
    public static final LibraryFunction FUN_HEX         = new LibraryFunction("hex$", singletonList(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("hex$"));
    public static final LibraryFunction FUN_INT         = new LibraryFunction("int", singletonList(F64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("int_F64"));
    public static final LibraryFunction FUN_LEN         = new LibraryFunction("len", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strlen"));
    public static final LibraryFunction FUN_LOG         = new LibraryFunction("log", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("log"));
    public static final LibraryFunction FUN_LTRIM       = new LibraryFunction("ltrim$", singletonList(Str.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("ltrim"));
    public static final LibraryFunction FUN_MKD         = new LibraryFunction("mkd$", singletonList(F64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("mkd$"));
    public static final LibraryFunction FUN_MKI         = new LibraryFunction("mki$", singletonList(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("mki$"));
    public static final LibraryFunction FUN_LBOUND      = new LibraryFunction("lbound", singletonList(Arr.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("lbound"));
    public static final LibraryFunction FUN_LBOUND_I64  = new LibraryFunction("lbound", asList(Arr.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("lbound_I64"));
    public static final LibraryFunction FUN_OCT         = new LibraryFunction("oct$", singletonList(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("oct$"));
    public static final LibraryFunction FUN_RANDOMIZE   = new LibraryFunction("randomize", singletonList(F64.INSTANCE), Void.INSTANCE, LIB_JCCBASIC, new ExternalFunction("randomize"));
    public static final LibraryFunction FUN_UBOUND      = new LibraryFunction("ubound", singletonList(Arr.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("ubound"));
    public static final LibraryFunction FUN_UBOUND_I64  = new LibraryFunction("ubound", asList(Arr.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("ubound_I64"));
    public static final LibraryFunction FUN_RND         = new LibraryFunction("rnd", emptyList(), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("rnd"));
    public static final LibraryFunction FUN_RND_F64     = new LibraryFunction("rnd", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("rnd_F64"));
    public static final LibraryFunction FUN_RTRIM       = new LibraryFunction("rtrim$", singletonList(Str.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("rtrim"));
    public static final LibraryFunction FUN_OPTION_BASE = new LibraryFunction("option_base", singletonList(I64.INSTANCE), Void.INSTANCE, LIB_JCCBASIC, new ExternalFunction("option_base"));
    public static final LibraryFunction FUN_SGN         = new LibraryFunction("sgn", singletonList(F64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("sgn"));
    public static final LibraryFunction FUN_SIN         = new LibraryFunction("sin", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("sin"));
    public static final LibraryFunction FUN_SQR         = new LibraryFunction("sqr", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("sqrt"));
    public static final LibraryFunction FUN_STR_F64     = new LibraryFunction("str$", singletonList(F64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("str_F64"));
    public static final LibraryFunction FUN_STR_I64     = new LibraryFunction("str$", singletonList(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("str_I64"));
    public static final LibraryFunction FUN_TAN         = new LibraryFunction("tan", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("tan"));
    public static final LibraryFunction FUN_TIME        = new LibraryFunction("time$", emptyList(), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("time$"));
    public static final LibraryFunction FUN_TIMER       = new LibraryFunction("timer", emptyList(), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("timer"));
    public static final LibraryFunction FUN_VAL         = new LibraryFunction("val", singletonList(Str.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("atof"));

    public static final AssemblyFunction FUN_CHR        = new BasicChrFunction();
    public static final AssemblyFunction FUN_INSTR2     = new BasicInstr2Function();
    public static final AssemblyFunction FUN_INSTR3     = new BasicInstr3Function();
    public static final AssemblyFunction FUN_LCASE      = new BasicLcaseFunction();
    public static final AssemblyFunction FUN_LEFT       = new BasicLeftFunction();
    public static final AssemblyFunction FUN_MID3       = new BasicMid3Function();
    public static final AssemblyFunction FUN_RIGHT      = new BasicRightFunction();
    public static final AssemblyFunction FUN_MID2       = new BasicMid2Function(); // Depends on FUN_RIGHT
    public static final AssemblyFunction FUN_SPACE      = new BasicSpaceFunction();
    public static final AssemblyFunction FUN_STRING_I64 = new BasicStringIntFunction();
    public static final AssemblyFunction FUN_STRING_STR = new BasicStringStrFunction();
    public static final AssemblyFunction FUN_UCASE      = new BasicUcaseFunction();

    private BasicBuiltInFunctions() { }
}
