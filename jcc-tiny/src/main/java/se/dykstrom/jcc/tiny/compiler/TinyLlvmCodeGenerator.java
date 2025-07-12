/*
 * Copyright (C) 2024 Johan Dykstrom
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

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.AbstractLlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;
import se.dykstrom.jcc.tiny.code.llvm.expression.TinyIdentDerefCodeGenerator;
import se.dykstrom.jcc.tiny.code.llvm.statement.ReadCodeGenerator;
import se.dykstrom.jcc.tiny.code.llvm.statement.TinyAssignCodeGenerator;
import se.dykstrom.jcc.tiny.code.llvm.statement.WriteCodeGenerator;

import java.util.ArrayList;
import java.util.Map;

public class TinyLlvmCodeGenerator extends AbstractLlvmCodeGenerator {

    public TinyLlvmCodeGenerator(final TypeManager typeManager,
                                 final SymbolTable symbolTable,
                                 final AstOptimizer optimizer) {
        super(typeManager, symbolTable, optimizer);

        statementDictionary.putAll(buildStatementDictionary());
        expressionDictionary.putAll(buildExpressionDictionary());
    }

    @Override
    public TargetProgram generate(final AstProgram astProgram) {
        final var lines = new ArrayList<Line>();

        // Wrap all statements in a main function
        final var mainFunction = generateMainFunction(astProgram.getStatements(), true);
        // Generate code for main function
        statement(mainFunction, lines, symbolTable());

        // Add declares of external functions
        lines.addFirst(Blank.INSTANCE);
        lines.addAll(0, generateDeclares(getCalledFunctions(lines)));

        // Add declarations of global variables/constants
        lines.addFirst(Blank.INSTANCE);
        lines.addAll(0, generateGlobals(symbolTable()));

        // Add file header
        lines.addFirst(Blank.INSTANCE);
        lines.addAll(0, generateHeader(astProgram.getSourcePath()));

        return new TargetProgram(lines);
    }

    private Map<Class<?>, LlvmStatementCodeGenerator<? extends Statement>> buildStatementDictionary() {
        return Map.of(
                AssignStatement.class, new TinyAssignCodeGenerator(this),
                ReadStatement.class, new ReadCodeGenerator(),
                WriteStatement.class, new WriteCodeGenerator(this)
        );
    }

    private Map<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>> buildExpressionDictionary() {
        return Map.of(IdentifierDerefExpression.class, new TinyIdentDerefCodeGenerator());
    }
}
