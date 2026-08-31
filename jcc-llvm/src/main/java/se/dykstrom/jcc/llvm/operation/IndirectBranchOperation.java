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

import se.dykstrom.jcc.llvm.code.Label;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;

import java.util.List;

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.llvm.LlvmOperator.INDIRECTBR;

public record IndirectBranchOperation(LlvmOperand address, List<Label> destinations) implements LlvmOperation {

    @Override
    public String toText() {
        return INDIRECTBR.toText() + " " +
                address.type().llvmName() + " " +
                address.toText() + ", " +
                "[" + toText(destinations) + "]";
    }

    private String toText(final List<Label> destinations) {
        return destinations.stream()
                .map(l -> "label %" + l.getMappedName())
                .collect(joining(", "));
    }
}
