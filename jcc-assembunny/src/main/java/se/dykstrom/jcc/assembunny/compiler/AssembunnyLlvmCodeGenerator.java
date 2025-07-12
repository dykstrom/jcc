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

package se.dykstrom.jcc.assembunny.compiler;

import se.dykstrom.jcc.assembunny.ast.AssembunnyRegister;
import se.dykstrom.jcc.assembunny.ast.CpyStatement;
import se.dykstrom.jcc.assembunny.ast.JnzStatement;
import se.dykstrom.jcc.assembunny.ast.OutnStatement;
import se.dykstrom.jcc.assembunny.code.llvm.statement.JnzCodeGenerator;
import se.dykstrom.jcc.assembunny.code.llvm.statement.OutnCodeGenerator;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.llvm.code.AbstractLlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.AssignCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static se.dykstrom.jcc.assembunny.compiler.AssembunnyUtils.END_JUMP_TARGET;
import static se.dykstrom.jcc.assembunny.compiler.AssembunnyUtils.IDE_A;

public class AssembunnyLlvmCodeGenerator extends AbstractLlvmCodeGenerator {

    public AssembunnyLlvmCodeGenerator(final TypeManager typeManager,
                                       final SymbolTable symbolTable,
                                       final AstOptimizer optimizer) {
        super(typeManager, symbolTable, optimizer);

        statementDictionary.putAll(buildStatementDictionary());
        expressionDictionary.putAll(buildExpressionDictionary());
    }

    @Override
    public TargetProgram generate(final AstProgram astProgram) {
        // Define registers as global variables
        for (AssembunnyRegister register : AssembunnyRegister.values()) {
            final var identifier = new Identifier(register.name(), I64.INSTANCE);
            final var globalIdentifier = new Identifier("@" + register.name(), I64.INSTANCE);
            symbolTable().addVariable(identifier, globalIdentifier.name());
            symbolTable().addVariable(globalIdentifier, "0");
        }

        final var lines = new ArrayList<Line>();

        // Wrap all statements in a main function
        final var statements = withReturn(astProgram.getStatements());
        final var mainFunction = generateMainFunction(statements, false);
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

    private static List<Statement> withReturn(final List<Statement> originalStatements) {
        final var statements = new ArrayList<>(originalStatements);
        final var expression = new TruncateExpression(0, 0, IDE_A, I32.INSTANCE);
        statements.add(new LabelledStatement(END_JUMP_TARGET, new ReturnStatement(0, 0, expression)));
        return statements;
    }

    private Map<Class<?>, LlvmStatementCodeGenerator<? extends Statement>> buildStatementDictionary() {
        return Map.of(
                CpyStatement.class, new AssignCodeGenerator(this),
                JnzStatement.class, new JnzCodeGenerator(this),
                OutnStatement.class, new OutnCodeGenerator(this)
        );
    }

    private Map<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>> buildExpressionDictionary() {
        return Map.of();
    }
}
