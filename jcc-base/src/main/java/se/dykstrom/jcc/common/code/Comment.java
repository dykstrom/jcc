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

package se.dykstrom.jcc.common.code;

import static java.util.Objects.requireNonNull;

/**
 * Represents a comment line in the target code.
 *
 * @author Johan Dykstrom
 */
public class Comment implements Line {

    private final String commentLeader;
    private final String text;

    protected Comment(final String commentLeader, final String text) {
        this.commentLeader = requireNonNull(commentLeader);
        this.text = normalize(text);
    }

    @Override
    public String toText() {
        return commentLeader + " " + text;
    }

    @Override
    public String toString() {
        return toText();
    }

    /**
     * Returns a copy of this comment with {@code prefix} as prefix.
     */
    public Comment withPrefix(final String prefix) {
        return new Comment(commentLeader, prefix + text);
    }

    /**
     * Returns a copy of this comment with {@code suffix} as suffix.
     */
    public Comment withSuffix(final String suffix) {
        return new Comment(commentLeader, text + suffix);
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
