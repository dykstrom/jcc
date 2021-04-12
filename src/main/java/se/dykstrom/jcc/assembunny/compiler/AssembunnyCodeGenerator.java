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

import se.dykstrom.jcc.assembunny.ast.DecStatement;
import se.dykstrom.jcc.assembunny.ast.IncStatement;
import se.dykstrom.jcc.assembunny.ast.*;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Blank;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.assembly.instruction.Jne;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.DefaultTypeManager;
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_PRINTF;

/**
 * The code generator for the Assembunny language.
 *
 * @author Johan Dykstrom
 */
class AssembunnyCodeGenerator extends AbstractCodeGenerator {

    private static final Identifier IDENT_FMT_PRINTF = new Identifier("_fmt_printf", Str.INSTANCE);
    private static final String VALUE_FMT_PRINTF = "\"%lld\",10,0";

    /** Maps Assembunny register to CPU register. */
    private final Map<AssembunnyRegister, StorageLocation> registerMap = new HashMap<>();

    AssembunnyCodeGenerator() {
        super(DefaultTypeManager.INSTANCE, new DefaultAstOptimizer(DefaultTypeManager.INSTANCE));
    }

    @Override
    public AsmProgram program(Program program) {
        // Allocate one CPU register for each Assembunny register
        allocateCpuRegisters();

        // Initialize all Assembunny registers to 0
        add(new Comment("Initialize registers to 0"));
        for (AssembunnyRegister assembunnyRegister : AssembunnyRegister.values()) {
            getCpuRegister(assembunnyRegister).moveImmToThis("0", this);
        }
        
        // Add program statements
        add(Blank.INSTANCE);
        add(new Comment("Main program"));
        add(Blank.INSTANCE);
        program.getStatements().forEach(this::statement);

        // Add an exit statement to make sure the program exits
        // Return the value in register A to the shell
        exitStatement(new ExitStatement(0, 0, new RegisterExpression(0, 0, AssembunnyRegister.A), AssembunnyUtils.END_JUMP_TARGET));

        // Create main program
        AsmProgram asmProgram = new AsmProgram(dependencies);

        // Add file header
        fileHeader(program.getSourceFilename()).lines().forEach(asmProgram::add);

        // Add import section
        importSection(dependencies).lines().forEach(asmProgram::add);

        // Add data section
        dataSection(symbols).lines().forEach(asmProgram::add);

        // Add code section
        codeSection(lines()).lines().forEach(asmProgram::add);

        return asmProgram;
    }

    @Override
    protected void statement(Statement statement) {
        if (statement instanceof DecStatement) {
            decStatement((DecStatement) statement);
        } else if (statement instanceof IncStatement) {
            incStatement((IncStatement) statement);
        } else if (statement instanceof CpyStatement) {
            cpyStatement((CpyStatement) statement);
        } else if (statement instanceof JnzStatement) {
            jnzStatement((JnzStatement) statement);
        } else if (statement instanceof OutnStatement) {
            outnStatement((OutnStatement) statement);
        }
        add(Blank.INSTANCE);
    }

    private void outnStatement(OutnStatement statement) {
        symbols.addConstant(IDENT_FMT_PRINTF, VALUE_FMT_PRINTF);

        addLabel(statement);
        Expression fmtExpression = IdentifierNameExpression.from(statement, IDENT_FMT_PRINTF);
        addFunctionCall(FUN_PRINTF, formatComment(statement), asList(fmtExpression, statement.getExpression()));
    }

    private void incStatement(IncStatement statement) {
        addLabel(statement);
        addFormattedComment(statement);
        StorageLocation location = getCpuRegister(statement.getRegister());
        location.incrementThis(this);
    }

    private void decStatement(DecStatement statement) {
        addLabel(statement);
        addFormattedComment(statement);
        StorageLocation location = getCpuRegister(statement.getRegister());
        location.decrementThis(this);
    }

    private void jnzStatement(JnzStatement statement) {
        addLabel(statement);
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
        addLabel(statement);
        addFormattedComment(statement);
        StorageLocation location = getCpuRegister(statement.getDestination());
        // Evaluating the expression, and storing the result in 'location', implements the entire cpy statement
        addAll(expression(statement.getSource(), location));
    }

    @Override
    public List<Line> expression(Expression expression, StorageLocation location) {
        if (expression instanceof RegisterExpression) {
            return registerExpression((RegisterExpression) expression, location);
        } else {
            return super.expression(expression, location);
        }
    }

    /**
     * Generates code for evaluating an Assembunny register expression, that is, storing 
     * the value of the register in the expression in the given storage location.
     */
    private List<Line> registerExpression(RegisterExpression expression, StorageLocation location) {
        CodeContainer cc = new CodeContainer();
        cc.add(formatComment(expression));
        location.moveLocToThis(getCpuRegister(expression.getRegister()), cc);
        return cc.lines();
    }
    
    /**
     * Allocates one CPU register for each Assembunny register.
     */
    private void allocateCpuRegisters() {
        for (AssembunnyRegister assembunnyRegister : AssembunnyRegister.values()) {
            StorageLocation location = storageFactory.allocateNonVolatile();
            registerMap.put(assembunnyRegister, location);
            add(new Comment("Register " + assembunnyRegister.toString().toLowerCase() + " is " + location));
        }
        add(Blank.INSTANCE);
    }

    /**
     * Returns the CPU register associated with the given Assembunny register.
     */
    private StorageLocation getCpuRegister(AssembunnyRegister assembunnyRegister) {
        return registerMap.get(assembunnyRegister);
    }
}
