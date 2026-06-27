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

import se.dykstrom.jcc.col.ast.expression.ChainedRelationalExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.expression.ExpressionSemanticsParser;

/**
 * Rejects a relational expression synthesized from an unparenthesized chain, such as '1 &lt; 2 &lt; 3'.
 * Relational operators are non-associative in COL; chaining them is almost always a mistake.
 */
public class ChainedRelationalSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<ChainedRelationalExpression> {

    public ChainedRelationalSemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final ChainedRelationalExpression expression) {
        final var msg = "relational operators cannot be chained: write `1 < 2 and 2 < 3`";
        reportError(expression, msg, new SemanticsException(msg));
        return expression;
    }
}
