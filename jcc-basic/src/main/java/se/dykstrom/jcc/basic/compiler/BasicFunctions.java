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
import se.dykstrom.jcc.basic.ast.expression.LboundExpression;
import se.dykstrom.jcc.basic.ast.expression.UboundExpression;
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
import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.*;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.*;
import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.*;

/**
 * Maps built-in functions to inlinable expressions and library functions
 * during LLVM IR code generation for the BASIC language.
 */
public final class BasicFunctions implements LlvmFunctions {

    /** Builds an inline expression for a built-in function from its arguments. */
    @FunctionalInterface
    private interface InlineBuilder {
        Expression build(List<Expression> args);
    }

    private final Map<Identifier, Function> libraryMap = new HashMap<>();
    private final Map<Identifier, InlineBuilder> inlineMap = new HashMap<>();

    public BasicFunctions() {
        addToLibraryMap(BF_ABS_F64, LF_ABS_F64);
        addToLibraryMap(BF_ATN_F64, LF_ATAN_F64);
        addToLibraryMap(BF_CHR_I64, JF_CHR_I64);
        addToLibraryMap(BF_COMMAND, JF_COMMAND);
        addToLibraryMap(BF_COS_F64, LF_COS_F64);
        addToLibraryMap(BF_CSRLIN, JF_CSRLIN);
        addToLibraryMap(BF_CVD_STR, JF_CVD_STR);
        addToLibraryMap(BF_CVI_STR, JF_CVI_STR);
        addToLibraryMap(BF_DATE, JF_DATE);
        addToLibraryMap(BF_EXP_F64, LF_EXP_F64);
        addToLibraryMap(BF_HEX_I64, JF_HEX_I64);
        addToLibraryMap(BF_INKEY, JF_INKEY);
        addToLibraryMap(BF_INSTR_I64_STR_STR, JF_INSTR_I64_STR_STR);
        addToLibraryMap(BF_INSTR_STR_STR, JF_INSTR_STR_STR);
        addToLibraryMap(BF_LCASE_STR, JF_LCASE_STR);
        addToLibraryMap(BF_LEFT_STR_I64, JF_LEFT_STR_I64);
        addToLibraryMap(BF_LEN_STR, CF_STRLEN_STR);
        addToLibraryMap(BF_LOG_F64, LF_LOG_F64);
        addToLibraryMap(BF_LTRIM_STR, JF_LTRIM_STR);
        addToLibraryMap(BF_MID_STR_I64, JF_MID_STR_I64);
        addToLibraryMap(BF_MID_STR_I64_I64, JF_MID_STR_I64_I64);
        addToLibraryMap(BF_MKD_F64, JF_MKD_F64);
        addToLibraryMap(BF_MKI_I64, JF_MKI_I64);
        addToLibraryMap(BF_OCT_I64, JF_OCT_I64);
        addToLibraryMap(BF_POS_I64, JF_POS_I64);
        addToLibraryMap(BF_RIGHT_STR_I64, JF_RIGHT_STR_I64);
        addToLibraryMap(BF_RND, JF_RND);
        addToLibraryMap(BF_RND_F64, JF_RND_F64);
        addToLibraryMap(BF_RTRIM_STR, JF_RTRIM_STR);
        addToLibraryMap(BF_SGN_F64, JF_SGN_F64);
        addToLibraryMap(BF_SIN_F64, LF_SIN_F64);
        addToLibraryMap(BF_SPACE_I64, JF_SPACE_I64);
        addToLibraryMap(BF_SQR_F64, LF_SQRT_F64);
        addToLibraryMap(BF_STRING_I64_I64, JF_STRING_I64_I64);
        addToLibraryMap(BF_STRING_I64_STR, JF_STRING_I64_STR);
        addToLibraryMap(BF_STR_F64, JF_STR_F64);
        addToLibraryMap(BF_STR_I64, JF_STR_I64);
        addToLibraryMap(BF_TAN_F64, LF_TAN_F64);
        addToLibraryMap(BF_TIME, JF_TIME);
        addToLibraryMap(BF_TIMER, JF_TIMER);
        addToLibraryMap(BF_UCASE_STR, JF_UCASE_STR);
        addToLibraryMap(BF_VAL_STR, CF_ATOF_STR);

        addToInlineMap(BF_ABS_I64, args -> new AbsExpression(args.getFirst(), LF_ABS_I64));
        addToInlineMap(BF_ASC_STR, args -> new AscExpression(args.getFirst()));
        addToInlineMap(BF_CDBL_F64, List::getFirst); // NOP
        addToInlineMap(BF_CDBL_I64, args -> new CastToFloatExpression(args.getFirst(), F64.INSTANCE));
        // CINT rounds half-to-even (QuickBASIC 4.5), so use llvm.roundeven, not llvm.round (issue #52)
        addToInlineMap(BF_CINT_F64, args -> new CastToIntExpression(new RoundExpression(args.getFirst(), LF_ROUNDEVEN_F64), I64.INSTANCE));
        addToInlineMap(BF_CINT_I64, List::getFirst); // NOP
        // FIX truncates toward zero, INT rounds toward negative infinity
        addToInlineMap(BF_FIX_F64, args -> new CastToIntExpression(new RoundExpression(args.getFirst(), LF_TRUNC_F64), I64.INSTANCE));
        addToInlineMap(BF_INT_F64, args -> new CastToIntExpression(new RoundExpression(args.getFirst(), LF_FLOOR_F64), I64.INSTANCE));
        // LBOUND/UBOUND are lowered inline from the array's compile-time metadata; the libjccbas
        // lbound/ubound symbols are never called, so LibJccBasBuiltIns declares no constants for them.
        addToInlineMap(BF_LBOUND_ARR, args -> new LboundExpression((IdentifierExpression) args.getFirst()));
        addToInlineMap(BF_LBOUND_ARR_I64, args -> new LboundExpression((IdentifierExpression) args.getFirst()));
        addToInlineMap(BF_UBOUND_ARR, args -> new UboundExpression((IdentifierExpression) args.getFirst(), IntegerLiteral.ONE));
        addToInlineMap(BF_UBOUND_ARR_I64, args -> new UboundExpression((IdentifierExpression) args.getFirst(), args.get(1)));
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
