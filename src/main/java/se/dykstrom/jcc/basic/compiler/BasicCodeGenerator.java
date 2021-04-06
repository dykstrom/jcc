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
import se.dykstrom.jcc.basic.code.statement.CommentCodeGenerator;
import se.dykstrom.jcc.basic.code.statement.GotoCodeGenerator;
import se.dykstrom.jcc.basic.code.statement.ReturnCodeGenerator;
import se.dykstrom.jcc.basic.code.statement.SwapCodeGenerator;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.*;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractGarbageCollectingCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.*;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.basic.compiler.BasicTypeHelper.updateTypes;
import static se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_RANDOMIZE;
import static se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_VAL;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;

/**
 * The code generator for the Basic language.
 *
 * @author Johan Dykstrom
 */
public class BasicCodeGenerator extends AbstractGarbageCollectingCodeGenerator {

    /** Contains all labels that have been used in a GOSUB call. */
    private final Set<String> usedGosubLabels = new HashSet<>();

    private final CommentCodeGenerator commentCodeGenerator;
    private final GotoCodeGenerator gotoCodeGenerator;
    private final ReturnCodeGenerator returnCodeGenerator;
    private final SwapCodeGenerator swapCodeGenerator;

    public BasicCodeGenerator(TypeManager typeManager, AstOptimizer optimizer) {
        super(typeManager, optimizer);
        Context context = new Context(symbols, typeManager, storageFactory, this);
        this.commentCodeGenerator = new CommentCodeGenerator(context);
        this.gotoCodeGenerator = new GotoCodeGenerator(context);
        this.returnCodeGenerator = new ReturnCodeGenerator(context);
        this.swapCodeGenerator = new SwapCodeGenerator(context);
    }

