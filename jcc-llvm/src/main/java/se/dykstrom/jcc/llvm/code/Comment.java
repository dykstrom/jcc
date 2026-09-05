/*
 * Copyright (C) 2023 Johan Dykstrom
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

import static java.util.Objects.requireNonNull;

/**
 * Represents a comment line in the generated LLVM IR.
 *
 * @author Johan Dykstrom
 */
public class Comment implements Line {

    private static final String COMMENT_LEADER = ";";

    private final String text;

    public Comment(final String text) {
        this.text = normalize(requireNonNull(text));
    }

    @Override
    public String toText() {
        return COMMENT_LEADER + " " + text;
    }

    @Override
    public String toString() {
        return toText();
    }

    /**
     * Returns a normalized string without newlines.
     */
    private static String normalize(final String s) {
        final var indexOfNewline = s.indexOf('\n');
        final var withoutNewline = (indexOfNewline != -1) ? s.substring(0, indexOfNewline) : s;
        return (withoutNewline.length() > 100) ? withoutNewline.substring(0, 97) + "..." : withoutNewline;
    }
}
