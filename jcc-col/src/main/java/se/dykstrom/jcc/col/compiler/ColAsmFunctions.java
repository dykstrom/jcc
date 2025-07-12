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

import java.util.List;
import java.util.Optional;

import static se.dykstrom.jcc.col.compiler.ColFunctions.*;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.*;

/**
 * This class maps a built-in function definition to an expression
 * or a library function. This class targets the specific combination
 * of directly generated assembly code for the COL language. The methods
 * in this class are called during code generation. How the built-in
 * functions are implemented does not affect the semantic analysis.
 */
public final class ColAsmFunctions {

    private ColAsmFunctions() { }

    /**
     * Returns an optional expression that can be used to inline
     * a call to the given function.
     */
    public static Optional<Expression> getInlineExpression(final Function function, final List<Expression> args) {
        final var identifier = function.getIdentifier();

        if (BF_F64_I32.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToF64Expression(args.getFirst()));
        } else if (BF_F64_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToF64Expression(args.getFirst()));
        } else if (BF_I32_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToI32Expression(args.getFirst()));
        } else if (BF_I32_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToI32Expression(args.getFirst()));
        } else if (BF_I64_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToI64Expression(args.getFirst()));
        } else if (BF_I64_I32.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToI64Expression(args.getFirst()));
        } else if (BF_PRINTLN_BOOL.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        } else if (BF_PRINTLN_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        } else if (BF_PRINTLN_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        } else if (BF_PRINTLN_I64_TO_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new PrintlnExpression(args.getFirst()));
        } else if (BF_ROUND_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new RoundExpression(args.getFirst()));
        } else if (BF_SQRT_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new SqrtExpression(args.getFirst()));
        } else if (BF_TRUNC_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new TruncExpression(args.getFirst()));
        }

        return Optional.empty();
    }

    /**
     * Returns the library function that implements the given built-in function.
     */
    public static Function getLibraryFunction(final Function function) {
        final var identifier = function.getIdentifier();

        if (BF_CEIL_F64.getIdentifier().equals(identifier)) {
            return LF_CEIL_F64;
        } else if (BF_FLOOR_F64.getIdentifier().equals(identifier)) {
            return LF_FLOOR_F64;
        }

        throw new IllegalArgumentException("unknown built-in function: " + function);
    }
}
