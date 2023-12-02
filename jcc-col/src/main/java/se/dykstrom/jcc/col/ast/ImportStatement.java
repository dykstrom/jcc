/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.col.ast;

import java.util.Objects;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.functions.LibraryFunction;

import static java.util.Objects.requireNonNull;

/**
 * Represents an import statement such as 'import msvcrt.abs64(i64) -> i64 as abs'.
 *
 * @author Johan Dykstrom
 */
public class ImportStatement extends AbstractNode implements Statement {

    private final LibraryFunction function;

    public ImportStatement(final int line, final int column, final LibraryFunction function) {
        super(line, column);
        this.function = requireNonNull(function);
    }

    @Override
    public String toString() {
        return "import " + function;
    }

    public LibraryFunction function() {
        return function;
    }

    public Statement withFunction(final LibraryFunction function) {
        return new ImportStatement(line(), column(), function);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ImportStatement that = (ImportStatement) o;
        return Objects.equals(function, that.function);
    }

    @Override
    public int hashCode() {
        return Objects.hash(function);
    }
}
