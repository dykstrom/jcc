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

package se.dykstrom.jcc.col.semantics.statement;

import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.HashSet;
import java.util.Set;

import static se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE;

public class FunDefPass2SemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements StatementSemanticsParser<FunctionDefinitionStatement> {

    private final VariableUsageTracker usageTracker;

    public FunDefPass2SemanticsParser(final SemanticsParser<T> semanticsParser, final VariableUsageTracker usageTracker) {
        super(semanticsParser);
        this.usageTracker = usageTracker;
    }

    @Override
    public Statement parse(final FunctionDefinitionStatement statement) {
        // The function scope is built from the global symbol table so that
        // top-level vals are not visible inside the function body
        return parser.withGlobalSymbolTable(() -> {
            final var functionName = statement.identifier().name();
            final var declarations = statement.declarations();

            // Save current tracking state for unused variable checks
            usageTracker.save();

            // Add formal arguments to local symbol table
            // Note: We only support scalar arguments for now
            final Set<String> parameterNames = new HashSet<>();
            declarations.forEach(d -> {
                final var name = d.name();
                if (parameterNames.contains(name)) {
                    final var msg = "parameter '" + name + "' is already defined, with type " +
                                    types().getTypeName(symbols().getType(name));
                    reportError(statement, msg, new DuplicateException(msg, name));
                }
                parameterNames.add(name);
                symbols().addParameter(new Identifier(name, d.type()));
                usageTracker.declare(name, d);
            });

            // Check and update expression
            final var expression = parser.expression(statement.expression());
            // Check for unused parameters
            usageTracker.check((n, m) -> reportWarning(n, m, UNUSED_VARIABLE));
            // Restore tracking state
            usageTracker.restore(parameterNames);

            // Check that expression type matches return type
            final var expressionType = getType(expression);
            final var returnType = ((Fun) statement.identifier().type()).getReturnType();
            if (!types().isAssignableFrom(returnType, expressionType)) {
                final var msg = "you cannot return a value of type " + types().getTypeName(expressionType) +
                                " from function '" + functionName + "' with return type " + types().getTypeName(returnType);
                reportError(statement, msg, new InvalidTypeException(msg, expressionType));
            }

            // The types were resolved and the function was added to the symbol table in pass 1,
            // so we just return the statement with the updated expression
            return statement.withExpression(expression);
        });
    }
}
