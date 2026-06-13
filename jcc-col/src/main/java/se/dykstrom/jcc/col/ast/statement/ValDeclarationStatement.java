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

package se.dykstrom.jcc.col.ast.statement;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.DeclarationAssignment;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents a val declaration statement such as 'val limit as i64 := 10_000'.
 *
 * @author Johan Dykstrom
 */
public class ValDeclarationStatement extends AbstractNode implements Statement {

    private final DeclarationAssignment declaration;

    public ValDeclarationStatement(final int line, final int column, final DeclarationAssignment declaration) {
        super(line, column);
        this.declaration = requireNonNull(declaration);
    }

    public ValDeclarationStatement(final DeclarationAssignment declaration) {
        this(0, 0, declaration);
    }

    @Override
    public String toString() {
        return "val " + declaration.name() +
                (declaration.type() != null ? " as " + declaration.type() : "") +
                (declaration.expression() != null ? " := " + declaration.expression() : "");
    }

    public DeclarationAssignment declaration() {
        return declaration;
    }

    public ValDeclarationStatement withDeclaration(final DeclarationAssignment declaration) {
        return new ValDeclarationStatement(line(), column(), declaration);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ValDeclarationStatement that = (ValDeclarationStatement) o;
        return Objects.equals(declaration, that.declaration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaration);
    }
}
