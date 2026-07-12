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

package se.dykstrom.jcc.llvm.code;

import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.operation.LlvmOperation;

import java.util.List;

/**
 * A garbage-collector code generator that emits nothing. It is the default strategy for the
 * LLVM backend, used by every language that does not (yet) use the collector, so no
 * {@code jcc_gc_*} calls leak into their output. BASIC replaces it with
 * {@link RuntimeGcCodeGenerator}.
 */
public final class NoOpGcCodeGenerator implements GcCodeGenerator {

    public static final NoOpGcCodeGenerator INSTANCE = new NoOpGcCodeGenerator();

    private NoOpGcCodeGenerator() { }

    @Override
    public List<Line> enterFunction(final UserDefinedFunction function) {
        return List.of();
    }

    @Override
    public List<Line> rootVariables(final UserDefinedFunction function, final SymbolTable symbolTable) {
        return List.of();
    }

    @Override
    public List<Line> exitFunction() {
        return List.of();
    }

    @Override
    public List<? extends LlvmOperation> globalRoots(final List<GcRootRange> ranges) {
        return List.of();
    }
}
