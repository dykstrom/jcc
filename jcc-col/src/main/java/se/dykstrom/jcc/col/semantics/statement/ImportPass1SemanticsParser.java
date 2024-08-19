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

import se.dykstrom.jcc.col.ast.ImportStatement;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;

public class ImportPass1SemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements StatementSemanticsParser<ImportStatement> {

    public ImportPass1SemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Statement parse(final ImportStatement statement) {
        LibraryFunction function = statement.function();

        final var argTypes = function.getArgTypes().stream()
                                     .map(type -> resolveType(statement, type, types()))
                                     .toList();
        final var returnType = resolveType(statement, function.getReturnType(), types());
        function = function.withArgsTypes(argTypes);
        function = function.withReturnType(returnType);

        // We know the external dependency is just one function in one library
        final var entry = function.getDependencies().entrySet().iterator().next();
        function = function.withExternalFunction(entry.getKey() + ".dll", entry.getValue().iterator().next());

        // Define function in symbol table
        defineFunction(statement, function);

        return statement.withFunction(function);
    }
}
