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
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractGarbageCollectingCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.intermediate.IntermediateProgram;
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
    public IntermediateProgram generate(final Program program) {
        // Add program statements
        program.getStatements().forEach(this::statement);

        // If the program does not contain any call to exit, add one at the end
        if (!containsExit()) {
            statement(new ExitStatement(0, 0, IntegerLiteral.ZERO));
        }

        // Create main program
        IntermediateProgram asmProgram = new IntermediateProgram();

        // Add file header
        fileHeader(program.getSourcePath()).lines().forEach(asmProgram::add);

        // Add import section
        importSection(dependencies).lines().forEach(asmProgram::add);

        // Add data section
        dataSection(symbols).lines().forEach(asmProgram::add);

        // Add code section
        codeSection(lines()).lines().forEach(asmProgram::add);

        // Add built-in functions
        builtInFunctions().lines().forEach(asmProgram::add);

        return asmProgram;
    }
}
