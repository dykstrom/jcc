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
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.storage.RegisterStorageLocation;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

import java.util.*;
import java.util.function.BiFunction;

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
    protected final Map<Class<? extends Statement>, StatementCodeGeneratorComponent<? extends Statement>> statementCodeGenerators = new HashMap<>();

    /** Expression code generators */
    protected final Map<Class<? extends Expression>, ExpressionCodeGeneratorComponent<? extends Expression>> expressionCodeGenerators = new HashMap<>();

    /** Helper class to generate code for function calls. */
    FunctionCallHelper functionCallHelper;

    /** Indexing all labels in the code, helping to create a unique name for each. */
    private int labelIndex = 0;

    protected AbstractCodeGenerator(TypeManager typeManager, AstOptimizer optimizer) {
        this.optimizer = optimizer;
        this.typeManager = typeManager;
        Context context = new Context(symbols, typeManager, storageFactory, this);
        this.functionCallHelper = new DefaultFunctionCallHelper(context);
        // Statements
        statementCodeGenerators.put(AddAssignStatement.class, new AddAssignCodeGenerator(context));
        statementCodeGenerators.put(DecStatement.class, new DecCodeGenerator(context));
        statementCodeGenerators.put(ExitStatement.class, new ExitCodeGenerator(context));
        statementCodeGenerators.put(IncStatement.class, new IncCodeGenerator(context));
        statementCodeGenerators.put(SubAssignStatement.class, new SubAssignCodeGenerator(context));
        statementCodeGenerators.put(VariableDeclarationStatement.class, new VariableDeclarationCodeGenerator(context));
        // Expressions
        expressionCodeGenerators.put(AddExpression.class, new AddCodeGenerator(context));
        expressionCodeGenerators.put(AndExpression.class, new AndCodeGenerator(context));
        expressionCodeGenerators.put(ArrayAccessExpression.class, new ArrayAccessCodeGenerator(context));
        expressionCodeGenerators.put(BooleanLiteral.class, new BooleanLiteralCodeGenerator(context));
        expressionCodeGenerators.put(DivExpression.class, new DivCodeGenerator(context));
        expressionCodeGenerators.put(EqualExpression.class, new EqualCodeGenerator(context));
        expressionCodeGenerators.put(FloatLiteral.class, new FloatLiteralCodeGenerator(context));
        expressionCodeGenerators.put(FunctionCallExpression.class, new FunctionCallCodeGenerator(context));
        expressionCodeGenerators.put(GreaterExpression.class, new GreaterCodeGenerator(context));
        expressionCodeGenerators.put(GreaterOrEqualExpression.class, new GreaterOrEqualCodeGenerator(context));
        expressionCodeGenerators.put(LessExpression.class, new LessCodeGenerator(context));
        expressionCodeGenerators.put(LessOrEqualExpression.class, new LessOrEqualCodeGenerator(context));
        expressionCodeGenerators.put(IdentifierDerefExpression.class, new IdentifierDerefCodeGenerator(context));
        expressionCodeGenerators.put(IdentifierNameExpression.class, new IdentifierNameCodeGenerator(context));
        expressionCodeGenerators.put(IDivExpression.class, new IDivCodeGenerator(context));
        expressionCodeGenerators.put(IntegerLiteral.class, new IntegerLiteralCodeGenerator(context));
        expressionCodeGenerators.put(ModExpression.class, new ModCodeGenerator(context));
        expressionCodeGenerators.put(MulExpression.class, new MulCodeGenerator(context));
        expressionCodeGenerators.put(NotExpression.class, new NotCodeGenerator(context));
        expressionCodeGenerators.put(NotEqualExpression.class, new NotEqualCodeGenerator(context));
        expressionCodeGenerators.put(OrExpression.class, new OrCodeGenerator(context));
        expressionCodeGenerators.put(ShiftLeftExpression.class, new ShiftLeftCodeGenerator(context));
        expressionCodeGenerators.put(StringLiteral.class, new StringLiteralCodeGenerator(context));
        expressionCodeGenerators.put(SubExpression.class, new SubCodeGenerator(context));
        expressionCodeGenerators.put(XorExpression.class, new XorCodeGenerator(context));
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
                Identifier dimensionIdentifier = deriveDimensionIdentifier(identifier, dimension);
                Long subscript = evaluatedSubscripts.get(dimension);
                section.add(new DataDefinition(dimensionIdentifier, subscript.toString(), true));
            }

            // Add a data definition for the number of dimensions
            Identifier numDimsIdent = new Identifier(identifier.getName() + "_num_dims", I64.INSTANCE);
            section.add(new DataDefinition(numDimsIdent, Integer.toString(numberOfDimensions), true));

            // Add a data definition for the actual array, with one instance of the default value for each element
            Type elementType = array.getElementType();
            Identifier arrayIdentifier = deriveArrayIdentifier(identifier);
            String defaultValue = elementType.getDefaultValue();
            long numberOfElements = evaluatedSubscripts.stream().reduce(1L, (a, b) -> a * b);
            String arrayValue = numberOfElements + " dup " + defaultValue;
            section.add(new DataDefinition(arrayIdentifier, arrayValue, false));
        });
    }

    /**
     * Derives an identifier for the "array start" property of the array identified by {@code identifier}.
     *
     * @param identifier An identifier that identifies the array.
     * @return The derived identifier.
     */
    protected Identifier deriveArrayIdentifier(Identifier identifier) {
        Arr array = (Arr) identifier.getType();
        return new Identifier(identifier.getName() + "_arr", array.getElementType());
    }

    /**
     * Derives an identifier for the "dimension" property of dimension number {@code dimensionIndex}
     * of the array identified by {@code identifier}.
     *
     * @param identifier An identifier that identifies an array.
     * @param dimensionIndex The index of the dimension for which to derive an identifier.
     * @return The derived identifier.
     */
    protected Identifier deriveDimensionIdentifier(Identifier identifier, int dimensionIndex) {
        return new Identifier(identifier.getName() + "_dim_" + dimensionIndex, I64.INSTANCE);
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
        StatementCodeGeneratorComponent<Statement> codeGeneratorComponent = getCodeGeneratorComponent(statement.getClass());
        if (codeGeneratorComponent != null) {
            addAll(codeGeneratorComponent.generate(statement));
        } else if (statement instanceof AssignStatement) {
            assignStatement((AssignStatement) statement);
        } else if (statement instanceof IfStatement) {
            ifStatement((IfStatement) statement);
        } else if (statement instanceof WhileStatement) {
            whileStatement((WhileStatement) statement);
        } else {
            throw new IllegalArgumentException("unsupported statement: " + statement.getClass().getSimpleName());
        }
        add(Blank.INSTANCE);
    }

    @SuppressWarnings("unchecked")
    protected StatementCodeGeneratorComponent<Statement> getCodeGeneratorComponent(Class<? extends Statement> statementClass) {
        return (StatementCodeGeneratorComponent<Statement>) statementCodeGenerators.get(statementClass);
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
                    addAll(expression(statement.getRhsExpression(), rhsLocation));
                    // Cast RHS value to LHS type
                    add(new Comment("Cast " + rhsType + " (" + rhsLocation + ") to " + lhsType + " (" + location + ")"));
                    location.convertAndMoveLocToThis(rhsLocation, this);
                }
            } else {
                // Evaluate expression
                addAll(expression(statement.getRhsExpression(), location));
            }

            // Store result in identifier
            addFormattedComment(statement);
            // Finally move result to variable
            addAll(withAddressOfIdentifier(statement.getLhsExpression(),
                    (base, offset) -> withCodeContainer(cc -> location.moveThisToMem(base + offset, cc))));
        }
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
            addAll(expression(statement.getExpression(), location));
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
            addAll(expression(statement.getExpression(), location));
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
    public List<Line> expression(Expression expression, StorageLocation location) {
        Type type = typeManager.getType(expression);
        if (location.stores(type)) {
            ExpressionCodeGeneratorComponent<Expression> codeGeneratorComponent = getCodeGeneratorComponent(expression);
            if (codeGeneratorComponent != null) {
                return codeGeneratorComponent.generate(expression, location);
            } else {
                throw new IllegalArgumentException("unsupported expression: " + expression.getClass().getSimpleName());
            }
        } else {
            CodeContainer cc = new CodeContainer();
            // If the current storage location cannot store the expression value,
            // we introduce a temporary storage location and add a later type cast
            try (StorageLocation tmp = storageFactory.allocateNonVolatile(type)) {
                cc.addAll(expression(expression, tmp));
                cc.add(new Comment("Cast temporary " + type + " expression: " + expression));
                location.convertAndMoveLocToThis(tmp, cc);
            }
            return cc.lines();
        }
    }

    @SuppressWarnings("unchecked")
    private ExpressionCodeGeneratorComponent<Expression> getCodeGeneratorComponent(Expression expression) {
        return (ExpressionCodeGeneratorComponent<Expression>) expressionCodeGenerators.get(expression.getClass());
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
     * @return The generated code.
     */
    public List<Line> withAddressOfIdentifier(IdentifierExpression expression, BiFunction<String, String, List<Line>> generateCodeFunction) {
        if (expression instanceof ArrayAccessExpression) {
            return withArrayAccessExpression((ArrayAccessExpression) expression, generateCodeFunction);
        } else {
            Identifier identifier = expression.getIdentifier();
            symbols.addVariable(identifier);
            return generateCodeFunction.apply(identifier.getMappedName(), "");
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
     * @return The generated code.
     */
    protected List<Line> withArrayAccessExpression(ArrayAccessExpression expression, BiFunction<String, String, List<Line>> generateCodeFunction) {
        CodeContainer cc = new CodeContainer();

        cc.add(formatComment(expression));

        // Get subscripts
        List<Expression> subscripts = expression.getSubscripts();

        try (StorageLocation accumulator = storageFactory.allocateNonVolatile();
             StorageLocation temp = storageFactory.allocateNonVolatile()) {
            // Evaluate first subscript expression
            cc.addAll(expression(subscripts.get(0), accumulator));

            // For each remaining dimension
            for (int i = 1; i < subscripts.size(); i++) {
                // Multiply accumulator with size of dimension
                Identifier dimensionIdentifier = deriveDimensionIdentifier(expression.getIdentifier(), i);
                temp.moveMemToThis(dimensionIdentifier.getMappedName(), cc);
                accumulator.multiplyLocWithThis(temp, cc);
                // Evaluate subscript expression and add to accumulator
                cc.addAll(expression(subscripts.get(i), temp));
                accumulator.addLocToThis(temp, cc);
            }

            Identifier arrayIdentifier = deriveArrayIdentifier(expression.getIdentifier());
            cc.addAll(generateCodeFunction.apply(arrayIdentifier.getMappedName(), "+8*" + ((RegisterStorageLocation) accumulator).getRegister()));
        }

        return cc.lines();
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
     * Generates code for calling the given {@code function}. This method is for cases when
     * you don't care about the function return value.
     */
    public List<Line> functionCall(Function function, Comment functionComment, List<Expression> args) {
        return functionCall(function, functionComment, args, null);
    }

    /**
     * Generates code for calling the given {@code function}.
     *
     * @see DefaultFunctionCallHelper#addFunctionCall(Function, Call, Comment, List, StorageLocation).
     */
    public List<Line> functionCall(Function function, Comment functionComment, List<Expression> args, StorageLocation returnLocation) {
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

        return functionCallHelper.addFunctionCall(function, functionCall, functionComment, args, returnLocation);
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
