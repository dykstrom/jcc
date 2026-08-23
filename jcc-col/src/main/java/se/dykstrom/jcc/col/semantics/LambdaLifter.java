/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.semantics;

import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the top-level function definitions synthesized from anonymous functions.
 * <p>
 * Anonymous functions have no captures, so lifting one is purely mechanical: give it a name,
 * turn it into an ordinary function definition, and replace it at its use site with a reference
 * to that name. The lifted definitions are prepended to the program's statement list once semantic
 * analysis is done, which is where code generation looks for functions to emit.
 *
 * @author Johan Dykstrom
 */
public class LambdaLifter {

    /**
     * Prefix of the synthesized names. A user identifier cannot contain a '.', so a name of this
     * shape can never collide with a user-defined function, not even after name mangling.
     */
    private static final String NAME_PREFIX = "lambda.";

    private final List<FunctionDefinitionStatement> functions = new ArrayList<>();

    private int counter = 0;

    /**
     * Returns a name for the next lifted function, unique within the program.
     */
    public String nextName() {
        return NAME_PREFIX + counter++;
    }

    /**
     * Adds a lifted function definition.
     */
    public void add(final FunctionDefinitionStatement function) {
        functions.add(function);
    }

    /**
     * Returns all lifted function definitions, in the order they were added.
     */
    public List<FunctionDefinitionStatement> functions() {
        return List.copyOf(functions);
    }

    /**
     * Discards all state, so this lifter can be reused for another program.
     */
    public void clear() {
        functions.clear();
        counter = 0;
    }
}
