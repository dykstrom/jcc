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

import java.util.List;
import java.util.Objects;

/**
 * Represents the entire program in the AST.
 *
 * @author Johan Dykstrom
 */
public class Program extends AbstractNode {

    private String sourceFilename;

    private final List<Statement> statements;

    public Program(int line, int column, List<Statement> statements) {
        super(line, column);
        this.statements = statements;
    }

    private Program(int line, int column, List<Statement> statements, String sourceFilename) {
        super(line, column);
        this.statements = statements;
        this.sourceFilename = sourceFilename;
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
    public Program withStatements(List<Statement> statements) {
        return new Program(line(), column(), statements, sourceFilename);
    }

    /**
     * Set the name of the source file.
     */
    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    /**
     * Returns the name of the source file.
     */
    public String getSourceFilename() {
        return sourceFilename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Program program = (Program) o;
        return Objects.equals(sourceFilename, program.sourceFilename) && Objects.equals(statements, program.statements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceFilename, statements);
    }
}
