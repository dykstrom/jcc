/*
 * Copyright (C) 2024 Johan Dykstrom
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

package se.dykstrom.jcc.llvm.code.expression;

import se.dykstrom.jcc.common.ast.CastToFloatExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.NumericType;
import se.dykstrom.jcc.llvm.LlvmOperator;
import se.dykstrom.jcc.llvm.LlvmUtils;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.ConvertOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.compiler.TypeManager.isFloat;
import static se.dykstrom.jcc.llvm.LlvmOperator.*;

public class CastToFloatCodeGenerator implements LlvmExpressionCodeGenerator<CastToFloatExpression> {

    private final LlvmCodeGenerator codeGenerator;

    public CastToFloatCodeGenerator(final LlvmCodeGenerator codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    @Override
    public LlvmOperand toLlvm(final CastToFloatExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var sourceType = (NumericType) codeGenerator.typeManager().getType(expression.getExpression());
        final var destinationType = (NumericType) expression.getType();

        final var sourceBits = sourceType.bits();
        final var destinationBits = destinationType.bits();

        final var opSource = codeGenerator.expression(expression.getExpression(), lines, symbolTable);
        final LlvmOperator operator;
        if (sourceBits < destinationBits) {
            // Floating point extend, or signed integer to floating point
            operator = LlvmUtils.typeToOperator(sourceType, FPEXT, SITOFP);
        } else if (sourceBits == destinationBits) {
            if (isFloat(sourceType)) {
                // If the source is a float as well, this is a no-op
                return opSource;
            } else {
                // Signed integer to floating point
                operator = SITOFP;
            }
        } else {
            // Floating point truncate, or signed integer to floating point
            operator = LlvmUtils.typeToOperator(sourceType, FPTRUNC, SITOFP);
        }
        final var opDestination = new TempOperand(symbolTable.nextTempName(), destinationType);
        lines.add(new ConvertOperation(opSource, operator, opDestination));
        return opDestination;
    }
}
