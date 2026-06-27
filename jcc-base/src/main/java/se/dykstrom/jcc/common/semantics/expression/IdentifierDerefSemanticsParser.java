/*
 * Copyright (C) 2024 Johan Dykstrom
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
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.types.AmbiguousType;

import static java.util.stream.Collectors.toSet;

public class IdentifierDerefSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<IdentifierDerefExpression> {

    private final VariableUsageTracker usageTracker;

    public IdentifierDerefSemanticsParser(final SemanticsParser<T> semanticsParser, final VariableUsageTracker usageTracker) {
        super(semanticsParser);
        this.usageTracker = usageTracker;
    }

    @Override
    public Expression parse(final IdentifierDerefExpression expression) {
        final var name = expression.getIdentifier().name();
        if (symbols().contains(name)) {
            usageTracker.use(name);
            // Use the identifier from the symbol table
            final var identifier = symbols().getIdentifier(name);
            return expression.withIdentifier(identifier);
        } else if (symbols().containsFunction(name)) {
            final var functions = symbols().getFunctions(name);
            // A function used as a value (a function reference) must be user-defined: only
            // user-defined functions are emitted as addressable globals, whereas built-in and
            // library functions are inlined or have no symbol to take the address of. Catching it
            // here gives a clear message instead of letting it slip through to a backend error.
            if (functions.stream().noneMatch(f -> f instanceof UserDefinedFunction)) {
                final var msg = "cannot use '" + name + "' as a function reference: only user-defined " +
                                "functions can be referenced, not built-in or library functions";
                reportError(expression, msg, new SemanticsException(msg));
                return expression;
            }
            if (functions.size() == 1) {
                // If there is only one function with this name, we have found a match
                final var functionIdentifier = functions.iterator().next().getIdentifier();
                return expression.withIdentifier(functionIdentifier);
            } else {
                // If there are several overloaded functions, we don't know which one to use
                // We need to use type inference where the expression is used
                final var functionTypes = functions.stream()
                                                   .map(f -> f.getIdentifier().type())
                                                   .collect(toSet());
                final var oneOfManyType = new AmbiguousType(functionTypes);
                final var functionIdentifier = expression.getIdentifier().withType(oneOfManyType);
                return expression.withIdentifier(functionIdentifier);
            }
        } else {
            final var msg = "undefined variable: " + name;
            reportError(expression, msg, new UndefinedException(msg, name));
            return expression;
        }
    }
}
