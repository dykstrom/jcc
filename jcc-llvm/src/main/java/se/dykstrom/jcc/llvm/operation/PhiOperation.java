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

import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.llvm.LlvmOperator.PHI;

public record PhiOperation(TempOperand result, List<LlvmOperand> values, List<Label> labels) implements LlvmOperation {

    public PhiOperation {
        requireNonNull(result);
        if (values.size() != labels.size()) {
            throw new IllegalArgumentException("number of values and labels differ");
        }
    }

    @Override
    public String toText() {
        return result.toText() + " = " +
                PHI.toText() + " " +
                result.type().llvmName() + " " +
                toText(values, labels);
    }

    private String toText(final List<LlvmOperand> values, final List<Label> labels) {
        final var pairs = new ArrayList<String>();
        for (int i = 0; i < values.size(); i++) {
            pairs.add("[ " + values.get(i).toText() + ", %" + labels.get(i).getName() + " ]");
        }
        return String.join(", ", pairs);
    }

    @Override
    public String toString() {
        return toText();
    }
}