    @Override
    public AsmProgram program(Program program) {
        // Add program statements
        program.getStatements().forEach(this::statement);

        // If the program does not contain any call to exit, add one at the end
        if (!containsExit()) {
            exitStatement(new ExitStatement(0, 0, IntegerLiteral.ZERO));
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
        AsmProgram asmProgram = new AsmProgram(dependencies);

        // Add file header
        fileHeader(program.getSourceFilename()).lines().forEach(asmProgram::add);

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
        add(new Comment("--- GOSUB bridge calls -->"));
        usedGosubLabels.stream().sorted().forEach(label -> {
            add(lineToLabel("gosub_" + label));
            add(new CallDirect(lineToLabel(label)));
            add(new Ret());
        });
        add(new Comment("<-- GOSUB bridge calls ---"));
    }

    /**
     * Adds a code block to catch RETURN without GOSUB errors.
     */
    private void addReturnWithoutGosubBlock() {
        int oldSize = lines().size();

        Label label1 = new Label("_after_return_without_gosub_1");
        Label label2 = new Label("_after_return_without_gosub_2");

        add(new Comment("--- RETURN without GOSUB -->"));
        add(new CallDirect(label1));
        printStatement(new PrintStatement(0, 0, singletonList(new StringLiteral(0, 0, "Error: RETURN without GOSUB"))));
        exitStatement(new ExitStatement(0, 0, IntegerLiteral.ONE));
        add(label1);
        add(new Comment("Align stack by making a second call"));
        add(new CallDirect(label2));
        add(new Ret());
        add(label2);
        add(new Comment("<-- RETURN without GOSUB ---"));
        add(Blank.INSTANCE);

        // Move this code block to the beginning of the list
        rotate(lines(), lines().size() - oldSize);
    }

    /**
     * Returns {@code true} if the program contains at least one RETURN statement
     */
    private boolean containsReturn(List<Statement> statements) {
        return statements.stream().anyMatch(statement -> statement instanceof ReturnStatement);
    }

    @Override
    protected void statement(Statement statement) {
        if (statement instanceof CommentStatement) {
            commentStatement((CommentStatement) statement);
        } else if (statement instanceof EndStatement) {
            endStatement((EndStatement) statement);
        } else if (statement instanceof GosubStatement) {
            gosubStatement((GosubStatement) statement);
        } else if (statement instanceof GotoStatement) {
            gotoStatement((GotoStatement) statement);
        } else if (statement instanceof OnGosubStatement) {
            onGosubStatement((OnGosubStatement) statement);
        } else if (statement instanceof OnGotoStatement) {
            onGotoStatement((OnGotoStatement) statement);
        } else if (statement instanceof LineInputStatement) {
            lineInputStatement((LineInputStatement) statement);
        } else if (statement instanceof PrintStatement) {
            printStatement((PrintStatement) statement);
        } else if (statement instanceof RandomizeStatement) {
            randomizeStatement((RandomizeStatement) statement);
        } else if (statement instanceof ReturnStatement) {
            returnStatement((ReturnStatement) statement);
        } else if (statement instanceof SwapStatement) {
            swapStatement((SwapStatement) statement);
        } else {
            super.statement(statement);
        }
        add(Blank.INSTANCE);
    }

    /**
     * See also {@code BasicSemanticsParser#derefExpression(IdentifierDerefExpression)}.
     */
    @Override
    protected void identifierDerefExpression(IdentifierDerefExpression expression, StorageLocation location) {
        Identifier identifier = expression.getIdentifier();
        // If the identifier is undefined, add it to the symbol table now
        if (!symbols.contains(identifier.getName())) {
            symbols.addVariable(identifier);
        }
        super.identifierDerefExpression(expression, location);
    }

    private void commentStatement(CommentStatement statement) {
        addAll(commentCodeGenerator.generate(statement));
    }

    private void endStatement(EndStatement statement) {
        addLabel(statement);
        addFunctionCall(FUN_EXIT, formatComment(statement), singletonList(statement.getExpression()));
    }

    private void gotoStatement(GotoStatement statement) {
        addAll(gotoCodeGenerator.generate(statement));
    }

    private void gosubStatement(GosubStatement statement) {
        addLabel(statement);
        addFormattedComment(statement);
        addCallToGosubLabel(statement.getJumpLabel());
    }

    private void addCallToGosubLabel(String label) {
        add(new CallDirect(lineToLabel("gosub_" + label)));
        usedGosubLabels.add(label);
    }

    private void onGosubStatement(OnGosubStatement statement) {
        addLabel(statement);

        // Allocate a storage location for the on-gosub expression
        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            add(new Comment("Evaluate ON-GOSUB expression"));

            // Generate code for the expression
            expression(statement.getExpression(), location);
            add(Blank.INSTANCE);
            addFormattedComment(statement);

            List<Label> indexLabels = new ArrayList<>();

            // Generate code for comparing with indices
            for (int index = 0; index < statement.getJumpLabels().size(); index++) {
                // Generate a unique label name for this index
                Label indexLabel = new Label(uniqifyLabelName("_on_gosub_index_"));
                indexLabels.add(indexLabel);

                // Compare with index and jump to index label
                location.compareThisWithImm(Integer.toString(index + 1), this);
                add(new Je(indexLabel));
            }

            // Generate a unique label name for the label that marks the end of the on-gosub statement
            Label endLabel = new Label(uniqifyLabelName("_on_gosub_end_"));
            add(new Jmp(endLabel));

            // Generate code for calling subroutines
            for (int index = 0; index < statement.getJumpLabels().size(); index++) {
                add(indexLabels.get(index));

                addCallToGosubLabel(statement.getJumpLabels().get(index));
                add(new Jmp(endLabel));
            }
            add(endLabel);
        }
    }

