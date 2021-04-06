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

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.ast.ArrayAccessExpression;
import se.dykstrom.jcc.common.ast.BooleanLiteral;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

public class ArrayAccessCodeGenerator extends AbstractExpressionCodeGeneratorComponent<ArrayAccessExpression, TypeManager, AbstractCodeGenerator> {

    public ArrayAccessCodeGenerator(Context context) { super(context); }

    @Override
    public List<Line> generate(ArrayAccessExpression expression, StorageLocation location) {
        CodeContainer codeContainer = new CodeContainer();

        // If start of array is "_c%_arr" and offset is "rsi", then generated code will be: mov rdi, [_c%_arr + 8 * rsi]
        codeGenerator.withAddressOfIdentifier(
                expression, (base, offset) -> location.moveMemToThis(base + offset, codeContainer)
        );

        return codeContainer.lines();
    }
}
