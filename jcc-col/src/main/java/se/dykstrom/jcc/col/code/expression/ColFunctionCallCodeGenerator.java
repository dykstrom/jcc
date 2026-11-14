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

package se.dykstrom.jcc.col.code.expression;

import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I128;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.llvm.code.Comment;
import se.dykstrom.jcc.llvm.code.GcCodeGenerator;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.FunctionCallCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.llvm.operation.StoreOperation;

import java.util.List;

import static se.dykstrom.jcc.col.compiler.ColSymbols.BF_STRING_I128;
import static se.dykstrom.jcc.col.compiler.LibJccColBuiltIns.JF_STRING_I128_PTR;

/**
 * COL specific class that calls string(i128) the way the C ABI of Windows x86-64 requires.
 * There, a C function takes an __int128 argument as a pointer to the value, while an i128
 * argument in LLVM IR goes in two registers. If {@code i128ByPointer} is true, the argument
 * is stored in a stack slot, and the slot is passed instead. All other calls are generated
 * by the default code generator.
 */
public record ColFunctionCallCodeGenerator(LlvmCodeGenerator lcg,
                                           FunctionCallCodeGenerator fcg,
                                           GcCodeGenerator gc,
                                           boolean i128ByPointer)
        implements LlvmExpressionCodeGenerator<FunctionCallExpression> {

    /** The name of the stack slot. The leading dot keeps it from colliding with a user identifier. */
    private static final Identifier SLOT = new Identifier(".i128.arg", I128.INSTANCE);

    @Override
    public LlvmOperand toLlvm(final FunctionCallExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        return i128ByPointer && BF_STRING_I128.equals(expression.function()) ?
                stringOfSlot(expression, lines, symbolTable) :
                fcg.toLlvm(expression, lines, symbolTable);
    }

    private LlvmOperand stringOfSlot(final FunctionCallExpression e, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new Comment(e.toString()));
        final var opArg = lcg.expression(e.getArgs().getFirst(), lines, symbolTable);
        // One slot per function is enough, because nothing is evaluated between the store and
        // the call. The enclosing function allocates it on entry with its other locals, so a
        // call in a loop does not grow the stack.
        if (!symbolTable.localIdentifiers().contains(SLOT)) {
            symbolTable.addVariable(SLOT, null);
        }
        final var opSlot = new TempOperand(symbolTable.mapName(SLOT), Ptr.INSTANCE);
        lines.add(new StoreOperation(opArg, opSlot));
        final var opResult = new TempOperand(symbolTable.nextTempName(), JF_STRING_I128_PTR.getReturnType());
        lines.add(new CallOperation(opResult, JF_STRING_I128_PTR, List.of(opSlot)));
        // col_string_i128 returns a freshly malloc'd string, like every other string conversion
        return gc.registerResult(opResult, lines, symbolTable);
    }
}
