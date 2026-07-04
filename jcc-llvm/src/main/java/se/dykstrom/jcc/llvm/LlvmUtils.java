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

package se.dykstrom.jcc.llvm;

import se.dykstrom.jcc.common.ast.ArrayAccessExpression;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.BranchOperation;
import se.dykstrom.jcc.llvm.operation.GetElementPtrOperation;
import se.dykstrom.jcc.llvm.operation.IndirectBranchOperation;

import java.util.List;

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateIntegerExpressions;

public final class LlvmUtils {

    private LlvmUtils() { }

    public static LlvmOperator typeToOperator(final Type type,
                                              final LlvmOperator fOperator,
                                              final LlvmOperator iOperator) {
        if (type instanceof F32 || type instanceof F64) {
            return fOperator;
        } else if (type instanceof Bool || type instanceof I32 || type instanceof I64) {
            return iOperator;
        } else {
            throw new IllegalArgumentException("unknown type: " + type.getName());
        }
    }

    /**
     * Adds a printf format string to the symbol table for the given type,
     * and returns an identifier to identify the global variable that will
     * be the result.
     */
    public static Identifier getCreateFormatIdentifier(final Type type,
                                                       final SymbolTable symbolTable,
                                                       final boolean eol) {
        return getCreateFormatIdentifier(List.of(type), symbolTable, eol);
    }

    /**
     * Adds a printf format string to the symbol table for the given types,
     * and returns an identifier to identify the global variable that will
     * be the result.
     */
    public static Identifier getCreateFormatIdentifier(final List<Type> types,
                                                       final SymbolTable symbolTable,
                                                       final boolean eol) {
        final var formatStr = types.stream().map(Type::getFormat).collect(joining()) + (eol ? "\n" : "");
        final var formatName = clean(".printf.fmt." + types.stream().map(Type::toString).collect(joining("."))) + (eol ? ".nl" : "");
        final var identifier = new Identifier(formatName, Str.INSTANCE);
        if (!symbolTable.contains(identifier.name())) {
            symbolTable.addConstant(new Constant(identifier, formatStr));
        }
        return identifier;
    }

    private static String clean(final String s) {
        return s.replace("(", "lp.")
                .replace(")", ".rp.")
                .replace("->", "to.");
    }

    /**
     * Adds a branch to {@code label} if the list of lines does not end with a branch already.
     */
    public static void addBranchIfNeeded(final List<Line> lines, final Label label) {
        if (endsWithBranch(lines)) {
            lines.add(new LlvmComment("Suppress branch to " + label.getName()));
        } else {
            lines.add(new BranchOperation(label));
        }
    }

    private static boolean endsWithBranch(final List<Line> lines) {
        if (lines.isEmpty()) {
            return false;
        }
        final var last = lines.getLast();
        return last instanceof BranchOperation || last instanceof IndirectBranchOperation;
    }

    /**
     * Computes the address of the element referenced by {@code expression} and returns an operand
     * pointing at it. The flat element index is computed with the same multiply-accumulate scheme as
     * the FASM backend, then the element address is obtained with a {@code getelementptr} into the
     * array's {@code [N x T]} global. Shared by array-element reads, assignments, and SWAP.
     */
    public static LlvmOperand arrayElementAddress(final LlvmCodeGenerator cg,
                                                  final ArrayAccessExpression expression,
                                                  final List<Line> lines,
                                                  final SymbolTable symbolTable) {
        final var arrayIdentifier = expression.getIdentifier();
        final var elementType = ((Arr) arrayIdentifier.type()).getElementType();
        final var subscripts = expression.getSubscripts();

        // Dimension sizes are the inclusive-adjusted declaration subscripts (constant expressions).
        final var storedSubscripts = symbolTable.getArrayValue(arrayIdentifier.name()).getSubscripts();
        final List<Long> sizes = evaluateIntegerExpressions(storedSubscripts, symbolTable, cg.optimizer().expressionOptimizer());

        // Flat index = sub[0]; for i >= 1: index = index * size[i] + sub[i]
        LlvmOperand opIndex = cg.expression(subscripts.getFirst(), lines, symbolTable);
        for (int i = 1; i < subscripts.size(); i++) {
            final var opMul = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
            lines.add(new BinaryOperation(opMul, LlvmOperator.MUL, opIndex, new LiteralOperand(sizes.get(i), I64.INSTANCE)));
            final var opSub = cg.expression(subscripts.get(i), lines, symbolTable);
            final var opAdd = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
            lines.add(new BinaryOperation(opAdd, LlvmOperator.ADD, opMul, opSub));
            opIndex = opAdd;
        }

        // Address of the element: getelementptr T, ptr @<array>, i64 <index>
        final var opBase = new TempOperand(symbolTable.mapName(arrayIdentifier), Ptr.INSTANCE);
        final var opAddress = new TempOperand(symbolTable.nextTempName(), elementType);
        lines.add(new GetElementPtrOperation(opAddress, opBase, opIndex));
        return opAddress;
    }
}
