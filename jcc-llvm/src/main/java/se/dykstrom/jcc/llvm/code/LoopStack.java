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

import se.dykstrom.jcc.common.ast.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A stack of the loops that enclose the code being generated. Each entry holds the class
 * of the loop statement, and the label after the loop, where a loop exit branches to.
 */
public class LoopStack {

    private record Entry(Class<? extends Statement> loop, Label exitLabel) {}

    private final List<Entry> entries = new ArrayList<>();

    /**
     * Runs {@code body}, which generates the body of a loop, with the loop pushed on the stack.
     * The loop is popped again when {@code body} returns or throws.
     */
    public void withLoop(final Class<? extends Statement> loop, final Label exitLabel, final Runnable body) {
        entries.addLast(new Entry(loop, exitLabel));
        try {
            body.run();
        } finally {
            entries.removeLast();
        }
    }

    /**
     * Returns the exit label of the innermost loop whose class is assignable to the given kind,
     * or an empty optional if there is no such loop.
     */
    public Optional<Label> innermost(final Class<? extends Statement> kind) {
        return entries.reversed().stream()
                .filter(e -> kind.isAssignableFrom(e.loop()))
                .map(Entry::exitLabel)
                .findFirst();
    }
}
