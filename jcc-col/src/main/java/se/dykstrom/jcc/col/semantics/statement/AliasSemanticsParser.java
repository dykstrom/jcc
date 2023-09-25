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

package se.dykstrom.jcc.col.semantics.statement;

import se.dykstrom.jcc.col.ast.AliasStatement;
import se.dykstrom.jcc.col.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.col.semantics.SemanticsParserContext;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.error.DuplicateException;

public class AliasSemanticsParser extends AbstractSemanticsParserComponent<ColTypeManager, SemanticsParser>
        implements StatementSemanticsParser<AliasStatement> {

    public AliasSemanticsParser(final SemanticsParserContext context) {
        super(context);
    }

    @Override
    public Statement parse(final AliasStatement statement) {
        if (types.getTypeFromName(statement.alias()).isPresent()) {
            final var msg = "cannot redefine type: " + statement.alias();
            reportSemanticsError(statement, msg, new DuplicateException(msg, statement.alias()));
            return statement;
        }

        final var resolvedType = resolveType(statement, statement.type(), types);
        types.defineTypeName(statement.alias(), resolvedType);
        return statement.withType(resolvedType);
    }
}
