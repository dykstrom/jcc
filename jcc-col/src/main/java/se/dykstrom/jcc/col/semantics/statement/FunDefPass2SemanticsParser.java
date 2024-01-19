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

import java.util.HashSet;
import java.util.Set;

import se.dykstrom.jcc.col.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;

public class FunDefPass2SemanticsParser extends AbstractSemanticsParserComponent<ColTypeManager, SemanticsParser<ColTypeManager>>
        implements StatementSemanticsParser<FunctionDefinitionStatement> {

    public FunDefPass2SemanticsParser(final SemanticsParser<ColTypeManager> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Statement parse(final FunctionDefinitionStatement statement) {
        return parser.withLocalSymbolTable(() -> {
            final var functionName = statement.identifier().name();
            final var declarations = statement.declarations();

            // Add formal arguments to local symbol table
            // Note: We only support scalar arguments for now
            final Set<String> usedArgNames = new HashSet<>();
            declarations.forEach(d -> {
                final var name = d.name();
                if (usedArgNames.contains(name)) {
                    final var msg = "parameter '" + name + "' is already defined, with type " +
                                    types().getTypeName(symbols().getType(name));
                    reportError(statement, msg, new DuplicateException(msg, name));
                }
                usedArgNames.add(name);
                symbols().addVariable(new Identifier(name, d.type()));
            });

            // Check and update expression
            final var expression = parser.expression(statement.expression());

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
