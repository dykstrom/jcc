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

import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.ast.ExitStatement;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;
import se.dykstrom.jcc.tiny.code.expression.TinyIdentifierDerefCodeGenerator;
import se.dykstrom.jcc.tiny.code.statement.ReadCodeGenerator;
import se.dykstrom.jcc.tiny.code.statement.WriteCodeGenerator;

import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO;

/**
 * The code generator for the Tiny language.
 *
 * @author Johan Dykstrom
 */
public class TinyCodeGenerator extends AbstractCodeGenerator {

    public TinyCodeGenerator(final TypeManager typeManager,
                             final SymbolTable symbolTable,
                             final AstOptimizer optimizer) {
        super(typeManager, symbolTable, optimizer);
        // Statements
        statementCodeGenerators.put(ReadStatement.class, new ReadCodeGenerator(this));
        statementCodeGenerators.put(WriteStatement.class, new WriteCodeGenerator(this));
        // Expressions
        expressionCodeGenerators.put(IdentifierDerefExpression.class, new TinyIdentifierDerefCodeGenerator(this));
    }

    @Override
    public TargetProgram generate(final AstProgram program) {
        // Add program statements
        program.getStatements().forEach(this::statement);

        // Add an exit statement to make sure the program exits
        statement(new ExitStatement(0, 0, ZERO));

        // Create main program
        final var asmProgram = new TargetProgram();

        // Add file header
        fileHeader(program.getSourcePath()).lines().forEach(asmProgram::add);

        // Add import section
        importSection(dependencies).lines().forEach(asmProgram::add);

        // Add data section
        dataSection(symbols).lines().forEach(asmProgram::add);

        // Add code section
        codeSection(lines()).lines().forEach(asmProgram::add);

        return asmProgram;
    }
}
