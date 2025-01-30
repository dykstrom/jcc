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

import se.dykstrom.jcc.basic.ast.*;
import se.dykstrom.jcc.basic.code.expression.BasicIdentifierDerefCodeGenerator;
import se.dykstrom.jcc.basic.code.expression.EqvCodeGenerator;
import se.dykstrom.jcc.basic.code.expression.ImpCodeGenerator;
import se.dykstrom.jcc.basic.code.statement.*;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.assembly.instruction.CallDirect;
import se.dykstrom.jcc.common.assembly.instruction.Ret;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.compiler.AbstractGarbageCollectingCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.rotate;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_PRINTF_STR_VAR;
import static se.dykstrom.jcc.common.utils.AsmUtils.lineToLabel;

/**
 * The code generator for the Basic language.
 *
 * @author Johan Dykstrom
 */
public class BasicCodeGenerator extends AbstractGarbageCollectingCodeGenerator {

    /** Contains all labels that have been used in a GOSUB call. */
    private final Set<String> usedGosubLabels = new HashSet<>();

    public BasicCodeGenerator(final TypeManager typeManager,
                              final SymbolTable symbolTable,
                              final AstOptimizer optimizer) {
        super(typeManager, symbolTable, optimizer);
        // Statements
        statementCodeGenerators.put(CommentStatement.class, new CommentCodeGenerator(this));
        statementCodeGenerators.put(DefDblStatement.class, new DefTypeCodeGenerator(this));
        statementCodeGenerators.put(DefIntStatement.class, new DefTypeCodeGenerator(this));
        statementCodeGenerators.put(DefStrStatement.class, new DefTypeCodeGenerator(this));
        statementCodeGenerators.put(EndStatement.class, new EndCodeGenerator(this));
        statementCodeGenerators.put(GosubStatement.class, new GosubCodeGenerator(this));
        statementCodeGenerators.put(GotoStatement.class, new GotoCodeGenerator(this));
        statementCodeGenerators.put(LineInputStatement.class, new LineInputCodeGenerator(this));
        statementCodeGenerators.put(OnGosubStatement.class, new OnGosubCodeGenerator(this));
        statementCodeGenerators.put(OnGotoStatement.class, new OnGotoCodeGenerator(this));
        statementCodeGenerators.put(OptionBaseStatement.class, new OptionBaseCodeGenerator(this));
        statementCodeGenerators.put(PrintStatement.class, new PrintCodeGenerator(this));
        statementCodeGenerators.put(RandomizeStatement.class, new RandomizeCodeGenerator(this));
        statementCodeGenerators.put(SleepStatement.class, new SleepCodeGenerator(this));
        statementCodeGenerators.put(SwapStatement.class, new SwapCodeGenerator(this));
        statementCodeGenerators.put(SystemStatement.class, new SystemCodeGenerator(this));
        // Expressions
        expressionCodeGenerators.put(EqvExpression.class, new EqvCodeGenerator(this));
        expressionCodeGenerators.put(ImpExpression.class, new ImpCodeGenerator(this));
        expressionCodeGenerators.put(IdentifierDerefExpression.class, new BasicIdentifierDerefCodeGenerator(this));
    }

    @Override
    public TargetProgram generate(final AstProgram program) {
        // Add program statements
        program.getStatements().forEach(this::statement);

        // If the program does not contain any call to exit, add one at the end
        if (!containsExit()) {
            statement(new ExitStatement(0, 0, IntegerLiteral.ZERO));
        }

        // If the program contains any RETURN statements, add a block for catching RETURN without GOSUB errors
        if (containsReturn(program.getStatements())) {
            addReturnWithoutGosubBlock();
        }
        // If the program contains any GOSUB statements, add a block for the GOSUB bridge calls
        if (containsGosub()) {
            addGosubBridgeBlock();
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

    /**
     * Returns {@code true} if the program contains at least one GOSUB statement.
     */
    private boolean containsGosub() {
        return !usedGosubLabels.isEmpty();
    }

    /**
     * Adds a code block with GOSUB bridge calls.
     */
    private void addGosubBridgeBlock() {
        add(Blank.INSTANCE);
        add(new AssemblyComment("--- GOSUB bridge calls -->"));
        usedGosubLabels.stream().sorted().forEach(label -> {
            add(lineToLabel("gosub_" + label));
            add(new CallDirect(lineToLabel(label)));
            add(new Ret());
        });
        add(new AssemblyComment("<-- GOSUB bridge calls ---"));
    }

    /**
     * Adds a code block to catch RETURN without GOSUB errors.
     */
    private void addReturnWithoutGosubBlock() {
        int oldSize = lines().size();

        Label label1 = new Label("_after_return_without_gosub_1");
        Label label2 = new Label("_after_return_without_gosub_2");

        add(new AssemblyComment("--- RETURN without GOSUB -->"));
        add(new CallDirect(label1));
        List<Expression> printExpressions = List.of(new StringLiteral(0, 0, "Error: RETURN without GOSUB"));
        statement(new PrintStatement(0, 0, printExpressions));
        statement(new ExitStatement(0, 0, IntegerLiteral.ONE));
        add(label1);
        add(new AssemblyComment("Align stack by making a second call"));
        add(new CallDirect(label2));
        add(new Ret());
        add(label2);
        add(new AssemblyComment("<-- RETURN without GOSUB ---"));
        add(Blank.INSTANCE);

        // Move this code block to the beginning of the list
        rotate(lines(), lines().size() - oldSize);
    }

    /**
     * Returns {@code true} if the program contains at least one RETURN statement
     */
    private boolean containsReturn(final List<Statement> statements) {
        for (Statement statement : statements) {
            if (statement instanceof ReturnStatement) {
                return true;
            } else if (statement instanceof LabelledStatement labelledStatement) {
                if (containsReturn(List.of(labelledStatement.statement()))) {
                    return true;
                }
            } else if (statement instanceof WhileStatement whileStatement) {
                if (containsReturn(whileStatement.getStatements())) {
                    return true;
                }
            } else if (statement instanceof IfStatement ifStatement) {
                if (containsReturn(ifStatement.getThenStatements()) || containsReturn(ifStatement.getElseStatements())) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Line> callGosubLabel(String label) {
        usedGosubLabels.add(label);
        return List.of(new CallDirect(lineToLabel("gosub_" + label)));
    }

    /**
     * Generates code that displays a prompt before asking for user input.
     */
    public List<Line> printPrompt(Statement statement, String prompt) {
        CodeContainer cc = new CodeContainer();

        String formatStringName = "_fmt_input_prompt";
        String formatStringValue = "\"" + Str.INSTANCE.getFormat() + "\",0";
        Identifier formatStringIdentifier = new Identifier(formatStringName, Str.INSTANCE);
        symbols.addConstant(formatStringIdentifier, formatStringValue);

        List<Expression> expressions = asList(
            IdentifierNameExpression.from(statement, formatStringIdentifier),
            StringLiteral.from(statement, prompt)
        );
        cc.addAll(functionCall(FUN_PRINTF_STR_VAR, new AssemblyComment(FUN_PRINTF_STR_VAR.getName() + "(\"" + prompt + "\")"), expressions));

        return cc.lines();
    }
}
