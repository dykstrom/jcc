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

package se.dykstrom.jcc.col.semantics.statement;

import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Unknown;

public class ValSemanticsParser<T extends TypeManager> extends AbstractDeclarationSemanticsParser<T>
        implements StatementSemanticsParser<ValDeclarationStatement> {

    private final VariableUsageTracker usageTracker;

    public ValSemanticsParser(final SemanticsParser<T> semanticsParser, final VariableUsageTracker usageTracker) {
        super(semanticsParser, "value");
        this.usageTracker = usageTracker;
    }

    @Override
    public Statement parse(final ValDeclarationStatement statement) {
        final var declaration = statement.declaration();
        final var name = declaration.name();

        checkBindOperator(statement, name);

        if (!checkInitializer(statement, declaration) || !checkName(statement, name)) {
            return statement;
        }

        final var declaredType = (declaration.type() != null) ? resolveType(statement, declaration.type(), types()) : null;
        final var expression = checkExpression(statement, name, parser.expression(declaration.expression()), declaredType);
        if (expression == null) {
            // The initializer was rejected, but the value is still what the programmer declared.
            // Defining it keeps every later use from being reported as an undefined variable,
            // which would be one message per use for a mistake already named here.
            symbols().addValue(new Identifier(name, (declaredType != null) ? declaredType : Unknown.INSTANCE));
            return statement;
        }
        final var type = (declaredType != null) ? declaredType : getType(expression);

        symbols().addValue(new Identifier(name, type));
        usageTracker.declare(name, declaration);

        return statement.withDeclaration(declaration.withType(type).withExpression(expression));
    }

    private void checkBindOperator(final ValDeclarationStatement statement, final String name) {
        if (statement.usesEquals()) {
            final var msg = "COL uses ':=' for binding: write 'val " + name + " := " +
                            statement.declaration().expression() + "'";
            reportError(statement, msg, new SemanticsException(msg));
        }
    }
}
