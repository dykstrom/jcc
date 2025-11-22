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
import static se.dykstrom.jcc.basic.functions.LibJccBasBuiltIns.*;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.*;
import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.*;

/**
 * Maps built-in functions to inlinable expressions and library functions
 * during LLVM IR code generation for the BASIC language.
 */
public final class BasicLlvmFunctions implements LlvmFunctions {

    private final Map<Identifier, Function> map = new HashMap<>();

    public BasicLlvmFunctions() {
        addToMap(BF_ABS_F64, LF_ABS_F64);
        // The LLVM intrinsic LF_ATAN_F64 is not recognized by all versions of LLVM
        addToMap(BF_ATN_F64, CF_ATAN_F64);
        addToMap(BF_CHR_I64, JF_CHR_I64);
        addToMap(BF_COMMAND, JF_COMMAND);
        addToMap(BF_COS_F64, LF_COS_F64);
        addToMap(BF_CSRLIN, JF_CSRLIN);
        addToMap(BF_CVD_STR, JF_CVD_STR);
        addToMap(BF_CVI_STR, JF_CVI_STR);
        addToMap(BF_DATE, JF_DATE);
        addToMap(BF_EXP_F64, LF_EXP_F64);
        addToMap(BF_FIX_F64, LF_TRUNC_F64);
        addToMap(BF_HEX_I64, JF_HEX_I64);
        addToMap(BF_INKEY, JF_INKEY);
        addToMap(BF_INSTR_I64_STR_STR, JF_INSTR_I64_STR_STR);
        addToMap(BF_INSTR_STR_STR, JF_INSTR_STR_STR);
        addToMap(BF_LBOUND_ARR, JF_LBOUND_ARR);
        addToMap(BF_LBOUND_ARR_I64, JF_LBOUND_ARR_I64);
        addToMap(BF_LCASE_STR, JF_LCASE_STR);
        addToMap(BF_LEFT_STR_I64, JF_LEFT_STR_I64);
        addToMap(BF_LEN_STR, CF_STRLEN_STR);
        addToMap(BF_LOG_F64, LF_LOG_F64);
        addToMap(BF_LTRIM_STR, JF_LTRIM_STR);
        addToMap(BF_MID_STR_I64, JF_MID_STR_I64);
        addToMap(BF_MID_STR_I64_I64, JF_MID_STR_I64_I64);
        addToMap(BF_MKD_F64, JF_MKD_F64);
        addToMap(BF_MKI_I64, JF_MKI_I64);
        addToMap(BF_OCT_I64, JF_OCT_I64);
        addToMap(BF_POS_I64, JF_POS_I64);
        addToMap(BF_RIGHT_STR_I64, JF_RIGHT_STR_I64);
        addToMap(BF_RND, JF_RND);
        addToMap(BF_RND_F64, JF_RND_F64);
        addToMap(BF_RTRIM_STR, JF_RTRIM_STR);
        addToMap(BF_SGN_F64, JF_SGN_F64);
        addToMap(BF_SIN_F64, LF_SIN_F64);
        addToMap(BF_SPACE_I64, JF_SPACE_I64);
        addToMap(BF_SQR_F64, LF_SQRT_F64);
        addToMap(BF_STRING_I64_I64, JF_STRING_I64_I64);
        addToMap(BF_STRING_I64_STR, JF_STRING_I64_STR);
        addToMap(BF_STR_F64, JF_STR_F64);
        addToMap(BF_STR_I64, JF_STR_I64);
        addToMap(BF_TAN_F64, LF_TAN_F64);
        addToMap(BF_TIME, JF_TIME);
        addToMap(BF_TIMER, JF_TIMER);
        addToMap(BF_UBOUND_ARR, JF_UBOUND_ARR);
        addToMap(BF_UBOUND_ARR_I64, JF_UBOUND_ARR_I64);
        addToMap(BF_UCASE_STR, JF_UCASE_STR);
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
