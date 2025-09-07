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

package se.dykstrom.jcc.llvm.operation;

import se.dykstrom.jcc.llvm.operand.LlvmOperand;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.llvm.LlvmOperator.GETELEMENTPTR;

public record GetElementPtrOperation(LlvmOperand result, LlvmOperand base, LlvmOperand... indices) implements LlvmOperation {

    @Override
    public String toText() {
        return result.toText() + " = " +
                GETELEMENTPTR.toText() + " " +
                result.type().llvmName() + ", " +
                "ptr " + base.toText() + ", " +
                toText(indices);
    }

    private String toText(final LlvmOperand[] indices) {
        return Arrays.stream(indices)
                .map(i -> i.type().llvmName() + " " + i.toText())
                .collect(joining(", "));
    }

    @Override
    public String toString() {
        return toText();
    }
}
