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
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.*;

import java.util.List;

/**
 * This class defines library function that are implemented as LLVM intrinsics.
 */
public final class LlvmBuiltIns {

    private static final String NONE = "N/A";

    public static final Function LF_CEIL_F32 = create(".ceil", List.of(F32.INSTANCE), F32.INSTANCE, "llvm.ceil.f32");
    public static final Function LF_CEIL_F64 = create(".ceil", List.of(F64.INSTANCE), F64.INSTANCE, "llvm.ceil.f64");
    public static final Function LF_FLOOR_F32 = create(".floor", List.of(F32.INSTANCE), F32.INSTANCE, "llvm.floor.f32");
    public static final Function LF_FLOOR_F64 = create(".floor", List.of(F64.INSTANCE), F64.INSTANCE, "llvm.floor.f64");
    public static final Function LF_ROUND_F32 = create(".round", List.of(F32.INSTANCE), F32.INSTANCE, "llvm.round.f32");
    public static final Function LF_ROUND_F64 = create(".round", List.of(F64.INSTANCE), F64.INSTANCE, "llvm.round.f64");
    public static final Function LF_SQRT_F32 = create(".sqrt", List.of(F32.INSTANCE), F32.INSTANCE, "llvm.sqrt.f32");
    public static final Function LF_SQRT_F64 = create(".sqrt", List.of(F64.INSTANCE), F64.INSTANCE, "llvm.sqrt.f64");
    public static final Function LF_TRUNC_F32 = create(".trunc", List.of(F32.INSTANCE), F32.INSTANCE, "llvm.trunc.f32");
    public static final Function LF_TRUNC_F64 = create(".trunc", List.of(F64.INSTANCE), F64.INSTANCE, "llvm.trunc.f64");
    public static final Function LF_MAX_F32_F32 = create(".max", List.of(F32.INSTANCE, F32.INSTANCE), F32.INSTANCE, "llvm.maximum.f32");
    public static final Function LF_MAX_F64_F64 = create(".max", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, "llvm.maximum.f64");
    public static final Function LF_MAX_I32_I32 = create(".max", List.of(I32.INSTANCE, I32.INSTANCE), I32.INSTANCE, "llvm.smax.i32");
    public static final Function LF_MAX_I64_I64 = create(".max", List.of(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "llvm.smax.i64");
    public static final Function LF_MIN_F32_F32 = create(".min", List.of(F32.INSTANCE, F32.INSTANCE), F32.INSTANCE, "llvm.minimum.f32");
    public static final Function LF_MIN_F64_F64 = create(".min", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, "llvm.minimum.f64");
    public static final Function LF_MIN_I32_I32 = create(".min", List.of(I32.INSTANCE, I32.INSTANCE), I32.INSTANCE, "llvm.smin.i32");
    public static final Function LF_MIN_I64_I64 = create(".min", List.of(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "llvm.smin.i64");

    private static LibraryFunction create(final String name, 
                                          final List<Type> argTypes, 
                                          final Type returnType, 
                                          final String externalName) {
        return new LibraryFunction(name, argTypes, returnType, NONE, new ExternalFunction(externalName));
    }

    private LlvmBuiltIns() { }
}
