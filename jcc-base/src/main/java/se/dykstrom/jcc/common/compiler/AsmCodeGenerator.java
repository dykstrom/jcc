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

package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Comment;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Specifies operations for assembly code generators.
 */
public interface AsmCodeGenerator extends CodeGenerator {

    /**
     * Generates code that evaluates the given {@code expression}, and stores the
     * result in {@code location}.
     *
     * @param expression The expression to evaluate.
     * @param location   The storage location where to store the result.
     * @return The generated code.
     */
    List<Line> expression(Expression expression, StorageLocation location);

    /**
     * Generates code for the given statement. The generated code is stored in the
     * code generator
     * itself, which therefore must inherit {@link CodeContainer}.
     *
     * @param statement The statement to generate code for.
     */
    void statement(final Statement statement);

    SymbolTable symbols();

    StorageFactory storageFactory();

    /**
     * Generates code for calling the given {@code function}.
     *
     * @see DefaultFunctionCallHelper#addFunctionCall(Function, Call, Comment, List,
     *      StorageLocation).
     */
    List<Line> functionCall(Function function, Comment functionComment, List<Expression> args,
            StorageLocation returnLocation);

    List<Line> withLocalSymbolTable(Supplier<List<Line>> supplier);

    List<Line> withLocalStorageFactory(Consumer<CodeContainer> functionCodeGenerator);

    /**
     * Adds dependencies to all external libraries and function specified in
     * {@code dependencies}.
     *
     * @param dependencies A map of library-to-functions that specifies
     *                     dependencies.
     */
    void addAllFunctionDependencies(Map<String, Set<Function>> dependencies);
}
