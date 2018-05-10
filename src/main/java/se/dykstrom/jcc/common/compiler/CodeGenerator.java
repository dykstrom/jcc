package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.storage.StorageLocation;

/**
 * Interface to be implemented by all code generators.
 */
interface CodeGenerator {
    /**
     * Generates code for the given {@code program}.
     *
     * @param program The program to generate code for.
     * @return The generated code, in the form of an assembly program.
     */
    AsmProgram program(Program program);

    /**
     * Generates code that evaluates the given {@code expression}, and stores the result in {@code location}.
     *
     * @param expression The expression to evaluate.
     * @param location   The storage location where to store the result.
     */
    void expression(Expression expression, StorageLocation location);
}
