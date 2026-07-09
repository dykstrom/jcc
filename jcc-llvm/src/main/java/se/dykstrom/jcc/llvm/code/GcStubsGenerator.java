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
import se.dykstrom.jcc.common.code.Text;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.llvm.LlvmComment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates temporary, in-module definitions ("stubs") of the {@code jcc_gc_*} runtime
 * functions the program calls. This is scaffolding for issue #63 phases 2-4: the compiler
 * emits calls into the collector before the real runtime (in {@code libjccbas}) exists, so
 * the generated IR must still define those symbols to link and run. The stubs are pure
 * no-ops with the correct signatures:
 * <ul>
 *   <li>{@code jcc_gc_register} / {@code jcc_gc_register_object} return their first argument
 *       unchanged (the ownership-transfer identity), so registration is a pass-through.</li>
 *   <li>every other GC function returns void.</li>
 * </ul>
 * They therefore never collect anything - programs leak, but behave correctly - which is the
 * intended state until phase 5 replaces these stubs with ordinary {@code declare}s of the
 * real runtime symbols. THIS WHOLE CLASS IS DELETED IN PHASE 5.
 * <p>
 * When GC debug output is requested ({@code -print-gc}), each stub body logs a fixed line
 * such as {@code jcc_gc: stub init} via {@code puts}. {@code puts} is used deliberately: JCC
 * never emits it otherwise, so declaring it here cannot clash with a {@code printf}
 * declaration generated elsewhere. The log lines let phase 4 integration tests observe that
 * the GC calls are wired up.
 */
public final class GcStubsGenerator {

    private GcStubsGenerator() { }

    /**
     * Generates stub definitions for the given called GC functions. LLVM IR is not
     * order-sensitive across globals/declares/definitions, so the returned lines (a
     * {@code puts} declaration and message-string globals when {@code printGc} is set,
     * followed by the stub definitions) can be spliced in wherever declares are emitted.
     *
     * @param gcFunctions The GC library functions actually called by the program.
     * @param printGc     Whether {@code -print-gc} was given, enabling per-stub logging.
     * @return The lines defining the stubs, or an empty list if no GC function is called.
     */
    public static List<Line> generateStubs(final Collection<LibraryFunction> gcFunctions, final boolean printGc) {
        if (gcFunctions.isEmpty()) {
            return List.of();
        }

        // Sort by external name so the output is deterministic (matches generateDeclares).
        final var sorted = gcFunctions.stream().sorted().toList();

        final var lines = new ArrayList<Line>();
        lines.add(new LlvmComment("--- Temporary GC runtime stubs (issue #63); removed in phase 5 ---"));

        if (printGc) {
            // puts is safe to declare here: JCC never emits it otherwise, so there is no
            // risk of a duplicate declaration clashing with printf.
            lines.add(new Text("declare i32 @puts(ptr)"));
            sorted.forEach(f -> lines.add(new Text(messageGlobal(f))));
        }

        for (final var function : sorted) {
            lines.addAll(stubDefinition(function, printGc));
            lines.add(new Text(""));
        }

        return lines;
    }

    /**
     * Builds the definition of a single stub function, e.g.
     * <pre>
     * define void @jcc_gc_init(i64 %0, i64 %1) {
     * ret void
     * }
     * </pre>
     */
    private static List<Line> stubDefinition(final LibraryFunction function, final boolean printGc) {
        final var lines = new ArrayList<Line>();

        final var argTypes = function.getArgTypes();
        final var params = new StringBuilder();
        for (int i = 0; i < argTypes.size(); i++) {
            if (i > 0) {
                params.append(", ");
            }
            params.append(argTypes.get(i).llvmName()).append(" %").append(i);
        }

        lines.add(new Text("define " + function.getReturnType().llvmName() +
                           " @" + function.externalName() + "(" + params + ") {"));
        if (printGc) {
            lines.add(new Text("call i32 @puts(ptr @" + messageGlobalName(function) + ")"));
        }
        // register / register_object are the only pointer-returning GC functions; they return
        // their first argument unchanged (ownership transfer is a no-op in the stub). Every
        // other GC function returns void.
        if (function.getReturnType() instanceof Ptr) {
            lines.add(new Text("ret ptr %0"));
        } else {
            lines.add(new Text("ret void"));
        }
        lines.add(new Text("}"));

        return lines;
    }

    /** Renders the private constant holding a stub's log message, e.g. {@code jcc_gc: stub init}. */
    private static String messageGlobal(final LibraryFunction function) {
        final var message = "jcc_gc: stub " + shortName(function);
        // + 1 for the trailing NUL required by puts.
        final var length = message.getBytes(UTF_8).length + 1;
        return "@" + messageGlobalName(function) + " = private constant [" + length + " x i8] c\"" + message + "\\00\"";
    }

    private static String messageGlobalName(final LibraryFunction function) {
        return ".str.gc." + shortName(function);
    }

    /** The GC function name without the {@code jcc_gc_} prefix, e.g. {@code init}, {@code register}. */
    private static String shortName(final LibraryFunction function) {
        return function.externalName().replaceFirst("^jcc_gc_", "");
    }
}
