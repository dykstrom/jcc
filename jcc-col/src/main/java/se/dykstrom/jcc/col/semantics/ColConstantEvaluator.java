/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.semantics;

import se.dykstrom.jcc.col.compiler.OpaqueConversionFunction;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.LiteralExpression;
import se.dykstrom.jcc.common.evaluation.ConstantEvaluator;
import se.dykstrom.jcc.common.evaluation.ConstantEvaluator.CallEvaluator;
import se.dykstrom.jcc.common.functions.Function;

import java.util.List;
import java.util.Set;

import static se.dykstrom.jcc.col.compiler.ColSymbols.*;

/**
 * Creates the {@link ConstantEvaluator} for COL. A COL comparison gives a bool. The numeric casts
 * are written as function calls, and an opaque conversion is the identity on the value.
 */
public final class ColConstantEvaluator {

    private static final Set<Function> CASTS = Set.of(
            BF_F32_F32, BF_F32_F64, BF_F32_I32, BF_F32_I64,
            BF_F64_F32, BF_F64_F64, BF_F64_I32, BF_F64_I64,
            BF_I32_F32, BF_I32_F64, BF_I32_I32, BF_I32_I64,
            BF_I64_F32, BF_I64_F64, BF_I64_I32, BF_I64_I64
    );

    private static final CallEvaluator CALL_EVALUATOR = new CallEvaluator() {
        @Override
        public boolean isConstant(final Function function) {
            return function instanceof OpaqueConversionFunction || CASTS.contains(function);
        }

        @Override
        public LiteralExpression evaluate(final FunctionCallExpression call, final List<LiteralExpression> args) {
            final var operand = args.getFirst();
            if (call.function() instanceof OpaqueConversionFunction) {
                return operand;
            }
            final var target = call.function().getReturnType();
            return target.isInteger()
                    ? ConstantEvaluator.castToInt(call, operand, target)
                    : ConstantEvaluator.castToFloat(call, operand, target);
        }
    };

    private ColConstantEvaluator() { }

    public static ConstantEvaluator create() {
        return new ConstantEvaluator(CALL_EVALUATOR, ConstantEvaluator::booleanLiteral);
    }
}
