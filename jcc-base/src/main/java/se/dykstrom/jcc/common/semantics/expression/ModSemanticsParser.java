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

package se.dykstrom.jcc.common.semantics.expression;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.ModExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.List;

import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_FMOD;

public class ModSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<ModExpression> {

    private static final Identifier FMOD = FUN_FMOD.getIdentifier();

    public ModSemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final ModExpression expression) {
        final var left = parser.expression(expression.getLeft());
        final var right = parser.expression(expression.getRight());
        final var checkedExpression = checkDivisionByZero(expression.withLeft(left).withRight(right));

        // If this is a MOD expression involving floats, call library function fmod
        if (getType(left) instanceof F64 || getType(right) instanceof F64) {
            final var args = List.of(left, right);
            return parser.expression(new FunctionCallExpression(expression.line(), expression.column(), FMOD, args));
        } else {
            return checkType(checkedExpression);
        }
    }
}
