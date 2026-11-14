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

package se.dykstrom.jcc.llvm.operation;

import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.llvm.CallingConvention;
import se.dykstrom.jcc.llvm.operand.TempOperand;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.llvm.LlvmOperator.DEFINE;

/**
 * Defines a function. The operands are the parameters as they are passed, so an i128 parameter
 * of a tailcc function is two i64 operands, see {@code I128Halves}.
 */
public record DefineOperation(UserDefinedFunction function, List<TempOperand> operands) implements LlvmOperation {

    public DefineOperation {
        requireNonNull(function);
        requireNonNull(operands);
    }

    @Override
    public String toText() {
        return DEFINE.toText() + " " +
               CallingConvention.of(function).toText() +
               function.getReturnType().llvmName() + " " +
               "@" + function.mangledName() + "(" +
               operands.stream().map(o -> o.type().llvmName() + " " + o.toText()).collect(joining(", ")) +
               ") {";
    }

    @Override
    public String toString() {
        return toText();
    }
}
