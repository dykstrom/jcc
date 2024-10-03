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
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.llvm.operand.TempOperand;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.llvm.LlvmOperator.DEFINE;

public record DefineOperation(UserDefinedFunction function, List<TempOperand> operands) implements LlvmOperation {

    public DefineOperation {
        requireNonNull(function);
        if (operands.size() != function.getArgTypes().size()) {
            throw new IllegalArgumentException("The number of operands (" + operands.size() +
                                               ") does not match the number of argument types (" +
                                               function.getArgTypes().size() + ")");
        }
    }

    @Override
    public String toText() {
        return DEFINE.toText() + " " +
               function.getReturnType().llvmName() + " " +
               "@" + function.getIdentifier().name() + "(" +
               toText(function.getArgTypes(), operands) +
               ") {";
    }

    private String toText(final List<Type> types, final List<TempOperand> operands) {
        final var builder = new StringBuilder();
        for (int i = 0; i < types.size(); i++) {
            builder.append(types.get(i).llvmName())
                   .append(" ")
                   .append(operands.get(i).toText());
            if (i < types.size() - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return toText();
    }
}
