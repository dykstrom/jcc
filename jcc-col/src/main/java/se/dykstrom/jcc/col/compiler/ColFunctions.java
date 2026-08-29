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

import se.dykstrom.jcc.col.ast.expression.PrintlnExpression;
import se.dykstrom.jcc.common.ast.AbsExpression;
import se.dykstrom.jcc.common.ast.CastToFloatExpression;
import se.dykstrom.jcc.common.ast.CastToIntExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.ModExpression;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.F32;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.llvm.code.LlvmFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static se.dykstrom.jcc.col.compiler.ColSymbols.*;
import static se.dykstrom.jcc.col.compiler.LibJccColBuiltIns.*;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.*;
import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.*;

/**
 * Maps built-in functions to inlinable expressions and library functions
 * during LLVM IR code generation for the COL language.
 */
public final class ColFunctions implements LlvmFunctions {

    /**
     * Builds an inline expression for a built-in function from its arguments.
     * <p>
     * A builder must use each argument <em>exactly once</em>. What it returns goes
     * straight to code generation, so an argument used twice is emitted twice: a
     * builder for SGN written as {@code (x > 0) - (x < 0)} would make {@code SGN(RND)}
     * draw two random numbers. When a lowering needs its argument more than once, give
     * it a dedicated AST node with one child - BASIC's {@code SgnExpression} is the
     * example - and evaluate that child once in the node's code generator.
     */
    @FunctionalInterface
    private interface InlineBuilder {
        Expression build(List<Expression> args);
    }

    private final Map<Identifier, Function> libraryMap = new HashMap<>();
    private final Map<Identifier, InlineBuilder> inlineMap = new HashMap<>();

