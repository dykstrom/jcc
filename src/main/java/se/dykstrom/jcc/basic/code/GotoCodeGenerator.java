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

package se.dykstrom.jcc.basic.code;

import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.instruction.Jmp;
import se.dykstrom.jcc.common.ast.GotoStatement;
import se.dykstrom.jcc.common.code.AbstractCodeGeneratorComponent;
import se.dykstrom.jcc.common.code.Context;

import java.util.List;

import static se.dykstrom.jcc.common.compiler.AbstractCodeGenerator.lineToLabel;

public class GotoCodeGenerator extends AbstractCodeGeneratorComponent<GotoStatement, BasicTypeManager, BasicCodeGenerator> {

    public GotoCodeGenerator(Context context) {
        super(context);
    }

    @Override
    public List<Code> generate(GotoStatement statement) {
        CodeContainer codeContainer = new CodeContainer();

        getLabel(statement).ifPresent(codeContainer::add);
        codeContainer.add(codeGenerator.formatComment(statement));
        codeContainer.add(new Jmp(lineToLabel(statement.getJumpLabel())));

        return codeContainer.codes();
    }
}
