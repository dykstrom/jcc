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

package se.dykstrom.jcc.col.code.asm.statement;

import se.dykstrom.jcc.col.ast.statement.AliasStatement;
import se.dykstrom.jcc.common.code.AbstractCodeGeneratorComponent;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.statement.StatementCodeGeneratorComponent;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;

import java.util.List;

import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class AliasCodeGenerator extends AbstractCodeGeneratorComponent<TypeManager, AsmCodeGenerator>
        implements StatementCodeGeneratorComponent<AliasStatement> {

    public AliasCodeGenerator(final AsmCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(final AliasStatement statement) {
        return List.of(getComment(statement));
    }
}
