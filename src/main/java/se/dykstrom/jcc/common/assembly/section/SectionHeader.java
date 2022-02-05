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

package se.dykstrom.jcc.common.assembly.section;

import se.dykstrom.jcc.common.assembly.base.Line;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Represents the header of a section, containing the section name and directives.
 *
 * @author Johan Dykstrom
 */
record SectionHeader(String name, List<String> directives) implements Line {

    SectionHeader(final String name, final List<String> directives) {
        this.name = requireNonNull(name);
        this.directives = new ArrayList<>(directives);
    }

    @Override
    public String toAsm() {
        return "section '" + name + "' " + toAsm(directives);
    }

    private String toAsm(final List<String> directives) {
        return String.join(" ", directives);
    }
}
