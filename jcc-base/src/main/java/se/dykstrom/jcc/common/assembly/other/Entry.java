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

package se.dykstrom.jcc.common.assembly.other;

import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.assembly.base.Label;

/**
 * Represents an entry directive.
 *
 * @author Johan Dykstrom
 */
record Entry(Label entry) implements Line {
    @Override
    public String toText() {
        return "entry " + entry.getMappedName();
    }
}
