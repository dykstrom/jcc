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

import se.dykstrom.jcc.llvm.LlvmOperator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;

import static java.util.Objects.requireNonNull;

public record BinaryOperation(TempOperand result,
                              LlvmOperator operator,
                              LlvmOperand left,
                              LlvmOperand right,
                              String... flags) implements LlvmOperation {

    public BinaryOperation {
        requireNonNull(result);
        requireNonNull(operator);
        requireNonNull(left);
        requireNonNull(right);
        requireNonNull(flags);
    }

    public BinaryOperation(TempOperand result,
                           LlvmOperator operator,
                           LlvmOperand left,
                           LlvmOperand right) {
        this(result, operator, left, right, new String[0]);
    }

    @Override
    public String toText() {
        return result.toText() + " = " +
                operator.toText() + " " +
                (flags.length > 0 ? String.join(" ", flags) + " " : "") +
                left.type().llvmName() + " " +
                left.toText() + ", " +
                right.toText();
    }

    @Override
    public String toString() {
        return toText();
    }
}
