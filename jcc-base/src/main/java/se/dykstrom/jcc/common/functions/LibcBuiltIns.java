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

package se.dykstrom.jcc.common.functions;

import se.dykstrom.jcc.common.types.*;

import java.util.List;

import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * This class defines library function that are implemented in the C standard library libc.
 * Each library function has a reference to an external function, which contains the name of
 * the function in the standard library.
 * <p>
 * Libc function constants are prefixed with the string "CF".
 */
public final class LibcBuiltIns {

    public static final Function CF_ABS_I64 = new LibraryFunction(".abs", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("_abs64"));
    public static final Function CF_ATN_F64 = new LibraryFunction(".atn", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("atan"));
    public static final Function CF_ATOF_STR = new LibraryFunction(".atof", List.of(Str.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("atof"));
    public static final Function CF_CEIL_F64 = new LibraryFunction(".ceil", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("ceil"));
    public static final Function CF_COS_F64 = new LibraryFunction(".cos", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("cos"));
    public static final Function CF_EXIT_I64 = new LibraryFunction("exit", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("exit"));
    public static final Function CF_EXP_F64 = new LibraryFunction(".exp", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("exp"));
    public static final Function CF_FABS_F64 = new LibraryFunction(".fabs", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("fabs"));
    public static final Function CF_FFLUSH_I64 = new LibraryFunction("fflush", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("fflush"));
    public static final Function CF_FLOOR_F64 = new LibraryFunction(".floor", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("floor"));
    public static final Function CF_FMOD_F64_F64 = new LibraryFunction(".fmod", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("fmod"));
    public static final Function CF_FREE_I64 = new LibraryFunction("free", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("free"));
    public static final Function CF_GETCHAR = new LibraryFunction("getchar", List.of(), I64.INSTANCE, LIB_LIBC, new ExternalFunction("getchar"));
    public static final Function CF_LOG_F64 = new LibraryFunction(".log", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("log"));
    public static final Function CF_MALLOC_I64 = new LibraryFunction("malloc", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("malloc"));
    public static final Function CF_PRINTF_STR_VAR = new LibraryFunction(".printf", List.of(Str.INSTANCE, Varargs.INSTANCE), I32.INSTANCE, LIB_LIBC, new ExternalFunction("printf"));
    public static final Function CF_POW_F64_F64 = new LibraryFunction(".pow", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("pow"));
    public static final Function CF_REALLOC_STR_I64 = new LibraryFunction("realloc", List.of(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("realloc"));
    public static final Function CF_SCANF_STR_VAR = new LibraryFunction(".scanf", List.of(Str.INSTANCE, Varargs.INSTANCE), I32.INSTANCE, LIB_LIBC, new ExternalFunction("scanf"));
    public static final Function CF_SIN_F64 = new LibraryFunction(".sin", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("sin"));
    public static final Function CF_SQRT_F64 = new LibraryFunction(".sqrt", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("sqrt"));
    public static final Function CF_STRCAT_STR_STR = new LibraryFunction("strcat", List.of(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strcat"));
    public static final Function CF_STRCMP_STR_STR = new LibraryFunction("strcmp", List.of(Str.INSTANCE, Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strcmp"));
    public static final Function CF_STRCPY_STR_STR = new LibraryFunction("strcpy", List.of(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strcpy"));
    public static final Function CF_STRDUP_STR = new LibraryFunction("strdup", List.of(Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("_strdup"));
    public static final Function CF_STRLEN_STR = new LibraryFunction("strlen", List.of(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strlen"));
    public static final Function CF_STRNCPY_STR_STR_I64 = new LibraryFunction("strncpy", List.of(Str.INSTANCE, Str.INSTANCE, I64.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strncpy"));
    public static final Function CF_STRSTR_STR_STR = new LibraryFunction("strstr", List.of(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strstr"));
    public static final Function CF_TAN_F64 = new LibraryFunction(".tan", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("tan"));
    public static final Function CF_TOLOWER_I64 = new LibraryFunction(".tolower", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("tolower"));
    public static final Function CF_TOUPPER_I64 = new LibraryFunction(".toupper", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("toupper"));

    private LibcBuiltIns() { }
}
