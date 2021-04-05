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

package se.dykstrom.jcc.common.assembly;

import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;

import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

/**
 * Represents the entire assembly program.
 *
 * @author Johan Dykstrom
 */
public class AsmProgram extends CodeContainer {

    private final Map<String, Set<String>> dependencies;

    public AsmProgram(Map<String, Set<String>> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, Set<String>> getDependencies() {
        return dependencies;
    }

    /**
     * Returns the entire assembly program as a string.
     */
    public String toAsm() {
        return lines().stream().map(Line::toAsm).collect(joining(EOL));
    }
}
