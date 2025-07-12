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

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.directive.DataDefinition;
import se.dykstrom.jcc.common.code.FixedLabel;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.assembly.instruction.Call;
import se.dykstrom.jcc.common.assembly.instruction.CallDirect;
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect;
import se.dykstrom.jcc.common.assembly.macro.Import;
import se.dykstrom.jcc.common.assembly.macro.Library;
import se.dykstrom.jcc.common.assembly.other.Epilogue;
import se.dykstrom.jcc.common.assembly.other.Header;
import se.dykstrom.jcc.common.assembly.other.Prologue;
import se.dykstrom.jcc.common.assembly.section.CodeSection;
import se.dykstrom.jcc.common.assembly.section.DataSection;
import se.dykstrom.jcc.common.assembly.section.ImportSection;
import se.dykstrom.jcc.common.assembly.section.Section;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Comment;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.expression.*;
import se.dykstrom.jcc.common.code.statement.*;
import se.dykstrom.jcc.common.functions.*;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.storage.MemoryStorageLocation;
import se.dykstrom.jcc.common.storage.RegisterStorageLocation;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_EXIT;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateIntegerExpressions;

/**
 * Abstract base class for all code generators.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractCodeGenerator extends CodeContainer implements AsmCodeGenerator {

    private static final Label LABEL_EXIT = new FixedLabel(FUN_EXIT.getMappedName());
    private static final Label LABEL_MAIN = new Label("_main");

    static final String SHADOW_SPACE = "20h";

    protected final TypeManager typeManager;
    protected final AstOptimizer optimizer;
    protected StorageFactory storageFactory = new StorageFactory();
    protected SymbolTable symbols;

    protected final Map<String, Set<String>> dependencies = new HashMap<>();

    /** All built-in functions that have actually been called, and needs to be linked into the program. */
    private final Set<AssemblyFunction> usedBuiltInFunctions = new HashSet<>();

    /** All user-defined functions that have been defined. */
    private final Map<UserDefinedFunction, Expression> userDefinedFunctions = new HashMap<>();

    /** Statement code generators */
    protected final Map<Class<? extends Statement>, StatementCodeGeneratorComponent<? extends Statement>> statementCodeGenerators = new HashMap<>();

    /** Expression code generators */
    protected final Map<Class<? extends Expression>, ExpressionCodeGeneratorComponent<? extends Expression>> expressionCodeGenerators = new HashMap<>();

    /** Helper class to generate code for function calls. */
    protected FunctionCallHelper functionCallHelper;
    /** Helper class to generate code for function definitions. */
    protected FunctionDefinitionHelper functionDefinitionHelper;

    /** Indexing all labels in the code, helping to create a unique name for each. */
    private int labelIndex = 0;

    protected AbstractCodeGenerator(final TypeManager typeManager,
                                    final SymbolTable symbolTable,
                                    final AstOptimizer optimizer) {
        this.typeManager = requireNonNull(typeManager);
        this.symbols = requireNonNull(symbolTable);
        this.optimizer = requireNonNull(optimizer);
        this.functionCallHelper = new DefaultFunctionCallHelper(this);
        this.functionDefinitionHelper = new DefaultFunctionDefinitionHelper(this);
        // Statements
        statementCodeGenerators.put(AddAssignStatement.class, new AddAssignCodeGenerator(this));
        statementCodeGenerators.put(ClsStatement.class, new ClsCodeGenerator(this));
        statementCodeGenerators.put(ConstDeclarationStatement.class, new ConstDeclarationCodeGenerator(this));
        statementCodeGenerators.put(DecStatement.class, new DecCodeGenerator(this));
        statementCodeGenerators.put(ExitStatement.class, new ExitCodeGenerator(this));
        statementCodeGenerators.put(FunctionDefinitionStatement.class, new FunctionDefinitionCodeGenerator(this));
        statementCodeGenerators.put(IDivAssignStatement.class, new IDivAssignCodeGenerator(this));
        statementCodeGenerators.put(IfStatement.class, new IfCodeGenerator(this));
        statementCodeGenerators.put(IncStatement.class, new IncCodeGenerator(this));
        statementCodeGenerators.put(LabelledStatement.class, new LabelledCodeGenerator(this));
        statementCodeGenerators.put(MulAssignStatement.class, new MulAssignCodeGenerator(this));
        statementCodeGenerators.put(ReturnStatement.class, new ReturnCodeGenerator(this));
        statementCodeGenerators.put(SubAssignStatement.class, new SubAssignCodeGenerator(this));
        statementCodeGenerators.put(VariableDeclarationStatement.class, new VariableDeclarationCodeGenerator(this));
        statementCodeGenerators.put(WhileStatement.class, new WhileCodeGenerator(this));
        // Expressions
        expressionCodeGenerators.put(AddExpression.class, new AddCodeGenerator(this));
        expressionCodeGenerators.put(AndExpression.class, new AndCodeGenerator(this));
        expressionCodeGenerators.put(ArrayAccessExpression.class, new ArrayAccessCodeGenerator(this));
        expressionCodeGenerators.put(BooleanLiteral.class, new BooleanLiteralCodeGenerator(this));
        expressionCodeGenerators.put(DivExpression.class, new DivCodeGenerator(this));
        expressionCodeGenerators.put(EqualExpression.class, new EqualCodeGenerator(this));
        expressionCodeGenerators.put(FloatLiteral.class, new FloatLiteralCodeGenerator(this));
        expressionCodeGenerators.put(FunctionCallExpression.class, new FunctionCallCodeGenerator(this));
        expressionCodeGenerators.put(GreaterExpression.class, new GreaterCodeGenerator(this));
        expressionCodeGenerators.put(GreaterOrEqualExpression.class, new GreaterOrEqualCodeGenerator(this));
        expressionCodeGenerators.put(LessExpression.class, new LessCodeGenerator(this));
        expressionCodeGenerators.put(LessOrEqualExpression.class, new LessOrEqualCodeGenerator(this));
        expressionCodeGenerators.put(LogicalAndExpression.class, new LogicalAndCodeGenerator(this));
        expressionCodeGenerators.put(LogicalNotExpression.class, new LogicalNotCodeGenerator(this));
        expressionCodeGenerators.put(LogicalOrExpression.class, new LogicalOrCodeGenerator(this));
        expressionCodeGenerators.put(LogicalXorExpression.class, new LogicalXorCodeGenerator(this));
        expressionCodeGenerators.put(IdentifierDerefExpression.class, new IdentifierDerefCodeGenerator(this));
        expressionCodeGenerators.put(IdentifierNameExpression.class, new IdentifierNameCodeGenerator(this));
        expressionCodeGenerators.put(IDivExpression.class, new IDivCodeGenerator(this));
        expressionCodeGenerators.put(IntegerLiteral.class, new IntegerLiteralCodeGenerator(this));
        expressionCodeGenerators.put(ModExpression.class, new ModCodeGenerator(this));
        expressionCodeGenerators.put(MulExpression.class, new MulCodeGenerator(this));
        expressionCodeGenerators.put(NegateExpression.class, new NegateCodeGenerator(this));
        expressionCodeGenerators.put(NotExpression.class, new NotCodeGenerator(this));
        expressionCodeGenerators.put(NotEqualExpression.class, new NotEqualCodeGenerator(this));
        expressionCodeGenerators.put(OrExpression.class, new OrCodeGenerator(this));
        expressionCodeGenerators.put(RoundExpression.class, new RoundCodeGenerator(this));
        expressionCodeGenerators.put(ShiftLeftExpression.class, new ShiftLeftCodeGenerator(this));
        expressionCodeGenerators.put(SqrtExpression.class, new SqrtCodeGenerator(this));
        expressionCodeGenerators.put(StringLiteral.class, new StringLiteralCodeGenerator(this));
        expressionCodeGenerators.put(SubExpression.class, new SubCodeGenerator(this));
        expressionCodeGenerators.put(TruncExpression.class, new TruncCodeGenerator(this));
        expressionCodeGenerators.put(XorExpression.class, new XorCodeGenerator(this));
    }

    @Override
    public TypeManager typeManager() { return typeManager; }

    @Override
    public SymbolTable symbols() { return symbols; }

    @Override
    public StorageFactory storageFactory() { return storageFactory; }

    /**
     * Returns a reference to the dependencies found.
     */
    public Map<String, Set<String>> dependencies() { return dependencies; }

    // -----------------------------------------------------------------------
    // Sections:
    // -----------------------------------------------------------------------

    protected Header fileHeader(final Path sourcePath) {
        return new Header(sourcePath, LABEL_MAIN);
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
                new DataDefinition(identifier, (String) symbols.getValue(identifier.name()), symbols.isConstant(identifier.name())))
        );
    }

    /**
     * Adds data definitions for all array identifiers to the given code section.
     */
    protected void addArrayDataDefinitions(List<Identifier> identifiers, SymbolTable symbols, Section section) {
        identifiers.forEach(identifier -> {
            Arr array = (Arr) identifier.type();
            int numberOfDimensions = array.getDimensions();
            List<Expression> subscripts = symbols.getArrayValue(identifier.name()).getSubscripts();
            List<Long> evaluatedSubscripts = evaluateIntegerExpressions(subscripts, symbols, optimizer.expressionOptimizer());

            // Add a data definition for each dimension, in reverse order
            for (int dimension = numberOfDimensions - 1; dimension >= 0; dimension--) {
                Identifier dimensionIdentifier = deriveDimensionIdentifier(identifier, dimension);
                Long subscript = evaluatedSubscripts.get(dimension);
                section.add(new DataDefinition(dimensionIdentifier, subscript.toString(), true));
            }

            // Add a data definition for the number of dimensions
            Identifier numDimsIdent = new Identifier(identifier.name() + Arr.SUFFIX + "_num_dims", I64.INSTANCE);
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
        Arr array = (Arr) identifier.type();
        return new Identifier(identifier.name() + Arr.SUFFIX, array.getElementType());
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
        return new Identifier(identifier.name() + Arr.SUFFIX + "_dim_" + dimensionIndex, I64.INSTANCE);
    }

    protected Section codeSection(List<Line> lines) {
        Section section = new CodeSection();

        // Add start of main program
        section.add(LABEL_MAIN);

        // Add prologue
        final Prologue prologue = new Prologue(
                storageFactory.getRegisterManager().getUsedNonVolatileRegisters(),
                storageFactory.getFloatRegisterManager().getUsedNonVolatileRegisters()
        );
        prologue.lines().forEach(section::add);

        // Add function code
        lines.forEach(section::add);

        return section;
    }

    /**
     * Returns {@code true} if the program contains at least one call to exit.
     */
    protected boolean containsExit() {
        return lines().contains(new CallIndirect(LABEL_EXIT));
    }

    // -----------------------------------------------------------------------
    // Statements:
    // -----------------------------------------------------------------------

    @Override
    public void statement(final Statement statement) {
        final var component = getCodeGeneratorComponent(statement.getClass());
        if (component != null) {
            addAll(component.generate(statement));
        } else if (statement instanceof AssignStatement assignStatement) {
            assignStatement(assignStatement);
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
                    add(new AssemblyComment("Cast " + rhsType + " (" + rhsLocation + ") to " + lhsType + " (" + location + ")"));
                    location.roundAndMoveLocToThis(rhsLocation, this);
                }
            } else {
                // Evaluate expression
                addAll(expression(statement.getRhsExpression(), location));
            }

            // Store result in identifier
            addFormattedComment(statement);
            // Finally, move result to variable
            addAll(withAddressOfIdentifier(statement.getLhsExpression(),
                    (base, offset) -> withCodeContainer(cc -> location.moveThisToMem(base + offset, cc))));
        }
    }

    // -----------------------------------------------------------------------
    // Expressions:
    // -----------------------------------------------------------------------

    @Override
    public List<Line> expression(Expression expression, StorageLocation location) {
        Type type = typeManager.getType(expression);
        if (location.stores(type)) {
            final var component = getCodeGeneratorComponent(expression);
            if (component != null) {
                return component.generate(expression, location);
            } else {
                throw new IllegalArgumentException("unsupported expression: " + expression.getClass().getSimpleName());
            }
        } else {
            return withCodeContainer(cc -> {
                // If the current storage location cannot store the expression value,
                // we introduce a temporary storage location and add a later type cast
                try (StorageLocation tmp = storageFactory.allocateNonVolatile(type)) {
                    cc.addAll(expression(expression, tmp));
                    cc.add(new AssemblyComment("Cast temporary " + type + " expression: " + expression));
                    location.roundAndMoveLocToThis(tmp, cc);
                }
            });
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
        if (expression instanceof ArrayAccessExpression arrayAccessExpression) {
            return withArrayAccessExpression(arrayAccessExpression, generateCodeFunction);
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
     *                             an optional offset. The offset is only used for array element identifiers.
     * @return The generated code.
     */
    protected List<Line> withArrayAccessExpression(final ArrayAccessExpression expression,
                                                   final BiFunction<String, String, List<Line>> generateCodeFunction) {
        CodeContainer cc = new CodeContainer();

        cc.add(getComment(expression));

        // Get subscripts
        List<Expression> subscripts = expression.getSubscripts();

        try (StorageLocation accumulator = storageFactory.allocateNonVolatile();
             StorageLocation temp = storageFactory.allocateNonVolatile()) {
            // Evaluate first subscript expression
            cc.addAll(expression(subscripts.getFirst(), accumulator));

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

            final var arrayIdentifier = deriveArrayIdentifier(expression.getIdentifier());
            if (accumulator instanceof MemoryStorageLocation memory) {
                // If accumulator is a temporary memory location, we have to move it
                // to a temporary register to execute the code generator function
                try (StorageLocation location = storageFactory.allocateVolatile(I64.INSTANCE)) {
                    location.moveLocToThis(memory, cc);
                    cc.addAll(generateCodeFunction.apply(arrayIdentifier.getMappedName(), "+8*" + ((RegisterStorageLocation) location).getRegister()));
                }
            } else {
                cc.addAll(generateCodeFunction.apply(arrayIdentifier.getMappedName(), "+8*" + ((RegisterStorageLocation) accumulator).getRegister()));
            }
        }

        return cc.lines();
    }

    /**
     * Creates a unique label name from the given prefix.
     */
    public String uniquifyLabelName(final String prefix) {
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

    @Override
    public List<Line> functionCall(Function function, Comment functionComment, List<Expression> args, StorageLocation returnLocation) {
        // Add dependencies needed by this function
        addAllFunctionDependencies(function.getDependencies());
        addAllConstantDependencies(function.getConstants());

        // Create function call
        Call functionCall;
        if (function instanceof AssemblyFunction assemblyFunction) {
            functionCall = new CallDirect(new Label(function.getMappedName()));
            // Remember that we have used this function
            addUsedBuiltInFunction(assemblyFunction);
        } else if (function instanceof UserDefinedFunction) {
            functionCall = new CallDirect(new Label(function.getMappedName()));
        } else if (function instanceof LibraryFunction) {
            functionCall = new CallIndirect(new FixedLabel(function.getMappedName()));
        } else if (function instanceof ReferenceFunction) {
            // Find the variable or parameter that holds the actual function to call
            final var functionIdentifier = symbols().getIdentifier(function.getName());
            // The assembler must be told the operand size when making a call to a memory address
            functionCall = new CallIndirect(new FixedLabel(functionIdentifier.getMappedName()), "qword");
        } else {
            throw new IllegalStateException("function '" + function.getName() + "' with unknown type: " + function.getClass().getSimpleName());
        }

        return functionCallHelper.addFunctionCall(function, functionCall, functionComment, args, returnLocation);
    }

    /**
     * Generates code for all the built-in functions that have actually been used in the program.
     */
    protected CodeContainer builtInFunctions() {
        CodeContainer cc = new CodeContainer();

        if (!usedBuiltInFunctions.isEmpty()) {
            cc.add(Blank.INSTANCE);
            cc.add(new AssemblyComment("--- Built-in functions -->"));

            // For each built-in function that has been used
            usedBuiltInFunctions.stream()
                    .sorted(Comparator.comparing(Function::getMappedName))
                    .forEach(function -> {
                        cc.add(Blank.INSTANCE);
                        cc.add(new AssemblyComment(function.toString()));

                        // Add label for start of function
                        cc.add(new Label(function.getMappedName()));

                        // Add function code lines
                        cc.addAll(function.lines());
                    });

            cc.add(Blank.INSTANCE);
            cc.add(new AssemblyComment("<-- Built-in functions ---"));
        }

        return cc;
    }

    /**
     * Generates code for all the user-defined functions that have been defined in the program.
     */
    protected CodeContainer userDefinedFunctions() {
        final var cc = new CodeContainer();

        if (!userDefinedFunctions.isEmpty()) {
            cc.add(Blank.INSTANCE);
            cc.add(new AssemblyComment("--- User-defined functions -->"));
            // For each user-defined function that has been defined
            userDefinedFunctions.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().getMappedName()))
                    .forEach(e -> cc.addAll(functionDefinitionHelper.addFunctionCode(e.getKey(), e.getValue())));
            cc.add(Blank.INSTANCE);
            cc.add(new AssemblyComment("<-- User-defined functions ---"));
        }

        return cc;
    }

    /**
     * Creates a local symbol table that inherits from the current symbol table,
     * sets the local symbol table as the current symbol table, calls the supplier,
     * and resets the current symbol table again.
     */
    @Override
    public List<Line> withLocalSymbolTable(final Supplier<List<Line>> supplier) {
        try {
            symbols = new SymbolTable(symbols);
            return supplier.get();
        } finally {
            symbols = symbols.pop();
        }
    }

    /**
     * Creates a local storage factory to use in a function, generates the actual code for the function
     * using the provided {@code functionCodeGenerator}, and then adds a function prologue at the beginning,
     * and a function epilogue at the end.
     */
    @Override
    public List<Line> withLocalStorageFactory(final Consumer<CodeContainer> functionCodeGenerator) {
        final var cc = new CodeContainer();
        try {
            storageFactory = new StorageFactory(storageFactory);
            functionCodeGenerator.accept(cc);
        } finally {
            final var prologue = new Prologue(
                    storageFactory.getRegisterManager().getUsedNonVolatileRegisters(),
                    storageFactory.getFloatRegisterManager().getUsedNonVolatileRegisters()
            );
            cc.addAllFirst(prologue.lines());

            final var epilogue = new Epilogue(
                    storageFactory.getRegisterManager().getUsedNonVolatileRegisters(),
                    storageFactory.getFloatRegisterManager().getUsedNonVolatileRegisters()
            );
            cc.addAll(epilogue.lines());

            storageFactory = storageFactory.pop();
        }
        return cc.lines();
    }

    protected void addUsedBuiltInFunction(AssemblyFunction function) {
        usedBuiltInFunctions.add(function);
    }

    public void addUserDefinedFunction(final UserDefinedFunction function, final Expression expression) {
        userDefinedFunctions.put(function, expression);
    }

    private void addFunctionDependency(se.dykstrom.jcc.common.functions.Function function, String library) {
        if (function instanceof AssemblyFunction assemblyFunction) {
            // If this is an assembly function, remember that it has been used
            addUsedBuiltInFunction(assemblyFunction);
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
        add(getComment(node));
    }
}
