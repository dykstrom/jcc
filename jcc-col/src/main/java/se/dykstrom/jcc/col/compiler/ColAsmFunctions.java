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
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static se.dykstrom.jcc.col.compiler.ColSymbols.*;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.*;

/**
 * This class maps a built-in function definition to an expression
 * or a library function. This class targets the specific combination
 * of directly generated assembly code for the COL language. The methods
 * in this class are called during code generation. How the built-in
 * functions are implemented does not affect the semantic analysis.
 */
public final class ColAsmFunctions {

    /** Builds an inline expression for a built-in function from its arguments. */
    @FunctionalInterface
    private interface InlineBuilder {
        Expression build(List<Expression> args);
    }

    private static final Map<Identifier, InlineBuilder> INLINE_MAP = new HashMap<>();

    static {
        addToInlineMap(BF_F64_I32, args -> new CastToF64Expression(args.getFirst()));
        addToInlineMap(BF_F64_I64, args -> new CastToF64Expression(args.getFirst()));
        addToInlineMap(BF_I32_F64, args -> new CastToI32Expression(args.getFirst()));
        addToInlineMap(BF_I32_I64, args -> new CastToI32Expression(args.getFirst()));
        addToInlineMap(BF_I64_F64, args -> new CastToI64Expression(args.getFirst()));
        addToInlineMap(BF_I64_I32, args -> new CastToI64Expression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_BOOL, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_F64, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_I32, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_I64, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_PRINTLN_I64_TO_I64, args -> new PrintlnExpression(args.getFirst()));
        addToInlineMap(BF_ROUND_F64, args -> new RoundExpression(args.getFirst(), null));
        addToInlineMap(BF_SQRT_F64, args -> new SqrtExpression(args.getFirst()));
        addToInlineMap(BF_TRUNC_F64, args -> new TruncExpression(args.getFirst()));
    }

    private ColAsmFunctions() { }

    /**
     * Returns an optional expression that can be used to inline
     * a call to the given function.
     */
    public static Optional<Expression> getInlineExpression(final Function function, final List<Expression> args) {
        final var builder = INLINE_MAP.get(function.getIdentifier());
        return Optional.ofNullable(builder).map(b -> b.build(args));
    }

    /**
     * Returns the library function that implements the given built-in function.
     */
    public static Function getLibraryFunction(final Function function) {
        final var identifier = function.getIdentifier();

        if (BF_CEIL_F64.getIdentifier().equals(identifier)) {
            return CF_CEIL_F64;
        } else if (BF_FLOOR_F64.getIdentifier().equals(identifier)) {
            return CF_FLOOR_F64;
        }

        throw new IllegalArgumentException("unknown built-in function: " + function);
    }

    private static void addToInlineMap(final Function bf, final InlineBuilder builder) {
        INLINE_MAP.put(bf.getIdentifier(), builder);
    }
}
