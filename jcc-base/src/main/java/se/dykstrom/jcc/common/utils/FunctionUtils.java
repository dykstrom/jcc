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

package se.dykstrom.jcc.common.utils;

/**
 * Contains utility methods and constants for functions.
 * 
 * @author Johan Dykstrom
 */
public class FunctionUtils {

    public static final String LIB_INTERNAL = "<internal>";
    public static final String LIB_JCC_BAS  = "libjccbas.dll";
    public static final String LIB_JCC_COL  = "libjcccol.a";
    public static final String LIB_LIBC     = "msvcrt.dll";
    // Logical marker for the LLVM garbage collector runtime (jcc_gc_*). It only tags GC
    // functions; it does not drive linking (the LLVM backend links a single standard library,
    // see LlvmAssembler). The GC symbols physically ship in libjccbas, so a plain declare of a
    // jcc_gc_* function resolves against the linked standard library (issue #63).
    public static final String LIB_JCC_GC   = "libjccgc";

    private FunctionUtils() { }
}
