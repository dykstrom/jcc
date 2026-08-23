/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.compiler;

import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;

import static se.dykstrom.jcc.common.utils.FunctionUtils.LIB_JCC_COL;

/**
 * This class defines library functions that are implemented in the JCC COL standard library libjcccol.
 * <p>
 * Libjcccol function constants are prefixed with the string "JF".
 * <p>
 * Every symbol below is signature mangled with lower-cased type tokens, because COL overloads
 * functions and libjcccol must be able to export more than one implementation of the same COL name.
 * Functions taking no arguments carry no suffix. Since jcc resolves purely by symbol string, a typo
 * here surfaces as an undefined symbol at link time rather than as a compilation error.
 * <p>
 * Each Str returning function returns a freshly malloc'd block, which the caller hands to the
 * collector; see the fresh-block contract in {@code include/jcccol/strings.h}.
 */
public final class LibJccColBuiltIns {

    /** Concatenates two strings into a freshly malloc'd one, which the caller hands to the GC. */
    public static final Function JF_CONCAT_STR_STR = new LibraryFunction(".concat", List.of(Str.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_concat_str_str"));
    /** Reports whether stdin is at end of input, without consuming a character. */
    public static final Function JF_EOF = new LibraryFunction(".eof", List.of(), Bool.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_eof"));
    /** Byte offset of the first occurrence of the second string in the first, or -1 if absent. */
    public static final Function JF_INDEXOF_STR_STR = new LibraryFunction(".indexof", List.of(Str.INSTANCE, Str.INSTANCE), I64.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_indexof_str_str"));
    public static final Function JF_MILLIS = new LibraryFunction(".millis", List.of(), I64.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_millis"));
    /** Reads one line from stdin, without the trailing newline, or the empty string at end of input. */
    public static final Function JF_READLN = new LibraryFunction(".readln", List.of(), Str.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_readln"));
    public static final Function JF_STRING_BOOL = new LibraryFunction(".string", List.of(Bool.INSTANCE), Str.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_string_bool"));
    public static final Function JF_STRING_F64 = new LibraryFunction(".string", List.of(F64.INSTANCE), Str.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_string_f64"));
    public static final Function JF_STRING_I64 = new LibraryFunction(".string", List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_string_i64"));
    /** Extracts a substring by byte offset. Out-of-range arguments clamp rather than failing. */
    public static final Function JF_SUBSTR_STR_I64_I64 = new LibraryFunction(".substr", List.of(Str.INSTANCE, I64.INSTANCE, I64.INSTANCE), Str.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_substr_str_i64_i64"));

    private LibJccColBuiltIns() { }
}
