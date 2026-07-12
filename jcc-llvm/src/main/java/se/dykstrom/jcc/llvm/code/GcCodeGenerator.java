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
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operation.LlvmOperation;

import java.util.List;

/**
 * A strategy for emitting garbage-collector plumbing (shadow-stack frames, roots, and the
 * global-roots table) into the shared LLVM code generators. It is composed into
 * {@link se.dykstrom.jcc.llvm.code.statement.FunDefCodeGenerator} and
 * {@link se.dykstrom.jcc.llvm.code.statement.ReturnCodeGenerator} rather than baked into them,
 * so a language that does not use the collector (COL, Tiny, Assembunny today) wires in
 * {@link NoOpGcCodeGenerator} and emits nothing, while BASIC wires in
 * {@link RuntimeGcCodeGenerator}. This is what gates GC emission - there is no flag and no
 * subclass. The runtime API and its semantics are specified in {@code docs/GarbageCollection.md}
 * (issue #63).
 */
public interface GcCodeGenerator {

    /**
     * Returns the lines emitted right after a function's {@code entry} label, before its
     * parameter slots. For {@code main} this is {@code jcc_gc_init}, then
     * {@code jcc_gc_set_global_roots}, then {@code jcc_gc_push_frame} (init must come first, per
     * the {@code jcc_gc.h} contract); for every other function it is just
     * {@code jcc_gc_push_frame}.
     *
     * @param function The function whose prologue is being generated.
     */
    List<Line> enterFunction(UserDefinedFunction function);

    /**
     * Returns the lines emitted after all parameter and local slots have been allocated, before
     * the function body: a {@code jcc_gc_add_root} for each string parameter slot, and a
     * {@code store ptr null} followed by {@code jcc_gc_add_root} for each non-parameter string
     * local slot (the null-init keeps a slot from being read as a stale pointer at mark time).
     *
     * @param function    The function whose variables are being rooted.
     * @param symbolTable The child symbol table holding the function's parameters and locals.
     */
    List<Line> rootVariables(UserDefinedFunction function, SymbolTable symbolTable);

    /**
     * Returns the lines emitted immediately before a {@code ret}: {@code jcc_gc_pop_frame}, which
     * drops every root added since the matching {@link #enterFunction} frame was pushed.
     */
    List<Line> exitFunction();

    /**
     * Registers a freshly-allocated string {@code value} with the collector and keeps it
     * reachable: emits {@code call ptr @jcc_gc_register(ptr <value>)}, stores the returned
     * pointer into a synthetic {@code .gc.slot.N} local (rooted for free by the prologue,
     * because {@link #rootVariables} roots every non-parameter string local), and returns the
     * registered pointer. Use for values a runtime/library function or a string operation just
     * malloc'd (e.g. concatenation, {@code UCASE$}).
     *
     * @param value       The freshly-allocated pointer to register.
     * @param lines       The line list to append the emitted operations to.
     * @param symbolTable The current function's symbol table (the synthetic slot is added here).
     * @return The registered pointer (to be used in place of {@code value} downstream).
     */
    LlvmOperand registerResult(LlvmOperand value, List<Line> lines, SymbolTable symbolTable);

    /**
     * Roots an already-registered string {@code value} without registering it again: stores it
     * into a synthetic {@code .gc.slot.N} local and returns it unchanged. Use for the result of
     * a user-defined function, which registered its own result inside the callee - registering
     * it a second time here would be a double registration.
     *
     * @param value       The already-registered pointer to keep reachable.
     * @param lines       The line list to append the emitted store to.
     * @param symbolTable The current function's symbol table (the synthetic slot is added here).
     * @return {@code value}, unchanged.
     */
    LlvmOperand protectResult(LlvmOperand value, List<Line> lines, SymbolTable symbolTable);

    /**
     * Registers a freshly-allocated string {@code value} with the collector and returns the
     * registered pointer, without creating a synthetic slot. Use only when the caller stores the
     * result straight into a slot that is already a root (e.g. LINE INPUT writing into its
     * destination variable), or when the value is transient and provably dead before the next
     * registration (e.g. the RANDOMIZE seed line, consumed by {@code atof} immediately).
     *
     * @param value       The freshly-allocated pointer to register.
     * @param lines       The line list to append the emitted register call to.
     * @param symbolTable The current function's symbol table.
     * @return The registered pointer (to be used in place of {@code value} downstream).
     */
    LlvmOperand register(LlvmOperand value, List<Line> lines, SymbolTable symbolTable);

    /**
     * Returns the operations defining the {@code @jcc.gc.global.roots} table for the globals
     * pass, built from the module's already-computed global root ranges (string scalars and
     * string arrays). The table is read by {@code jcc_gc_set_global_roots}.
     *
     * @param ranges The global root ranges, in emission order (may be empty).
     */
    List<? extends LlvmOperation> globalRoots(List<GcRootRange> ranges);
}
