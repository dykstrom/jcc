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

package se.dykstrom.jcc.llvm.code.statement;

import se.dykstrom.jcc.common.ast.Declaration;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.ReturnStatement;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.code.FixedLabel;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.Text;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.AllocateOperation;
import se.dykstrom.jcc.llvm.operation.DefineOperation;
import se.dykstrom.jcc.llvm.operation.StoreOperation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public class FunDefCodeGenerator implements LlvmStatementCodeGenerator<FunctionDefinitionStatement> {

    private final LlvmCodeGenerator codeGenerator;

    public FunDefCodeGenerator(final LlvmCodeGenerator codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    @Override
    public void toLlvm(final FunctionDefinitionStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        // Create a child symbol table for parameters and local variables
        final var childSymbolTable = new SymbolTable(symbolTable);
        // Create function
        final var function = createFunction(statement, childSymbolTable);
        
        // Generate code for function prologue
        final var prologue = generatePrologue(function, childSymbolTable);
        // Generate code for statements
        final var statements = generateStatementLines(statement, childSymbolTable);
        // Generate code for local variables
        final var locals = generateLocals(function, childSymbolTable);
        // Generate code for function epilogue
        final var epilogue = generateEpilogue();

        lines.addAll(prologue);
        lines.addAll(locals);
        lines.addAll(statements);
        lines.addAll(epilogue);
    }

    public static UserDefinedFunction createFunction(final FunctionDefinitionStatement statement,
                                                     final SymbolTable symbolTable) {
        final var argNames = statement.declarations().stream().map(Declaration::name).toList();
        final var argTypes = statement.declarations().stream().map(Declaration::type).toList();
        final var returnType = ((Fun) statement.identifier().type()).getReturnType();

        final var functionName = statement.identifier().name();
        final var function = new UserDefinedFunction(functionName, argNames, argTypes, returnType);
        if (!symbolTable.containsFunction(functionName, argTypes)) {
            symbolTable.addFunction(function);
        }

        return function;
    }

    private List<Line> generatePrologue(final UserDefinedFunction function, final SymbolTable symbolTable) {
        final var lines = new ArrayList<Line>();

        final var temporaries = function.getArgTypes().stream()
                .map(t -> new TempOperand(symbolTable.nextTempName(), t))
                .toList();

        lines.add(new LlvmComment(formatComment(function)));
        lines.add(new DefineOperation(function, temporaries));
        lines.add(new FixedLabel("entry"));
        for (int i = 0; i < function.argNames().size(); i++) {
            final var name = function.argNames().get(i);
            final var type = function.getArgTypes().get(i);
            final var temp = temporaries.get(i);

            // Generate an LLVM address for the argument
            final var address = "%" + name;

            // Allocate stack space for the argument
            final var opResult = new TempOperand(address, type);
            lines.add(new AllocateOperation(opResult));

            // Store temp value in arg
            lines.add(new StoreOperation(temp, opResult));

            // Add argument to symbol table (together with its address)
            symbolTable.addVariable(new Identifier(name, type), address);
        }

        return lines;
    }

    private static String formatComment(final Function function) {
        return function.getName() +
                function.getArgTypes().stream()
                        .map(Type::llvmName)
                        .collect(joining(", ", "(", ")")) +
                " -> " +
                function.getReturnType().llvmName();
    }

    private static List<? extends Line> generateLocals(final UserDefinedFunction function,
                                                       final SymbolTable symbolTable) {
        final var argNames = new HashSet<>(function.argNames());
        return symbolTable.localIdentifiers().stream()
                .sorted()
                // Function args are allocated in the prologue
                .filter(i -> !argNames.contains(i.name()))
                .map(i -> {
                    // Allocate stack space for the local variable
                    final var opResult = new TempOperand("%" + i.name(), i.type());
                    return new AllocateOperation(opResult);
                })
                .toList();
    }

    private List<Line> generateStatementLines(final FunctionDefinitionStatement statement,
                                              final SymbolTable symbolTable) {
        final var lines = new ArrayList<Line>();

        final List<Statement> statements;
        if (statement.expression() != null) {
            // Create a statement from the single expression
            statements = List.of(new ReturnStatement(statement.line(), statement.column(), statement.expression()));
        } else {
            statements = statement.statements();
        }
        statements.forEach(s -> codeGenerator.statement(s, lines, symbolTable));

        return lines;
    }

    private List<Line> generateEpilogue() {
        return List.of(new Text("}"));
    }
}
