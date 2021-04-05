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

package se.dykstrom.jcc.common.assembly.base;

/**
 * Represents a comment line in the code.
 *
 * @author Johan Dykstrom
 */
public class Comment implements Line {

    private final String commentLeader;
    private final String text;

    public Comment(String text) {
        this(text, 2);
    }

    public Comment(String text, int numberOfCommentChars) {
        this.commentLeader = ";".repeat(numberOfCommentChars) + " ";
        this.text = text;
    }

    @Override
    public String toAsm() {
        return commentLeader + text;
    }

    /**
     * Returns a copy of this comment with {@code prefix} as prefix.
     */
    public Comment withPrefix(String prefix) {
        return new Comment(prefix + text);
    }

    /**
     * Returns a copy of this comment with {@code suffix} as suffix.
     */
    public Comment withSuffix(String suffix) {
        return new Comment(text + suffix);
    }
}