    public ColFunctions() {
        addToLibraryMap(BF_ABS_F32, LF_ABS_F32);
        addToLibraryMap(BF_ABS_F64, LF_ABS_F64);
        addToLibraryMap(BF_CEIL_F32, LF_CEIL_F32);
        addToLibraryMap(BF_CEIL_F64, LF_CEIL_F64);
        addToLibraryMap(BF_FLOOR_F32, LF_FLOOR_F32);
        addToLibraryMap(BF_FLOOR_F64, LF_FLOOR_F64);
        addToLibraryMap(BF_MAX_F32_F32, LF_MAX_F32_F32);
        addToLibraryMap(BF_MAX_F64_F64, LF_MAX_F64_F64);
        addToLibraryMap(BF_MAX_I32_I32, LF_MAX_I32_I32);
        addToLibraryMap(BF_MAX_I64_I64, LF_MAX_I64_I64);
        addToLibraryMap(BF_MIN_F32_F32, LF_MIN_F32_F32);
        addToLibraryMap(BF_MIN_F64_F64, LF_MIN_F64_F64);
        addToLibraryMap(BF_MIN_I32_I32, LF_MIN_I32_I32);
        addToLibraryMap(BF_MIN_I64_I64, LF_MIN_I64_I64);
        addToLibraryMap(BF_MILLIS, JF_MILLIS);
        addToLibraryMap(BF_ROUND_F32, LF_ROUND_F32);
        addToLibraryMap(BF_ROUND_F64, LF_ROUND_F64);
        addToLibraryMap(BF_SQRT_F32, LF_SQRT_F32);
        addToLibraryMap(BF_SQRT_F64, LF_SQRT_F64);
        addToLibraryMap(BF_TRUNC_F32, LF_TRUNC_F32);
        addToLibraryMap(BF_TRUNC_F64, LF_TRUNC_F64);

        // Math (intrinsics)
        addToLibraryMap(BF_ATAN_F32, LF_ATAN_F32);
        addToLibraryMap(BF_ATAN_F64, LF_ATAN_F64);
        addToLibraryMap(BF_COS_F32, LF_COS_F32);
        addToLibraryMap(BF_COS_F64, LF_COS_F64);
        addToLibraryMap(BF_EXP_F32, LF_EXP_F32);
        addToLibraryMap(BF_EXP_F64, LF_EXP_F64);
        addToLibraryMap(BF_EXP2_F32, LF_EXP2_F32);
        addToLibraryMap(BF_EXP2_F64, LF_EXP2_F64);
        addToLibraryMap(BF_FMA_F32, LF_FMA_F32);
        addToLibraryMap(BF_FMA_F64, LF_FMA_F64);
        addToLibraryMap(BF_FMULADD_F32, LF_FMULADD_F32);
        addToLibraryMap(BF_FMULADD_F64, LF_FMULADD_F64);
        addToLibraryMap(BF_LOG_F32, LF_LOG_F32);
        addToLibraryMap(BF_LOG_F64, LF_LOG_F64);
        addToLibraryMap(BF_LOG2_F32, LF_LOG2_F32);
        addToLibraryMap(BF_LOG2_F64, LF_LOG2_F64);
        addToLibraryMap(BF_LOG10_F32, LF_LOG10_F32);
        addToLibraryMap(BF_LOG10_F64, LF_LOG10_F64);
        addToLibraryMap(BF_POW_F32_F32, LF_POW_F32_F32);
        addToLibraryMap(BF_POW_F64_F64, LF_POW_F64_F64);
        addToLibraryMap(BF_SIN_F32, LF_SIN_F32);
        addToLibraryMap(BF_SIN_F64, LF_SIN_F64);
        addToLibraryMap(BF_TAN_F32, LF_TAN_F32);
        addToLibraryMap(BF_TAN_F64, LF_TAN_F64);

        // Math (direct libm)
        addToLibraryMap(BF_CBRT_F32, CF_CBRT_F32);
        addToLibraryMap(BF_CBRT_F64, CF_CBRT_F64);

        // Strings. Every string-returning call is handed to the collector by
        // FunctionCallCodeGenerator, which registers any Str result of a built-in; len and indexof
        // return integers and register nothing. len goes straight to libc, like cbrt and fmod, so
        // it needs no libjcccol symbol and allocates nothing.
        addToLibraryMap(BF_EOF, JF_EOF);
        addToLibraryMap(BF_INDEXOF_STR_STR, JF_INDEXOF_STR_STR);
        addToLibraryMap(BF_LEN_STR, CF_STRLEN_STR);
        addToLibraryMap(BF_READLN, JF_READLN);
        addToLibraryMap(BF_STRING_BOOL, JF_STRING_BOOL);
        addToLibraryMap(BF_STRING_F64, JF_STRING_F64);
        addToLibraryMap(BF_STRING_I64, JF_STRING_I64);
        addToLibraryMap(BF_SUBSTR_STR_I64_I64, JF_SUBSTR_STR_I64_I64);

        addToInlineMap(BF_ABS_I32, args -> new AbsExpression(args.getFirst(), LF_ABS_I32));
        addToInlineMap(BF_ABS_I64, args -> new AbsExpression(args.getFirst(), LF_ABS_I64));
        // LLVM's frem is defined to produce the value libm fmod does, minus the errno side
        // effect that nothing in COL reads, so fmod needs no libm call at all (issue #99)
        addToInlineMap(BF_FMOD_F32_F32, args -> new ModExpression(args.getFirst(), args.get(1)));
        addToInlineMap(BF_FMOD_F64_F64, args -> new ModExpression(args.getFirst(), args.get(1)));
        // A same-type cast is the identity, so it inlines to the argument itself and emits nothing
        addToInlineMap(BF_F32_F32, List::getFirst);
        addToInlineMap(BF_F64_F64, List::getFirst);
        addToInlineMap(BF_I32_I32, List::getFirst);
        addToInlineMap(BF_I64_I64, List::getFirst);
        addToInlineMap(BF_F32_F64, args -> new CastToFloatExpression(args.getFirst(), F32.INSTANCE));
        addToInlineMap(BF_F32_I32, args -> new CastToFloatExpression(args.getFirst(), F32.INSTANCE));
        addToInlineMap(BF_F32_I64, args -> new CastToFloatExpression(args.getFirst(), F32.INSTANCE));
        addToInlineMap(BF_F64_F32, args -> new CastToFloatExpression(args.getFirst(), F64.INSTANCE));
        addToInlineMap(BF_F64_I32, args -> new CastToFloatExpression(args.getFirst(), F64.INSTANCE));
        addToInlineMap(BF_F64_I64, args -> new CastToFloatExpression(args.getFirst(), F64.INSTANCE));
        addToInlineMap(BF_I32_F32, args -> new CastToIntExpression(args.getFirst(), I32.INSTANCE));
        addToInlineMap(BF_I32_F64, args -> new CastToIntExpression(args.getFirst(), I32.INSTANCE));
        addToInlineMap(BF_I32_I64, args -> new CastToIntExpression(args.getFirst(), I32.INSTANCE));
        addToInlineMap(BF_I64_F32, args -> new CastToIntExpression(args.getFirst(), I64.INSTANCE));
        addToInlineMap(BF_I64_F64, args -> new CastToIntExpression(args.getFirst(), I64.INSTANCE));
        addToInlineMap(BF_I64_I32, args -> new CastToIntExpression(args.getFirst(), I64.INSTANCE));
        // println(bool) prints "true"/"false" rather than 1/0, by routing through the same
        // col_string_bool that string(bool) calls - so println(b) and println(string(b)) agree,
        // which is the divergence jcccol/strings.h anticipated. The conversion allocates, and its
        // result is registered and rooted like any other string a built-in returns.
        addToInlineMap(BF_PRINTLN_BOOL, args -> new PrintlnExpression(new FunctionCallExpression(BF_STRING_BOOL.getIdentifier(), args, BF_STRING_BOOL)));
        addToInlineMap(BF_PRINTLN_F32, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_F64, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_I32, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_I64, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_I64_TO_I64, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_STR, args -> new PrintlnExpression(args.getFirst()));
    }

    @Override
    public Optional<Expression> getInlineExpression(final Function function, final List<Expression> args) {
        final var builder = inlineMap.get(function.getIdentifier());
        return Optional.ofNullable(builder).map(b -> b.build(args));
    }

    @Override
    public Function getLibraryFunction(final Function function) {
        final var identifier = function.getIdentifier();
        final var lf = libraryMap.get(identifier);
        if (lf != null) {
            return lf;
        }
        throw new IllegalArgumentException("unknown built-in function: " + function);
    }

    private void addToLibraryMap(final Function bf, final Function lf) {
        libraryMap.put(bf.getIdentifier(), lf);
    }

    private void addToInlineMap(final Function bf, final InlineBuilder builder) {
        inlineMap.put(bf.getIdentifier(), builder);
    }
}
