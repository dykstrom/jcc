package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Interface to be implemented by all code generators.
 */
public interface CodeGenerator {
    /**
     * Generates code for the given {@code program} in AST format.
     *
     * @param program The program to generate code for.
     * @return The generated code, in the form of a target language program, e.g. assembly code.
     */
    TargetProgram generate(final AstProgram program);

    /**
     * Generates code that evaluates the given {@code expression}, and stores the result in {@code location}.
     *
     * @param expression The expression to evaluate.
     * @param location   The storage location where to store the result.
     * @return The generated code.
     */
    List<Line> expression(Expression expression, StorageLocation location);

    /**
     * Generates code for the given statement. The generated code is stored in the code generator
     * itself, which therefore must inherit {@link CodeContainer}.
     *
     * @param statement The statement to generate code for.
     */
    void statement(final Statement statement);

    TypeManager types();

    SymbolTable symbols();

    StorageFactory storageFactory();

    List<Line> withLocalSymbolTable(Supplier<List<Line>> supplier);

    List<Line> withLocalStorageFactory(Consumer<CodeContainer> functionCodeGenerator);

    /**
     * Adds dependencies to all external libraries and function specified in {@code dependencies}.
     *
     * @param dependencies A map of library-to-functions that specifies dependencies.
     */
    void addAllFunctionDependencies(Map<String, Set<Function>> dependencies);
}
