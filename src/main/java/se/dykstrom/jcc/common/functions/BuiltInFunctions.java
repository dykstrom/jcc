/*
 * Copyright (C) 2018 Johan Dykstrom
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

import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Contains a number of common built-in functions.
 *
 * @author Johan Dykstrom
 */
public final class BuiltInFunctions {

    public static final LibraryFunction FUN_EXIT    = new LibraryFunction("exit", singletonList(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("exit"));
    public static final LibraryFunction FUN_FLOOR   = new LibraryFunction("floor", singletonList(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("floor"));
    public static final LibraryFunction FUN_FREE    = new LibraryFunction("free", singletonList(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("free"));
    public static final LibraryFunction FUN_GETCHAR = new LibraryFunction("getchar", emptyList(), I64.INSTANCE, LIB_LIBC, new ExternalFunction("getchar"));
    public static final LibraryFunction FUN_MALLOC  = new LibraryFunction("malloc", singletonList(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("malloc"));
    public static final LibraryFunction FUN_MEMSET  = new LibraryFunction("memset", asList(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("memset"));
    public static final LibraryFunction FUN_PRINTF  = new LibraryFunction("printf", true, emptyList(), I64.INSTANCE, LIB_LIBC, new ExternalFunction("printf"));
    public static final LibraryFunction FUN_REALLOC = new LibraryFunction("realloc", asList(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("realloc"));
    public static final LibraryFunction FUN_SCANF   = new LibraryFunction("scanf", true, emptyList(), Str.INSTANCE, LIB_LIBC, new ExternalFunction("scanf"));
    public static final LibraryFunction FUN_SPRINTF = new LibraryFunction("sprintf", true, emptyList(), I64.INSTANCE, LIB_LIBC, new ExternalFunction("sprintf"));
    public static final LibraryFunction FUN_STRCAT  = new LibraryFunction("strcat", asList(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strcat"));
    public static final LibraryFunction FUN_STRCPY  = new LibraryFunction("strcpy", asList(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strcpy"));
    public static final LibraryFunction FUN_STRCMP  = new LibraryFunction("strcmp", asList(Str.INSTANCE, Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strcmp"));
    public static final LibraryFunction FUN_STRLEN  = new LibraryFunction("strlen", singletonList(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strlen"));
    public static final LibraryFunction FUN_STRNCPY = new LibraryFunction("strncpy", asList(Str.INSTANCE, Str.INSTANCE, I64.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strncpy"));
    public static final LibraryFunction FUN_STRSTR  = new LibraryFunction("strstr", asList(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_LIBC, new ExternalFunction("strstr"));
    public static final LibraryFunction FUN_TOLOWER = new LibraryFunction("tolower", singletonList(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("tolower"));
    public static final LibraryFunction FUN_TOUPPER = new LibraryFunction("toupper", singletonList(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("toupper"));

    public static final AssemblyFunction FUN_GETLINE = new GetLineFunction();

    // Memory management
    public static final AssemblyFunction FUN_MEMORY_MARK     = new MemoryMarkFunction();
    public static final AssemblyFunction FUN_MEMORY_SWEEP    = new MemorySweepFunction();
    public static final AssemblyFunction FUN_MEMORY_REGISTER = new MemoryRegisterFunction();

    private BuiltInFunctions() { }
}
