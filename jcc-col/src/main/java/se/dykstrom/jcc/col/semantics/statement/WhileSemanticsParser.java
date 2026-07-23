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

import se.dykstrom.jcc.col.ast.statement.FunCallStatement;
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.WhileStatement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.types.Bool;

import java.util.List;

public class WhileSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements StatementSemanticsParser<WhileStatement> {

    public WhileSemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Statement parse(final WhileStatement statement) {
        final var expression = parser.expression(statement.getExpression());
        final var type = getType(expression);
        if (!(type instanceof Bool)) {
            final var msg = "while condition must be a boolean expression, found: " + types().getTypeName(type);
            reportError(expression, msg, new InvalidTypeException(msg, type));
        }

        // Parse the body in a nested scope so that vals declared inside the loop are
        // invisible outside it, and so that a body val shadowing an enclosing name is
        // rejected by ValSemanticsParser's parent-walking duplicate check
        final List<Statement> statements = parser.withLocalSymbolTable(
                () -> statement.getStatements().stream().map(this::bodyStatement).toList()
        );

        return statement.withExpression(expression).withStatements(statements);
    }

    private Statement bodyStatement(final Statement statement) {
        if (!(statement instanceof FunCallStatement
                || statement instanceof ValDeclarationStatement
                || statement instanceof WhileStatement)) {
            final var msg = "statement not allowed in while body: " + statement;
            reportError(statement, msg, new SemanticsException(msg));
            return statement;
        }
        return parser.statement(statement);
    }
}
