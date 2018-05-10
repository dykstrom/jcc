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
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_EXIT;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_STRCMP;

/**
 * Abstract base class for all code generators.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractCodeGenerator extends CodeContainer implements CodeGenerator {

    private static final Label LABEL_ANON_FWD = new FixedLabel("@f");
    private static final Label LABEL_ANON_TARGET = new FixedLabel("@@");
    private static final Label LABEL_EXIT = new FixedLabel(FUN_EXIT.getMappedName());
    private static final Label LABEL_MAIN = new Label("_main");

    protected final StorageFactory storageFactory = new StorageFactory();

    protected final SymbolTable symbols = new SymbolTable();

    protected final TypeManager typeManager;

    protected final Map<String, Set<String>> dependencies = new HashMap<>();

    /** All built-in functions that have actually been called, and needs to be linked into the program. */
    private final Set<AssemblyFunction> usedBuiltInFunctions = new HashSet<>();

    /** Helper class to generate code for function calls. */
    private final FunctionCallHelper functionCallHelper;

    /** Indexing all static strings in the code, helping to create a unique name for each. */
    private int stringIndex = 0;

    /** Indexing all static floats in the code, helping to create a unique name for each. */
    private int floatIndex = 0;

    /** Indexing all labels in the code, helping to create a unique name for each. */
    private int labelIndex = 0;

    protected AbstractCodeGenerator(TypeManager typeManager) {
        this.typeManager = typeManager;
        this.functionCallHelper = new FunctionCallHelper(this, this, storageFactory, typeManager);
    }

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
        Prologue prologue = new Prologue(storageFactory.getRegisterManager().getUsedNonVolatileRegisters(),
                                         storageFactory.getFloatRegisterManager().getUsedNonVolatileRegisters());
        prologue.codes().forEach(section::add);

        // Add function code
        codes.forEach(section::add);

        return section;
    }

    /**
     * Returns {@code true} if the program contains at least one call to exit.
     */
    protected boolean containsExit() {
        return contains(new CallIndirect(LABEL_EXIT));
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

        // Find type of expression
        Type type = typeManager.getType(statement.getExpression());

        // Allocate storage for evaluated expression
        try (StorageLocation location = storageFactory.allocateNonVolatile(type)) {
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
        ExitStatement statement = new ExitStatement(0, 0, expression, label);
        addLabel(statement);
        addFunctionCall(FUN_EXIT, formatComment(statement), singletonList(expression));
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

    @Override
    public void expression(Expression expression, StorageLocation location) {
        StorageLocation savedLocation = null;

        // If the current storage location cannot store the expression value,
        // we introduce a temporary storage location and add a later type cast
        Type type = typeManager.getType(expression);
        if (!location.stores(type)) {
            savedLocation = location;
            location = storageFactory.allocateNonVolatile(type);
        }

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
        } else if (expression instanceof FloatLiteral) {
            floatLiteral((FloatLiteral) expression, location);
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

        // If we have a saved location, and thus also a temporary location, we need to add a type cast
        if (savedLocation != null) {
            add(new Comment("Cast temporary " + type + " expression: " + expression));
            // Moving the value from one location to another will automatically type cast it
            savedLocation.moveLocToThis(location, this);
            // Free the temporary storage location again
            location.close();
        }
    }

    private void functionCallExpression(FunctionCallExpression expression, StorageLocation location) {
        String name = expression.getIdentifier().getName();
        
        // Get arguments
        List<Expression> args = expression.getArgs();
        // Get types of arguments
        List<Type> argTypes = typeManager.getTypes(args);

        // Get function from symbol table
        se.dykstrom.jcc.common.functions.Function function = typeManager.resolveFunction(name, argTypes, symbols);

        // Call function
        add(Blank.INSTANCE);
        add(new Comment("Evaluate arguments for call " + expression));
        addFunctionCall(function, formatComment(expression), args, location);
        // Move result of function call to given storage location
        moveResultToStorageLocation(function, location);
        add(Blank.INSTANCE);
    }

    private void moveResultToStorageLocation(se.dykstrom.jcc.common.functions.Function function, StorageLocation location) {
        if (function.getReturnType() instanceof F64) {
            add(new Comment("Move result of call (xmm0) to storage location (" + location + ")"));
            location.moveLocToThis(storageFactory.xmm0, this);
        } else {
            add(new Comment("Move result of call (rax) to storage location (" + location + ")"));
            location.moveLocToThis(storageFactory.rax, this);
        }
    }

    private void booleanLiteral(BooleanLiteral expression, StorageLocation location) {
        addFormattedComment(expression);
        location.moveImmToThis(expression.getValue(), this);
    }

    private void floatLiteral(FloatLiteral expression, StorageLocation location) {
        String value = expression.getValue();

        // Try to find an existing float constant with this value
        Identifier identifier = symbols.getConstantByTypeAndValue(F64.INSTANCE, value);

        // If there was no float constant with this exact value before, create one
        if (identifier == null) {
            identifier = new Identifier(getUniqueFloatName(), F64.INSTANCE);
            symbols.addConstant(identifier, value);
        }

        addFormattedComment(expression);
        // Store the identifier contents (not its address)
        location.moveMemToThis(identifier.getMappedName(), this);
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
            identifier = new Identifier(getUniqueStringName(), Str.INSTANCE);
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

        // Find type of right sub expression
        Type type = typeManager.getType(expression.getRight());

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile(type)) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for adding sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.addLocToThis(rightLocation, this);
        }
    }

    /**
     * Generates code for a floating point division.
     */
    private void divExpression(DivExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        // Find type of right sub expression
        Type type = typeManager.getType(expression.getRight());

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile(type)) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for dividing sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.divideThisWithLoc(rightLocation, this);
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

        // Find type of right sub expression
        Type type = typeManager.getType(expression.getRight());

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile(type)) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for multiplying sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.multiplyLocWithThis(rightLocation, this);
        }
    }

    private void modExpression(ModExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        // Find type of right sub expression
        Type type = typeManager.getType(expression.getRight());

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile(type)) {
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

        // Find type of right sub expression
        Type type = typeManager.getType(expression.getRight());

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile(type)) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for subtracting sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.subtractLocFromThis(rightLocation, this);
        }
    }

    private void equalExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Je::new, Je::new);
    }

    private void notEqualExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jne::new, Jne::new);
    }

    private void greaterExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jg::new, Ja::new);
    }

    private void greaterOrEqualExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jge::new, Jae::new);
    }

    private void lessExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jl::new, Jb::new);
    }

    private void lessOrEqualExpression(BinaryExpression expression, StorageLocation leftLocation) {
        relationalExpression(expression, leftLocation, Jle::new, Jbe::new);
    }

    /**
     * Generates code for the relational expression denoted by {@code expression},
     * storing the result in {@code leftLocation}. The functions {@code branchFunction}
     * and {@code floatBranchFunction} should be functions that take a label as input,
     * and return a conditional branch instruction to that label.
     *
     * As an example, for an equal expression (==) the given functions should both generate a
     * JE instruction (jump if equal). For a less than expression (<) the {@code branchFunction}
     * should generate a JL instruction. The {@code floatBranchFunction} should generate a
     * JB instruction, that is used after comparing floats.
     */
    private void relationalExpression(BinaryExpression expression, 
                                      StorageLocation leftLocation, 
                                      Function<Label, Instruction> branchFunction,
                                      Function<Label, Instruction> floatBranchFunction) {
        Type leftType = typeManager.getType(expression.getLeft());
        Type rightType = typeManager.getType(expression.getRight());

        if (leftType == Str.INSTANCE) {
            relationalStringExpression(expression, leftLocation, branchFunction);
        } else if (leftType == F64.INSTANCE || rightType == F64.INSTANCE) {
            relationalFloatExpression(expression, leftLocation, floatBranchFunction);
        } else {
            relationalIntegerExpression(expression, leftLocation, branchFunction);
        }
    }

    /**
     * Generates code for comparing one or more floating point values.
     */
    private void relationalFloatExpression(BinaryExpression expression, StorageLocation leftLocation, Function<Label, Instruction> branchFunction) {
        try (
            StorageLocation leftFloatLocation = storageFactory.allocateNonVolatile(F64.INSTANCE);
            StorageLocation rightFloatLocation = storageFactory.allocateNonVolatile(F64.INSTANCE)
        ) {
            // Generate code for left sub expression, and store result in leftFloatLocation
            expression(expression.getLeft(), leftFloatLocation);
            // Generate code for right sub expression, and store result in rightFloatLocation
            expression(expression.getRight(), rightFloatLocation);

            // Generate a unique label name
            Label afterCmpLabel = new Label(uniqifyLabelName("after_cmp_"));

            // Generate code for comparing sub expressions, and store result in leftLocation
            addFormattedComment(expression);
            leftFloatLocation.compareThisWithLoc(rightFloatLocation, this);
            add(branchFunction.apply(LABEL_ANON_FWD));
            leftLocation.moveImmToThis("0", this); // Boolean FALSE
            add(new Jmp(afterCmpLabel));
            add(LABEL_ANON_TARGET);
            leftLocation.moveImmToThis("-1", this); // Boolean TRUE
            add(afterCmpLabel);
        }
    }

    /**
     * Generates code for comparing two integer values.
     */
    private void relationalIntegerExpression(BinaryExpression expression, StorageLocation leftLocation, Function<Label, Instruction> branchFunction) {
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
            add(branchFunction.apply(LABEL_ANON_FWD));
            leftLocation.moveImmToThis("0", this); // Boolean FALSE
            add(new Jmp(afterCmpLabel));
            add(LABEL_ANON_TARGET);
            leftLocation.moveImmToThis("-1", this); // Boolean TRUE
            add(afterCmpLabel);
        }
    }

    /**
     * Generates code for comparing two string values.
     */
    private void relationalStringExpression(BinaryExpression expression, StorageLocation leftLocation, Function<Label, Instruction> branchFunction) {
        // Evaluate expressions, and call strcmp, ending up with the result in RAX
        addFunctionCall(FUN_STRCMP, formatComment(expression), asList(expression.getLeft(), expression.getRight()), leftLocation);
        
        // Generate a unique label name
        Label afterCmpLabel = new Label(uniqifyLabelName("after_cmp_"));

        // Generate code for comparing the result of calling strcmp (RAX) with 0, and store result in leftLocation
        add(new CmpRegWithImm(RAX, "0"));
        add(branchFunction.apply(LABEL_ANON_FWD));
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
     * Returns a unique string constant name to use in the symbol table.
     */
    private String getUniqueStringName() {
        return "_string_" + stringIndex++;
    }

    /**
     * Returns a unique floating point constant name to use in the symbol table.
     */
    private String getUniqueFloatName() {
        return "_float_" + floatIndex++;
    }

    /**
     * Creates a unique label name from the given prefix.
     */
    private String uniqifyLabelName(String prefix) {
        return prefix + labelIndex++;
    }

    // -----------------------------------------------------------------------
    // Functions:
    // -----------------------------------------------------------------------

    /**
     * Adds code for making the given {@code functionCall}. For more information, see method
     * {@link FunctionCallHelper#addFunctionCall(se.dykstrom.jcc.common.functions.Function, Call, Comment, List, StorageLocation)}.
     */
    protected void addFunctionCall(se.dykstrom.jcc.common.functions.Function function, Comment functionComment, List<Expression> args) {
        // Find type of first expression
        Type type = (args.size() > 0) ? typeManager.getType(args.get(0)) : I64.INSTANCE;

        try (StorageLocation location = storageFactory.allocateNonVolatile(type)) {
            addFunctionCall(function, functionComment, args, location);
        }
    }

    /**
     * Adds code for making the given {@code functionCall}. For more information, see method
     * {@link FunctionCallHelper#addFunctionCall(se.dykstrom.jcc.common.functions.Function, Call, Comment, List, StorageLocation)}.
     */
    private void addFunctionCall(se.dykstrom.jcc.common.functions.Function function, Comment functionComment, List<Expression> args, StorageLocation firstLocation) {
        // Add dependencies needed by this function
        addAllDependencies(function.getDependencies());

        // Create function call
        Call functionCall;
        if (function instanceof AssemblyFunction) {
            functionCall = new CallDirect(new Label(function.getMappedName()));
            // Remember that we have used this function
            usedBuiltInFunctions.add((AssemblyFunction) function);
        } else if (function instanceof LibraryFunction) {
            functionCall = new CallIndirect(new FixedLabel(function.getMappedName()));
        } else {
            throw new IllegalStateException("function '" + function.getName() + "' with unknown type: " + function.getClass().getSimpleName());
        }

        functionCallHelper.addFunctionCall(function, functionCall, functionComment, args, firstLocation);
    }

    /**
     * Generates code to define all the built-in functions that have actually been used in the program.
     */
    protected CodeContainer builtInFunctions() {
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

    private void addDependency(String function, String library) {
        dependencies.computeIfAbsent(library, k -> new HashSet<>()).add(function);
    }

    private void addAllDependencies(Map<String, Set<String>> dependencies) {
        dependencies.forEach((key, value) -> value.forEach(function -> addDependency(function, key)));
    }

    // -----------------------------------------------------------------------
    // Comments:
    // -----------------------------------------------------------------------

    protected void addFormattedComment(Node node) {
        add(formatComment(node));
    }

    protected Comment formatComment(Node node) {
        return new Comment((node.getLine() != 0 ? node.getLine() + ": " : "") + format(node));
    }

    private String format(Node node) {
        String s = node.toString();
        return (s.length() > 53) ? s.substring(0, 50) + "..." : s;
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
}
