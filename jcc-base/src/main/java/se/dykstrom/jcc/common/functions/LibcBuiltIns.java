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

import static se.dykstrom.jcc.common.utils.FunctionUtils.LIB_LIBC;

/**
 * This class defines library function that are implemented in the C standard library libc.
 * Each library function has a reference to an external function, which contains the name of
 * the function in the standard library.
 * <p>
 * Libc function constants are prefixed with the string "CF".
 */
public final class LibcBuiltIns {

    public static final Function CF_ATOF_STR = new LibraryFunction(".atof", List.of(Str.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("atof"));
    public static final Function CF_CBRT_F32 = new LibraryFunction(".cbrt", List.of(F32.INSTANCE), F32.INSTANCE, LIB_LIBC, new ExternalFunction("cbrtf"));
    public static final Function CF_CBRT_F64 = new LibraryFunction(".cbrt", List.of(F64.INSTANCE), F64.INSTANCE, LIB_LIBC, new ExternalFunction("cbrt"));
    public static final Function CF_EXIT_I64 = new LibraryFunction("exit", List.of(I64.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("exit"));
    public static final Function CF_PRINTF_STR_VAR = new LibraryFunction(".printf", List.of(Str.INSTANCE, Varargs.INSTANCE), I32.INSTANCE, LIB_LIBC, new ExternalFunction("printf"));
    public static final Function CF_SCANF_STR_VAR = new LibraryFunction(".scanf", List.of(Str.INSTANCE, Varargs.INSTANCE), I32.INSTANCE, LIB_LIBC, new ExternalFunction("scanf"));
    public static final Function CF_STRCMP_STR_STR = new LibraryFunction("strcmp", List.of(Str.INSTANCE, Str.INSTANCE), I32.INSTANCE, LIB_LIBC, new ExternalFunction("strcmp"));
    public static final Function CF_STRLEN_STR = new LibraryFunction("strlen", List.of(Str.INSTANCE), I64.INSTANCE, LIB_LIBC, new ExternalFunction("strlen"));

    private LibcBuiltIns() { }
}
