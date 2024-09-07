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

import se.dykstrom.jcc.common.code.Comment;

/**
 * Represents a comment line in the assembly code.
 *
 * @author Johan Dykstrom
 */
public class AssemblyComment extends Comment {

    public AssemblyComment(final String text) {
        this(text, 2);
    }

    public AssemblyComment(final String text, final int numberOfCommentChars) {
        super(";".repeat(numberOfCommentChars), text);
    }
}
