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

import java.util.*;

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.functions.LibraryFunction.mapName;
import static se.dykstrom.jcc.common.utils.FileUtils.getBasename;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

/**
 * Represents an import directive.
 *
 * @author Johan Dykstrom
 */
public class Import implements Line {

    private final String library;
    private final List<String> functions;

    public Import(String library, Set<String> functions) {
        this.library = library;
        this.functions = new ArrayList<>(functions);
        Collections.sort(this.functions);
    }

    public List<String> getFunctions() {
        return functions;
    }

    @Override
    public String toText() {
        return "import " + getBasename(library) + ",\\" + EOL + toText(functions);
    }

    private String toText(List<String> functions) {
        return functions.stream().map(function -> mapName(function) + ",'" + function + "'").collect(joining(",\\" + EOL));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Import.class.getSimpleName() + "[", "]")
                .add("library='" + library + "'")
                .add("functions=" + functions)
                .toString();
    }
}
