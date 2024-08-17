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

package se.dykstrom.jcc.common.code;

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.ast.Node;
import se.dykstrom.jcc.common.compiler.CodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import static java.util.Objects.requireNonNull;

public abstract class AbstractCodeGeneratorComponent<T extends TypeManager, C extends CodeGenerator> {

    protected final C codeGenerator;

    protected AbstractCodeGeneratorComponent(final C codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    @SuppressWarnings("unchecked")
    protected T types() { return (T) codeGenerator.types(); }

    protected SymbolTable symbols() { return codeGenerator.symbols(); }

    protected StorageFactory storageFactory() { return codeGenerator.storageFactory(); }

    /**
     * Returns a {@link Comment} created from the given node.
     */
    protected Comment getComment(final Node node) {
        return new AssemblyComment((node.line() != 0 ? node.line() + ": " : "") + format(node));
    }

    private String format(final Node node) {
        String s = node.toString();
        return (s.length() > 53) ? s.substring(0, 50) + "..." : s;
    }
}
