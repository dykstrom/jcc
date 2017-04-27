/*
 * Copyright (C) 2016 Johan Dykstrom
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

package se.dykstrom.jcc.tiny.compiler;

import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Blank;
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;

import static java.util.Arrays.asList;

/**
 * The code generator for the Tiny language.
 *
 * @author Johan Dykstrom
 */
class TinyCodeGenerator extends AbstractCodeGenerator {

    private static final Identifier IDENT_FMT_PRINTF = new Identifier("_fmt_printf", Str.INSTANCE);
    private static final Identifier IDENT_FMT_SCANF = new Identifier("_fmt_scanf", Str.INSTANCE);

    private static final String VALUE_FMT_PRINTF = "\"%lld\",10,0";
    private static final String VALUE_FMT_SCANF = "\"%lld\",0";

    private static final String FUNC_PRINTF = "printf";
    private static final String FUNC_SCANF = "scanf";

    public AsmProgram program(Program program) {
        // Add program statements
        program.getStatements().forEach(this::statement);

        // If the program does not end with a call to exit, we add one to make sure the program exits
        if (!isLastInstructionExit()) {
            exitStatement();
        }

        // Create main program
        AsmProgram asmProgram = new AsmProgram(dependencies);

        // Add file header
        fileHeader(program.getSourceFilename()).codes().forEach(asmProgram::add);

        // Add import section
        importSection(dependencies).codes().forEach(asmProgram::add);

        // Add data section
        dataSection(symbols).codes().forEach(asmProgram::add);

        // Add code section
        codeSection(codes()).codes().forEach(asmProgram::add);

        return asmProgram;
    }

    private void statement(Statement statement) {
        if (statement instanceof AssignStatement) {
            assignStatement((AssignStatement) statement);
        } else if (statement instanceof ReadStatement) {
            readStatement((ReadStatement) statement);
        } else if (statement instanceof WriteStatement) {
            writeStatement((WriteStatement) statement);
        }
        add(Blank.INSTANCE);
    }

    private void assignStatement(AssignStatement statement) {
        symbols.add(statement.getIdentifier());

        // Allocate storage for evaluated expression
        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            // Evaluate expression
            expression(statement.getExpression(), location);
            // Store result in identifier
            addFormattedComment(statement);
            location.moveThisToMem(statement.getIdentifier(), this);
        }
    }

    private void readStatement(ReadStatement statement) {
        addDependency(FUNC_SCANF, LIB_MSVCRT);
        symbols.add(IDENT_FMT_SCANF, VALUE_FMT_SCANF);

        Expression fmtExpression = IdentifierNameExpression.from(statement, IDENT_FMT_SCANF);
        statement.getIdentifiers().forEach(identifier -> {
            symbols.add(identifier);
            Expression expression = IdentifierNameExpression.from(statement, identifier);
            addFunctionCall(new CallIndirect(FUNC_SCANF), formatComment(statement), asList(fmtExpression, expression));
        });
    }

    private void writeStatement(WriteStatement statement) {
        addDependency(FUNC_PRINTF, LIB_MSVCRT);
        symbols.add(IDENT_FMT_PRINTF, VALUE_FMT_PRINTF);

        Expression fmtExpression = IdentifierNameExpression.from(statement, IDENT_FMT_PRINTF);
        statement.getExpressions().forEach(expression ->
            addFunctionCall(new CallIndirect(FUNC_PRINTF), formatComment(statement), asList(fmtExpression, expression))
        );
    }
}
