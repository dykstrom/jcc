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

package se.dykstrom.jcc.common.code.statement;

import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.code.AbstractCodeGeneratorComponent;
import se.dykstrom.jcc.common.compiler.CodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;

public abstract class AbstractStatementCodeGenerator<S extends Statement, T extends TypeManager, C extends CodeGenerator>
        extends AbstractCodeGeneratorComponent<T, C>
        implements StatementCodeGeneratorComponent<S> {
    protected AbstractStatementCodeGenerator(final C codeGenerator) {
        super(codeGenerator);
    }
}
