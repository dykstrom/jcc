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

package se.dykstrom.jcc.col.semantics.expression;

import se.dykstrom.jcc.col.ast.expression.BecomeExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.expression.ExpressionSemanticsParser;

/**
 * Type-checks the function call wrapped by a {@link BecomeExpression}. Context-dependent rules
 * (tail position, exact return type, callee kind, top-level use) are verified where the enclosing
 * context is known: in {@code FunDefPass2SemanticsParser} for function bodies, and in
 * {@code ColSemanticsParser} for top-level statements.
 */
public class BecomeSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<BecomeExpression> {

    public BecomeSemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final BecomeExpression expression) {
        final var functionCall = (FunctionCallExpression) parser.expression(expression.functionCall());
        return expression.withFunctionCall(functionCall);
    }
}
