/*
 * Copyright (C) 2025 Johan Dykstrom
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

package se.dykstrom.jcc.llvm.code;

import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.F64;

import java.util.List;

/**
 * This class defines library function that are implemented as LLVM intrinsics.
 */
public final class LlvmBuiltIns {

    private static final String NONE = "N/A";

    public static final LibraryFunction LF_CEIL_F64 = new LibraryFunction(".ceil", List.of(F64.INSTANCE), F64.INSTANCE, NONE, new ExternalFunction("llvm.ceil.f64"));
    public static final LibraryFunction LF_FLOOR_F64 = new LibraryFunction(".floor", List.of(F64.INSTANCE), F64.INSTANCE, NONE, new ExternalFunction("llvm.floor.f64"));
    public static final LibraryFunction LF_ROUND_F64 = new LibraryFunction(".round", List.of(F64.INSTANCE), F64.INSTANCE, NONE, new ExternalFunction("llvm.round.f64"));
    public static final LibraryFunction LF_SQRT_F64 = new LibraryFunction(".sqrt", List.of(F64.INSTANCE), F64.INSTANCE, NONE, new ExternalFunction("llvm.sqrt.f64"));
    public static final LibraryFunction LF_TRUNC_F64 = new LibraryFunction(".trunc", List.of(F64.INSTANCE), F64.INSTANCE, NONE, new ExternalFunction("llvm.trunc.f64"));

    private LlvmBuiltIns() { }
}
