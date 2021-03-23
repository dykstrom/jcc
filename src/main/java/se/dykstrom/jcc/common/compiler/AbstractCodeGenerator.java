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
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.storage.*;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_EXIT;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_STRCMP;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateConstantIntegerExpressions;

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

    static final String SHADOW_SPACE = "20h";

    protected final StorageFactory storageFactory = new StorageFactory();

    protected final SymbolTable symbols = new SymbolTable();

    protected final AstOptimizer optimizer;
    protected final TypeManager typeManager;

    protected final Map<String, Set<String>> dependencies = new HashMap<>();

    /** All built-in functions that have actually been called, and needs to be linked into the program. */
    private final Set<AssemblyFunction> usedBuiltInFunctions = new HashSet<>();

    /** Helper class to generate code for function calls. */
    FunctionCallHelper functionCallHelper;

    /** Indexing all static strings in the code, helping to create a unique name for each. */
    private int stringIndex = 0;

    /** Indexing all static floats in the code, helping to create a unique name for each. */
    private int floatIndex = 0;

    /** Indexing all labels in the code, helping to create a unique name for each. */
    private int labelIndex = 0;

    protected AbstractCodeGenerator(TypeManager typeManager, AstOptimizer optimizer) {
        this.optimizer = optimizer;
        this.typeManager = typeManager;
        this.functionCallHelper = new DefaultFunctionCallHelper(this, this, storageFactory, typeManager);
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
        // so they are later added to the data section. Using type I64 is OK, because they
        // either contain an integer, or an address to a string.
        storageFactory.getMemoryManager().getUsedMemoryAddresses().stream()
            .sorted()
            .forEach(name -> symbols.addVariable(new Identifier(name, I64.INSTANCE)));

        // Always define an empty string constant
        symbols.addConstant(new Identifier(Str.EMPTY_STRING_NAME, Str.INSTANCE), Str.EMPTY_STRING_VALUE);

        Section section = new DataSection();

        // Add one data definition for each scalar identifier
        List<Identifier> identifiers = new ArrayList<>(symbols.identifiers());
        Collections.sort(identifiers);
        addScalarDataDefinitions(identifiers, symbols, section);

        // Add one data definition for each array identifier
        identifiers = new ArrayList<>(symbols.arrayIdentifiers());
        Collections.sort(identifiers);
        addArrayDataDefinitions(identifiers, symbols, section);

        section.add(Blank.INSTANCE);
        return section;
    }

    /**
     * Adds data definitions for all identifiers to the given code section.
     */
    protected void addScalarDataDefinitions(List<Identifier> identifiers, SymbolTable symbols, Section section) {
        identifiers.forEach(identifier -> section.add(
                new DataDefinition(identifier, (String) symbols.getValue(identifier.getName()), symbols.isConstant(identifier.getName())))
        );
    }

    /**
     * Adds data definitions for all array identifiers to the given code section.
     */
    protected void addArrayDataDefinitions(List<Identifier> identifiers, SymbolTable symbols, Section section) {
        identifiers.forEach(identifier -> {
            Arr array = (Arr) identifier.getType();
            int numberOfDimensions = array.getDimensions();
            List<Expression> subscripts = symbols.getArrayValue(identifier.getName()).getSubscripts();
            List<Long> evaluatedSubscripts = evaluateConstantIntegerExpressions(subscripts, optimizer.expressionOptimizer());

            // Add a data definition for each dimension, in reverse order
            for (int dimension = numberOfDimensions - 1; dimension >= 0; dimension--) {
                Identifier ident = deriveDimensionIdentifier(identifier, dimension);
                Long subscript = evaluatedSubscripts.get(dimension);
                section.add(new DataDefinition(ident, subscript.toString(), true));
            }

            // Add a data definition for the number of dimensions
            Identifier numDimsIdent = new Identifier(identifier.getName() + "_num_dims", I64.INSTANCE);
            section.add(new DataDefinition(numDimsIdent, Integer.toString(numberOfDimensions), true));

            // Add a data definition for the actual array, with one instance of the default value for each element
            Type elementType = array.getElementType();
            Identifier ident = deriveArrayIdentifier(identifier);
            String defaultValue = elementType.getDefaultValue();
            long numberOfElements = evaluatedSubscripts.stream().reduce(1L, (a, b) -> a * b);
            String arrayValue = numberOfElements + " dup " + defaultValue;
            section.add(new DataDefinition(ident, arrayValue, false));
        });
    }

    /**
     * Derives an identifier for the "array start" property of the array identified by {@code arrayIdentifier}.
     *
     * @param arrayIdentifier An identifier that identifies the array.
     * @return The derived identifier.
     */
    private Identifier deriveArrayIdentifier(Identifier arrayIdentifier) {
        Arr array = (Arr) arrayIdentifier.getType();
        return new Identifier(arrayIdentifier.getName() + "_arr", array.getElementType());
    }

    /**
     * Derives an identifier for the "dimension" property of dimension number {@code dimensionIndex}
     * of the array identified by {@code arrayIdentifier}.
     *
     * @param arrayIdentifier An identifier that identifies an array.
     * @param dimensionIndex The index of the dimension for which to derive an identifier.
     * @return The derived identifier.
     */
    private Identifier deriveDimensionIdentifier(Identifier arrayIdentifier, int dimensionIndex) {
        return new Identifier(arrayIdentifier.getName() + "_dim_" + dimensionIndex, I64.INSTANCE);
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
     * Generates code for the given statement. Language specific statements should be handled
     * in the overridden method in the language specific subclass.
     */
    protected void statement(Statement statement) {
        if (statement instanceof AddAssignStatement) {
            addAssignStatement((AddAssignStatement) statement);
        } else if (statement instanceof AssignStatement) {
            assignStatement((AssignStatement) statement);
        } else if (statement instanceof DecStatement) {
            decStatement((DecStatement) statement);
        } else if (statement instanceof IfStatement) {
            ifStatement((IfStatement) statement);
        } else if (statement instanceof IncStatement) {
            incStatement((IncStatement) statement);
        } else if (statement instanceof SubAssignStatement) {
            subAssignStatement((SubAssignStatement) statement);
        } else if (statement instanceof VariableDeclarationStatement) {
            variableDeclarationStatement((VariableDeclarationStatement) statement);
        } else if (statement instanceof WhileStatement) {
            whileStatement((WhileStatement) statement);
        }
    }

    /**
     * Generates code for an assignment statement.
     */
    protected void assignStatement(AssignStatement statement) {
        addLabel(statement);

        // Find type of variable
        Type lhsType = statement.getIdentifier().getType();
        // Find type of expression
        Type rhsType = typeManager.getType(statement.getExpression());

        // Add variable to symbol table
        symbols.addVariable(statement.getIdentifier());

        // Allocate temporary storage for variable (actually for result of evaluating RHS expression)
        try (StorageLocation location = storageFactory.allocateNonVolatile(lhsType)) {
            // If this location cannot store the expression type, we need to allocate temporary storage
            if (!location.stores(rhsType)) {
                try (StorageLocation rhsLocation = storageFactory.allocateNonVolatile(rhsType)) {
                    // Evaluate expression
                    expression(statement.getExpression(), rhsLocation);
                    // Cast RHS value to LHS type
                    add(new Comment("Cast " + rhsType + " (" + rhsLocation + ") to " + lhsType + " (" + location + ")"));
                    location.convertAndMoveLocToThis(rhsLocation, this);
                }
            } else {
                // Evaluate expression
                expression(statement.getExpression(), location);
            }

            // Store result in identifier
            addFormattedComment(statement);
            // Finally move result to variable
            location.moveThisToMem(statement.getIdentifier().getMappedName(), this);
        }
    }

    /**
     * Generates code for an add-assignment statement.
     */
    private void addAssignStatement(AddAssignStatement statement) {
        addLabel(statement);

        // Find type of variable
        Type lhsType = statement.getIdentifier().getType();
        // Add variable to symbol table
        symbols.addVariable(statement.getIdentifier());

        // Allocate temporary storage for variable
        try (StorageLocation location = storageFactory.allocateNonVolatile(lhsType)) {
            // Store result in identifier
            addFormattedComment(statement);
            // Add literal value to variable
            location.addImmToMem(statement.getExpression().getValue(), statement.getIdentifier().getMappedName(), this);
        }
    }

    /**
     * Generates code for a sub-assignment statement.
     */
    private void subAssignStatement(SubAssignStatement statement) {
        addLabel(statement);

        // Find type of variable
        Type lhsType = statement.getIdentifier().getType();
        // Add variable to symbol table
        symbols.addVariable(statement.getIdentifier());

        // Allocate temporary storage for variable
        try (StorageLocation location = storageFactory.allocateNonVolatile(lhsType)) {
            // Store result in identifier
            addFormattedComment(statement);
            // Subtract literal value from variable
            location.subtractImmFromMem(statement.getExpression().getValue(), statement.getIdentifier().getMappedName(), this);
        }
    }

    /**
     * Generates code for a variable declaration statement.
     */
    private void variableDeclarationStatement(VariableDeclarationStatement statement) {
        // For each declaration
        statement.getDeclarations().forEach(declaration -> {
            // Add variable to symbol table
            if (declaration.getType() instanceof Arr) {
                symbols.addArray(new Identifier(declaration.getName(), declaration.getType()), (ArrayDeclaration) declaration);
                // For $DYNAMIC arrays we also need to add initialization code here
            } else {
                symbols.addVariable(new Identifier(declaration.getName(), declaration.getType()));
            }
        });
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
    private void ifStatement(IfStatement statement) {
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
        if (statement.getElseStatements() != null) {
            add(Blank.INSTANCE);
            statement.getElseStatements().forEach(this::statement);
            add(afterElseLabel);
        }
    }

    /**
     * Generates code for a while statement.
     */
    private void whileStatement(WhileStatement statement) {
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

    /**
     * Generates code for a decrement statement.
     */
    private void decStatement(DecStatement statement) {
        Identifier identifier = statement.getIdentifier();
        if (identifier.getType() instanceof I64) {
            addFormattedComment(statement);
            add(new DecMem(identifier.getMappedName()));
            // Add variable to symbol table
            symbols.addVariable(identifier);
        } else {
            throw new IllegalArgumentException("decrease '" + identifier.getName() + " : " + identifier.getType() + "' not supported");
        }
    }

    /**
     * Generates code for an increment statement.
     */
    private void incStatement(IncStatement statement) {
        Identifier identifier = statement.getIdentifier();
        if (identifier.getType() instanceof I64) {
            addFormattedComment(statement);
            add(new IncMem(statement.getIdentifier().getMappedName()));
            // Add variable to symbol table
            symbols.addVariable(statement.getIdentifier());
        } else {
            throw new IllegalArgumentException("increase '" + identifier.getName() + " : " + identifier.getType() + "' not supported");
        }
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
        } else if (expression instanceof ArrayAccessExpression) {
            arrayAccessExpression((ArrayAccessExpression) expression, location);
        } else if (expression instanceof BooleanLiteral) {
            booleanLiteral((BooleanLiteral) expression, location);
        } else if (expression instanceof DivExpression) {
            divExpression((DivExpression) expression, location);
        } else if (expression instanceof EqualExpression) {
            equalExpression((EqualExpression) expression, location);
        } else if (expression instanceof EvaluatedExpression) {
            evaluatedExpression((EvaluatedExpression) expression, location);
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
        } else if (expression instanceof ShiftLeftExpression) {
            salExpression((ShiftLeftExpression) expression, location);
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
            savedLocation.convertAndMoveLocToThis(location, this);
            // Free the temporary storage location again
            location.close();
        }
    }

    private void arrayAccessExpression(ArrayAccessExpression expression, StorageLocation location) {
        addFormattedComment(expression);

        // Get subscripts
        List<Expression> subscripts = expression.getSubscripts();

        // We don't know if 'location' can store integers, so we allocate two temporary storage locations
        try (StorageLocation accumulator = storageFactory.allocateNonVolatile();
             StorageLocation temp = storageFactory.allocateNonVolatile()) {
            // Evaluate first subscript expression
            expression(subscripts.get(0), accumulator);

            // For each remaining dimension
            for (int i = 1; i < subscripts.size(); i++) {
                // Multiply accumulator with size of dimension
                Identifier ident = deriveDimensionIdentifier(expression.getIdentifier(), i);
                temp.moveMemToThis(ident.getMappedName(), this);
                accumulator.multiplyLocWithThis(temp, this);
                // Evaluate subscript expression and add to accumulator
                expression(subscripts.get(i), temp);
                accumulator.addLocToThis(temp, this);
            }

            // If start of array is "_c%_arr" and offset is "rsi" (accumulator),
            // then generated code will be: mov rdi, [_c%_arr + 8 * rsi]

            Identifier ident = deriveArrayIdentifier(expression.getIdentifier());
            location.moveMemToThis(ident.getMappedName(), 8, ((RegisterStorageLocation) accumulator).getRegister(), this);
        }

        add(Blank.INSTANCE);
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
        addFunctionCall(function, formatComment(expression), args, location);
        add(Blank.INSTANCE);
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

    private void evaluatedExpression(EvaluatedExpression expression, StorageLocation location) {
        addFormattedComment(expression);
        location.moveLocToThis(expression.getLocation(), this);
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

    protected void identifierDerefExpression(IdentifierDerefExpression expression, StorageLocation location) {
        addFormattedComment(expression);
        // Store the identifier contents (not its address)
        location.moveMemToThis(expression.getIdentifier().getMappedName(), this);
    }

    private void identifierNameExpression(IdentifierNameExpression expression, StorageLocation location) {
        addFormattedComment(expression);
        // Store the identifier address (not its contents)
        location.moveImmToThis(expression.getIdentifier().getMappedName(), this);
    }

    /**
     * Generates code for evaluating an add expression. This method can only add integers and
     * floats - not strings.
     */
    protected void addExpression(AddExpression expression, StorageLocation leftLocation) {
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

    private void salExpression(ShiftLeftExpression expression, StorageLocation leftLocation) {
        // Generate code for left sub expression, and store result in leftLocation
        expression(expression.getLeft(), leftLocation);

        // Shift expressions always have a right sub expression of type integer
        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile(I64.INSTANCE)) {
            // Generate code for right sub expression, and store result in rightLocation
            expression(expression.getRight(), rightLocation);
            // Generate code for shifting left expression, and store result in leftLocation
            addFormattedComment(expression);
            leftLocation.shiftThisLeftByLoc(rightLocation, this);
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

        // Generate code for comparing the result of calling strcmp with 0, and store result in leftLocation
        leftLocation.compareThisWithImm("0", this);
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
    protected String uniqifyLabelName(String prefix) {
        return prefix + labelIndex++;
    }

    // -----------------------------------------------------------------------
    // Functions:
    // -----------------------------------------------------------------------

    /**
     * Adds code for making the given {@code functionCall}. This method is for cases when
     * you don't care about the function return value.
     */
    protected void addFunctionCall(se.dykstrom.jcc.common.functions.Function function, Comment functionComment, List<Expression> args) {
        addFunctionCall(function, functionComment, args, null);
    }

    /**
     * Adds code for making the given {@code functionCall}. For more information, see method
     * {@link DefaultFunctionCallHelper#addFunctionCall(se.dykstrom.jcc.common.functions.Function, Call, Comment, List, StorageLocation)}.
     */
    protected void addFunctionCall(se.dykstrom.jcc.common.functions.Function function, Comment functionComment, List<Expression> args, StorageLocation returnLocation) {
        // Add dependencies needed by this function
        addAllFunctionDependencies(function.getDependencies());
        addAllConstantDependencies(function.getConstants());

        // Create function call
        Call functionCall;
        if (function instanceof AssemblyFunction) {
            functionCall = new CallDirect(new Label(function.getMappedName()));
            // Remember that we have used this function
            addUsedBuiltInFunction((AssemblyFunction) function);
        } else if (function instanceof LibraryFunction) {
            functionCall = new CallIndirect(new FixedLabel(function.getMappedName()));
        } else {
            throw new IllegalStateException("function '" + function.getName() + "' with unknown type: " + function.getClass().getSimpleName());
        }

        functionCallHelper.addFunctionCall(function, functionCall, functionComment, args, returnLocation);
    }

    /**
     * Generates code to define all the built-in functions that have actually been used in the program.
     */
    protected CodeContainer builtInFunctions() {
        CodeContainer codeContainer = new CodeContainer();

        if (!usedBuiltInFunctions.isEmpty()) {
            codeContainer.add(Blank.INSTANCE);
            codeContainer.add(new Comment("--- Built-in functions ---"));

            // For each built-in function that has been used
            usedBuiltInFunctions.forEach(function -> {
                codeContainer.add(Blank.INSTANCE);
                codeContainer.add(new Comment(function.toString()));

                // Add label for start of function
                codeContainer.add(new Label(function.getMappedName()));

                // Add function code lines
                codeContainer.addAll(function.codes());
            });

            codeContainer.add(Blank.INSTANCE);
            codeContainer.add(new Comment("--- Built-in functions ---"));
        }

        return codeContainer;
    }

    protected void addUsedBuiltInFunction(AssemblyFunction function) {
        usedBuiltInFunctions.add(function);
    }

    private void addFunctionDependency(se.dykstrom.jcc.common.functions.Function function, String library) {
        if (function instanceof AssemblyFunction) {
            // If this is an assembly function, remember that it has been used
            addUsedBuiltInFunction((AssemblyFunction) function);
        } else {
            // Otherwise, add it as a library dependency
            dependencies.computeIfAbsent(library, k -> new HashSet<>()).add(function.getName());
        }
        // Add all dependencies this function has
        addAllFunctionDependencies(function.getDependencies());
        addAllConstantDependencies(function.getConstants());
    }

    @Override
    public void addAllFunctionDependencies(Map<String, Set<se.dykstrom.jcc.common.functions.Function>> dependencies) {
        dependencies.forEach((key, value) -> value.forEach(function -> addFunctionDependency(function, key)));
    }

    void addAllConstantDependencies(Set<Constant> dependencies) {
        dependencies.forEach(symbols::addConstant);
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
     * Converts a line number or line label to a Label object.
     */
    protected Label lineToLabel(String label) {
        return new Label("_line_" + label);
    }
}
