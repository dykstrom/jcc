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

import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.functions.ReferenceFunction;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Varargs;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.llvm.LlvmOperator.CALL;

public record CallOperation(TempOperand result, Function function, List<LlvmOperand> args) implements LlvmOperation {

    public CallOperation {
        requireNonNull(result);
        requireNonNull(function);
        requireNonNull(args);
    }

    @Override
    public String toText() {
        return result.toText() + " = " +
                CALL.toText() + " " +
                function.getReturnType().llvmName() + " " +
                argTypesIfVarargs(function.getArgTypes()) +
                prefix() + callee() + "(" +
                toText(args) + ")";
    }

    @Override
    public String toString() {
        return toText();
    }

    private String argTypesIfVarargs(final List<Type> types) {
        if (types.contains(Varargs.INSTANCE)) {
            return function.getArgTypes().stream()
                           .map(Type::llvmName)
                           .collect(joining(", ", "(", ")")) + " ";
        } else {
            return "";
        }
    }

    private String toText(final List<LlvmOperand> args) {
        return args.stream()
                   .map(o -> {
                       final Type type = o.type();
                       return type.llvmName() + " " + o.toText();
                   })
                   .collect(joining(", "));
    }

    private String prefix() {
        if (function instanceof ReferenceFunction) {
            // No prefix for reference functions, because they are
            // already prefixed with % in FunctionCallCodeGenerator
            return "";
        } else {
            return "@";
        }
    }

    public String callee() {
        if (function instanceof LibraryFunction lf) {
            return lf.externalName();
        } else {
            return function.mangledName();
        }
    }
}
