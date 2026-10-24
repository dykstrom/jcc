/*
 * Copyright (C) 2026 Johan Dykstrom
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

import se.dykstrom.jcc.basic.ast.statement.InitCommandLineStatement;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_INIT_COMMAND_LINE;

/**
 * Generates code that initializes the command line by calling the runtime function
 * init_command_line with the program arguments. The arguments argc and argv are the parameters of
 * the main function, allocated and stored by the function prologue, so they can be dereferenced as
 * ordinary variables here.
 *
 * @author Johan Dykstrom
 */
public record InitCommandLineCodeGenerator(LlvmCodeGenerator codeGenerator)
        implements LlvmStatementCodeGenerator<InitCommandLineStatement> {

    /**
     * Names of the main function parameters; must match the declarations in BasicCodeGenerator.
     * The leading dot is not allowed in BASIC identifiers, so the parameters can never shadow
     * user variables named argc or argv.
     */
    public static final String ARGC = ".argc";
    public static final String ARGV = ".argv";

    public InitCommandLineCodeGenerator {
        requireNonNull(codeGenerator);
    }

    @Override
    public void toLlvm(final InitCommandLineStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final var argc = new IdentifierDerefExpression(0, 0, new Identifier(ARGC, I32.INSTANCE));
        final var argv = new IdentifierDerefExpression(0, 0, new Identifier(ARGV, Ptr.INSTANCE));
        final var opArgc = codeGenerator.expression(argc, lines, symbolTable);
        final var opArgv = codeGenerator.expression(argv, lines, symbolTable);
        lines.add(new CallOperation(null, JF_INIT_COMMAND_LINE, List.of(opArgc, opArgv)));
    }
}
