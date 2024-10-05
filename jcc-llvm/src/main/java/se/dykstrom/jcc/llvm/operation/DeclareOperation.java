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

import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.llvm.LlvmOperator.DECLARE;

public record DeclareOperation(LibraryFunction function) implements LlvmOperation {

    @Override
    public String toText() {
        return DECLARE.toText() + " " +
               function.getReturnType().llvmName() + " " +
               "@" + function.externalName() + "(" +
               toText(function.getArgTypes()) +
               ")";
    }

    private String toText(final List<Type> types) {
        return types.stream().map(Type::llvmName).collect(joining(", "));
    }

    @Override
    public String toString() {
        return toText();
    }
}
