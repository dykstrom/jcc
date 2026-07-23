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
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents a guaranteed tail call, such as 'become fac_iter(n - 1, n * result)'. The wrapped
 * function call must be in tail position; this is verified during semantic analysis. In the LLVM
 * backend the call is emitted as a {@code musttail} call.
 *
 * @author Johan Dykstrom
 */
public class BecomeExpression extends AbstractNode implements TypedExpression {

    private final FunctionCallExpression functionCall;

    public BecomeExpression(final int line, final int column, final FunctionCallExpression functionCall) {
        super(line, column);
        this.functionCall = requireNonNull(functionCall);
    }

    public BecomeExpression(final FunctionCallExpression functionCall) {
        this(0, 0, functionCall);
    }

    /**
     * The type of a become expression is the return type of the function it tail-calls.
     */
    @Override
    public Type type() {
        return functionCall.type();
    }

    /**
     * Returns the function call this become expression tail-calls.
     */
    public FunctionCallExpression functionCall() {
        return functionCall;
    }

    /**
     * Returns a copy of this become expression, with the function call updated.
     */
    public BecomeExpression withFunctionCall(final FunctionCallExpression functionCall) {
        return new BecomeExpression(line(), column(), functionCall);
    }

    @Override
    public String toString() {
        return "become " + functionCall;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BecomeExpression that = (BecomeExpression) o;
        return Objects.equals(functionCall, that.functionCall);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionCall);
    }
}
