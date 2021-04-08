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
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.code.expression.*;
import se.dykstrom.jcc.common.code.statement.*;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.storage.RegisterStorageLocation;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

import java.util.*;
import java.util.function.BiConsumer;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_EXIT;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateConstantIntegerExpressions;

/**
 * Abstract base class for all code generators.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractCodeGenerator extends CodeContainer implements CodeGenerator {

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

    /** Statement code generators */
    protected final AddAssignCodeGenerator addAssignCodeGenerator;
    protected final DecCodeGenerator decCodeGenerator;
    protected final IncCodeGenerator incCodeGenerator;
    protected final SubAssignCodeGenerator subAssignCodeGenerator;
    protected final VariableDeclarationCodeGenerator variableDeclarationCodeGenerator;

    /** Expression code generators */
    protected AddCodeGenerator addCodeGenerator;
    protected final AndCodeGenerator andCodeGenerator;
    protected final ArrayAccessCodeGenerator arrayAccessCodeGenerator;
    protected final BooleanLiteralCodeGenerator booleanLiteralCodeGenerator;
    protected final DivCodeGenerator divCodeGenerator;
    protected final EqualCodeGenerator equalCodeGenerator;
    protected final FloatLiteralCodeGenerator floatLiteralCodeGenerator;
    protected final FunctionCallCodeGenerator functionCallCodeGenerator;
    protected final GreaterCodeGenerator greaterCodeGenerator;
    protected final GreaterOrEqualCodeGenerator greaterOrEqualCodeGenerator;
    protected IdentifierDerefCodeGenerator identifierDerefCodeGenerator;
    protected final IdentifierNameCodeGenerator identifierNameCodeGenerator;
    protected final IDivCodeGenerator idivCodeGenerator;
    protected final IntegerLiteralCodeGenerator integerLiteralCodeGenerator;
    protected final LessCodeGenerator lessCodeGenerator;
    protected final LessOrEqualCodeGenerator lessOrEqualCodeGenerator;
    protected final ModCodeGenerator modCodeGenerator;
    protected final MulCodeGenerator mulCodeGenerator;
    protected final NotCodeGenerator notCodeGenerator;
    protected final NotEqualCodeGenerator notEqualCodeGenerator;
    protected final OrCodeGenerator orCodeGenerator;
    protected final ShiftLeftCodeGenerator shiftLeftCodeGenerator;
    protected final StringLiteralCodeGenerator stringLiteralCodeGenerator;
    protected final SubCodeGenerator subCodeGenerator;
    protected final XorCodeGenerator xorCodeGenerator;

    /** Helper class to generate code for function calls. */
    FunctionCallHelper functionCallHelper;

    /** Indexing all labels in the code, helping to create a unique name for each. */
    private int labelIndex = 0;

    protected AbstractCodeGenerator(TypeManager typeManager, AstOptimizer optimizer) {
        this.optimizer = optimizer;
        this.typeManager = typeManager;
        this.functionCallHelper = new DefaultFunctionCallHelper(this, this, storageFactory, typeManager);
        Context context = new Context(symbols, typeManager, storageFactory, this);
        // Statements
        this.addAssignCodeGenerator = new AddAssignCodeGenerator(context);
        this.decCodeGenerator = new DecCodeGenerator(context);
        this.incCodeGenerator = new IncCodeGenerator(context);
        this.subAssignCodeGenerator = new SubAssignCodeGenerator(context);
        this.variableDeclarationCodeGenerator = new VariableDeclarationCodeGenerator(context);
        // Expressions
        this.addCodeGenerator = new AddCodeGenerator(context);
        this.andCodeGenerator = new AndCodeGenerator(context);
        this.arrayAccessCodeGenerator = new ArrayAccessCodeGenerator(context);
        this.booleanLiteralCodeGenerator = new BooleanLiteralCodeGenerator(context);
        this.divCodeGenerator = new DivCodeGenerator(context);
        this.equalCodeGenerator = new EqualCodeGenerator(context);
        this.floatLiteralCodeGenerator = new FloatLiteralCodeGenerator(context);
        this.functionCallCodeGenerator = new FunctionCallCodeGenerator(context);
        this.greaterCodeGenerator = new GreaterCodeGenerator(context);
        this.greaterOrEqualCodeGenerator = new GreaterOrEqualCodeGenerator(context);
        this.lessCodeGenerator = new LessCodeGenerator(context);
        this.lessOrEqualCodeGenerator = new LessOrEqualCodeGenerator(context);
        this.identifierDerefCodeGenerator = new IdentifierDerefCodeGenerator(context);
        this.identifierNameCodeGenerator = new IdentifierNameCodeGenerator(context);
        this.idivCodeGenerator = new IDivCodeGenerator(context);
        this.integerLiteralCodeGenerator = new IntegerLiteralCodeGenerator(context);
        this.modCodeGenerator = new ModCodeGenerator(context);
        this.mulCodeGenerator = new MulCodeGenerator(context);
        this.notCodeGenerator = new NotCodeGenerator(context);
        this.notEqualCodeGenerator = new NotEqualCodeGenerator(context);
        this.orCodeGenerator = new OrCodeGenerator(context);
        this.shiftLeftCodeGenerator = new ShiftLeftCodeGenerator(context);
        this.stringLiteralCodeGenerator = new StringLiteralCodeGenerator(context);
        this.subCodeGenerator = new SubCodeGenerator(context);
        this.xorCodeGenerator = new XorCodeGenerator(context);
    }

    /**
     * Returns a reference to the symbol table.
     */
    public SymbolTable symbols() {
        return symbols;
    }

    /**
     * Returns a reference to the storage factory.
     */
    public StorageFactory storageFactory() { return storageFactory; }
    
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
    protected Identifier deriveArrayIdentifier(Identifier arrayIdentifier) {
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
    protected Identifier deriveDimensionIdentifier(Identifier arrayIdentifier, int dimensionIndex) {
        return new Identifier(arrayIdentifier.getName() + "_dim_" + dimensionIndex, I64.INSTANCE);
    }

    protected Section codeSection(List<Line> lines) {
        Section section = new CodeSection();

        // Add start of main program
        section.add(LABEL_MAIN);

        // Add prologue
        Prologue prologue = new Prologue(storageFactory.getRegisterManager().getUsedNonVolatileRegisters(),
                                         storageFactory.getFloatRegisterManager().getUsedNonVolatileRegisters());
        prologue.lines().forEach(section::add);

        // Add function code
        lines.forEach(section::add);

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
            addAll(addAssignCodeGenerator.generate((AddAssignStatement) statement));
        } else if (statement instanceof AssignStatement) {
            assignStatement((AssignStatement) statement);
        } else if (statement instanceof DecStatement) {
            addAll(decCodeGenerator.generate((DecStatement) statement));
        } else if (statement instanceof IfStatement) {
            ifStatement((IfStatement) statement);
        } else if (statement instanceof IncStatement) {
            addAll(incCodeGenerator.generate((IncStatement) statement));
        } else if (statement instanceof SubAssignStatement) {
            addAll(subAssignCodeGenerator.generate((SubAssignStatement) statement));
        } else if (statement instanceof VariableDeclarationStatement) {
            addAll(variableDeclarationCodeGenerator.generate((VariableDeclarationStatement) statement));
        } else if (statement instanceof WhileStatement) {
            whileStatement((WhileStatement) statement);
        }
    }

    /**
     * Generates code for an assignment statement.
     */
    protected void assignStatement(AssignStatement statement) {
        addLabel(statement);

        Type lhsType = typeManager.getType(statement.getLhsExpression());
        Type rhsType = typeManager.getType(statement.getRhsExpression());

        // Allocate temporary storage for variable (actually for result of evaluating RHS expression)
        try (StorageLocation location = storageFactory.allocateNonVolatile(lhsType)) {
            // If this location cannot store the expression type, we need to allocate temporary storage
            if (!location.stores(rhsType)) {
                try (StorageLocation rhsLocation = storageFactory.allocateNonVolatile(rhsType)) {
                    // Evaluate expression
                    expression(statement.getRhsExpression(), rhsLocation);
                    // Cast RHS value to LHS type
                    add(new Comment("Cast " + rhsType + " (" + rhsLocation + ") to " + lhsType + " (" + location + ")"));
                    location.convertAndMoveLocToThis(rhsLocation, this);
                }
            } else {
                // Evaluate expression
                expression(statement.getRhsExpression(), location);
            }

            // Store result in identifier
            addFormattedComment(statement);
            // Finally move result to variable
            withAddressOfIdentifier(statement.getLhsExpression(), (base, offset) -> location.moveThisToMem(base + offset, this));
        }
    }

    protected void exitStatement(ExitStatement statement) {
        addLabel(statement);
        addFunctionCall(FUN_EXIT, formatComment(statement), singletonList(statement.getExpression()));
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
            addAll(addCodeGenerator.generate((AddExpression) expression, location));
        } else if (expression instanceof AndExpression) {
            addAll(andCodeGenerator.generate((AndExpression) expression, location));
        } else if (expression instanceof ArrayAccessExpression) {
            addAll(arrayAccessCodeGenerator.generate((ArrayAccessExpression) expression, location));
        } else if (expression instanceof BooleanLiteral) {
            addAll(booleanLiteralCodeGenerator.generate((BooleanLiteral) expression, location));
        } else if (expression instanceof DivExpression) {
            addAll(divCodeGenerator.generate((DivExpression) expression, location));
        } else if (expression instanceof EqualExpression) {
            addAll(equalCodeGenerator.generate((EqualExpression) expression, location));
        } else if (expression instanceof FloatLiteral) {
            addAll(floatLiteralCodeGenerator.generate((FloatLiteral) expression, location));
        } else if (expression instanceof FunctionCallExpression) {
            addAll(functionCallCodeGenerator.generate((FunctionCallExpression) expression, location));
        } else if (expression instanceof GreaterExpression) {
            addAll(greaterCodeGenerator.generate((GreaterExpression) expression, location));
        } else if (expression instanceof GreaterOrEqualExpression) {
            addAll(greaterOrEqualCodeGenerator.generate((GreaterOrEqualExpression) expression, location));
        } else if (expression instanceof IdentifierDerefExpression) {
            addAll(identifierDerefCodeGenerator.generate((IdentifierDerefExpression) expression, location));
        } else if (expression instanceof IdentifierNameExpression) {
            addAll(identifierNameCodeGenerator.generate((IdentifierNameExpression) expression, location));
        } else if (expression instanceof IDivExpression) {
            addAll(idivCodeGenerator.generate((IDivExpression) expression, location));
        } else if (expression instanceof IntegerLiteral) {
            addAll(integerLiteralCodeGenerator.generate((IntegerLiteral) expression, location));
        } else if (expression instanceof LessExpression) {
            addAll(lessCodeGenerator.generate((LessExpression) expression, location));
        } else if (expression instanceof LessOrEqualExpression) {
            addAll(lessOrEqualCodeGenerator.generate((LessOrEqualExpression) expression, location));
        } else if (expression instanceof ModExpression) {
            addAll(modCodeGenerator.generate((ModExpression) expression, location));
        } else if (expression instanceof MulExpression) {
            addAll(mulCodeGenerator.generate((MulExpression) expression, location));
        } else if (expression instanceof NotExpression) {
            addAll(notCodeGenerator.generate((NotExpression) expression, location));
        } else if (expression instanceof NotEqualExpression) {
            addAll(notEqualCodeGenerator.generate((NotEqualExpression) expression, location));
        } else if (expression instanceof OrExpression) {
            addAll(orCodeGenerator.generate((OrExpression) expression, location));
        } else if (expression instanceof ShiftLeftExpression) {
            addAll(shiftLeftCodeGenerator.generate((ShiftLeftExpression) expression, location));
        } else if (expression instanceof StringLiteral) {
            addAll(stringLiteralCodeGenerator.generate((StringLiteral) expression, location));
        } else if (expression instanceof SubExpression) {
            addAll(subCodeGenerator.generate((SubExpression) expression, location));
        } else if (expression instanceof XorExpression) {
            addAll(xorCodeGenerator.generate((XorExpression) expression, location));
        }

        // If we have a saved location, and thus also a temporary location, we need to add a type cast
        if (savedLocation != null) {
            add(new Comment("Cast temporary " + type + " expression: " + expression));
            savedLocation.convertAndMoveLocToThis(location, this);
            // Free the temporary storage location again
            location.close();
        }
    }

    /**
     * Evaluates the given expression to get the memory address of the identifier/array element,
     * and then calls {@code generateCodeFunction} with this memory address to generate code to
     * read/write some data in this address.
     *
     * @param expression An expression that refers to an identifier or an array element that we can take the address of.
     * @param generateCodeFunction A function that generates code to access some data in the memory address. The given
     *                             function will receive two arguments, the base address of the identifier, and an optional
     *                             offset. The offset is only used for array element identifiers.
     */
    public void withAddressOfIdentifier(IdentifierExpression expression, BiConsumer<String, String> generateCodeFunction) {
        if (expression instanceof ArrayAccessExpression) {
            withArrayAccessExpression((ArrayAccessExpression) expression, generateCodeFunction);
        } else {
            Identifier identifier = expression.getIdentifier();
            symbols.addVariable(identifier);
            generateCodeFunction.accept(identifier.getMappedName(), "");
        }
    }

    /**
     * Evaluates the given array access expression to get the memory address of the array element,
     * and then calls {@code generateCodeFunction} with this memory address and an offset to generate
     * code to read or write some data in this location.
     *
     * @param expression           An expression that is used to calculate the base and offset of the array element.
     * @param generateCodeFunction A function that generates code to read or write some data in the memory address.
     *                             The given function will receive two arguments, the base address of the array, and
     *                             an offset that points out the actual element.
     */
    protected void withArrayAccessExpression(ArrayAccessExpression expression, BiConsumer<String, String> generateCodeFunction) {
        addFormattedComment(expression);

        // Get subscripts
        List<Expression> subscripts = expression.getSubscripts();

        try (StorageLocation accumulator = storageFactory.allocateNonVolatile();
             StorageLocation temp = storageFactory.allocateNonVolatile()) {
            // Evaluate first subscript expression
            expression(subscripts.get(0), accumulator);

            // For each remaining dimension
            for (int i = 1; i < subscripts.size(); i++) {
                // Multiply accumulator with size of dimension
                Identifier dimensionIdentifier = deriveDimensionIdentifier(expression.getIdentifier(), i);
                temp.moveMemToThis(dimensionIdentifier.getMappedName(), this);
                accumulator.multiplyLocWithThis(temp, this);
                // Evaluate subscript expression and add to accumulator
                expression(subscripts.get(i), temp);
                accumulator.addLocToThis(temp, this);
            }

            Identifier arrayIdentifier = deriveArrayIdentifier(expression.getIdentifier());
            generateCodeFunction.accept(arrayIdentifier.getMappedName(), "+8*" + ((RegisterStorageLocation) accumulator).getRegister());
        }
    }

    /**
     * Creates a unique label name from the given prefix.
     */
    public String uniqifyLabelName(String prefix) {
        return prefix + labelIndex++;
    }

    // -----------------------------------------------------------------------
    // Functions:
    // -----------------------------------------------------------------------

    /**
     * Adds code for making the given {@code functionCall}. This method is for cases when
     * you don't care about the function return value.
     */
    public void addFunctionCall(se.dykstrom.jcc.common.functions.Function function, Comment functionComment, List<Expression> args) {
        addFunctionCall(function, functionComment, args, null);
    }

    /**
     * Adds code for making the given {@code functionCall}. For more information, see method
     * {@link DefaultFunctionCallHelper#addFunctionCall(se.dykstrom.jcc.common.functions.Function, Call, Comment, List, StorageLocation)}.
     */
    public void addFunctionCall(se.dykstrom.jcc.common.functions.Function function, Comment functionComment, List<Expression> args, StorageLocation returnLocation) {
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
                codeContainer.addAll(function.lines());
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

    public void addFormattedComment(Node node) {
        add(formatComment(node));
    }

    public Comment formatComment(Node node) {
        return new Comment((node.line() != 0 ? node.line() + ": " : "") + format(node));
    }

    private String format(Node node) {
        String s = node.toString();
        return (s.length() > 53) ? s.substring(0, 50) + "..." : s;
    }

    /**
     * Adds a label before this statement, if there is a label defined.
     */
    public void addLabel(Statement statement) {
        if (statement.label() != null) {
            add(lineToLabel(statement.label()));
        }
    }

    /**
     * Converts a line number or line label to a Label object.
     */
    public static Label lineToLabel(String label) {
        return new Label("_line_" + label);
    }
}
