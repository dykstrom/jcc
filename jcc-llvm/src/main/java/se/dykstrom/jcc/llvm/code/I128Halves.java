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

package se.dykstrom.jcc.llvm.code;

import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I128;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Opaque;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.llvm.CallingConvention;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.ConvertOperation;

import java.util.ArrayList;
import java.util.List;

import static se.dykstrom.jcc.llvm.LlvmOperator.LSHR;
import static se.dykstrom.jcc.llvm.LlvmOperator.OR;
import static se.dykstrom.jcc.llvm.LlvmOperator.SHL;
import static se.dykstrom.jcc.llvm.LlvmOperator.TRUNC;
import static se.dykstrom.jcc.llvm.LlvmOperator.ZEXT;

/**
 * Passes an i128 to a tailcc function as two i64 values, the low half first. Clang miscompiles
 * an i128 argument of a tailcc call on some platforms, see ADR 0019.
 */
public final class I128Halves {

    private static final LlvmOperand OP_64 = new LiteralOperand(64, I128.INSTANCE);

    private I128Halves() { }

    /**
     * Returns true if a parameter of the given type is passed to the given function as two halves.
     */
    public static boolean isSplit(final Function function, final Type type) {
        return CallingConvention.of(function) == CallingConvention.TAIL && Opaque.unwrap(type) instanceof I128;
    }

    /**
     * Returns the arguments to pass to the given function, with every i128 argument replaced by
     * its two halves.
     */
    public static List<LlvmOperand> split(final Function function,
                                          final List<LlvmOperand> args,
                                          final List<Line> lines,
                                          final SymbolTable symbolTable) {
        final var result = new ArrayList<LlvmOperand>();
        for (final var arg : args) {
            if (isSplit(function, arg.type())) {
                final var opLow = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
                lines.add(new ConvertOperation(arg, TRUNC, opLow));
                final var opShifted = new TempOperand(symbolTable.nextTempName(), I128.INSTANCE);
                lines.add(new BinaryOperation(opShifted, LSHR, arg, OP_64));
                final var opHigh = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
                lines.add(new ConvertOperation(opShifted, TRUNC, opHigh));
                result.add(opLow);
                result.add(opHigh);
            } else {
                result.add(arg);
            }
        }
        return result;
    }

    /**
     * Returns an i128 operand built from the given halves.
     */
    public static LlvmOperand join(final LlvmOperand opLow,
                                   final LlvmOperand opHigh,
                                   final List<Line> lines,
                                   final SymbolTable symbolTable) {
        final var opLowWide = new TempOperand(symbolTable.nextTempName(), I128.INSTANCE);
        lines.add(new ConvertOperation(opLow, ZEXT, opLowWide));
        final var opHighWide = new TempOperand(symbolTable.nextTempName(), I128.INSTANCE);
        lines.add(new ConvertOperation(opHigh, ZEXT, opHighWide));
        final var opShifted = new TempOperand(symbolTable.nextTempName(), I128.INSTANCE);
        lines.add(new BinaryOperation(opShifted, SHL, opHighWide, OP_64));
        final var opResult = new TempOperand(symbolTable.nextTempName(), I128.INSTANCE);
        lines.add(new BinaryOperation(opResult, OR, opShifted, opLowWide));
        return opResult;
    }
}
