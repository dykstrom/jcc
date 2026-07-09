/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.basic.code.llvm.statement;

import se.dykstrom.jcc.basic.ast.statement.GcInitStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.utils.GcOptions;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.llvm.code.JccGcBuiltIns.GF_INIT;

/**
 * Generates the call that initializes the garbage collector at the start of main:
 * {@code call void @jcc_gc_init(i64 <threshold>, i64 <flags>)}. The arguments come from the
 * command-line GC options (shared with the FASM backend):
 * <ul>
 *   <li>{@code threshold} is {@code -initial-gc-threshold} (the runtime treats a
 *       non-positive value as "use the default");</li>
 *   <li>{@code flags} carries the {@code JCC_GC_DEBUG} bit (value 1) when {@code -print-gc}
 *       was given, and is 0 otherwise.</li>
 * </ul>
 *
 * @author Johan Dykstrom
 */
public record GcInitCodeGenerator(LlvmCodeGenerator codeGenerator)
        implements LlvmStatementCodeGenerator<GcInitStatement> {

    /** The JCC_GC_DEBUG flag, matching the value defined in jcc_gc.h. */
    private static final long JCC_GC_DEBUG = 1L;

    public GcInitCodeGenerator {
        requireNonNull(codeGenerator);
    }

    @Override
    public void toLlvm(final GcInitStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final long threshold = GcOptions.INSTANCE.getInitialGcThreshold();
        final long flags = GcOptions.INSTANCE.isPrintGc() ? JCC_GC_DEBUG : 0L;

        final LlvmOperand opThreshold = new LiteralOperand(threshold, I64.INSTANCE);
        final LlvmOperand opFlags = new LiteralOperand(flags, I64.INSTANCE);
        lines.add(new CallOperation(null, GF_INIT, List.of(opThreshold, opFlags)));
    }
}
