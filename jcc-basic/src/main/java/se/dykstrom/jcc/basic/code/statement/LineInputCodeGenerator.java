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
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGenerator;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;

import static java.util.Collections.emptyList;
import static se.dykstrom.jcc.basic.compiler.BasicTypeHelper.updateTypes;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_GETLINE;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class LineInputCodeGenerator extends AbstractStatementCodeGenerator<LineInputStatement, BasicTypeManager, BasicCodeGenerator> {

    public LineInputCodeGenerator(final BasicCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(LineInputStatement s) {
        LineInputStatement statement = updateTypes(s, symbols());

        return withCodeContainer(cc -> {
            cc.add(getComment(statement));

            // Add variable to symbol table
            Identifier identifier = statement.identifier();
            symbols().addVariable(identifier);

            // Print prompt if required
            if (statement.prompt() != null) {
                cc.addAll(codeGenerator.printPrompt(statement, statement.prompt()));
            }

            // Allocate a storage location for the result of getline
            try (StorageLocation location = storageFactory().allocateNonVolatile(Str.INSTANCE)) {
                cc.add(Blank.INSTANCE);
                // Call getline to read string
                cc.addAll(codeGenerator.functionCall(FUN_GETLINE, new AssemblyComment(FUN_GETLINE.getName() + "()"), emptyList(), location));
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
