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

package se.dykstrom.jcc.basic.code.expression;

import se.dykstrom.jcc.basic.ast.expression.SgnExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.ConvertOperation;

import java.util.List;

import static se.dykstrom.jcc.common.ast.FloatLiteral.FL_F64_0_0;
import static se.dykstrom.jcc.llvm.LlvmOperator.FCMP;
import static se.dykstrom.jcc.llvm.LlvmOperator.SUB;
import static se.dykstrom.jcc.llvm.LlvmOperator.ZEXT;

/**
 * Generates code for the inlined 'sgn' function.
 * <p>
 * SGN is computed as {@code (x > 0) - (x < 0)}: two compares, two zero extensions and a
 * subtract, with no branch and no call. NaN compares false both ways and so yields 0.
 * The argument is evaluated once, and both compares read that one result.
 */
public record SgnCodeGenerator(LlvmCodeGenerator cg) implements LlvmExpressionCodeGenerator<SgnExpression> {

    private static final LlvmOperand OP_ZERO = new LiteralOperand(FL_F64_0_0.getValue(), FL_F64_0_0.type());

    @Override
    public LlvmOperand toLlvm(final SgnExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new LlvmComment(expression.toString()));

        final var opValue = cg.expression(expression.expression(), lines, symbolTable);

        final var opPositive = compareToZero(opValue, "ogt", lines, symbolTable);
        final var opNegative = compareToZero(opValue, "olt", lines, symbolTable);

        final var opResult = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
        lines.add(new BinaryOperation(opResult, SUB, opPositive, opNegative));
        return opResult;
    }

    /**
     * Compares {@code opValue} to zero using the given comparison flag, and extends the
     * boolean result to an i64.
     */
    private LlvmOperand compareToZero(final LlvmOperand opValue,
                                      final String flag,
                                      final List<Line> lines,
                                      final SymbolTable symbolTable) {
        final var opCompared = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
        lines.add(new BinaryOperation(opCompared, FCMP, opValue, OP_ZERO, new String[]{flag}));

        final var opExtended = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
        lines.add(new ConvertOperation(opCompared, ZEXT, opExtended));
        return opExtended;
    }
}
