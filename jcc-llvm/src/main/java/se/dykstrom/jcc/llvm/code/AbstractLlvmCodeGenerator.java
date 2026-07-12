/*
 * Copyright (C) 2024 Johan Dykstrom
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

package se.dykstrom.jcc.llvm.code;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.Text;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.utils.GcOptions;
import se.dykstrom.jcc.common.types.Arr;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.expression.*;
import se.dykstrom.jcc.llvm.code.statement.*;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operation.*;

import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO_I32;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateIntegerExpressions;
import static se.dykstrom.jcc.common.utils.FunctionUtils.LIB_JCC_GC;
import static se.dykstrom.jcc.llvm.LlvmOperator.*;
import static se.dykstrom.jcc.common.symbols.Scope.NONE;

public abstract class AbstractLlvmCodeGenerator implements LlvmCodeGenerator {

    /** The name of the generated main function, shared with GC code generation. */
    public static final String MAIN_FUNCTION_NAME = "main";

    /** A {@code return 0} statement, for use as a trailing statement in the main function. */
    protected static final Statement RETURN_I32_ZERO = new ReturnStatement(0, 0, ZERO_I32);

    protected final Map<Class<?>, LlvmStatementCodeGenerator<? extends Statement>> statementDictionary;
    protected final Map<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>> expressionDictionary;

    protected final RelationalCodeGenerator eqCodeGenerator = new RelationalCodeGenerator(this, "oeq", "eq");
    // Unordered une, so that NaN != NaN is true per IEEE 754; all other comparisons are false for NaN
    protected final RelationalCodeGenerator neCodeGenerator = new RelationalCodeGenerator(this, "une", "ne");
    protected final RelationalCodeGenerator gtCodeGenerator = new RelationalCodeGenerator(this, "ogt", "sgt");
    protected final RelationalCodeGenerator geCodeGenerator = new RelationalCodeGenerator(this, "oge", "sge");
    protected final RelationalCodeGenerator ltCodeGenerator = new RelationalCodeGenerator(this, "olt", "slt");
    protected final RelationalCodeGenerator leCodeGenerator = new RelationalCodeGenerator(this, "ole", "sle");

    private final TypeManager typeManager;
    private final SymbolTable symbolTable;
    private final AstOptimizer optimizer;
    private final GcCodeGenerator gc;

    private final LabelStack labelStack = new LabelStack();


    public AbstractLlvmCodeGenerator(final TypeManager typeManager,
                                     final SymbolTable symbolTable,
                                     final AstOptimizer optimizer) {
        this(typeManager, symbolTable, optimizer, NoOpGcCodeGenerator.INSTANCE);
    }

    /**
     * Creates a code generator that emits garbage-collector plumbing through the given strategy.
     * Languages that use the collector (BASIC) pass a {@link RuntimeGcCodeGenerator}; the others
     * inherit the {@link NoOpGcCodeGenerator} default via the three-argument constructor.
     */
    public AbstractLlvmCodeGenerator(final TypeManager typeManager,
                                     final SymbolTable symbolTable,
                                     final AstOptimizer optimizer,
                                     final GcCodeGenerator gc) {
        this.typeManager = requireNonNull(typeManager);
        this.symbolTable = requireNonNull(symbolTable);
        this.optimizer = requireNonNull(optimizer);
        this.gc = requireNonNull(gc);

        this.statementDictionary = buildStatementDictionary();
        this.expressionDictionary = buildExpressionDictionary();

        // Add LLVM specific constants
        symbolTable.addConstant(new Identifier(".str.empty", Str.INSTANCE), "");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void statement(Statement statement, List<Line> lines, SymbolTable symbolTable) {
        Optional.ofNullable(statementDictionary.get(statement.getClass()))
                .map(c -> (LlvmStatementCodeGenerator<Statement>) c)
                .orElseThrow(() -> new IllegalArgumentException("unknown statement: " + statement.getClass().getSimpleName()))
                .toLlvm(statement, lines, symbolTable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public LlvmOperand expression(Expression expression, List<Line> lines, SymbolTable symbolTable) {
        return Optional.ofNullable(expressionDictionary.get(expression.getClass()))
                .map(c -> (LlvmExpressionCodeGenerator<Expression>) c)
                .orElseThrow(() -> new IllegalArgumentException("unknown expression: " + expression.getClass().getSimpleName()))
                .toLlvm(expression, lines, symbolTable);
    }

    @Override
    public TypeManager typeManager() {
        return typeManager;
    }

    public SymbolTable symbolTable() {
        return symbolTable;
    }

    public AstOptimizer optimizer() {
        return optimizer;
    }

    /**
     * The garbage-collector strategy for this backend ({@link RuntimeGcCodeGenerator} for BASIC,
     * {@link NoOpGcCodeGenerator} otherwise). Exposed so subclasses can thread it into the
     * string-producing code generators they wire up, which emit registration through it.
     */
    protected GcCodeGenerator gc() {
        return gc;
    }

    /**
     * Generates a parameterless main function, wrapping the program statements with trailing
     * statements (e.g. a return).
     */
    protected static Statement generateMainFunction(final List<Statement> statements,
                                                    final List<Statement> trailing) {
        return generateMainFunction(statements, List.of(), List.of(), trailing);
    }

    /**
     * Generates the main function, optionally declaring parameters and wrapping the program statements
     * with leading statements (e.g. for runtime initialization) and trailing statements (e.g. a return).
     */
    protected static Statement generateMainFunction(final List<Statement> statements,
                                                    final List<Declaration> parameters,
                                                    final List<Statement> leading,
                                                    final List<Statement> trailing) {
        final var argTypes = parameters.stream().map(Declaration::type).toList();
        final var mainIdentifier = new Identifier(MAIN_FUNCTION_NAME, Fun.from(argTypes, I32.INSTANCE));

        final var list = new ArrayList<>(leading);
        statements.stream()
                .filter(s -> !(s instanceof FunctionDefinitionStatement)) // Ignore function definitions
                .forEach(list::add);
        list.addAll(trailing);
        return new FunctionDefinitionStatement(0, 0, mainIdentifier, parameters, list);
    }

    protected static List<Line> generateHeader(final Path path) {
        return List.of(
                new LlvmComment("ModuleID = '" + path + "'"),
                new Text("source_filename = \"" + path + "\"")
        );
    }

    protected List<? extends Line> generateDeclares(final Set<LibraryFunction> calledFunctions) {
        // Partition the called library functions into GC runtime functions and the rest.
        // The GC functions have no real implementation yet (issue #63 phase 2-4), so instead
        // of declaring them they are given temporary in-module stub definitions - declaring
        // AND defining the same symbol would be invalid IR. Ordinary library functions get a
        // plain declare. This is a no-op for languages that call no GC functions.
        final var gcFunctions = calledFunctions.stream()
                .filter(AbstractLlvmCodeGenerator::isGcFunction)
                .collect(toSet());

        final var lines = new ArrayList<Line>();
        // Add a declare operation for each called non-GC library function
        calledFunctions.stream()
                .filter(f -> !isGcFunction(f))
                .sorted()
                .map(DeclareOperation::new)
                .forEach(lines::add);
        // Add temporary stub definitions for the called GC functions
        lines.addAll(GcStubsGenerator.generateStubs(gcFunctions, GcOptions.INSTANCE.isPrintGc()));
        return lines;
    }

    /** Returns true if the given library function is part of the GC runtime (jcc_gc_*). */
    private static boolean isGcFunction(final LibraryFunction function) {
        return LIB_JCC_GC.equals(function.libraryFileName());
    }

    protected Set<LibraryFunction> getCalledFunctions(final List<Line> operations) {
        return operations.stream()
                .filter(o -> o instanceof CallOperation)
                .map(o -> (CallOperation) o)
                .map(CallOperation::function)
                .filter(f -> f instanceof LibraryFunction)
                .map(f -> (LibraryFunction) f)
                .collect(toSet());
    }

    protected List<? extends LlvmOperation> generateGlobals(final SymbolTable symbolTable) {
        final List<LlvmOperation> operations = new ArrayList<>();
        // Scalar variables and constants
        symbolTable.identifiers().stream()
                .sorted()
                .map(i -> generateGlobal(i, symbolTable))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(operations::add);
        // Static arrays: element storage plus a dimension-size metadata global
        symbolTable.arrayIdentifiers().stream()
                .sorted()
                .forEach(i -> operations.addAll(generateArrayGlobals(i, symbolTable)));
        // Garbage-collector global-roots table (empty for languages that do not use the GC)
        operations.addAll(generateGlobalRoots(symbolTable));
        return operations;
    }

    /**
     * Builds the GC global-roots table from every string variable that must survive collection:
     * non-constant string scalars (one slot each) and string arrays (the whole element region).
     * String constants live in read-only memory and are not roots. The ranges are handed to the
     * GC strategy, which emits the table only when the collector is in use.
     */
    private List<? extends LlvmOperation> generateGlobalRoots(final SymbolTable symbolTable) {
        final var ranges = new ArrayList<GcRootRange>();
        symbolTable.identifiers().stream()
                .filter(i -> i.type() instanceof Str)
                .filter(i -> !symbolTable.isConstant(i.name()))
                .sorted()
                .forEach(i -> ranges.add(new GcRootRange(symbolTable.mapName(i), 1)));
        symbolTable.arrayIdentifiers().stream()
                .filter(i -> ((Arr) i.type()).getElementType() instanceof Str)
                .sorted()
                .forEach(i -> ranges.add(new GcRootRange(symbolTable.mapName(i), arrayLength(i, symbolTable))));
        return gc.globalRoots(ranges);
    }

    private Optional<LlvmOperation> generateGlobal(final Identifier identifier, final SymbolTable symbolTable) {
        return Optional.ofNullable(symbolTable.getValue(identifier.name()))
                .filter(v -> v instanceof String)
                .map(v -> (String) v)
                .map(v -> (symbolTable.isConstant(identifier.name()))
                        ? new ConstOperation(identifier, v)
                        : new GlobalOperation(identifier, v));
    }

    private List<LlvmOperation> generateArrayGlobals(final Identifier identifier, final SymbolTable symbolTable) {
        final var arrayType = (Arr) identifier.type();
        final var sizes = arrayDimensionSizes(identifier, symbolTable);
        final long length = sizes.stream().reduce(1L, (a, b) -> a * b);
        return List.of(
                new ArrayGlobalOperation(identifier, arrayType.getElementType(), length),
                new ArrayDimsOperation(identifier, sizes)
        );
    }

    /** Returns the number of elements in a static array (the product of its dimension sizes). */
    private long arrayLength(final Identifier identifier, final SymbolTable symbolTable) {
        return arrayDimensionSizes(identifier, symbolTable).stream().reduce(1L, (a, b) -> a * b);
    }

    private List<Long> arrayDimensionSizes(final Identifier identifier, final SymbolTable symbolTable) {
        final var subscripts = symbolTable.getArrayValue(identifier.name()).getSubscripts();
        // The subscripts are already inclusive-adjusted (+1) by semantic analysis, and are
        // constant expressions (dynamic arrays are rejected); evaluate them to sizes.
        return evaluateIntegerExpressions(subscripts, symbolTable, optimizer.expressionOptimizer());
    }

    private Map<Class<?>, LlvmStatementCodeGenerator<? extends Statement>> buildStatementDictionary() {
        final var map = new HashMap<Class<?>, LlvmStatementCodeGenerator<? extends Statement>>();
        map.put(AddAssignStatement.class, new AddAssignCodeGenerator(this, NONE));
        map.put(AssignStatement.class, new AssignCodeGenerator(this, NONE));
        map.put(CommentStatement.class, new CommentCodeGenerator());
        map.put(ConstDeclarationStatement.class, new ConstDeclarationCodeGenerator());
        map.put(DecStatement.class, new DecCodeGenerator(this, NONE));
        map.put(FunctionDefinitionStatement.class, new FunDefCodeGenerator(this, gc));
        map.put(GotoStatement.class, new GotoCodeGenerator());
        map.put(IDivAssignStatement.class, new IDivAssignCodeGenerator(this, NONE));
        map.put(IfStatement.class, new IfCodeGenerator(this));
        map.put(IncStatement.class, new IncCodeGenerator(this, NONE));
        map.put(LabelledStatement.class, new LabelledCodeGenerator(this));
        map.put(MulAssignStatement.class, new MulAssignCodeGenerator(this, NONE));
        map.put(ReturnStatement.class, new ReturnCodeGenerator(this, gc));
        map.put(SubAssignStatement.class, new SubAssignCodeGenerator(this, NONE));
        map.put(VariableDeclarationStatement.class, new VariableDeclarationCodeGenerator());
        map.put(WhileStatement.class, new WhileCodeGenerator(this));
        return map;
    }

    private Map<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>> buildExpressionDictionary() {
        final var map = new HashMap<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>>();
        map.put(AbsExpression.class, new AbsCodeGenerator(this));
        map.put(AddExpression.class, new BinaryCodeGenerator(this, FADD, ADD));
        map.put(AndExpression.class, new BinaryCodeGenerator(this, null, AND));
        map.put(BooleanLiteral.class, new LiteralCodeGenerator());
        map.put(CastToFloatExpression.class, new CastToFloatCodeGenerator(this));
        map.put(CastToF64Expression.class, new CastToFloatCodeGenerator(this));
        map.put(CastToIntExpression.class, new CastToIntCodeGenerator(this));
        map.put(CastToI64Expression.class, new CastToIntCodeGenerator(this));
        map.put(DivExpression.class, new BinaryCodeGenerator(this, FDIV, null));
        map.put(EqualExpression.class, eqCodeGenerator);
        map.put(PowExpression.class, new PowCodeGenerator(this));
        map.put(FloatLiteral.class, new LiteralCodeGenerator());
        map.put(GreaterExpression.class, gtCodeGenerator);
        map.put(GreaterOrEqualExpression.class, geCodeGenerator);
        map.put(IdentifierDerefExpression.class, new IdentDerefCodeGenerator(NONE));
        map.put(IDivExpression.class, new BinaryCodeGenerator(this, null, SDIV));
        map.put(IfExpression.class, new IfExpressionCodeGenerator(this, labelStack));
        map.put(IntegerLiteral.class, new LiteralCodeGenerator());
        map.put(LessExpression.class, ltCodeGenerator);
        map.put(LessOrEqualExpression.class, leCodeGenerator);
        map.put(LogicalAndExpression.class, new LogicalAndCodeGenerator(this, labelStack));
        map.put(LogicalNotExpression.class, new LogicalNotCodeGenerator(this));
        map.put(LogicalOrExpression.class, new LogicalOrCodeGenerator(this, labelStack));
        map.put(LogicalXorExpression.class, new LogicalXorCodeGenerator(this));
        map.put(ModExpression.class, new BinaryCodeGenerator(this, FREM, SREM));
        map.put(MulExpression.class, new BinaryCodeGenerator(this, FMUL, MUL));
        map.put(NegateExpression.class, new NegateCodeGenerator(this));
        map.put(NotEqualExpression.class, neCodeGenerator);
        map.put(NotExpression.class, new NotCodeGenerator(this));
        map.put(OrExpression.class, new BinaryCodeGenerator(this, null, OR));
        map.put(RoundExpression.class, new RoundCodeGenerator(this));
        map.put(ShiftLeftExpression.class, new BinaryCodeGenerator(this, null, SHL));
        map.put(StringLiteral.class, new StringLiteralCodeGenerator());
        map.put(SubExpression.class, new BinaryCodeGenerator(this, FSUB, SUB));
        map.put(TruncateExpression.class, new TruncateCodeGenerator(this));
        map.put(XorExpression.class, new BinaryCodeGenerator(this, null, XOR));
        return map;
    }
}
