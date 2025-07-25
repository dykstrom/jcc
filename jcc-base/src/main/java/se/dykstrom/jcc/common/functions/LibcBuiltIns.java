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
 */
public final class LibcBuiltIns {

    public static final LibraryFunction LF_ABS_I64         = new LibraryFunction("abs", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("_abs64"));
    public static final LibraryFunction LF_CEIL_F64        = new LibraryFunction("ceil", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("ceil"));
    public static final LibraryFunction FUN_EXIT           = new LibraryFunction("exit", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("exit"));
    public static final LibraryFunction LF_FABS_F64        = new LibraryFunction("fabs", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("fabs"));
    public static final LibraryFunction FUN_FFLUSH         = new LibraryFunction("fflush", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("fflush"));
    public static final LibraryFunction LF_FLOOR_F64       = new LibraryFunction("floor", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("floor"));
    public static final LibraryFunction LF_FMOD_F64_F64    = new LibraryFunction("fmod", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("fmod"));
    public static final LibraryFunction FUN_FREE           = new LibraryFunction("free", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("free"));
    public static final LibraryFunction FUN_GETCHAR        = new LibraryFunction("getchar", List.of(), I64.INSTANCE, LIB_LIBC, new ExternalFunction("getchar"));
    public static final LibraryFunction FUN_MALLOC         = new LibraryFunction("malloc", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("malloc"));
    public static final LibraryFunction LF_POW_F64_F64     = new LibraryFunction("pow", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("pow"));
    public static final LibraryFunction FUN_REALLOC        = new LibraryFunction("realloc", List.of(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("realloc"));
    public static final LibraryFunction LF_SQRT_F64        = new LibraryFunction("sqrt", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("sqrt"));
    public static final LibraryFunction FUN_STRCAT         = new LibraryFunction("strcat", List.of(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strcat"));
    public static final LibraryFunction FUN_STRCPY         = new LibraryFunction("strcpy", List.of(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strcpy"));
    public static final LibraryFunction FUN_STRCMP         = new LibraryFunction("strcmp", List.of(Str.INSTANCE, Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strcmp"));
    public static final LibraryFunction FUN_STRDUP         = new LibraryFunction("strdup", List.of(Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("_strdup"));
    public static final LibraryFunction FUN_STRLEN         = new LibraryFunction("strlen", List.of(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strlen"));
    public static final LibraryFunction FUN_STRNCPY        = new LibraryFunction("strncpy", List.of(Str.INSTANCE, Str.INSTANCE, I64.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strncpy"));
    public static final LibraryFunction FUN_STRSTR         = new LibraryFunction("strstr", List.of(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strstr"));
    public static final LibraryFunction FUN_TOLOWER        = new LibraryFunction("tolower", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("tolower"));
    public static final LibraryFunction FUN_TOUPPER        = new LibraryFunction("toupper", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("toupper"));

    // Not directly callable
    public static final LibraryFunction LF_PRINTF_STR_VAR = new LibraryFunction(".printf", List.of(Str.INSTANCE, Varargs.INSTANCE), I32.INSTANCE, LIB_LIBC, new ExternalFunction("printf"));
    public static final LibraryFunction LF_SCANF_STR_VAR  = new LibraryFunction(".scanf", List.of(Str.INSTANCE, Varargs.INSTANCE), I32.INSTANCE, LIB_LIBC, new ExternalFunction("scanf"));

    private LibcBuiltIns() { }
}
