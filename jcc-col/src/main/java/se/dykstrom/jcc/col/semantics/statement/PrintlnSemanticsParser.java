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

import se.dykstrom.jcc.col.ast.PrintlnStatement;
import se.dykstrom.jcc.col.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;

public class PrintlnSemanticsParser extends AbstractSemanticsParserComponent<ColTypeManager, SemanticsParser<ColTypeManager>>
        implements StatementSemanticsParser<PrintlnStatement> {

    public PrintlnSemanticsParser(final SemanticsParser<ColTypeManager> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Statement parse(final PrintlnStatement statement) {
        return statement.withExpression(parser.expression(statement.expression()));
    }
}
