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

package se.dykstrom.jcc.llvm.code;

import se.dykstrom.jcc.common.code.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * A specialized stack that keeps track of jump labels while generating code
 * for IF, AND, and OR expressions. An instance of this class is shared between
 * the different code generators. This solves the problem that the code generation
 * of the THEN expression may change the basic block and the label used in the
 * PHI operation must be updated.
 * <p>
 * The LLVM Kaleidoscope tutorial comments the problem like this:
 * <p>
 * Codegen of 'Then' can change the current block, update ThenBB for the PHI.
 */
public class LabelStack {

    private final List<Label> labels = new ArrayList<>();

    /**
     * Pushes a new label on the stack.
     */
    public void push(final Label label) {
        labels.addLast(label);
    }

    /**
     * Pops the top label from the stack.
     */
    public Label pop() {
        return labels.removeLast();
    }

    /**
     * Replaces the top label on the stack.
     */
    public void replace(final Label label) {
        labels.removeLast();
        labels.addLast(label);
    }

    /**
     * Returns true if the stack is not empty.
     */
    public boolean isNotEmpty() {
        return !labels.isEmpty();
    }
}
