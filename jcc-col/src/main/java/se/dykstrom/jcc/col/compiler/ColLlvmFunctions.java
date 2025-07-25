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
import se.dykstrom.jcc.common.ast.CastToFloatExpression;
import se.dykstrom.jcc.common.ast.CastToIntExpression;
import se.dykstrom.jcc.common.ast.Expression;
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
import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.*;

/**
 * Maps built-in functions to inlinable expressions and library functions
 * during LLVM IR code generation for the COL language.
 */
public final class ColLlvmFunctions implements LlvmFunctions {

    private final Map<Identifier, Function> map = new HashMap<>();

    public ColLlvmFunctions() {
        addToMap(BF_CEIL_F32, LF_CEIL_F32);
        addToMap(BF_CEIL_F64, LF_CEIL_F64);
        addToMap(BF_FLOOR_F32, LF_FLOOR_F32);
        addToMap(BF_FLOOR_F64, LF_FLOOR_F64);
        addToMap(BF_MAX_F32_F32, LF_MAX_F32_F32);
        addToMap(BF_MAX_F64_F64, LF_MAX_F64_F64);
        addToMap(BF_MAX_I32_I32, LF_MAX_I32_I32);
        addToMap(BF_MAX_I64_I64, LF_MAX_I64_I64);
        addToMap(BF_MIN_F32_F32, LF_MIN_F32_F32);
        addToMap(BF_MIN_F64_F64, LF_MIN_F64_F64);
        addToMap(BF_MIN_I32_I32, LF_MIN_I32_I32);
        addToMap(BF_MIN_I64_I64, LF_MIN_I64_I64);
        addToMap(BF_ROUND_F32, LF_ROUND_F32);
        addToMap(BF_ROUND_F64, LF_ROUND_F64);
        addToMap(BF_SQRT_F32, LF_SQRT_F32);
        addToMap(BF_SQRT_F64, LF_SQRT_F64);
        addToMap(BF_TRUNC_F32, LF_TRUNC_F32);
        addToMap(BF_TRUNC_F64, LF_TRUNC_F64);
    }

    @Override
    public Optional<Expression> getInlineExpression(final Function function, final List<Expression> args) {
        final var identifier = function.getIdentifier();

        if (BF_F32_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToFloatExpression(args.getFirst(), F32.INSTANCE));
        } else if (BF_F32_I32.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToFloatExpression(args.getFirst(), F32.INSTANCE));
        } else if (BF_F32_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToFloatExpression(args.getFirst(), F32.INSTANCE));
        } else if (BF_F64_F32.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToFloatExpression(args.getFirst(), F64.INSTANCE));
        } else if (BF_F64_I32.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToFloatExpression(args.getFirst(), F64.INSTANCE));
        } else if (BF_F64_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToFloatExpression(args.getFirst(), F64.INSTANCE));
        } else if (BF_I32_F32.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToIntExpression(args.getFirst(), I32.INSTANCE));
        } else if (BF_I32_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToIntExpression(args.getFirst(), I32.INSTANCE));
        } else if (BF_I32_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToIntExpression(args.getFirst(), I32.INSTANCE));
        } else if (BF_I64_F32.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToIntExpression(args.getFirst(), I64.INSTANCE));
        } else if (BF_I64_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToIntExpression(args.getFirst(), I64.INSTANCE));
        } else if (BF_I64_I32.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToIntExpression(args.getFirst(), I64.INSTANCE));
        } else if (BF_PRINTLN_BOOL.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        } else if (BF_PRINTLN_F32.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        } else if (BF_PRINTLN_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        } else if (BF_PRINTLN_I32.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        } else if (BF_PRINTLN_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        } else if (BF_PRINTLN_I64_TO_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        }

        return Optional.empty();
    }

    @Override
    public Function getLibraryFunction(final Function function) {
        final var identifier = function.getIdentifier();
        final var lf = map.get(identifier);
        if (lf != null) {
            return lf;
        }
        throw new IllegalArgumentException("unknown built-in function: " + function);
    }

    private void addToMap(final Function bf, final Function lf) {
        map.put(bf.getIdentifier(), lf);
    }
}
