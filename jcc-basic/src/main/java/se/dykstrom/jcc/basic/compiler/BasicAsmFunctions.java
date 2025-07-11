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

package se.dykstrom.jcc.basic.compiler;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.SqrtExpression;
import se.dykstrom.jcc.common.functions.Function;

import java.util.List;
import java.util.Optional;

import static se.dykstrom.jcc.basic.compiler.BasicFunctions.*;
import static se.dykstrom.jcc.basic.functions.LibJccBasBuiltIns.LF_ASC_STR;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.*;

/**
 * This class maps a built-in function definition to an expression
 * or a library function. This class targets the specific combination
 * of directly generated assembly code for the BASIC language. The methods
 * in this class are called during code generation. How the built-in
 * functions are implemented does not affect the semantic analysis.
 */
public final class BasicAsmFunctions {

    private BasicAsmFunctions() { }

    /**
     * Returns an optional expression that can be used to inline
     * a call to the given function.
     */
    public static Optional<Expression> getInlineExpression(final Function function, final List<Expression> args) {
        final var identifier = function.getIdentifier();

        if (BF_SQR_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new SqrtExpression(args.get(0)));
        }

        return Optional.empty();
    }

    /**
     * Returns the library function that implements the given built-in function.
     */
    public static Function getLibraryFunction(final Function function) {
        final var identifier = function.getIdentifier();

        if (BF_ABS_F64.getIdentifier().equals(identifier)) {
            return LF_FABS_F64;
        } else if (BF_ABS_I64.getIdentifier().equals(identifier)) {
            return LF_ABS_I64;
        } else if (BF_ASC_STR.getIdentifier().equals(identifier)) {
            return LF_ASC_STR;
        } else if (BF_SQR_F64.getIdentifier().equals(identifier)) {
            return LF_SQRT_F64;
        }

        throw new IllegalArgumentException("unknown built-in function: " + function);
    }
}
