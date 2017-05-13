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

package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.assembly.base.*;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.*;
import se.dykstrom.jcc.common.assembly.section.CodeSection;
import se.dykstrom.jcc.common.assembly.section.DataSection;
import se.dykstrom.jcc.common.assembly.section.ImportSection;
import se.dykstrom.jcc.common.assembly.section.Section;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import java.util.*;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;

/**
 * Abstract base class for all code generators.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractCodeGenerator extends CodeContainer {

    protected static final String LIB_MSVCRT = "msvcrt.dll";
    protected static final String FUNC_EXIT = "exit";

    private static final String LABEL_MAIN = "_main";

    protected final StorageFactory storageFactory = new StorageFactory();

    protected final SymbolTable symbols = new SymbolTable();

    protected final Map<String, Set<String>> dependencies = new HashMap<>();

    /** Indexing all static strings in the code, helping to create a unique name for each. */
    private int stringIndex = 0;

    // -----------------------------------------------------------------------
    // Sections:
    // -----------------------------------------------------------------------

    protected Header fileHeader(String sourceFilename) {
        return new Header(sourceFilename, LABEL_MAIN);
    }

    protected Section importSection(Map<String, Set<String>> dependencies) {
        List<String> libraries = new ArrayList<>(dependencies.keySet());
        Collections.sort(libraries);

        Section section = new ImportSection();

        // Add library directive
        section.add(new Library(libraries)).add(Blank.INSTANCE);

        // Add one import directive for each library
        libraries.forEach(library -> section.add(new Import(library, dependencies.get(library))).add(Blank.INSTANCE));

        return section;
    }

    protected Section dataSection(SymbolTable symbols) {
        // An empty data section results in an invalid executable
        if (symbols.isEmpty()) {
            symbols.addVariable(new Identifier("_dummy", I64.INSTANCE));
        }

        List<Identifier> identifiers = new ArrayList<>(symbols.identifiers());
        Collections.sort(identifiers);

        Section section = new DataSection();

        // Add one data definition for each identifier
        identifiers.forEach(identifier -> section.add(
            new DataDefinition(identifier, identifier.getType(), symbols.getValue(identifier.getName()), symbols.isConstant(identifier.getName()))
        ));
        section.add(Blank.INSTANCE);

        return section;
    }

    protected Section codeSection(List<Code> codes) {
        Section section = new CodeSection();

        // Add start of main program
        section.add(new Label(LABEL_MAIN));

        // Add prologue
        Prologue prologue = new Prologue(storageFactory.getRegisterManager().getUsedNonVolatileRegisters());
        prologue.codes().forEach(section::add);

        // Add function code
        codes.forEach(section::add);

        return section;
    }

    /**
     * Returns {@code true} if the last instruction (ignoring blank lines and comments) is a call to exit.
     */
    protected boolean isLastInstructionExit() {
        return new CallIndirect(FUNC_EXIT).equals(lastInstruction());
    }

    /**
     * Adds a call to exit to make sure the program exits.
     */
    protected void exitStatement() {
        addDependency(FUNC_EXIT, LIB_MSVCRT);
        Statement statement = new ExitStatement(0, 0, 0);
        Expression expression = IntegerLiteral.from(statement, "0");
        addFunctionCall(new CallIndirect(FUNC_EXIT), formatComment(statement), singletonList(expression));
    }

    /**
     * Processes an assignment statement.
     */
    protected void assignStatement(AssignStatement statement) {
        symbols.addVariable(statement.getIdentifier());

        // Allocate storage for evaluated expression
        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            // Evaluate expression
            expression(statement.getExpression(), location);
            // Store result in identifier
            addFormattedComment(statement);
            location.moveThisToMem(statement.getIdentifier(), this);
        }
    }

    // -----------------------------------------------------------------------
    // Expressions:
    // -----------------------------------------------------------------------

    protected void expression(Expression expression, StorageLocation location) {
        if (expression instanceof AddExpression) {
            addExpression((AddExpression) expression, location);
        } else if (expression instanceof DivExpression) {
            divExpression((DivExpression) expression, location);
        } else if (expression instanceof IdentifierDerefExpression) {
            identifierExpression((IdentifierDerefExpression) expression, location);
        } else if (expression instanceof IdentifierNameExpression) {
            identifierNameExpression((IdentifierNameExpression) expression, location);
        } else if (expression instanceof MulExpression) {
            mulExpression((MulExpression) expression, location);
        } else if (expression instanceof IntegerLiteral) {
            integerLiteral((IntegerLiteral) expression, location);
        } else if (expression instanceof StringLiteral) {
            stringLiteral((StringLiteral) expression, location);
        } else if (expression instanceof SubExpression) {
            subExpression((SubExpression) expression, location);
        }
    }

    private void integerLiteral(IntegerLiteral expression, StorageLocation location) {
        addFormattedComment(expression);
        location.moveImmToThis(expression.getValue(), this);
    }

    private void stringLiteral(StringLiteral expression, StorageLocation location) {
        String name = "_string_" + stringIndex++;
        Identifier ident = new Identifier(name, Str.INSTANCE);
        symbols.addConstant(ident, "\"" + expression.getValue() + "\",0");
        addFormattedComment(expression);
        location.moveImmToThis(ident, this);
    }

    private void identifierExpression(IdentifierDerefExpression expression, StorageLocation location) {
        addFormattedComment(expression);
        location.moveMemToThis(expression.getIdentifier(), this);
    }

    private void identifierNameExpression(IdentifierNameExpression expression, StorageLocation location) {
        addFormattedComment(expression);
        location.moveImmToThis(expression.getIdentifier(), this);
    }

    private void addExpression(AddExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for adding sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.addLocToThis(rightLocation, this);
        }
    }

    private void divExpression(DivExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for dividing sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.divThisWithLoc(rightLocation, this);
        }
    }

    private void mulExpression(MulExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for multiplying sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.mulThisWithLoc(rightLocation, this);
        }
    }

    private void subExpression(SubExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for subtracting sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.subtractLocFromThis(rightLocation, this);
        }
    }

    // -----------------------------------------------------------------------
    // Function calls:
    // -----------------------------------------------------------------------

    /**
     * Adds code for making the given {@code functionCall}. The list of expressions is evaluated, and the
     * values are stored in the function call registers (RCX, RDX, R8, and R9) and on the stack if needed.
     * Shadow space is also allocated and cleaned up if needed.
     *
     * @param functionCall The function call to make.
     * @param functionComment A function call comment to insert before calling the function.
     * @param args The arguments to the function.
     */
    protected void addFunctionCall(Call functionCall, Comment functionComment, List<Expression> args) {
        List<Expression> expressions = new ArrayList<>(args);

        // Evaluate the first four arguments (if there are so many)
        List<StorageLocation> storedArgs = new ArrayList<>();
        while (!expressions.isEmpty() && storedArgs.size() < 4) {
            processFirstExpression(expressions, storedArgs);
        }

        // Evaluate for any extra arguments
        int numberOfPushedArgs = 0;
        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            // Push arguments in reverse order
            for (int i = expressions.size() - 1; i >= 0; i--) {
                expression(expressions.get(i), location);
                location.pushThis(this);
                numberOfPushedArgs++;
            }
        }

        // Move register arguments to function call registers
        moveArgToRegister(storedArgs, 0, RCX);
        moveArgToRegister(storedArgs, 1, RDX);
        moveArgToRegister(storedArgs, 2, R8);
        moveArgToRegister(storedArgs, 3, R9);

        // Clean up register arguments
        storedArgs.forEach(StorageLocation::close);

        // If any args were pushed on the stack, we must allocate new shadow space before calling the function
        if (numberOfPushedArgs > 0) {
            add(new Comment("Allocate shadow space"));
            add(new SubImmFromReg(Integer.toString(0x20), RSP));
        }
        add(functionComment);
        add(functionCall);
        // If any args were pushed on the stack, we must consequently clean up the stack after the call
        if (numberOfPushedArgs > 0) {
            // Calculate size of shadow space plus pushed args that must be popped
            Integer stackSpace = 0x20 + numberOfPushedArgs * 0x8;
            add(new Comment("Clean up shadow space and " + numberOfPushedArgs + " pushed arg(s)"));
            add(new AddImmToReg(stackSpace.toString(), RSP));
        }
    }

    /**
     * Generates code for moving the argument pointed to by {@code index} to the given register.
     *
     * @param storedArgs An array of all arguments stored in temporary storage.
     * @param index The index of the argument to move.
     * @param register The register to move the argument to.
     */
    private void moveArgToRegister(List<StorageLocation> storedArgs, int index, Register register) {
        if (storedArgs.size() > index) {
            try (StorageLocation location = storageFactory.allocate(register)) {
                location.moveLocToThis(storedArgs.get(index), this);
            }
        }
    }

    /**
     * Processes the first expression in the list by removing it from the expression list, evaluating it,
     * and adding the result to the {@code storedArgs} list.
     */
    private void processFirstExpression(List<Expression> expressions, List<StorageLocation> storedArgs) {
        StorageLocation location = storageFactory.allocateNonVolatile();
        try {
            expression(expressions.remove(0), location);
            storedArgs.add(location);
        } catch (RuntimeException e) {
            location.close();
            throw e;
        }
    }

    // -----------------------------------------------------------------------
    // Comments:
    // -----------------------------------------------------------------------

    protected void addFormattedComment(Node node) {
        add(formatComment(node));
    }

    protected Comment formatComment(Node node) {
        return new Comment(node.getLine() + ": " + format(node));
    }

    private String format(Node node) {
        String s = node.toString();
        return (s.length() > 33) ? s.substring(0, 30) + "..." : s;
    }

    protected void addDependency(String function, String library) {
        dependencies.computeIfAbsent(library, k -> new HashSet<>()).add(function);
    }
}
