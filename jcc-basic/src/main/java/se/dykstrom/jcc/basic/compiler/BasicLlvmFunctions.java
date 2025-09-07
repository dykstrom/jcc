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

import se.dykstrom.jcc.basic.ast.expression.AscExpression;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.llvm.code.LlvmFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static se.dykstrom.jcc.basic.compiler.BasicSymbols.*;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_ATOF_STR;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_STRLEN_STR;
import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.*;

/**
 * Maps built-in functions to inlinable expressions and library functions
 * during LLVM IR code generation for the BASIC language.
 */
public final class BasicLlvmFunctions implements LlvmFunctions {

    private final Map<Identifier, Function> map = new HashMap<>();

    public BasicLlvmFunctions() {
        addToMap(BF_ABS_F64, LF_ABS_F64);
        addToMap(BF_ATN_F64, LF_ATAN_F64);
        addToMap(BF_COS_F64, LF_COS_F64);
        addToMap(BF_EXP_F64, LF_EXP_F64);
        addToMap(BF_FIX_F64, LF_TRUNC_F64);
        addToMap(BF_LEN_STR, CF_STRLEN_STR);
        addToMap(BF_LOG_F64, LF_LOG_F64);
        addToMap(BF_SIN_F64, LF_SIN_F64);
        addToMap(BF_SQR_F64, LF_SQRT_F64);
        addToMap(BF_TAN_F64, LF_TAN_F64);
        addToMap(BF_VAL_STR, CF_ATOF_STR);
    }

    @Override
    public Optional<Expression> getInlineExpression(final Function function, final List<Expression> args) {
        final var identifier = function.getIdentifier();

        if (BF_ABS_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new AbsExpression(args.getFirst(), LF_ABS_I64));
        } else if (BF_CDBL_F64.getIdentifier().equals(identifier)) {
            return Optional.of(args.getFirst()); // NOP
        } else if (BF_CDBL_I64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToFloatExpression(args.getFirst(), F64.INSTANCE));
        } else if (BF_CINT_F64.getIdentifier().equals(identifier)) {
            return Optional.of(new CastToIntExpression(new RoundExpression(args.getFirst(), LF_ROUND_F64), I64.INSTANCE));
        } else if (BF_CINT_I64.getIdentifier().equals(identifier)) {
            return Optional.of(args.getFirst()); // NOP
        } else if (BF_ASC_STR.getIdentifier().equals(identifier)) {
            return Optional.of(new AscExpression(args.getFirst()));
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
