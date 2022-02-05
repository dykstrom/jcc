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

package se.dykstrom.jcc.basic.code.statement;

import se.dykstrom.jcc.basic.ast.LineInputStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.assembly.base.Blank;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGeneratorComponent;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;

import static java.util.Collections.emptyList;
import static se.dykstrom.jcc.basic.compiler.BasicTypeHelper.updateTypes;
import static se.dykstrom.jcc.common.assembly.base.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_GETLINE;

public class LineInputCodeGenerator extends AbstractStatementCodeGeneratorComponent<LineInputStatement, BasicTypeManager, BasicCodeGenerator> {

    public LineInputCodeGenerator(Context context) {
        super(context);
    }

    @Override
    public List<Line> generate(LineInputStatement s) {
        // TODO: Replace with "withAddressOfIdentifier" below when enabling array elements.
        LineInputStatement statement = updateTypes(s, symbols, types);

        return withCodeContainer(cc -> {
            cc.add(getComment(statement));

            // Add variable to symbol table
            Identifier identifier = statement.identifier();
            symbols.addVariable(identifier);

            // Print prompt if required
            if (statement.prompt() != null) {
                cc.addAll(codeGenerator.printPrompt(statement, statement.prompt()));
            }

            // Allocate a storage location for the result of getline
            try (StorageLocation location = storageFactory.allocateNonVolatile(Str.INSTANCE)) {
                cc.add(Blank.INSTANCE);
                // Call getline to read string
                cc.addAll(codeGenerator.functionCall(FUN_GETLINE, new Comment(FUN_GETLINE.getName() + "()"), emptyList(), location));
                // Save returned string in variable
                location.moveThisToMem(identifier.getMappedName(), cc);
            }
            // Manage dynamic memory
            cc.addAll(codeGenerator.registerDynamicMemory(IdentifierNameExpression.from(statement, identifier)));

            // Print newline if required
            /*

            INFO: The newline is always echoed to the console atm, so this code does not make any sense.

            if (!statement.inhibitNewline()) {
                add(Blank.INSTANCE);
                String formatStringName = "_fmt_line_input_newline";
                String formatStringValue = "10,0";
                Identifier formatStringIdent = new Identifier(formatStringName, Str.INSTANCE);
                symbols.addConstant(formatStringIdent, formatStringValue);

                List<Expression> expressions = singletonList(IdentifierNameExpression.from(statement, formatStringIdent));
                addFunctionCall(FUN_PRINTF, new Comment(FUN_PRINTF.getName() + "(\"\")"), expressions);
            }
            */
        });
    }
}
