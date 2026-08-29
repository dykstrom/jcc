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

package se.dykstrom.jcc.basic.code.statement;

import se.dykstrom.jcc.basic.ast.statement.SwapStatement;
import se.dykstrom.jcc.common.ast.ArrayAccessExpression;
import se.dykstrom.jcc.common.ast.CastToFloatExpression;
import se.dykstrom.jcc.common.ast.CastToIntExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.ast.IdentifierExpression;
import se.dykstrom.jcc.common.ast.RoundExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.LlvmUtils;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.StoreOperation;

import java.util.List;

import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.LF_ROUNDEVEN_F64;

public record SwapCodeGenerator(LlvmCodeGenerator cg) implements LlvmStatementCodeGenerator<SwapStatement> {

    @Override
    public void toLlvm(final SwapStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new LlvmComment(statement.toString()));

        final var first = statement.first();
        final var second = statement.second();

        final var ft = cg.typeManager().getType(first);
        final var st = cg.typeManager().getType(second);

        Expression exprFirst = readExpression(first);
        Expression exprSecond = readExpression(second);
        if (!ft.equals(st)) {
            // float->int rounds half-to-even, matching QuickBASIC 4.5 (issue #52)
            if (ft.isInteger()) {
                exprSecond = new CastToIntExpression(new RoundExpression(exprSecond, LF_ROUNDEVEN_F64), ft);
            } else {
                exprSecond = new CastToFloatExpression(exprSecond, ft);
            }
            if (st.isInteger()) {
                exprFirst = new CastToIntExpression(new RoundExpression(exprFirst, LF_ROUNDEVEN_F64), st);
            } else {
                exprFirst = new CastToFloatExpression(exprFirst, st);
            }
        }

        // Read both values before writing either, so an element can be swapped with itself.
        final var opLoadFirst = cg.expression(exprFirst, lines, symbolTable);
        final var opLoadSecond = cg.expression(exprSecond, lines, symbolTable);

        final var opSaveFirst = destination(first, ft, lines, symbolTable);
        final var opSaveSecond = destination(second, st, lines, symbolTable);

        lines.add(new StoreOperation(opLoadSecond, opSaveFirst));
        lines.add(new StoreOperation(opLoadFirst, opSaveSecond));

        // No GC action is needed for string operands: roots are slot addresses (issue #63), and
        // both slots are already rooted, so exchanging their contents keeps both pointers
        // reachable. The collector reads each slot's current value at mark time.
    }

    /** The expression that reads an operand's current value: an array element access, or a scalar deref. */
    private static Expression readExpression(final IdentifierExpression operand) {
        return (operand instanceof ArrayAccessExpression) ? operand : IdentifierDerefExpression.from(operand);
    }

    /** The pointer an operand's new value is stored into: an array element address, or a scalar variable. */
    private LlvmOperand destination(final IdentifierExpression operand,
                                    final Type type,
                                    final List<Line> lines,
                                    final SymbolTable symbolTable) {
        if (operand instanceof ArrayAccessExpression arrayAccess) {
            return LlvmUtils.arrayElementAddress(cg, arrayAccess, lines, symbolTable);
        }
        return new TempOperand(symbolTable.mapName(operand.getIdentifier()), type);
    }
}
