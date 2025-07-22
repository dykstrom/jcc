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
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.expression.*;
import se.dykstrom.jcc.llvm.code.statement.*;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operation.*;

import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO_I32;
import static se.dykstrom.jcc.llvm.LlvmOperator.*;

public abstract class AbstractLlvmCodeGenerator implements LlvmCodeGenerator {

    private static final Identifier MAIN = new Identifier("main", Fun.from(List.of(), I32.INSTANCE));

    private final TypeManager typeManager;
    private final SymbolTable symbolTable;
    private final AstOptimizer optimizer;

    protected final Map<Class<?>, LlvmStatementCodeGenerator<? extends Statement>> statementDictionary;
    protected final Map<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>> expressionDictionary;

    public AbstractLlvmCodeGenerator(final TypeManager typeManager,
                                     final SymbolTable symbolTable,
                                     final AstOptimizer optimizer) {
        this.typeManager = requireNonNull(typeManager);
        this.symbolTable = requireNonNull(symbolTable);
        this.optimizer = requireNonNull(optimizer);

        this.statementDictionary = buildStatementDictionary();
        this.expressionDictionary = buildExpressionDictionary();
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

    protected static Statement generateMainFunction(final List<Statement> statements, final boolean addReturn) {
        final var list = statements.stream()
                .filter(s -> !(s instanceof FunctionDefinitionStatement)) // Ignore function definitions
                .collect(toCollection(ArrayList::new));
        if (addReturn) {
            list.add(new ReturnStatement(0, 0, ZERO_I32));
        }
        return new FunctionDefinitionStatement(0, 0, MAIN, List.of(), list);
    }

    protected static List<Line> generateHeader(final Path path) {
        return List.of(
                new LlvmComment("ModuleID = '" + path + "'"),
                new Text("source_filename = \"" + path + "\"")
        );
    }

    protected List<? extends LlvmOperation> generateDeclares(final Set<String> calledFunctions) {
        // Look up all library functions in symbol table, and find the ones that have been called
        final var libraryFunctions = getCalledLibraryFunctions(calledFunctions, getLibraryFunctions());
        // Add a declare operation for each called library function
        return libraryFunctions.stream()
                .sorted()
                .map(DeclareOperation::new)
                .toList();
    }

    protected Set<String> getCalledFunctions(final List<Line> operations) {
        return operations.stream()
                .filter(o -> o instanceof CallOperation)
                .map(o -> (CallOperation) o)
                // Extract callee name
                .map(CallOperation::callee)
                .collect(toSet());
    }

    private Set<LibraryFunction> getLibraryFunctions() {
        return symbolTable.functionIdentifiers().stream()
                .map(symbolTable::getFunction)
                .filter(f -> f instanceof LibraryFunction)
                .map(f -> (LibraryFunction) f)
                .collect(toSet());
    }

    private Set<LibraryFunction> getCalledLibraryFunctions(final Set<String> calledFunctions,
                                                           final Set<LibraryFunction> definedFunctions) {
        return definedFunctions.stream()
                .filter(f -> calledFunctions.contains(f.externalName()))
                .collect(toSet());
    }

    protected List<? extends LlvmOperation> generateGlobals(final SymbolTable symbolTable) {
        return symbolTable.identifiers().stream()
                // Global variables start with @
                .filter(i -> i.name().startsWith("@"))
                .sorted()
                .map(i -> generateGlobal(i, symbolTable))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<LlvmOperation> generateGlobal(final Identifier identifier, final SymbolTable symbolTable) {
        return Optional.ofNullable(symbolTable.getValue(identifier.name()))
                .filter(v -> v instanceof String)
                .map(v -> (String) v)
                .map(v -> (symbolTable.isConstant(identifier.name()))
                        ? new ConstOperation(identifier, v)
                        : new GlobalOperation(identifier, v));
    }

    private Map<Class<?>, LlvmStatementCodeGenerator<? extends Statement>> buildStatementDictionary() {
        final var map = new HashMap<Class<?>, LlvmStatementCodeGenerator<? extends Statement>>();
        map.put(AddAssignStatement.class, new AddAssignCodeGenerator(this));
        map.put(AssignStatement.class, new AssignCodeGenerator(this));
        map.put(DecStatement.class, new DecCodeGenerator(this));
        map.put(FunctionDefinitionStatement.class, new FunDefCodeGenerator(this));
        map.put(IncStatement.class, new IncCodeGenerator(this));
        map.put(LabelledStatement.class, new LabelCodeGenerator(this));
        map.put(ReturnStatement.class, new ReturnCodeGenerator(this));
        map.put(SubAssignStatement.class, new SubAssignCodeGenerator(this));
        return map;
    }

    private Map<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>> buildExpressionDictionary() {
        final var map = new HashMap<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>>();
        map.put(AddExpression.class, new BinaryCodeGenerator(this, FADD, ADD));
        map.put(AndExpression.class, new BinaryCodeGenerator(this, null, AND));
        map.put(BooleanLiteral.class, new LiteralCodeGenerator());
        map.put(CastToFloatExpression.class, new CastToFloatCodeGenerator(this));
        map.put(CastToIntExpression.class, new CastToIntCodeGenerator(this));
        map.put(DivExpression.class, new BinaryCodeGenerator(this, FDIV, null));
        map.put(EqualExpression.class, new RelationalCodeGenerator(this, "oeq", "eq"));
        map.put(FloatLiteral.class, new LiteralCodeGenerator());
        map.put(GreaterExpression.class, new RelationalCodeGenerator(this, "ogt", "sgt"));
        map.put(GreaterOrEqualExpression.class, new RelationalCodeGenerator(this, "oge", "sge"));
        map.put(IdentifierDerefExpression.class, new IdentDerefCodeGenerator());
        map.put(IDivExpression.class, new BinaryCodeGenerator(this, null, SDIV));
        map.put(IntegerLiteral.class, new LiteralCodeGenerator());
        map.put(LessExpression.class, new RelationalCodeGenerator(this, "olt", "slt"));
        map.put(LessOrEqualExpression.class, new RelationalCodeGenerator(this, "ole", "sle"));
        map.put(LogicalAndExpression.class, new LogicalAndCodeGenerator(this));
        map.put(LogicalNotExpression.class, new LogicalNotCodeGenerator(this));
        map.put(LogicalOrExpression.class, new LogicalOrCodeGenerator(this));
        map.put(LogicalXorExpression.class, new LogicalXorCodeGenerator(this));
        map.put(ModExpression.class, new BinaryCodeGenerator(this, null, SREM));
        map.put(MulExpression.class, new BinaryCodeGenerator(this, FMUL, MUL));
        map.put(NegateExpression.class, new NegateCodeGenerator(this));
        map.put(NotEqualExpression.class, new RelationalCodeGenerator(this, "one", "ne"));
        map.put(NotExpression.class, new NotCodeGenerator(this));
        map.put(OrExpression.class, new BinaryCodeGenerator(this, null, OR));
        map.put(SubExpression.class, new BinaryCodeGenerator(this, FSUB, SUB));
        map.put(TruncateExpression.class, new TruncateCodeGenerator(this));
        map.put(XorExpression.class, new BinaryCodeGenerator(this, null, XOR));
        return map;
    }
}
