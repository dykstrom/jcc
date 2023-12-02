/*
 * Copyright (C) 2016 Johan Dykstrom
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

import java.util.function.Supplier;

import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import static java.util.Objects.requireNonNull;

/**
 * Abstract base class for all semantics parsers. Provides functionality to report semantics errors.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractSemanticsParser<T extends TypeManager> implements SemanticsParser<T> {

    protected final CompilationErrorListener errorListener;
    protected SymbolTable symbols;
    protected final T types;

    protected AbstractSemanticsParser(final CompilationErrorListener errorListener,
                                      final SymbolTable symbolTable,
                                      final T typeManager) {
        this.errorListener = requireNonNull(errorListener);
        this.symbols = requireNonNull(symbolTable);
        this.types = requireNonNull(typeManager);
    }

    @Override
    public T types() { return types; }

    @Override
    public SymbolTable symbols() { return symbols; }

    /**
     * Creates a local symbol table that inherits from the current symbol table,
     * sets the local symbol table as the current symbol table, calls the supplier,
     * and resets the current symbol table again.
     */
    protected <R> R withLocalSymbolTable(final Supplier<R> supplier) {
        try {
            symbols = new SymbolTable(symbols);
            return supplier.get();
        } finally {
            symbols = symbols.pop();
        }
    }

    /**
     * Reports a semantics error at the given line and column.
     */
    @Override
    public void reportSemanticsError(int line, int column, String msg, SemanticsException exception) {
        errorListener.semanticsError(line, column, msg, exception);
    }
}
