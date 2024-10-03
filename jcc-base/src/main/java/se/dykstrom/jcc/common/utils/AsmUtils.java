/*
 * Copyright (C) 2024 Johan Dykstrom
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

package se.dykstrom.jcc.common.utils;

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.ast.Node;

public final class AsmUtils {

    private AsmUtils() { }

    /**
     * Converts a line number or line label to a Label object.
     */
    public static Label lineToLabel(final String label) {
        return new Label("_line_" + label);
    }

    /**
     * Returns an {@link AssemblyComment} created from the given node.
     */
    public static AssemblyComment getComment(final Node node) {
        return new AssemblyComment((node.line() != 0 ? node.line() + ": " : "") + format(node));
    }

    private static String format(final Node node) {
        String s = node.toString();
        return (s.length() > 53) ? s.substring(0, 50) + "..." : s;
    }
}
