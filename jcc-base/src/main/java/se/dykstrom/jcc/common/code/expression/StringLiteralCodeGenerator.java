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

import se.dykstrom.jcc.common.intermediate.CodeContainer;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.ast.StringLiteral;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;
import java.util.Optional;

public class StringLiteralCodeGenerator extends AbstractExpressionCodeGeneratorComponent<StringLiteral, TypeManager, AbstractCodeGenerator> {

    /** Indexing all static strings in the code, helping to create a unique name for each. */
    private int stringIndex = 0;

    public StringLiteralCodeGenerator(Context context) { super(context); }

    @Override
    public List<Line> generate(StringLiteral expression, StorageLocation location) {
        CodeContainer codeContainer = new CodeContainer();

        String value = "\"" + expression.getValue() + "\",0";

        // Try to find an existing string constant with this value
        Optional<Identifier> optionalIdentifier = symbols.getConstantByTypeAndValue(Str.INSTANCE, value);

        // If there was no string constant with this exact value before, create one
        Identifier identifier = optionalIdentifier.orElseGet(
                () -> symbols.addConstant(new Identifier(getUniqueStringName(), Str.INSTANCE), value)
        );

        codeContainer.add(getComment(expression));
        // Store the identifier address (not its contents)
        location.moveImmToThis(identifier.getMappedName(), codeContainer);

        return codeContainer.lines();
    }

    /**
     * Returns a unique string constant name to use in the symbol table.
     */
    private String getUniqueStringName() {
        return "_string_" + stringIndex++;
    }
}
