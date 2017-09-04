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

package se.dykstrom.jcc.basic.compiler;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;

import se.dykstrom.jcc.basic.ast.EndStatement;
import se.dykstrom.jcc.basic.ast.GotoStatement;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.basic.ast.RemStatement;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Blank;
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect;
import se.dykstrom.jcc.common.assembly.instruction.Jmp;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

/**
 * The code generator for the Basic language.
 *
 * @author Johan Dykstrom
 */
class BasicCodeGenerator extends AbstractCodeGenerator {

    private static final String FUNC_PRINTF = "printf";

    private final TypeManager typeManager = new BasicTypeManager();

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
        } else if (statement instanceof EndStatement) {
            endStatement((EndStatement) statement);
        } else if (statement instanceof GotoStatement) {
            gotoStatement((GotoStatement) statement);
        } else if (statement instanceof PrintStatement) {
            printStatement((PrintStatement) statement);
        } else if (statement instanceof RemStatement) {
            remStatement((RemStatement) statement);
        }
        add(Blank.INSTANCE);
    }

    private void endStatement(EndStatement statement) {
        addDependency(FUNC_EXIT, LIB_MSVCRT);
        addLabel(statement);

        Expression expression = IntegerLiteral.from(statement, Integer.toString(statement.getStatus()));
        addFunctionCall(new CallIndirect(FUNC_EXIT), formatComment(statement), singletonList(expression));
    }

    private void gotoStatement(GotoStatement statement) {
        addLabel(statement);
        addFormattedComment(statement);
        add(new Jmp(lineToLabel(statement.getGotoLine())));
    }

    private void printStatement(PrintStatement statement) {
        addDependency(FUNC_PRINTF, LIB_MSVCRT);
        addLabel(statement);

        String formatStringName = buildFormatStringIdent(statement.getExpressions());
        String formatStringValue = buildFormatStringValue(statement.getExpressions());
        Identifier formatStringIdent = new Identifier(formatStringName, Str.INSTANCE);
        symbols.addConstant(formatStringIdent, formatStringValue);

        List<Expression> expressions = new ArrayList<>(statement.getExpressions());
        expressions.add(0, IdentifierNameExpression.from(statement, formatStringIdent));
        addFunctionCall(new CallIndirect(FUNC_PRINTF), formatComment(statement), expressions);
    }

    private void remStatement(RemStatement statement) {
        addLabel(statement);
        addFormattedComment(statement);
    }

    // -----------------------------------------------------------------------

    private String buildFormatStringIdent(List<Expression> expressions) {
        return "_fmt_" + expressions.stream()
                .map(typeManager::getType)
                .map(Type::getName)
                .collect(joining("_"));
    }

    private String buildFormatStringValue(List<Expression> expressions) {
        return "\"" + expressions.stream()
                .map(typeManager::getType)
                .map(Type::getFormat)
                .collect(joining()) + "\",10,0";
    }
}
