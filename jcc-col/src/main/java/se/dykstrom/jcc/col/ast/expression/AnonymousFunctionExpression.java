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

package se.dykstrom.jcc.col.ast.expression;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Declaration;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Represents an anonymous function, such as 'fun(a as i64) -> i64 := a + 1'. The return type is
 * optional; when it is omitted it is inferred from the body during semantic analysis.
 * <p>
 * An anonymous function is not a closure: its body may reference only its own parameters and
 * global functions. This lets semantic analysis lift it to a synthesized top-level function, so
 * no node of this class survives into code generation.
 *
 * @author Johan Dykstrom
 */
public class AnonymousFunctionExpression extends AbstractNode implements TypedExpression {

    private final List<Declaration> declarations;
    private final Expression expression;
    private final Type returnType;

    public AnonymousFunctionExpression(final int line,
                                       final int column,
                                       final List<Declaration> declarations,
                                       final Expression expression,
                                       final Type returnType) {
        super(line, column);
        this.declarations = requireNonNull(declarations);
        this.expression = requireNonNull(expression);
        this.returnType = returnType;
    }

    public AnonymousFunctionExpression(final List<Declaration> declarations,
                                       final Expression expression,
                                       final Type returnType) {
        this(0, 0, declarations, expression, returnType);
    }

    /**
     * The type of an anonymous function is a function type built from its parameter types and its
     * return type. Before semantic analysis the return type is null if it was omitted in the source.
     */
    @Override
    public Type type() {
        return Fun.from(declarations.stream().map(Declaration::type).toList(), returnType);
    }

    /**
     * Returns the formal parameters of this anonymous function.
     */
    public List<Declaration> declarations() {
        return declarations;
    }

    /**
     * Returns the body of this anonymous function.
     */
    public Expression expression() {
        return expression;
    }

    /**
     * Returns the declared return type, or null if it was omitted and should be inferred.
     */
    public Type returnType() {
        return returnType;
    }

    /**
     * Returns a copy of this anonymous function, with the parameters updated.
     */
    public AnonymousFunctionExpression withDeclarations(final List<Declaration> declarations) {
        return new AnonymousFunctionExpression(line(), column(), declarations, expression, returnType);
    }

    /**
     * Returns a copy of this anonymous function, with the body updated.
     */
    public AnonymousFunctionExpression withExpression(final Expression expression) {
        return new AnonymousFunctionExpression(line(), column(), declarations, expression, returnType);
    }

    /**
     * Returns a copy of this anonymous function, with the return type updated.
     */
    public AnonymousFunctionExpression withReturnType(final Type returnType) {
        return new AnonymousFunctionExpression(line(), column(), declarations, expression, returnType);
    }

    @Override
    public String toString() {
        return "fun(" + declarations.stream().map(Declaration::toString).collect(joining(", ")) + ")" +
               ((returnType != null) ? " -> " + returnType : "") + " := " + expression;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AnonymousFunctionExpression that = (AnonymousFunctionExpression) o;
        return Objects.equals(declarations, that.declarations) &&
               Objects.equals(expression, that.expression) &&
               Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declarations, expression, returnType);
    }
}
