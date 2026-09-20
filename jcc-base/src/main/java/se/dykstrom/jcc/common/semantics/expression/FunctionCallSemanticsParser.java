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
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

import static java.util.Objects.requireNonNull;

public class FunctionCallSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<FunctionCallExpression> {

    private final VariableUsageTracker usageTracker;

    public FunctionCallSemanticsParser(final SemanticsParser<T> semanticsParser,
                                       final VariableUsageTracker usageTracker) {
        super(semanticsParser);
        this.usageTracker = requireNonNull(usageTracker);
    }

    @Override
    public Expression parse(final FunctionCallExpression expression) {
        // Check and update arguments
        var args = expression.getArgs().stream().map(parser::expression).toList();
        // Get types of arguments
        final var actualArgTypes = types().getTypes(args);

        Identifier identifier = expression.getIdentifier();
        String name = identifier.name();

        if (symbols().containsFunction(name) || symbols().contains(name)) {
            if (symbols().contains(name)) {
                // Calling a value of function type is a use of that value, as much as passing it
                // on is. Only a value is tracked: a function is not a variable.
                usageTracker.use(name);
            }
            // If the identifier is a function identifier
            try {
                // Match the function with the expected argument types
                Function function = types().resolveFunction(name, actualArgTypes, symbols());
                identifier = function.getIdentifier();
                // Resolve any arguments that need type inference
                args = types().resolveArgs(args, function.getArgTypes());
                return expression.withIdentifier(identifier).withArgs(args).withFunction(function);
            } catch (SemanticsException e) {
                // An argument whose type could not be determined - a nested call that failed to
                // resolve, or an expression already reported - has already reported its own error,
                // and no overload can match it. The no-match here is that failure travelling
                // outwards, so reporting it would bury the real one under a list of candidate
                // signatures
                if (!actualArgTypes.contains(null) && actualArgTypes.stream().noneMatch(Type::isUnknown)) {
                    reportError(expression, e.getMessage(), e);
                }
            }
        } else {
            String msg = "undefined function: " + name;
            reportError(expression, msg, new UndefinedException(msg, name));
        }

        return unresolvedCall(expression);
    }

    /**
     * Returns the given call with the unknown type. A call that did not resolve has no return
     * type: the syntax visitor leaves it null, not knowing which overload would be chosen, and a
     * null type reaches every arithmetic rule as a NullPointerException waiting to happen.
     * Unknown is the type of an expression that has already been reported, and every check
     * accepts it; see diagnostics.md.
     */
    private static Expression unresolvedCall(final FunctionCallExpression expression) {
        final var identifier = expression.getIdentifier();
        final var type = (Fun) identifier.type();
        return expression.withIdentifier(identifier.withType(Fun.from(type.getArgTypes(), Unknown.INSTANCE)));
    }
}
