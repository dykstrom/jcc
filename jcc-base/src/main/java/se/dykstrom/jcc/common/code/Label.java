/*
 * Copyright (C) 2016 Johan Dykstrom
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

package se.dykstrom.jcc.common.code;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Represents a basic block label in the target code.
 *
 * @author Johan Dykstrom
 */
public class Label implements Line {

    /** Prefixes a source-supplied label name to keep it out of the generated namespace. */
    private static final String PREFIX = ".";

    private final String name;

    /**
     * The labels of the basic blocks that branch to this one, in the order they were added.
     * <p>
     * These are emitted as a trailing {@code ; preds = %a, %b} comment on the label line,
     * matching what LLVM itself prints when it round-trips a module. They are documentation
     * only: LLVM derives the real predecessor set from the terminators that target this
     * block, so an incomplete or absent list changes nothing about the compiled program,
     * and a wrong one cannot make the IR invalid. Code generators add a predecessor where
     * it happens to be at hand, so the list is not exhaustive.
     */
    private final List<Label> preds = new ArrayList<>();

    public Label(final String name) {
        this.name = requireNonNull(name);
    }

    /**
     * Returns the real name of the label, not the mapped name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the name to use in generated code: the label name behind a {@value #PREFIX} prefix.
     * <p>
     * The prefix keeps names taken from the source in a namespace of their own, which matters for
     * two reasons. A BASIC line number would otherwise emit a numeric identifier, and LLVM numbers
     * unnamed values sequentially in that same space, so a block called {@code 10} both consumes
     * counter slots that later temporaries expect and can be shadowed by a temporary that reaches
     * 10. And labels jcc generates itself are {@link FixedLabel}s, which are not prefixed, so a
     * source label called {@code L0} or {@code entry} would collide with one of those.
     * <p>
     * Prefixing is injective, so two distinct source labels never map to the same name.
     */
    public String getMappedName() {
        return PREFIX + name;
    }

    @Override
    public String toText() {
        final var builder = new StringBuilder();
        builder.append(getMappedName()).append(":");
        if (!preds.isEmpty()) {
            // Pad to column 40 so the preds comments line up, as they do in LLVM's own output.
            final var padding = (40 - builder.length() > 1) ? " ".repeat(40 - builder.length()) : " ";
            builder.append(padding);
            builder.append("; preds = ").append(toText(preds));
        }
        return builder.toString();
    }

    private String toText(final List<Label> preds) {
        return preds.stream()
                .map(p -> "%" + p.getMappedName())
                .collect(joining(", "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label = (Label) o;
        return name.equals(label.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Label: " + name;
    }

    /**
     * Records {@code label} as a block that branches to this one, for the {@code ; preds = ...}
     * comment on the label line. Documentation only -- see {@link #preds}.
     */
    public Label withPred(final Label label) {
        preds.add(label);
        return this;
    }
}
