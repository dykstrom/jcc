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

package se.dykstrom.jcc.common.code.expression;

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Node;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.symbols.SymbolTable;

public abstract class AbstractExpressionCodeGeneratorComponent<E extends Expression, T extends TypeManager, C extends AbstractCodeGenerator>
        implements ExpressionCodeGeneratorComponent<E> {

    protected final SymbolTable symbols;
    protected final T types;
    protected final C codeGenerator;
    protected final StorageFactory storageFactory;

    @SuppressWarnings("unchecked")
    protected AbstractExpressionCodeGeneratorComponent(Context context) {
        this.symbols = context.symbols();
        this.types = (T) context.types();
        this.codeGenerator = (C) context.codeGenerator();
        this.storageFactory = context.storageFactory();
    }

    /**
     * Returns a {@link AssemblyComment} created from the given node.
     */
    protected AssemblyComment getComment(Node node) {
        return new AssemblyComment((node.line() != 0 ? node.line() + ": " : "") + format(node));
    }

    private String format(Node node) {
        String s = node.toString();
        return (s.length() > 53) ? s.substring(0, 50) + "..." : s;
    }
}
