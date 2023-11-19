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

import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.assembly.instruction.Jbe;
import se.dykstrom.jcc.common.assembly.instruction.Jle;
import se.dykstrom.jcc.common.ast.LessOrEqualExpression;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

public class LessOrEqualCodeGenerator extends AbstractRelationalExpressionCodeGenerator<LessOrEqualExpression> {

    public LessOrEqualCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(LessOrEqualExpression expression, StorageLocation leftLocation) {
        return relationalExpression(expression, leftLocation, Jle::new, Jbe::new);
    }
}
