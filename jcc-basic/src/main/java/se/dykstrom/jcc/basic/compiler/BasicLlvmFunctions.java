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

import se.dykstrom.jcc.common.ast.AbsExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.llvm.code.LlvmFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.*;
import static se.dykstrom.jcc.basic.compiler.BasicSymbols.*;

/**
 * Maps built-in functions to inlinable expressions and library functions
 * during LLVM IR code generation for the BASIC language.
 */
public final class BasicLlvmFunctions implements LlvmFunctions {

    private final Map<Identifier, Function> map = new HashMap<>();

    public BasicLlvmFunctions() {
        addToMap(BF_ABS_F64, LF_ABS_F64);
        addToMap(BF_SQR_F64, LF_SQRT_F64);
    }

    @Override
    public Optional<Expression> getInlineExpression(final Function function, final List<Expression> args) {
        final var identifier = function.getIdentifier();

        if (BF_ABS_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new AbsExpression(args.getFirst(), LF_ABS_I64));
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
