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

package se.dykstrom.jcc.common.assembly.macro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.utils.FileUtils.getBasename;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

/**
 * Represents a library macro.
 *
 * @author Johan Dykstrom
 */
public record Library(List<String> libraries) implements Macro {

    public Library(final List<String> libraries) {
        this.libraries = new ArrayList<>(libraries);
        Collections.sort(this.libraries);
    }

    @Override
    public String toText() {
        return "library " + toText(libraries);
    }

    private String toText(final List<String> libraries) {
        return libraries.stream().map(library -> getBasename(library) + ",'" + library + "'").collect(joining(",\\" + EOL));
    }
}
