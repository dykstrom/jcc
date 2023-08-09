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

package se.dykstrom.jcc.common.intermediate;

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

/**
 * Represents the entire program in an intermediate language,
 * such as assembly code, C, or Java.
 *
 * @author Johan Dykstrom
 */
public class IntermediateProgram extends CodeContainer {

    /**
     * Returns the textual representation of the entire program in the intermediate language,
     * including blank lines, comments, and line breaks.
     */
    public String toText() {
        return lines().stream().map(Line::toText).collect(joining(EOL));
    }
}
