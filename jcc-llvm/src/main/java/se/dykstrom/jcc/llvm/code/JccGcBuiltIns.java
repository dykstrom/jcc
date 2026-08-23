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

import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Void;

import java.util.List;

import static se.dykstrom.jcc.common.utils.FunctionUtils.LIB_JCC_GC;

/**
 * Defines the runtime garbage collector API (the {@code jcc_gc_*} functions) as library
 * functions, so the LLVM backend can emit calls into the collector. The API and its
 * semantics are specified in {@code docs/GarbageCollection.md} (issue #63).
 * <p>
 * This class is language-agnostic and lives in the LLVM module so every language that
 * targets LLVM (BASIC and COL today) can reuse it. The functions are tagged with the
 * {@link se.dykstrom.jcc.common.utils.FunctionUtils#LIB_JCC_GC} library marker. They are
 * emitted as ordinary declares by {@link AbstractLlvmCodeGenerator} and resolve against the real
 * runtime, whose canonical copy ships in libjccbas and is vendored identically in libjcccol.
 * <p>
 * The full API surface is declared here so all consumers can reuse it. GC function constants
 * are prefixed with the string "GF".
 */
public final class JccGcBuiltIns {

    /** {@code void jcc_gc_init(i64 initial_threshold, i64 flags)} - initialize the collector. */
    public static final Function GF_INIT =
            create(".gc_init", List.of(I64.INSTANCE, I64.INSTANCE), Void.INSTANCE, "jcc_gc_init");

    /** {@code void jcc_gc_set_global_roots(ptr ranges)} - register the global root table. */
    public static final Function GF_SET_GLOBAL_ROOTS =
            create(".gc_set_global_roots", List.of(Ptr.INSTANCE), Void.INSTANCE, "jcc_gc_set_global_roots");

    /** {@code void jcc_gc_push_frame(void)} - open a shadow-stack frame on function entry. */
    public static final Function GF_PUSH_FRAME =
            create(".gc_push_frame", List.of(), Void.INSTANCE, "jcc_gc_push_frame");

    /** {@code void jcc_gc_pop_frame(void)} - close the current shadow-stack frame. */
    public static final Function GF_POP_FRAME =
            create(".gc_pop_frame", List.of(), Void.INSTANCE, "jcc_gc_pop_frame");

    /** {@code void jcc_gc_add_root(ptr slot)} - add a pointer slot to the current frame. */
    public static final Function GF_ADD_ROOT =
            create(".gc_add_root", List.of(Ptr.INSTANCE), Void.INSTANCE, "jcc_gc_add_root");

    /** {@code ptr jcc_gc_register(ptr p)} - transfer ownership of p to the GC and return p. */
    public static final Function GF_REGISTER =
            create(".gc_register", List.of(Ptr.INSTANCE), Ptr.INSTANCE, "jcc_gc_register");

    /** {@code ptr jcc_gc_register_object(ptr p, ptr type)} - as register, with a type descriptor. */
    public static final Function GF_REGISTER_OBJECT =
            create(".gc_register_object", List.of(Ptr.INSTANCE, Ptr.INSTANCE), Ptr.INSTANCE, "jcc_gc_register_object");

    /** {@code void jcc_gc_collect(void)} - run a full mark-sweep collection now. */
    public static final Function GF_COLLECT =
            create(".gc_collect", List.of(), Void.INSTANCE, "jcc_gc_collect");

    private static LibraryFunction create(final String name,
                                          final List<Type> argTypes,
                                          final Type returnType,
                                          final String externalName) {
        return new LibraryFunction(name, argTypes, returnType, LIB_JCC_GC, new ExternalFunction(externalName));
    }

    private JccGcBuiltIns() { }
}
