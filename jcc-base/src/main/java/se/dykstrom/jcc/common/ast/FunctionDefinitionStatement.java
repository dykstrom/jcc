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

package se.dykstrom.jcc.common.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import se.dykstrom.jcc.common.types.Identifier;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Represents a function definition.
 *
 * @author Johan Dykstrom
 */
public class FunctionDefinitionStatement extends AbstractNode implements Statement {

    private final Identifier identifier;
    private final List<Declaration> declarations;
    private final Expression expression;

    public FunctionDefinitionStatement(final int line,
                                       final int column,
                                       final Identifier identifier,
                                       final List<Declaration> declarations,
                                       final Expression expression) {
        super(line, column);
        this.identifier = requireNonNull(identifier);
        this.declarations = new ArrayList<>(declarations);
        this.expression = expression;
    }

    public Identifier identifier() {
        return identifier;
    }

    public List<Declaration> declarations() {
        return declarations;
    }

    public Expression expression() {
        return expression;
    }

    public FunctionDefinitionStatement withIdentifier(final Identifier identifier) {
        return new FunctionDefinitionStatement(line(), column(), identifier, declarations, expression);
    }

    public FunctionDefinitionStatement withDeclarations(final List<Declaration> declarations) {
        return new FunctionDefinitionStatement(line(), column(), identifier, declarations, expression);
    }

    public FunctionDefinitionStatement withExpression(final Expression expression) {
        return new FunctionDefinitionStatement(line(), column(), identifier, declarations, expression);
    }

    @Override
    public String toString() {
        return "FUNCTION " + identifier + "(" + toString(declarations) + ")" + ((expression != null) ? (" = " + expression) : "");
    }

    private String toString(final List<Declaration> declarations) {
        return declarations.stream().map(Declaration::toString).collect(joining(", "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionDefinitionStatement that = (FunctionDefinitionStatement) o;
        return Objects.equals(identifier, that.identifier) &&
               Objects.equals(declarations, that.declarations) &&
               Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, declarations, expression);
    }
}
