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

package se.dykstrom.jcc.col.compiler;

import se.dykstrom.jcc.col.ast.AliasStatement;
import se.dykstrom.jcc.col.ast.FunCallStatement;
import se.dykstrom.jcc.col.ast.ImportStatement;
import se.dykstrom.jcc.col.ast.PrintlnStatement;
import se.dykstrom.jcc.col.code.statement.AliasCodeGenerator;
import se.dykstrom.jcc.col.code.statement.FunCallCodeGenerator;
import se.dykstrom.jcc.col.code.statement.ImportCodeGenerator;
import se.dykstrom.jcc.col.code.statement.PrintlnCodeGenerator;
import se.dykstrom.jcc.common.ast.ExitStatement;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.compiler.AbstractGarbageCollectingCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;

public class ColCodeGenerator extends AbstractGarbageCollectingCodeGenerator {

    public ColCodeGenerator(final TypeManager typeManager,
                            final SymbolTable symbolTable,
                            final AstOptimizer optimizer) {
        super(typeManager, symbolTable, optimizer);
        // Statements
        statementCodeGenerators.put(AliasStatement.class, new AliasCodeGenerator(this));
        statementCodeGenerators.put(ImportStatement.class, new ImportCodeGenerator(this));
        statementCodeGenerators.put(PrintlnStatement.class, new PrintlnCodeGenerator(this));
        statementCodeGenerators.put(FunCallStatement.class, new FunCallCodeGenerator(this));
    }

    @Override
    public TargetProgram generate(final AstProgram program) {
        // Add program statements
        program.getStatements().forEach(this::statement);

        // If the program does not contain any call to exit, add one at the end
        if (!containsExit()) {
            statement(new ExitStatement(0, 0, IntegerLiteral.ZERO));
        }

        // Create main program
        TargetProgram asmProgram = new TargetProgram();

        // Add file header
        fileHeader(program.getSourcePath()).lines().forEach(asmProgram::add);

        // Process user-defined functions to find out which functions and other symbols they use
        final var udfLines = userDefinedFunctions().lines();

        // Add import section
        importSection(dependencies).lines().forEach(asmProgram::add);

        // Add data section
        dataSection(symbols).lines().forEach(asmProgram::add);

        // Add code section
        codeSection(lines()).lines().forEach(asmProgram::add);

        // Add built-in functions
        builtInFunctions().lines().forEach(asmProgram::add);

        // Add user-defined functions to the end of the text
        udfLines.forEach(asmProgram::add);

        return asmProgram;
    }
}
