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

/**
 * Interface to be implemented by all code lines in the intermediate language
 * used as input to the backend. This includes instructions, imports, directives,
 * comments etc.
 *
 * @author Johan Dykstrom
 */
public interface Line {
    /**
     * Returns the textual representation of this code line in the intermediate language.
     */
    String toText();
}
