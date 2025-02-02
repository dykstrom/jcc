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

package se.dykstrom.jcc.col.compiler;

import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_JCC_COL;

public final class LibJccColBuiltIns {

    public static final LibraryFunction FUN_F64_FROM_I64 = new LibraryFunction("f64", List.of(I64.INSTANCE), F64.INSTANCE, LIB_JCC_COL, new ExternalFunction("N/A"));
    public static final LibraryFunction FUN_I32_FROM_I64 = new LibraryFunction("i32", List.of(I64.INSTANCE), I32.INSTANCE, LIB_JCC_COL, new ExternalFunction("N/A"));
    public static final LibraryFunction FUN_I64_FROM_F64 = new LibraryFunction("i64", List.of(F64.INSTANCE), I64.INSTANCE, LIB_JCC_COL, new ExternalFunction("N/A"));

    private LibJccColBuiltIns() { }
}