    private void onGotoStatement(OnGotoStatement statement) {
        addLabel(statement);

        // Allocate a storage location for the on-goto expression
        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            add(new Comment("Evaluate ON-GOTO expression"));

            // Generate code for the expression
            expression(statement.getExpression(), location);
            add(Blank.INSTANCE);
            addFormattedComment(statement);

            for (int index = 0; index < statement.getJumpLabels().size(); index++) {
                location.compareThisWithImm(Integer.toString(index + 1), this);
                Label jumpLabel = lineToLabel(statement.getJumpLabels().get(index));
                add(new Je(jumpLabel));
            }
        }
    }

    private void lineInputStatement(LineInputStatement statement) {
        statement = updateTypes(statement, symbols, (BasicTypeManager) typeManager);
        addLabel(statement);
        addFormattedComment(statement);

        // Add variable to symbol table
        Identifier identifier = statement.identifier();
        symbols.addVariable(identifier);

        // Print prompt if required
        if (statement.prompt() != null) {
            printPrompt(statement, statement.prompt());
        }

        // Allocate a storage location for the result of getline
        try (StorageLocation location = storageFactory.allocateNonVolatile(Str.INSTANCE)) {
            add(Blank.INSTANCE);
            // Call getline to read string
            addFunctionCall(FUN_GETLINE, new Comment(FUN_GETLINE.getName() + "()"), emptyList(), location);
            // Save returned string in variable
            location.moveThisToMem(identifier.getMappedName(), this);
        }
        // Manage dynamic memory
        registerDynamicMemory(IdentifierNameExpression.from(statement, identifier));

        // Print newline if required
        /*

        INFO: The newline is always echoed to the console atm, so this code does not make any sense.

        if (!statement.inhibitNewline()) {
            add(Blank.INSTANCE);
            String formatStringName = "_fmt_line_input_newline";
            String formatStringValue = "10,0";
            Identifier formatStringIdent = new Identifier(formatStringName, Str.INSTANCE);
            symbols.addConstant(formatStringIdent, formatStringValue);

            List<Expression> expressions = singletonList(IdentifierNameExpression.from(statement, formatStringIdent));
            addFunctionCall(FUN_PRINTF, new Comment(FUN_PRINTF.getName() + "(\"\")"), expressions);
        }
        */
    }

    private void printPrompt(Statement statement, String prompt) {
        String formatStringName = "_fmt_input_prompt";
        String formatStringValue = "\"" + Str.INSTANCE.getFormat() + "\",0";
        Identifier formatStringIdent = new Identifier(formatStringName, Str.INSTANCE);
        symbols.addConstant(formatStringIdent, formatStringValue);

        List<Expression> expressions = asList(
            IdentifierNameExpression.from(statement, formatStringIdent),
            StringLiteral.from(statement, prompt)
        );
        addFunctionCall(FUN_PRINTF, new Comment(FUN_PRINTF.getName() + "(\"" + prompt + "\")"), expressions);
    }

    private void printStatement(PrintStatement statement) {
        addLabel(statement);

        String formatStringName = buildFormatStringIdent(statement.getExpressions());
        String formatStringValue = buildFormatStringValue(statement.getExpressions());
        Identifier formatStringIdent = new Identifier(formatStringName, Str.INSTANCE);
        symbols.addConstant(formatStringIdent, formatStringValue);

        List<Expression> expressions = new ArrayList<>(statement.getExpressions());
        expressions.add(0, IdentifierNameExpression.from(statement, formatStringIdent));
        addFunctionCall(FUN_PRINTF, formatComment(statement), expressions);
    }

    private void returnStatement(ReturnStatement statement) {
        addAll(returnCodeGenerator.generate(statement));
    }

    private void randomizeStatement(RandomizeStatement statement) {
        addLabel(statement);
        addFormattedComment(statement);

        Expression expression = statement.getExpression();
        if (expression == null) {
            // Print prompt
            printPrompt(statement, "Random Number Seed (-32768 to 32767)? ");
            add(Blank.INSTANCE);
            // Read user input
            expression = new FunctionCallExpression(statement.line(), statement.column(), FUN_GETLINE.getIdentifier(), emptyList());
            expression = new FunctionCallExpression(statement.line(), statement.column(), FUN_VAL.getIdentifier(), singletonList(expression));
        }
        // Call randomize
        addFunctionCall(FUN_RANDOMIZE, new Comment(FUN_RANDOMIZE.getName() + "(" + expression + ")"), singletonList(expression));
    }

    private void swapStatement(SwapStatement statement) {
        addAll(swapCodeGenerator.generate(statement));
    }

    // -----------------------------------------------------------------------

    private String buildFormatStringIdent(List<Expression> expressions) {
        return "_fmt_" + expressions.stream()
                .map(typeManager::getType)
                .map(Type::getName)
                .collect(joining("_"));
    }

    private String buildFormatStringValue(List<Expression> expressions) {
        return "\"" + expressions.stream()
                .map(typeManager::getType)
                .map(Type::getFormat)
                .collect(joining()) + "\",10,0";
    }
}
