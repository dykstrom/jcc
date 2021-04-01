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

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import java.util.Optional;

import static se.dykstrom.jcc.common.compiler.AbstractCodeGenerator.lineToLabel;

public abstract class AbstractCodeGeneratorComponent<S extends Statement, T extends TypeManager, C extends AbstractCodeGenerator>
        implements CodeGeneratorComponent<S> {

    protected final SymbolTable symbols;
    protected final T types;
    protected final C codeGenerator;
    protected final StorageFactory storageFactory;

    @SuppressWarnings("unchecked")
    protected AbstractCodeGeneratorComponent(Context context) {
        this.symbols = context.symbols();
        this.types = (T) context.types();
        this.codeGenerator = (C) context.codeGenerator();
        this.storageFactory = context.storageFactory();
    }

    /**
     * Returns an optional {@link Label} created from the given statement.
     */
    protected Optional<Code> getLabel(Statement statement) {
        if (statement.getLabel() != null) {
            return Optional.of(lineToLabel(statement.getLabel()));
        } else {
            return Optional.empty();
        }
    }
}
