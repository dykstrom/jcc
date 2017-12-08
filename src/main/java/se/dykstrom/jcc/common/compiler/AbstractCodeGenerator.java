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
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;

/**
 * Abstract base class for all code generators.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractCodeGenerator extends CodeContainer {

    protected static final Label FUNC_EXIT = new FixedLabel("exit");
    protected static final Label FUNC_PRINTF = new FixedLabel("printf");
    protected static final Label FUNC_SCANF = new FixedLabel("scanf");
    protected static final Label FUNC_STRCMP = new FixedLabel("strcmp");
    
    private static final Label LABEL_MAIN = new Label("_main");
    private static final Label LABEL_ANON_FWD = new FixedLabel("@f");
    private static final Label LABEL_ANON_TARGET = new FixedLabel("@@");

    protected final StorageFactory storageFactory = new StorageFactory();

    protected final SymbolTable symbols = new SymbolTable();

    protected final Map<String, Set<String>> dependencies = new HashMap<>();

    /** All built-in functions that have actually been called, and needs to be linked into the program. */
    protected final Set<AssemblyFunction> usedBuiltInFunctions = new HashSet<>();
    
    /** Indexing all static strings in the code, helping to create a unique name for each. */
    private int stringIndex = 0;

    /** Indexing all labels in the code, helping to create a unique name for each. */
    private int labelIndex = 0;

    /**
     * Returns a reference to the symbol table.
     */
    public SymbolTable getSymbols() {
        return symbols;
    }
    
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
        // Add all temporary memory addresses from the storage factory to the symbol table,
        // so they are later added to the data section. Using type I64 is OK, becuase they
        // either contain an integer, or an address to a string.
        storageFactory.getMemoryManager().getUsedMemoryAddresses().stream()
            .sorted()
            .forEach(name -> symbols.addVariable(new Identifier(name, I64.INSTANCE)));
        
        // An empty data section results in an invalid executable
        if (symbols.isEmpty()) {
            symbols.addVariable(new Identifier("_dummy", I64.INSTANCE));
        }

        List<Identifier> identifiers = new ArrayList<>(symbols.identifiers());
        Collections.sort(identifiers);

        Section section = new DataSection();

        // Add one data definition for each identifier, except for functions, that are defined elsewhere
        identifiers.forEach(identifier -> section.add(
                new DataDefinition(identifier, identifier.getType(), (String) symbols.getValue(identifier.getName()), symbols.isConstant(identifier.getName()))
        ));
        section.add(Blank.INSTANCE);

        return section;
    }

    protected Section codeSection(List<Code> codes) {
        Section section = new CodeSection();

        // Add start of main program
        section.add(LABEL_MAIN);

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

    // -----------------------------------------------------------------------
    // Statements:
    // -----------------------------------------------------------------------

    /**
     * Generates code for the given statement.
     */
    protected abstract void statement(Statement statement);

    /**
     * Generates code for an assignment statement.
     */
    protected void assignStatement(AssignStatement statement) {
        symbols.addVariable(statement.getIdentifier());
        addLabel(statement);

        // Allocate storage for evaluated expression
        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            // Evaluate expression
            expression(statement.getExpression(), location);
            // Store result in identifier
            addFormattedComment(statement);
            location.moveThisToMem(statement.getIdentifier().getMappedName(), this);
        }
    }
    
    /**
     * Generates code for an exit statement.
     * 
     * @param expression The exit status expression.
     * @param label The statement label, or {@code null} if no label.
     */
    protected void exitStatement(Expression expression, String label) {
        addDependency(FUNC_EXIT.getName(), CompilerUtils.LIB_LIBC);
        ExitStatement statement = new ExitStatement(0, 0, expression, label);
        addLabel(statement);
        addFunctionCall(new CallIndirect(FUNC_EXIT), formatComment(statement), singletonList(expression));
    }

    /**
     * Generates code for an if statement.
     */
    protected void ifStatement(IfStatement statement) {
        addLabel(statement);
        
        // Generate unique label names
        Label afterThenLabel = new Label(uniqifyLabelName("after_then_"));
        Label afterElseLabel = new Label(uniqifyLabelName("after_else_"));

        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            // Generate code for the if expression
            expression(statement.getExpression(), location);
            add(Blank.INSTANCE);
            addFormattedComment(statement);
            // If FALSE, jump to ELSE clause
            location.compareThisWithImm("0", this); // Boolean FALSE
            add(new Je(afterThenLabel));
        }
        
        // Generate code for THEN clause
        add(Blank.INSTANCE);
        statement.getThenStatements().forEach(this::statement);
        if (!statement.getElseStatements().isEmpty()) {
            // Only generate jump if there actually is an else clause
            add(new Jmp(afterElseLabel));
        }
        add(afterThenLabel);
        
        // Generate code for ELSE clause
        if (!statement.getElseStatements().isEmpty()) {
            add(Blank.INSTANCE);
            statement.getElseStatements().forEach(this::statement);
            add(afterElseLabel);
        }
    }

    /**
     * Generates code for a while statement.
     */
    protected void whileStatement(WhileStatement statement) {
        addLabel(statement);
        
        // Generate unique label names
        Label beforeWhileLabel = new Label(uniqifyLabelName("before_while_"));
        Label afterWhileLabel = new Label(uniqifyLabelName("after_while_"));

        // Add a label before the WHILE test
        add(beforeWhileLabel);

        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            // Generate code for the expression
            expression(statement.getExpression(), location);
            add(Blank.INSTANCE);
            addFormattedComment(statement);
            // If FALSE, jump to after WHILE clause
            location.compareThisWithImm("0", this); // Boolean FALSE
            add(new Je(afterWhileLabel));
        }
        
        // Generate code for WHILE clause
        add(Blank.INSTANCE);
        statement.getStatements().forEach(this::statement);
        // Jump back to perform the test again
        add(new Jmp(beforeWhileLabel));
        // Add a label after the WHILE clause
        add(afterWhileLabel);
    }

    // -----------------------------------------------------------------------
    // Expressions:
    // -----------------------------------------------------------------------

    /**
     * Evaluate the given {@code expression}, and store the result in {@code location}.
     */
    protected void expression(Expression expression, StorageLocation location) {
        if (expression instanceof AddExpression) {
            addExpression((AddExpression) expression, location);
        } else if (expression instanceof AndExpression) {
            andExpression((AndExpression) expression, location);
        } else if (expression instanceof BooleanLiteral) {
            booleanLiteral((BooleanLiteral) expression, location);
        } else if (expression instanceof DivExpression) {
            divExpression((DivExpression) expression, location);
        } else if (expression instanceof EqualExpression) {
            equalExpression((EqualExpression) expression, location);
        } else if (expression instanceof FunctionCallExpression) {
            functionCallExpression((FunctionCallExpression) expression, location);
        } else if (expression instanceof GreaterExpression) {
            greaterExpression((GreaterExpression) expression, location);
        } else if (expression instanceof GreaterOrEqualExpression) {
            greaterOrEqualExpression((GreaterOrEqualExpression) expression, location);
        } else if (expression instanceof IdentifierDerefExpression) {
            identifierDerefExpression((IdentifierDerefExpression) expression, location);
        } else if (expression instanceof IdentifierNameExpression) {
            identifierNameExpression((IdentifierNameExpression) expression, location);
        } else if (expression instanceof IDivExpression) {
            idivExpression((IDivExpression) expression, location);
        } else if (expression instanceof IntegerLiteral) {
            integerLiteral((IntegerLiteral) expression, location);
        } else if (expression instanceof LessExpression) {
            lessExpression((LessExpression) expression, location);
        } else if (expression instanceof LessOrEqualExpression) {
            lessOrEqualExpression((LessOrEqualExpression) expression, location);
        } else if (expression instanceof ModExpression) {
            modExpression((ModExpression) expression, location);
        } else if (expression instanceof MulExpression) {
            mulExpression((MulExpression) expression, location);
        } else if (expression instanceof NotExpression) {
            notExpression((NotExpression) expression, location);
        } else if (expression instanceof NotEqualExpression) {
            notEqualExpression((NotEqualExpression) expression, location);
        } else if (expression instanceof OrExpression) {
            orExpression((OrExpression) expression, location);
        } else if (expression instanceof StringLiteral) {
            stringLiteral((StringLiteral) expression, location);
        } else if (expression instanceof SubExpression) {
            subExpression((SubExpression) expression, location);
        } else if (expression instanceof XorExpression) {
            xorExpression((XorExpression) expression, location);
        }
    }

    private void functionCallExpression(FunctionCallExpression expression, StorageLocation location) {
        String name = expression.getIdentifier().getName();
        
        // Get arguments
        List<Expression> args = expression.getArgs();
        // Get types of arguments
        TypeManager typeManager = getTypeManager();
        List<Type> argTypes = args.stream().map(typeManager::getType).collect(toList());

        // Get function from symbol table
        se.dykstrom.jcc.common.functions.Function function = symbols.getFunction(name, argTypes);

        // Add dependencies needed by this function
        addAllDependencies(function.getDependencies());
        
        // Create function call
        Call functionCall;
        if (function instanceof AssemblyFunction) {
            functionCall = new CallDirect(new Label(((AssemblyFunction) function).getMappedName()));
            // Remember that we have used this function
            usedBuiltInFunctions.add((AssemblyFunction) function);
        } else if (function instanceof LibraryFunction) {
            functionCall = new CallIndirect(new FixedLabel(((LibraryFunction) function).getFunctionName()));
        } else {
            throw new IllegalStateException("function '" + name + "' with unknown type: " + function.getClass().getSimpleName());
        }
        
        // Call function
        addFunctionCall(functionCall, formatComment(expression), args, location);
        // Move result of function call (RAX) to given storage location
        try (StorageLocation rax = storageFactory.allocate(RAX)) {
            location.moveLocToThis(rax, this);
        }
        add(Blank.INSTANCE);
    }

    private void booleanLiteral(BooleanLiteral expression, StorageLocation location) {
        addFormattedComment(expression);
        location.moveImmToThis(expression.getValue(), this);
    }

    private void integerLiteral(IntegerLiteral expression, StorageLocation location) {
        addFormattedComment(expression);
        location.moveImmToThis(expression.getValue(), this);
    }

    /**
     * Generates code for evaluating a string literal. This involves adding the string literal as a
     * constant to the symbol table, and generating code to move the constant to the given location.
     */
    private void stringLiteral(StringLiteral expression, StorageLocation location) {
        String value = "\"" + expression.getValue() + "\",0";

        // Try to find an existing string constant with this value
        Identifier identifier = symbols.getConstantByTypeAndValue(Str.INSTANCE, value);

        // If there was no string constant with this exact value before, create one
        if (identifier == null) {
            identifier = new Identifier(uniqifyStringName("_string_"), Str.INSTANCE);
            symbols.addConstant(identifier, value);
        }
        
        addFormattedComment(expression);
        // Store the identifier address (not its contents)
        location.moveImmToThis(identifier.getMappedName(), this);
    }

    private void identifierDerefExpression(IdentifierDerefExpression expression, StorageLocation location) {
        addFormattedComment(expression);
        // Store the identifier contents (not its address)
        location.moveMemToThis(expression.getIdentifier().getMappedName(), this);
    }

    private void identifierNameExpression(IdentifierNameExpression expression, StorageLocation location) {
        addFormattedComment(expression);
        // Store the identifier address (not its contents)
        location.moveImmToThis(expression.getIdentifier().getMappedName(), this);
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

    /**
     * Generates code for a floating point division.
     * 
     * At the moment, floating point numbers are not supported, 
     * so this method generates code for an integer division instead.
     */
    private void divExpression(DivExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for dividing sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.idivThisWithLoc(rightLocation, this);
        }
    }

    /**
     * Generates code for a signed integer division.
     */
    private void idivExpression(IDivExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for dividing sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.idivThisWithLoc(rightLocation, this);
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
            leftLocation.imulLocWithThis(rightLocation, this);
        }
    }

    private void modExpression(ModExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for doing modulo on sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.modThisWithLoc(rightLocation, this);
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

    private void equalExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Je::new);
    }

    private void notEqualExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jne::new);
    }

    private void greaterExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jg::new);
    }

    private void greaterOrEqualExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jge::new);
    }

    private void lessExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jl::new);
    }

    private void lessOrEqualExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jle::new);
    }

    /**
     * Generates code for the relational expression denoted by {@code expression},
     * storing the result in {@code leftLocation}. The given {@code function} should
     * be a function that takes a label as input, and returns a conditional jump
     * instruction to that label. As an example, for an equal expression (==) the 
     * given function should generate a JE instruction (jump if equal).
     */
    private void relationalExpression(BinaryExpression expression, 
                                      StorageLocation leftLocation, 
                                      Function<Label, Instruction> function) {
        Type type = getTypeManager().getType(expression.getLeft());
        if (type == Str.INSTANCE) {
            relationalStringExpression(expression, leftLocation, function);
        } else {
            relationalNumericExpression(expression, leftLocation, function);
        }
    }

    private void relationalNumericExpression(BinaryExpression expression, StorageLocation leftLocation, Function<Label, Instruction> function) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate a unique label name
            Label afterCmpLabel = new Label(uniqifyLabelName("after_cmp_"));

            // Generate code for comparing sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.compareThisWithLoc(rightLocation, this);
            add(function.apply(LABEL_ANON_FWD));
            leftLocation.moveImmToThis("0", this); // Boolean FALSE
            add(new Jmp(afterCmpLabel));
            add(LABEL_ANON_TARGET);
            leftLocation.moveImmToThis("-1", this); // Boolean TRUE
            add(afterCmpLabel);
        }
    }

    private void relationalStringExpression(BinaryExpression expression, StorageLocation leftLocation, Function<Label, Instruction> function) {
        addDependency(FUNC_STRCMP.getName(), CompilerUtils.LIB_LIBC);

        // Evaluate expresisons, and call strcmp, ending up with the result in RAX
        addFunctionCall(new CallIndirect(FUNC_STRCMP), formatComment(expression), asList(expression.getLeft(), expression.getRight()), leftLocation);
        
        // Generate a unique label name
        Label afterCmpLabel = new Label(uniqifyLabelName("after_cmp_"));

        // Generate code for comparing the result of calling strcmp (RAX) with 0, and store result in leftLocation
        add(new CmpRegWithImm(RAX, "0"));
        add(function.apply(LABEL_ANON_FWD));
        leftLocation.moveImmToThis("0", this); // Boolean FALSE
        add(new Jmp(afterCmpLabel));
        add(LABEL_ANON_TARGET);
        leftLocation.moveImmToThis("-1", this); // Boolean TRUE
        add(afterCmpLabel);
    }

    private void andExpression(AndExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for and:ing sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.andLocWithThis(rightLocation, this);
        }
    }

    private void orExpression(OrExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for or:ing sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.orLocWithThis(rightLocation, this);
        }
    }

    private void xorExpression(XorExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for xor:ing sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.xorLocWithThis(rightLocation, this);
        }
    }

    private void notExpression(NotExpression expression, StorageLocation leftLocation) {
        // Generate code for sub expression, and store result in leftLocation
        expression(expression.getExpression(), leftLocation);
        // Generate code for not:ing sub expression, and store result in leftLocation
        addFormattedComment(expression);
        leftLocation.notThis(this);
    }

    /**
     * Creates a unique string name from the given prefix.
     */
    private String uniqifyStringName(String prefix) {
        return prefix + stringIndex++;
    }

    /**
     * Creates a unique label name from the given prefix.
     */
    protected String uniqifyLabelName(String prefix) {
        return prefix + labelIndex++;
    }

    // -----------------------------------------------------------------------
    // Function calls:
    // -----------------------------------------------------------------------

    /**
     * Adds code for making the given {@code functionCall}. For more information, see method
     * {@link #addFunctionCall(Call, Comment, List, StorageLocation).
     */
    protected void addFunctionCall(Call functionCall, Comment functionComment, List<Expression> args) {
        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            addFunctionCall(functionCall, functionComment, args, location);
        }
    }
        
    /**
     * Adds code for making the given {@code functionCall}. The list of expressions is evaluated, and the
     * values are stored in the function call registers (RCX, RDX, R8, and R9) and on the stack if needed.
     * Shadow space is also allocated and cleaned up if needed. The already allocated {@link StorageLocation}
     * given to this method is used as the first storage location when evaluating the function argument 
     * expressions. If more storage locations are required, they are allocated and deallocated inside the
     * method.
     *
     * @param functionCall The function call to make.
     * @param functionComment A function call comment to insert before calling the function.
     * @param args The arguments to the function.
     * @param firstLocation An already allocated storage location to use when evaluating expressions.
     */
    protected void addFunctionCall(Call functionCall, Comment functionComment, List<Expression> args, StorageLocation firstLocation) {
        List<Expression> expressions = new ArrayList<>(args);
        
        // Evaluate first argument
        if (!expressions.isEmpty()) {
            expression(expressions.remove(0), firstLocation);
        }
        
        // Evaluate the next three arguments (if there are so many)
        List<StorageLocation> storedArgs = new ArrayList<>();
        while (!expressions.isEmpty() && storedArgs.size() < 3) {
            removeAndEvaluateFirstExpression(expressions, storedArgs);
        }

        // Evaluate any extra arguments
        int numberOfPushedArgs = 0;
        // Check that there actually _are_ extra arguments
        if (!expressions.isEmpty()) {
            try (StorageLocation location = storageFactory.allocateNonVolatile()) {
                // Push arguments in reverse order
                for (int i = expressions.size() - 1; i >= 0; i--) {
                    expression(expressions.get(i), location);
                    location.pushThis(this);
                    numberOfPushedArgs++;
                }
            }
        }

        // Move register arguments to function call registers
        if (!args.isEmpty()) {
            try (StorageLocation location = storageFactory.allocate(RCX)) {
                location.moveLocToThis(firstLocation, this);
            }
        }
        moveArgToRegister(storedArgs, 0, RDX);
        moveArgToRegister(storedArgs, 1, R8);
        moveArgToRegister(storedArgs, 2, R9);

        // Clean up register arguments
        storedArgs.forEach(StorageLocation::close);

        // If any args were pushed on the stack, we must allocate new shadow space before calling the function
        // Otherwise, we let the called function reuse the shadow space of this function?
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
     * Generates code for moving the result of evaluating the argument pointed to by {@code index}
     * to the given register.
     *
     * @param storedArgs A list of all arguments stored in temporary storage.
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
    private void removeAndEvaluateFirstExpression(List<Expression> expressions, List<StorageLocation> storedArgs) {
        StorageLocation location = storageFactory.allocateNonVolatile();
        try {
            expression(expressions.remove(0), location);
            storedArgs.add(location);
        } catch (RuntimeException e) {
            location.close();
            throw e;
        }
    }

    /**
     * Generates code to define all the built-in functions that have actually been used in the program.
     */
    protected CodeContainer builtInFunctions(SymbolTable symbols) {
        CodeContainer codeContainer = new CodeContainer();

        if (!usedBuiltInFunctions.isEmpty()) {
            codeContainer.add(Blank.INSTANCE);
            codeContainer.add(new Comment("Function definitions"));
        }
        
        // For each built-in function that has been used
        usedBuiltInFunctions.forEach(function -> {
            codeContainer.add(Blank.INSTANCE);
            codeContainer.add(new Comment(function.toString()));

            // Add label for start of function
            codeContainer.add(new Label(function.getMappedName()));

            // Add function code lines
            codeContainer.addAll(function.codes());
        });

        return codeContainer;
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
        return (s.length() > 53) ? s.substring(0, 50) + "..." : s;
    }

    protected void addDependency(String function, String library) {
        dependencies.computeIfAbsent(library, k -> new HashSet<>()).add(function);
    }

    protected void addAllDependencies(Map<String, Set<String>> dependencies) {
        dependencies.forEach((key, value) -> value.forEach(function -> addDependency(function, key)));
    }
    
    /**
     * Adds a label before this statement, if there is a label defined.
     */
    protected void addLabel(Statement statement) {
        if (statement.getLabel() != null) {
            add(lineToLabel(statement.getLabel()));
        }
    }

    /**
     * Converts a line number to a label.
     */
    protected Label lineToLabel(Object line) {
        return new Label("_line_" + line);
    }

    // -----------------------------------------------------------------------
    // Types:
    // -----------------------------------------------------------------------

    /**
     * Returns the type manager of the concrete code generator, or {@code null} if the code generator does not have any type manager.
     */
    protected abstract TypeManager getTypeManager();
}
