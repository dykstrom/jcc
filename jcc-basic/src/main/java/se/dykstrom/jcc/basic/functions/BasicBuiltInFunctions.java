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

import java.util.List;

import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_JCCBASIC;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Contains a number of built-in functions for the BASIC language.
 *
 * @author Johan Dykstrom
 */
public final class BasicBuiltInFunctions {

    public static final LibraryFunction FUN_ABS         = new LibraryFunction("abs",          List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("_abs64"));
    public static final LibraryFunction FUN_ASC         = new LibraryFunction("asc",          List.of(Str.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("asc"));
    public static final LibraryFunction FUN_ATN         = new LibraryFunction("atn",          List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("atan"));
    public static final LibraryFunction FUN_CDBL        = new LibraryFunction("cdbl",         List.of(F64.INSTANCE), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("cdbl"));
    public static final LibraryFunction FUN_CHR         = new LibraryFunction("chr$",         List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("chr$"));
    public static final LibraryFunction FUN_CINT        = new LibraryFunction("cint",         List.of(F64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("cint"));
    public static final LibraryFunction FUN_COS         = new LibraryFunction("cos",          List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("cos"));
    public static final LibraryFunction FUN_COMMAND     = new LibraryFunction("command$",     List.of(), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("command$"));
    public static final LibraryFunction FUN_CVD         = new LibraryFunction("cvd",          List.of(Str.INSTANCE), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("cvd"));
    public static final LibraryFunction FUN_CVI         = new LibraryFunction("cvi",          List.of(Str.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("cvi"));
    public static final LibraryFunction FUN_DATE        = new LibraryFunction("date$",        List.of(), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("date$"));
    public static final LibraryFunction FUN_EXP         = new LibraryFunction("exp",          List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("exp"));
    public static final LibraryFunction FUN_FABS        = new LibraryFunction("abs",          List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("fabs"));
    public static final LibraryFunction FUN_FIX         = new LibraryFunction("fix",          List.of(F64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("fix"));
    public static final LibraryFunction FUN_HEX         = new LibraryFunction("hex$",         List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("hex$"));
    public static final LibraryFunction FUN_INKEY       = new LibraryFunction("inkey$",       List.of(), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("inkey$"));
    public static final LibraryFunction FUN_INT         = new LibraryFunction("int",          List.of(F64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("int_F64"));
    public static final LibraryFunction FUN_LEN         = new LibraryFunction("len",          List.of(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strlen"));
    public static final LibraryFunction FUN_LOG         = new LibraryFunction("log",          List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("log"));
    public static final LibraryFunction FUN_LTRIM       = new LibraryFunction("ltrim$",       List.of(Str.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("ltrim"));
    public static final LibraryFunction FUN_MKD         = new LibraryFunction("mkd$",         List.of(F64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("mkd$"));
    public static final LibraryFunction FUN_MKI         = new LibraryFunction("mki$",         List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("mki$"));
    public static final LibraryFunction FUN_LBOUND      = new LibraryFunction("lbound",       List.of(Arr.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("lbound"));
    public static final LibraryFunction FUN_LBOUND_I64  = new LibraryFunction("lbound",       List.of(Arr.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("lbound_I64"));
    public static final LibraryFunction FUN_OCT         = new LibraryFunction("oct$",         List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("oct$"));
    public static final LibraryFunction FUN_RANDOMIZE   = new LibraryFunction("randomize",    List.of(F64.INSTANCE), Void.INSTANCE, LIB_JCCBASIC, new ExternalFunction("randomize"));
    public static final LibraryFunction FUN_SLEEP       = new LibraryFunction("_sleep",       List.of(F64.INSTANCE), Void.INSTANCE, LIB_JCCBASIC, new ExternalFunction("sleep_F64"));
    public static final LibraryFunction FUN_UBOUND      = new LibraryFunction("ubound",       List.of(Arr.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("ubound"));
    public static final LibraryFunction FUN_UBOUND_I64  = new LibraryFunction("ubound",       List.of(Arr.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("ubound_I64"));
    public static final LibraryFunction FUN_RND         = new LibraryFunction("rnd",          List.of(), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("rnd"));
    public static final LibraryFunction FUN_RND_F64     = new LibraryFunction("rnd",          List.of(F64.INSTANCE), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("rnd_F64"));
    public static final LibraryFunction FUN_RTRIM       = new LibraryFunction("rtrim$",       List.of(Str.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("rtrim"));
    public static final LibraryFunction FUN_OPTION_BASE = new LibraryFunction("_option_base", List.of(I64.INSTANCE), Void.INSTANCE, LIB_JCCBASIC, new ExternalFunction("option_base"));
    public static final LibraryFunction FUN_SGN         = new LibraryFunction("sgn",          List.of(F64.INSTANCE), I64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("sgn"));
    public static final LibraryFunction FUN_SIN         = new LibraryFunction("sin",          List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("sin"));
    public static final LibraryFunction FUN_SPACE       = new LibraryFunction("space$",       List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("space$"));
    public static final LibraryFunction FUN_SQR         = new LibraryFunction("sqr",          List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("sqrt"));
    public static final LibraryFunction FUN_STR_F64     = new LibraryFunction("str$",         List.of(F64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("str_F64"));
    public static final LibraryFunction FUN_STR_I64     = new LibraryFunction("str$",         List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("str_I64"));
    public static final LibraryFunction FUN_STRING_I64  = new LibraryFunction("string$",      List.of(I64.INSTANCE, I64.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("string$_I64"));
    public static final LibraryFunction FUN_STRING_STR  = new LibraryFunction("string$",      List.of(I64.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("string$_Str"));
    public static final LibraryFunction FUN_TAN         = new LibraryFunction("tan",          List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("tan"));
    public static final LibraryFunction FUN_TIME        = new LibraryFunction("time$",        List.of(), Str.INSTANCE, LIB_JCCBASIC, new ExternalFunction("time$"));
    public static final LibraryFunction FUN_TIMER       = new LibraryFunction("timer",        List.of(), F64.INSTANCE, LIB_JCCBASIC, new ExternalFunction("timer"));
    public static final LibraryFunction FUN_VAL         = new LibraryFunction("val",          List.of(Str.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("atof"));

    public static final AssemblyFunction FUN_INSTR2     = new BasicInstr2Function();
    public static final AssemblyFunction FUN_INSTR3     = new BasicInstr3Function();
    public static final AssemblyFunction FUN_LCASE      = new BasicLcaseFunction();
    public static final AssemblyFunction FUN_LEFT       = new BasicLeftFunction();
    public static final AssemblyFunction FUN_MID3       = new BasicMid3Function();
    public static final AssemblyFunction FUN_RIGHT      = new BasicRightFunction();
    public static final AssemblyFunction FUN_MID2       = new BasicMid2Function(); // Depends on FUN_RIGHT
    public static final AssemblyFunction FUN_UCASE      = new BasicUcaseFunction();

    private BasicBuiltInFunctions() { }
}
