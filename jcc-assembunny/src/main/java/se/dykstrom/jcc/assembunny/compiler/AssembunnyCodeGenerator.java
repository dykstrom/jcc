/*
 * Copyright (C) 2017 Johan Dykstrom
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

import se.dykstrom.jcc.assembunny.ast.*;
import se.dykstrom.jcc.assembunny.code.asm.expression.AssembunnyIdentifierDerefCodeGenerator;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.instruction.Jne;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.assembunny.compiler.AssembunnyUtils.*;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;
import static se.dykstrom.jcc.common.utils.AsmUtils.lineToLabel;

/**
 * The code generator for the Assembunny language.
 *
 * @author Johan Dykstrom
 */
public class AssembunnyCodeGenerator extends AbstractCodeGenerator {

    private static final Identifier IDENT_FMT_PRINTF = new Identifier("_fmt_printf", Str.INSTANCE);
    private static final String VALUE_FMT_PRINTF = "\"%lld\",10,0";

    public AssembunnyCodeGenerator(final TypeManager typeManager,
                                   final SymbolTable symbolTable,
                                   final AstOptimizer optimizer) {
        super(typeManager, symbolTable, optimizer);
        // Expressions
        expressionCodeGenerators.put(IdentifierDerefExpression.class, new AssembunnyIdentifierDerefCodeGenerator(this));
    }

    @Override
    public TargetProgram generate(final AstProgram program) {
        // Allocate one CPU register for each Assembunny register
        allocateCpuRegisters(storageFactory);

        // Initialize all Assembunny registers to 0
        for (AssembunnyRegister assembunnyRegister : AssembunnyRegister.values()) {
            final var identifier = new Identifier(assembunnyRegister.name(), I64.INSTANCE);
            add(new AssemblyComment("Initialize register " + identifier.name()));
            getCpuRegister(identifier).moveImmToThis("0", this);
        }

        // Add program statements
        add(Blank.INSTANCE);
        add(new AssemblyComment("Main program"));
        add(Blank.INSTANCE);
        program.getStatements().forEach(this::statement);

        // Add an exit statement to make sure the program exits
        // Return the value in register A to the shell
        statement(new LabelledStatement(END_JUMP_TARGET, new ExitStatement(0, 0, IDE_A)));

        // Create main program
        TargetProgram asmProgram = new TargetProgram();

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

    @Override
    public void statement(Statement statement) {
        if (statement instanceof DecStatement decStatement) {
            decStatement(decStatement);
        } else if (statement instanceof IncStatement incStatement) {
            incStatement(incStatement);
        } else if (statement instanceof CpyStatement cpyStatement) {
            cpyStatement(cpyStatement);
        } else if (statement instanceof JnzStatement jnzStatement) {
            jnzStatement(jnzStatement);
        } else if (statement instanceof OutnStatement outnStatement) {
            outnStatement(outnStatement);
        } else {
            super.statement(statement);
        }
        add(Blank.INSTANCE);
    }

    private void outnStatement(OutnStatement statement) {
        symbols.addConstant(IDENT_FMT_PRINTF, VALUE_FMT_PRINTF);

        Expression fmtExpression = IdentifierNameExpression.from(statement, IDENT_FMT_PRINTF);
        addAll(functionCall(LF_PRINTF_STR_VAR, getComment(statement), asList(fmtExpression, statement.getExpression())));
    }

    private void incStatement(final IncStatement statement) {
        addFormattedComment(statement);
        final var location = getCpuRegister(statement.getLhsExpression().getIdentifier());
        location.incrementThis(this);
    }

    private void decStatement(DecStatement statement) {
        addFormattedComment(statement);
        final var location = getCpuRegister(statement.getLhsExpression().getIdentifier());
        location.decrementThis(this);
    }

    private void jnzStatement(JnzStatement statement) {
        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            // Generate code for the expression
            addAll(expression(statement.getExpression(), location));
            add(Blank.INSTANCE);
            addFormattedComment(statement);
            // If expression evaluates to not 0, then make the jump
            location.compareThisWithImm("0", this);
            add(new Jne(lineToLabel(statement.getTarget())));
        }
    }

    private void cpyStatement(CpyStatement statement) {
        addFormattedComment(statement);
        final var location = getCpuRegister(statement.getLhsExpression().getIdentifier());
        // Evaluating the expression, and storing the result in 'location', implements the entire cpy statement
        addAll(expression(statement.getRhsExpression(), location));
    }
}
