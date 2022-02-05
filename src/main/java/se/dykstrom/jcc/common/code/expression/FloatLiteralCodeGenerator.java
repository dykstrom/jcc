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
import se.dykstrom.jcc.common.ast.FloatLiteral;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.List;
import java.util.Optional;

public class FloatLiteralCodeGenerator extends AbstractExpressionCodeGeneratorComponent<FloatLiteral, TypeManager, AbstractCodeGenerator> {

    /** Indexing all static floats in the code, helping to create a unique name for each. */
    private int floatIndex = 0;

    public FloatLiteralCodeGenerator(Context context) { super(context); }

    @Override
    public List<Line> generate(FloatLiteral expression, StorageLocation location) {
        CodeContainer codeContainer = new CodeContainer();

        String value = expression.getValue();

        // Try to find an existing float constant with this value
        Optional<Identifier> optionalIdentifier = symbols.getConstantByTypeAndValue(F64.INSTANCE, value);

        // If there was no float constant with this exact value before, create one
        Identifier identifier = optionalIdentifier.orElse(
                symbols.addConstant(new Identifier(getUniqueFloatName(), F64.INSTANCE), value)
        );

        codeContainer.add(getComment(expression));
        // Store the identifier contents (not its address)
        location.moveMemToThis(identifier.getMappedName(), codeContainer);

        return codeContainer.lines();
    }

    /**
     * Returns a unique floating point constant name to use in the symbol table.
     */
    private String getUniqueFloatName() {
        return "_float_" + floatIndex++;
    }
}
