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

/**
 * A symbol table specific for COL, loaded with all built-in functions.
 * This class defines all built-in functions in the COL language, and makes
 * them available for semantic analysis.
 */
public class ColSymbols extends SymbolTable {

    // The same-type overloads are the identity cast: they let a cast be written wherever the
    // programmer expects it to be allowed, and lower to nothing at all
    public static final Function BF_F32_F32 = new BuiltInFunction("f32", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_F32_F64 = new BuiltInFunction("f32", List.of(F64.INSTANCE), F32.INSTANCE);
    public static final Function BF_F32_I32 = new BuiltInFunction("f32", List.of(I32.INSTANCE), F32.INSTANCE);
    public static final Function BF_F32_I64 = new BuiltInFunction("f32", List.of(I64.INSTANCE), F32.INSTANCE);
    public static final Function BF_F64_F32 = new BuiltInFunction("f64", List.of(F32.INSTANCE), F64.INSTANCE);
    public static final Function BF_F64_F64 = new BuiltInFunction("f64", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_F64_I32 = new BuiltInFunction("f64", List.of(I32.INSTANCE), F64.INSTANCE);
    public static final Function BF_F64_I64 = new BuiltInFunction("f64", List.of(I64.INSTANCE), F64.INSTANCE);
    public static final Function BF_I32_F32 = new BuiltInFunction("i32", List.of(F32.INSTANCE), I32.INSTANCE);
    public static final Function BF_I32_F64 = new BuiltInFunction("i32", List.of(F64.INSTANCE), I32.INSTANCE);
    public static final Function BF_I32_I32 = new BuiltInFunction("i32", List.of(I32.INSTANCE), I32.INSTANCE);
    public static final Function BF_I32_I64 = new BuiltInFunction("i32", List.of(I64.INSTANCE), I32.INSTANCE);
    public static final Function BF_I64_F32 = new BuiltInFunction("i64", List.of(F32.INSTANCE), I64.INSTANCE);
    public static final Function BF_I64_F64 = new BuiltInFunction("i64", List.of(F64.INSTANCE), I64.INSTANCE);
    public static final Function BF_I64_I32 = new BuiltInFunction("i64", List.of(I32.INSTANCE), I64.INSTANCE);
    public static final Function BF_I64_I64 = new BuiltInFunction("i64", List.of(I64.INSTANCE), I64.INSTANCE);

    public static final Function BF_CEIL_F32 = new BuiltInFunction("ceil", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_CEIL_F64 = new BuiltInFunction("ceil", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_FLOOR_F32 = new BuiltInFunction("floor", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_FLOOR_F64 = new BuiltInFunction("floor", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_ROUND_F32 = new BuiltInFunction("round", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_ROUND_F64 = new BuiltInFunction("round", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_TRUNC_F32 = new BuiltInFunction("trunc", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_TRUNC_F64 = new BuiltInFunction("trunc", List.of(F64.INSTANCE), F64.INSTANCE);

    public static final Function BF_ABS_F32 = new BuiltInFunction("abs", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_ABS_F64 = new BuiltInFunction("abs", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_ABS_I32 = new BuiltInFunction("abs", List.of(I32.INSTANCE), I32.INSTANCE);
    public static final Function BF_ABS_I64 = new BuiltInFunction("abs", List.of(I64.INSTANCE), I64.INSTANCE);
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

    public static final Function BF_ATAN_F32 = new BuiltInFunction("atan", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_ATAN_F64 = new BuiltInFunction("atan", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_CBRT_F32 = new BuiltInFunction("cbrt", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_CBRT_F64 = new BuiltInFunction("cbrt", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_COS_F32 = new BuiltInFunction("cos", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_COS_F64 = new BuiltInFunction("cos", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_EXP_F32 = new BuiltInFunction("exp", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_EXP_F64 = new BuiltInFunction("exp", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_EXP2_F32 = new BuiltInFunction("exp2", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_EXP2_F64 = new BuiltInFunction("exp2", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_FMA_F32 = new BuiltInFunction("fma", List.of(F32.INSTANCE, F32.INSTANCE, F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_FMA_F64 = new BuiltInFunction("fma", List.of(F64.INSTANCE, F64.INSTANCE, F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_FMOD_F32_F32 = new BuiltInFunction("fmod", List.of(F32.INSTANCE, F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_FMOD_F64_F64 = new BuiltInFunction("fmod", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_LOG_F32 = new BuiltInFunction("log", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_LOG_F64 = new BuiltInFunction("log", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_LOG2_F32 = new BuiltInFunction("log2", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_LOG2_F64 = new BuiltInFunction("log2", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_LOG10_F32 = new BuiltInFunction("log10", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_LOG10_F64 = new BuiltInFunction("log10", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_MULADD_F32 = new BuiltInFunction("muladd", List.of(F32.INSTANCE, F32.INSTANCE, F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_MULADD_F64 = new BuiltInFunction("muladd", List.of(F64.INSTANCE, F64.INSTANCE, F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_POW_F32_F32 = new BuiltInFunction("pow", List.of(F32.INSTANCE, F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_POW_F64_F64 = new BuiltInFunction("pow", List.of(F64.INSTANCE, F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_SIN_F32 = new BuiltInFunction("sin", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_SIN_F64 = new BuiltInFunction("sin", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_TAN_F32 = new BuiltInFunction("tan", List.of(F32.INSTANCE), F32.INSTANCE);
    public static final Function BF_TAN_F64 = new BuiltInFunction("tan", List.of(F64.INSTANCE), F64.INSTANCE);

    public static final Function BF_MILLIS = new BuiltInFunction("millis", List.of(), I64.INSTANCE);

    // Strings. Everything here is byte oriented - there are no codepoint semantics anywhere: len
    // counts bytes, and substr and indexof work on byte offsets. The source string is the first
    // argument throughout, so that a future method-call sugar - "hello".indexof("ll") meaning
    // indexof("hello", "ll") - stays available.
    public static final Function BF_EOF = new BuiltInFunction("eof", List.of(), Bool.INSTANCE);
    public static final Function BF_INDEXOF_STR_STR = new BuiltInFunction("indexof", List.of(Str.INSTANCE, Str.INSTANCE), I64.INSTANCE);
    public static final Function BF_LEN_STR = new BuiltInFunction("len", List.of(Str.INSTANCE), I64.INSTANCE);
    public static final Function BF_READLN = new BuiltInFunction("readln", List.of(), Str.INSTANCE);
    // 'string' reuses the type's own name, as the numeric casts do - but it converts *to* text and
    // is not a numeric cast, so string is never assignable from a number in the type manager
    public static final Function BF_STRING_BOOL = new BuiltInFunction("string", List.of(Bool.INSTANCE), Str.INSTANCE);
    public static final Function BF_STRING_F64 = new BuiltInFunction("string", List.of(F64.INSTANCE), Str.INSTANCE);
    public static final Function BF_STRING_I64 = new BuiltInFunction("string", List.of(I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_SUBSTR_STR_I64_I64 = new BuiltInFunction("substr", List.of(Str.INSTANCE, I64.INSTANCE, I64.INSTANCE), Str.INSTANCE);

    public static final Function BF_PRINTLN_BOOL = new BuiltInFunction("println", List.of(Bool.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_F32 = new BuiltInFunction("println", List.of(F32.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_F64 = new BuiltInFunction("println", List.of(F64.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_I32 = new BuiltInFunction("println", List.of(I32.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_I64 = new BuiltInFunction("println", List.of(I64.INSTANCE), I32.INSTANCE);
    public static final Function BF_PRINTLN_I64_TO_I64 = new BuiltInFunction("println", List.of(Fun.from(List.of(I64.INSTANCE), I64.INSTANCE)), I32.INSTANCE);
    public static final Function BF_PRINTLN_STR = new BuiltInFunction("println", List.of(Str.INSTANCE), I32.INSTANCE);

    public ColSymbols() {
        // Casting
        addFunction(BF_F32_F32);
        addFunction(BF_F32_F64);
        addFunction(BF_F32_I32);
        addFunction(BF_F32_I64);
        addFunction(BF_F64_F32);
        addFunction(BF_F64_F64);
        addFunction(BF_F64_I32);
        addFunction(BF_F64_I64);
        addFunction(BF_I32_F32);
        addFunction(BF_I32_F64);
        addFunction(BF_I32_I32);
        addFunction(BF_I32_I64);
        addFunction(BF_I64_F32);
        addFunction(BF_I64_F64);
        addFunction(BF_I64_I32);
        addFunction(BF_I64_I64);

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
        addFunction(BF_ABS_F32);
        addFunction(BF_ABS_F64);
        addFunction(BF_ABS_I32);
        addFunction(BF_ABS_I64);
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
        addFunction(BF_ATAN_F32);
        addFunction(BF_ATAN_F64);
        addFunction(BF_CBRT_F32);
        addFunction(BF_CBRT_F64);
        addFunction(BF_COS_F32);
        addFunction(BF_COS_F64);
        addFunction(BF_EXP_F32);
        addFunction(BF_EXP_F64);
        addFunction(BF_EXP2_F32);
        addFunction(BF_EXP2_F64);
        addFunction(BF_FMA_F32);
        addFunction(BF_FMA_F64);
        addFunction(BF_FMOD_F32_F32);
        addFunction(BF_FMOD_F64_F64);
        addFunction(BF_LOG_F32);
        addFunction(BF_LOG_F64);
        addFunction(BF_LOG2_F32);
        addFunction(BF_LOG2_F64);
        addFunction(BF_LOG10_F32);
        addFunction(BF_LOG10_F64);
        addFunction(BF_MULADD_F32);
        addFunction(BF_MULADD_F64);
        addFunction(BF_POW_F32_F32);
        addFunction(BF_POW_F64_F64);
        addFunction(BF_SIN_F32);
        addFunction(BF_SIN_F64);
        addFunction(BF_TAN_F32);
        addFunction(BF_TAN_F64);

        // Time
        addFunction(BF_MILLIS);

        // Strings
        addFunction(BF_EOF);
        addFunction(BF_INDEXOF_STR_STR);
        addFunction(BF_LEN_STR);
        addFunction(BF_READLN);
        addFunction(BF_STRING_BOOL);
        addFunction(BF_STRING_F64);
        addFunction(BF_STRING_I64);
        addFunction(BF_SUBSTR_STR_I64_I64);

        // Temporary?
        addFunction(BF_PRINTLN_BOOL);
        addFunction(BF_PRINTLN_F32);
        addFunction(BF_PRINTLN_F64);
        addFunction(BF_PRINTLN_I32);
        addFunction(BF_PRINTLN_I64);
        addFunction(BF_PRINTLN_I64_TO_I64);
        addFunction(BF_PRINTLN_STR);
    }
}
