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

package se.dykstrom.jcc.common.ast;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Represents the entire program in AST format.
 *
 * @author Johan Dykstrom
 */
public class AstProgram extends AbstractNode {

    private final Path sourcePath;

    private final List<Statement> statements;

    public AstProgram(int line, int column, List<Statement> statements) {
        this(line, column, statements, null);
    }

    private AstProgram(int line, int column, List<Statement> statements, Path sourcePath) {
        super(line, column);
        this.statements = statements;
        this.sourcePath = sourcePath;
    }

    /**
     * Returns the statements of this program.
     */
    public List<Statement> getStatements() {
        return statements;
    }

    /**
     * Returns a copy of this program with statements set to {@code statements}.
     */
    public AstProgram withStatements(List<Statement> statements) {
        return new AstProgram(line(), column(), statements, sourcePath);
    }

    /**
     * Returns the path of the source file.
     */
    public Path getSourcePath() {
        return sourcePath;
    }

    /**
     * Returns a copy of this program with source path set to {@code sourcePath}.
     */
    public AstProgram withSourcePath(final Path sourcePath) {
        return new AstProgram(line(), column(), statements, sourcePath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AstProgram program = (AstProgram) o;
        return Objects.equals(sourcePath, program.sourcePath) && Objects.equals(statements, program.statements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePath, statements);
    }
}
