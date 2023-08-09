/*
 * Copyright (C) 2021 Johan Dykstrom
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

package se.dykstrom.jcc.common.code;

import se.dykstrom.jcc.common.compiler.*;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import static java.util.Objects.requireNonNull;

public record Context(SymbolTable symbols,
                      TypeManager types,
                      StorageFactory storageFactory,
                      CodeGenerator codeGenerator) {
    public Context(final SymbolTable symbols,
                   final TypeManager types,
                   final StorageFactory storageFactory,
                   final CodeGenerator codeGenerator) {
        this.symbols = requireNonNull(symbols);
        this.types = requireNonNull(types);
        this.storageFactory = requireNonNull(storageFactory);
        this.codeGenerator = requireNonNull(codeGenerator);
    }
}
