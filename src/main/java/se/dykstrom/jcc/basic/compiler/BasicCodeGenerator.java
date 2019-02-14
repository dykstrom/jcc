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
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Blank;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.CallDirect;
import se.dykstrom.jcc.common.assembly.instruction.Je;
import se.dykstrom.jcc.common.assembly.instruction.Jmp;
import se.dykstrom.jcc.common.assembly.instruction.Ret;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractGarbageCollectingCodeGenerator;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.rotate;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_EXIT;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_PRINTF;

/**
 * The code generator for the Basic language.
 *
 * @author Johan Dykstrom
 */
class BasicCodeGenerator extends AbstractGarbageCollectingCodeGenerator {

    /** Contains all labels that have been used in a GOSUB call. */
    private final Set<String> usedGosubLabels = new HashSet<>();

    BasicCodeGenerator() {
        super(new BasicTypeManager());
    }

    @Override
    public AsmProgram program(Program program) {
        // Add program statements
        program.getStatements().forEach(this::statement);

        // If the program does not contain any call to exit, add one at the end
        if (!containsExit()) {
            exitStatement(new IntegerLiteral(0, 0, "0"), null);
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
        fileHeader(program.getSourceFilename()).codes().forEach(asmProgram::add);

        // Add import section
        importSection(dependencies).codes().forEach(asmProgram::add);

        // Add data section
        dataSection(symbols).codes().forEach(asmProgram::add);

        // Add code section
        codeSection(codes()).codes().forEach(asmProgram::add);

        // Add built-in functions
        builtInFunctions().codes().forEach(asmProgram::add);
        
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
        add(new Comment("--- GOSUB bridge calls ---"));
        usedGosubLabels.stream().sorted().forEach(label -> {
            add(lineToLabel("gosub_" + label));
            add(new CallDirect(lineToLabel(label)));
            add(new Ret());
        });
        add(new Comment("--- GOSUB bridge calls ---"));
    }

    /**
     * Adds a code block to catch RETURN without GOSUB errors.
     */
    private void addReturnWithoutGosubBlock() {
        int oldSize = codes().size();

        Label label1 = new Label("_after_return_without_gosub_1");
        Label label2 = new Label("_after_return_without_gosub_2");

        add(new Comment("--- RETURN without GOSUB ---"));
        add(new CallDirect(label1));
        printStatement(new PrintStatement(0, 0, singletonList(new StringLiteral(0, 0, "Error: RETURN without GOSUB"))));
        exitStatement(new IntegerLiteral(0, 0, "1"), null);
        add(label1);
        add(new Comment("Align stack by making a second call"));
        add(new CallDirect(label2));
        add(new Ret());
        add(label2);
        add(new Comment("--- RETURN without GOSUB ---"));
        add(Blank.INSTANCE);

        // Move this code block to the beginning of the list
        rotate(codes(), codes().size() - oldSize);
    }

    /**
     * Returns {@code true} if the program contains at least one RETURN statement
     */
    private boolean containsReturn(List<Statement> statements) {
        return statements.stream().anyMatch(statement -> statement instanceof ReturnStatement);
    }

    @Override
    protected void statement(Statement statement) {
        if (statement instanceof AssignStatement) {
            assignStatement((AssignStatement) statement);
        } else if (statement instanceof CommentStatement) {
            commentStatement((CommentStatement) statement);
        } else if (statement instanceof AbstractDefTypeStatement) {
            deftypeStatement((AbstractDefTypeStatement) statement);
        } else if (statement instanceof EndStatement) {
            endStatement((EndStatement) statement);
        } else if (statement instanceof GosubStatement) {
            gosubStatement((GosubStatement) statement);
        } else if (statement instanceof GotoStatement) {
            gotoStatement((GotoStatement) statement);
        } else if (statement instanceof IfStatement) {
            ifStatement((IfStatement) statement);
        } else if (statement instanceof OnGosubStatement) {
            onGosubStatement((OnGosubStatement) statement);
        } else if (statement instanceof OnGotoStatement) {
            onGotoStatement((OnGotoStatement) statement);
        } else if (statement instanceof PrintStatement) {
            printStatement((PrintStatement) statement);
        } else if (statement instanceof ReturnStatement) {
            returnStatement((ReturnStatement) statement);
        } else if (statement instanceof VariableDeclarationStatement) {
            variableDeclarationStatement((VariableDeclarationStatement) statement);
        } else if (statement instanceof WhileStatement) {
            whileStatement((WhileStatement) statement);
        }
        add(Blank.INSTANCE);
    }

    /**
     * Extends the generic code generation for assign statements with some Basic specific functionality.
     * If a DEFtype statement has been used to define variable types, we use this information to lookup
     * the type of the LHS identifier.
     *
     * @see #deftypeStatement(AbstractDefTypeStatement)
     */
    @Override
    protected void assignStatement(AssignStatement statement) {
        Identifier identifier = statement.getIdentifier();

        // Find type of variable
        Type identType = identifier.getType();

        // If the variable has no type, look it up using type manager
        if (identType instanceof Unknown) {
            identType = ((BasicTypeManager) typeManager).getIdentType(identifier.getName());
            // Update identifier with new type, and statement with new identifier
            statement = statement.withIdentifier(identifier.withType(identType));
        }

        super.assignStatement(statement);
    }

    /**
     * See also {@code BasicSemanticsParser#derefExpression(IdentifierDerefExpression)}.
     */
    @Override
    protected void identifierDerefExpression(IdentifierDerefExpression expression, StorageLocation location) {
        Identifier identifier = expression.getIdentifier();
        // If the identifier is undefined, make sure it has a type, and add it to the symbol table now
        if (!symbols.contains(identifier.getName())) {
            // If the identifier has no type, look it up using type manager
            if (identifier.getType() instanceof Unknown) {
                identifier = identifier.withType(typeManager.getType(expression));
            }
            symbols.addVariable(identifier);

            expression = (IdentifierDerefExpression) expression.withIdentifier(identifier);
        }
        super.identifierDerefExpression(expression, location);
    }

    private void commentStatement(CommentStatement statement) {
        addLabel(statement);
        addFormattedComment(statement);
    }

    private void deftypeStatement(AbstractDefTypeStatement statement) {
        BasicTypeManager basicTypeManager = (BasicTypeManager) typeManager;
        basicTypeManager.defineIdentType(statement.getLetters(), statement.getType());
    }

    private void endStatement(EndStatement statement) {
        addLabel(statement);
        addFunctionCall(FUN_EXIT, formatComment(statement), singletonList(statement.getExpression()));
    }

    private void gotoStatement(GotoStatement statement) {
        addLabel(statement);
        addFormattedComment(statement);
        add(new Jmp(lineToLabel(statement.getJumpLabel())));
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
        addLabel(statement);
        add(new Ret());
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
