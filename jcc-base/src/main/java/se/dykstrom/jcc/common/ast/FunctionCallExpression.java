/*
 * Copyright (C) 2017 Johan Dykstrom
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

import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Represents a function call expression, such as 'abs(-1)'.
 *
 * @author Johan Dykstrom
 */
public class FunctionCallExpression extends AbstractNode implements TypedExpression {

    private final Identifier identifier;
    private final List<Expression> args;

    public FunctionCallExpression(final int line,
                                  final int column,
                                  final Identifier identifier,
                                  final List<Expression> args) {
        super(line, column);
        this.identifier = identifier;
        this.args = args;
    }

    public FunctionCallExpression(final Identifier identifier, final List<Expression> args) {
        this(0, 0, identifier, args);
    }

    @Override
    public String toString() {
        return identifier.name() + "(" + toString(args) + ")";
    }

    private String toString(List<Expression> args) {
        return args.stream().map(Expression::toString).collect(joining(", "));
    }

    @Override
    public Type getType() {
        return ((Fun) identifier.type()).getReturnType();
    }

    /**
     * Returns the function identifier.
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Returns a copy of this function call expression, with the identifier updated.
     */
    public FunctionCallExpression withIdentifier(Identifier identifier) {
        return new FunctionCallExpression(line(), column(), identifier, args);
    }

    /**
     * Returns the function arguments.
     */
    public List<Expression> getArgs() {
        return args;
    }

    /**
     * Returns a copy of this function call expression, with the arguments updated.
     */
    public FunctionCallExpression withArgs(List<Expression> args) {
        return new FunctionCallExpression(line(), column(), identifier, args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionCallExpression that = (FunctionCallExpression) o;
        return Objects.equals(this.identifier, that.identifier) && Objects.equals(this.args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, args);
    }
}
