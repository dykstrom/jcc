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

import se.dykstrom.jcc.common.types.Void;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;

import static se.dykstrom.jcc.llvm.LlvmOperator.RET;

public record ReturnOperation(LlvmOperand source) implements LlvmOperation {

    public ReturnOperation() {
        this(null);
    }

    @Override
    public String toText() {
        if (source != null) {
            return RET.toText() + " " +
                   source.type().llvmName() + " " +
                   source.toText();
        } else {
            return RET.toText() + " " +
                   Void.INSTANCE.llvmName();
        }
    }

    @Override
    public String toString() {
        return toText();
    }
}
