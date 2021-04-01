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

public class Context {

    private final SymbolTable symbols;
    private final TypeManager types;
    private final StorageFactory storageFactory;
    private final CodeGenerator codeGenerator;

    public Context(SymbolTable symbols, TypeManager types, StorageFactory storageFactory, CodeGenerator codeGenerator) {
        this.symbols = symbols;
        this.types = types;
        this.storageFactory = storageFactory;
        this.codeGenerator = codeGenerator;
    }

    public SymbolTable symbols() {
        return symbols;
    }

    public TypeManager types() {
        return types;
    }

    public StorageFactory storageFactory() {
        return storageFactory;
    }

    public CodeGenerator codeGenerator() {
        return codeGenerator;
    }
}
