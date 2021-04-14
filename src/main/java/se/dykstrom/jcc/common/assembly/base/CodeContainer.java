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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.joining;

/**
 * The base class for all code containers.
 *
 * @author Johan Dykstrom
 */
public class CodeContainer {

    private final List<Line> lines = new ArrayList<>();

    /**
     * Creates a new {@code CodeContainer}, executes the specified {@code codeGenerator} function with this container,
     * and returns any code lines that were added to the container by the code generator function. After this method
     * has returned, the code container is no longer valid.
     *
     * @param codeGenerator A function that, given a {@code CodeContainer}, generates code, and adds it to the container.
     * @return The code lines that were added by the code generator function.
     */
    public static List<Line> withCodeContainer(Consumer<CodeContainer> codeGenerator) {
        CodeContainer cc = new CodeContainer();
        codeGenerator.accept(cc);
        return cc.lines();
    }

    /**
     * Adds a new line of code to the end of this code container.
     *
     * @param line The code line to add.
     * @return A reference to this, to enable chained calls.
     */
    public CodeContainer add(Line line) {
        lines.add(line);
        return this;
    }

    /**
     * Adds a new line of code to the beginning of this code container.
     *
     * @param line The code line to add.
     * @return A reference to this, to enable chained calls.
     */
    public CodeContainer addFirst(Line line) {
        lines.add(0, line);
        return this;
    }

    /**
     * Adds the given lines of code to the end of this code container.
     *
     * @param lines The code lines to add.
     * @return A reference to this, to enable chained calls.
     */
    public CodeContainer addAll(List<Line> lines) {
        this.lines.addAll(lines);
        return this;
    }

    /**
     * Returns the list of all code lines added so far.
     */
    public List<Line> lines() {
        return lines;
    }

    /**
     * Returns {@code true} if this code container contains the given code line.
     */
    protected boolean contains(Line line) {
        return lines.contains(line);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("(#").append(lines.size()).append(") ");
        builder.append("[");
        builder.append(lines.stream().limit(10).map(Line::toAsm).collect(joining(", ")));
        if (lines.size() > 10) {
            builder.append(", ...");
        }
        builder.append("]");
        return builder.toString();
    }
}
