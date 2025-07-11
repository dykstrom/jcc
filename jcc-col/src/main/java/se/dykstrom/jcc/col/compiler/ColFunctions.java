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

package se.dykstrom.jcc.col.compiler;

import se.dykstrom.jcc.common.functions.BuiltInFunction;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.common.types.Void;

import java.util.List;

/**
 * This class defines all built-in functions in the COL language.
 * The function instances defined here are added to the symbol table
 * and used for semantic analysis.
 */
public final class ColFunctions {

    public static final Function BF_F64_I32 = new BuiltInFunction("f64", List.of(I32.INSTANCE), F64.INSTANCE);
    public static final Function BF_F64_I64 = new BuiltInFunction("f64", List.of(I64.INSTANCE), F64.INSTANCE);
    public static final Function BF_I32_F64 = new BuiltInFunction("i32", List.of(F64.INSTANCE), I32.INSTANCE);
    public static final Function BF_I32_I64 = new BuiltInFunction("i32", List.of(I64.INSTANCE), I32.INSTANCE);
    public static final Function BF_I64_F64 = new BuiltInFunction("i64", List.of(F64.INSTANCE), I64.INSTANCE);
    public static final Function BF_I64_I32 = new BuiltInFunction("i64", List.of(I32.INSTANCE), I64.INSTANCE);

    public static final Function BF_CEIL_F64 = new BuiltInFunction("ceil", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_FLOOR_F64 = new BuiltInFunction("floor", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_ROUND_F64 = new BuiltInFunction("round", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_SQRT_F64 = new BuiltInFunction("sqrt", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_TRUNC_F64 = new BuiltInFunction("trunc", List.of(F64.INSTANCE), F64.INSTANCE);

    public static final Function BF_PRINTLN_BOOL = new BuiltInFunction("println", List.of(Bool.INSTANCE), Void.INSTANCE);
    public static final Function BF_PRINTLN_F64 = new BuiltInFunction("println", List.of(F64.INSTANCE), Void.INSTANCE);
    public static final Function BF_PRINTLN_I64 = new BuiltInFunction("println", List.of(I64.INSTANCE), Void.INSTANCE);
    public static final Function BF_PRINTLN_I64_TO_I64 = new BuiltInFunction("println", List.of(Fun.from(List.of(I64.INSTANCE), I64.INSTANCE)), Void.INSTANCE);

    private ColFunctions() { }
}
