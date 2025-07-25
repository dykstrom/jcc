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

import se.dykstrom.jcc.common.functions.BuiltInFunction;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

import java.util.List;

import static se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.*;

/**
 * A symbol table specific for COL, loaded with all built-in functions.
 * This class defines all built-in functions in the COL language, and makes
 * them available for semantic analysis.
 */
public class ColSymbols extends SymbolTable {

    public static final Function BF_F32_F64 = new BuiltInFunction("f32", List.of(F64.INSTANCE), F32.INSTANCE);
    public static final Function BF_F32_I32 = new BuiltInFunction("f32", List.of(I32.INSTANCE), F32.INSTANCE);
    public static final Function BF_F32_I64 = new BuiltInFunction("f32", List.of(I64.INSTANCE), F32.INSTANCE);
    public static final Function BF_F64_F32 = new BuiltInFunction("f64", List.of(F32.INSTANCE), F64.INSTANCE);
    public static final Function BF_F64_I32 = new BuiltInFunction("f64", List.of(I32.INSTANCE), F64.INSTANCE);
    public static final Function BF_F64_I64 = new BuiltInFunction("f64", List.of(I64.INSTANCE), F64.INSTANCE);
    public static final Function BF_I32_F32 = new BuiltInFunction("i32", List.of(F32.INSTANCE), I32.INSTANCE);
    public static final Function BF_I32_F64 = new BuiltInFunction("i32", List.of(F64.INSTANCE), I32.INSTANCE);
    public static final Function BF_I32_I64 = new BuiltInFunction("i32", List.of(I64.INSTANCE), I32.INSTANCE);
    public static final Function BF_I64_F32 = new BuiltInFunction("i64", List.of(F32.INSTANCE), I64.INSTANCE);
    public static final Function BF_I64_F64 = new BuiltInFunction("i64", List.of(F64.INSTANCE), I64.INSTANCE);
    public static final Function BF_I64_I32 = new BuiltInFunction("i64", List.of(I32.INSTANCE), I64.INSTANCE);

    public static final Function BF_CEIL_F32 = new BuiltInFunction("ceil", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_CEIL_F64 = new BuiltInFunction("ceil", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_FLOOR_F32 = new BuiltInFunction("floor", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_FLOOR_F64 = new BuiltInFunction("floor", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_ROUND_F32 = new BuiltInFunction("round", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_ROUND_F64 = new BuiltInFunction("round", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_TRUNC_F32 = new BuiltInFunction("trunc", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_TRUNC_F64 = new BuiltInFunction("trunc", List.of(F64.INSTANCE), F64.INSTANCE);

    public static final Function BF_MAX_F32_F32 = new BuiltInFunction("max", List.of(F32.INSTANCE, F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_MAX_F64_F64 = new BuiltInFunction("max", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_MAX_I32_I32 = new BuiltInFunction("max", List.of(I32.INSTANCE, I32.INSTANCE), I32.INSTANCE);
    public static final Function BF_MAX_I64_I64 = new BuiltInFunction("max", List.of(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE);
    public static final Function BF_MIN_F32_F32 = new BuiltInFunction("min", List.of(F32.INSTANCE, F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_MIN_F64_F64 = new BuiltInFunction("min", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_MIN_I32_I32 = new BuiltInFunction("min", List.of(I32.INSTANCE, I32.INSTANCE), I32.INSTANCE);
    public static final Function BF_MIN_I64_I64 = new BuiltInFunction("min", List.of(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE);
    public static final Function BF_SQRT_F32 = new BuiltInFunction("sqrt", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_SQRT_F64 = new BuiltInFunction("sqrt", List.of(F64.INSTANCE), F64.INSTANCE);

    public static final Function BF_PRINTLN_BOOL = new BuiltInFunction("println", List.of(Bool.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_F32 = new BuiltInFunction("println", List.of(F32.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_F64 = new BuiltInFunction("println", List.of(F64.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_I32 = new BuiltInFunction("println", List.of(I32.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_I64 = new BuiltInFunction("println", List.of(I64.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_I64_TO_I64 = new BuiltInFunction("println", List.of(Fun.from(List.of(I64.INSTANCE), I64.INSTANCE)), I32.INSTANCE);

    public ColSymbols() {
        // Casting
        addFunction(BF_F32_F64);
        addFunction(BF_F32_I32);
        addFunction(BF_F32_I64);
        addFunction(BF_F64_F32);
        addFunction(BF_F64_I32);
        addFunction(BF_F64_I64);
        addFunction(BF_I32_F32);
        addFunction(BF_I32_F64);
        addFunction(BF_I32_I64);
        addFunction(BF_I64_F32);
        addFunction(BF_I64_F64);
        addFunction(BF_I64_I32);

        // Rounding
        addFunction(BF_CEIL_F32);
        addFunction(BF_CEIL_F64);
        addFunction(BF_FLOOR_F32);
        addFunction(BF_FLOOR_F64);
        addFunction(BF_ROUND_F32);
        addFunction(BF_ROUND_F64);
        addFunction(BF_TRUNC_F32);
        addFunction(BF_TRUNC_F64);

        // Math
        addFunction(BF_MAX_F32_F32);
        addFunction(BF_MAX_F64_F64);
        addFunction(BF_MAX_I32_I32);
        addFunction(BF_MAX_I64_I64);
        addFunction(BF_MIN_F32_F32);
        addFunction(BF_MIN_F64_F64);
        addFunction(BF_MIN_I32_I32);
        addFunction(BF_MIN_I64_I64);
        addFunction(BF_SQRT_F32);
        addFunction(BF_SQRT_F64);

        // Temporary?
        addFunction(BF_PRINTLN_BOOL);
        addFunction(BF_PRINTLN_F32);
        addFunction(BF_PRINTLN_F64);
        addFunction(BF_PRINTLN_I32);
        addFunction(BF_PRINTLN_I64);
        addFunction(BF_PRINTLN_I64_TO_I64);

        // Not directly callable
        addFunction(LF_CEIL_F32);
        addFunction(LF_CEIL_F64);
        addFunction(LF_FLOOR_F32);
        addFunction(LF_FLOOR_F64);
        addFunction(LF_MAX_F32_F32);
        addFunction(LF_MAX_F64_F64);
        addFunction(LF_MAX_I32_I32);
        addFunction(LF_MAX_I64_I64);
        addFunction(LF_MIN_F32_F32);
        addFunction(LF_MIN_F64_F64);
        addFunction(LF_MIN_I32_I32);
        addFunction(LF_MIN_I64_I64);
        addFunction(LF_PRINTF_STR_VAR);
        addFunction(LF_ROUND_F32);
        addFunction(LF_ROUND_F64);
        addFunction(LF_SQRT_F32);
        addFunction(LF_SQRT_F64);
        addFunction(LF_TRUNC_F32);
        addFunction(LF_TRUNC_F64);
    }
}
