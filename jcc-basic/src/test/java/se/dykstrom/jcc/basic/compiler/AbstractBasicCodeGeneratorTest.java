/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.basic.compiler;

import se.dykstrom.jcc.basic.optimization.BasicAstOptimizer;
import se.dykstrom.jcc.common.intermediate.IntermediateProgram;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.assembly.instruction.Call;
import se.dykstrom.jcc.common.assembly.other.Import;
import se.dykstrom.jcc.common.assembly.other.Library;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public abstract class AbstractBasicCodeGeneratorTest {

    private static final Path SOURCE_PATH = Path.of("file.bas");

    static final Arr TYPE_ARR_I64_1 = Arr.from(1, I64.INSTANCE);
    static final Arr TYPE_ARR_I64_2 = Arr.from(2, I64.INSTANCE);
    static final Arr TYPE_ARR_I64_3 = Arr.from(3, I64.INSTANCE);
    static final Arr TYPE_ARR_F64_1 = Arr.from(1, F64.INSTANCE);
    static final Arr TYPE_ARR_STR_1 = Arr.from(1, Str.INSTANCE);

    static final IntegerLiteral IL_0 = new IntegerLiteral(0, 0, "0");
    static final IntegerLiteral IL_1 = new IntegerLiteral(0, 0, "1");
    static final IntegerLiteral IL_2 = new IntegerLiteral(0, 0, "2");
    static final IntegerLiteral IL_3 = new IntegerLiteral(0, 0, "3");
    static final IntegerLiteral IL_4 = new IntegerLiteral(0, 0, "4");
    static final IntegerLiteral IL_M1 = new IntegerLiteral(0, 0, "-1");

    static final FloatLiteral FL_3_14 = new FloatLiteral(0, 0, "3.14");
    static final FloatLiteral FL_17_E4 = new FloatLiteral(0, 0, "17E+4");

    static final StringLiteral SL_FOO = new StringLiteral(0, 0, "foo");
    static final StringLiteral SL_BAR = new StringLiteral(0, 0, "bar");
    static final StringLiteral SL_ONE = new StringLiteral(0, 0, "One");
    static final StringLiteral SL_TWO = new StringLiteral(0, 0, "Two");

    static final Identifier IDENT_F64_F = new Identifier("f#", F64.INSTANCE);
    static final Identifier IDENT_F64_G = new Identifier("g#", F64.INSTANCE);
    static final Identifier IDENT_I64_A = new Identifier("a%", I64.INSTANCE);
    static final Identifier IDENT_I64_H = new Identifier("h%", I64.INSTANCE);
    static final Identifier IDENT_STR_B = new Identifier("b$", Str.INSTANCE);
    static final Identifier IDENT_STR_S = new Identifier("s$", Str.INSTANCE);
    static final Identifier IDENT_ARR_I64_A = new Identifier("a%", TYPE_ARR_I64_1);
    static final Identifier IDENT_ARR_I64_B = new Identifier("b%", TYPE_ARR_I64_2);
    static final Identifier IDENT_ARR_I64_C = new Identifier("c%", TYPE_ARR_I64_3);
    static final Identifier IDENT_ARR_F64_D = new Identifier("d#", TYPE_ARR_F64_1);
    static final Identifier IDENT_ARR_STR_S = new Identifier("s$", TYPE_ARR_STR_1);
    static final Identifier IDENT_ARR_I64_X = new Identifier("x", TYPE_ARR_I64_1);

    static final Expression IDE_I64_A = new IdentifierDerefExpression(0, 0, IDENT_I64_A);
    static final Expression IDE_I64_H = new IdentifierDerefExpression(0, 0, IDENT_I64_H);
    static final Expression IDE_F64_F = new IdentifierDerefExpression(0, 0, IDENT_F64_F);
    static final Expression IDE_STR_B = new IdentifierDerefExpression(0, 0, IDENT_STR_B);

    static final IdentifierExpression INE_I64_A = new IdentifierNameExpression(0, 0, IDENT_I64_A);
    static final IdentifierExpression INE_STR_B = new IdentifierNameExpression(0, 0, IDENT_STR_B);
    static final IdentifierExpression INE_F64_F = new IdentifierNameExpression(0, 0, IDENT_F64_F);
    static final IdentifierExpression INE_F64_G = new IdentifierNameExpression(0, 0, IDENT_F64_G);
    static final IdentifierExpression INE_I64_H = new IdentifierNameExpression(0, 0, IDENT_I64_H);
    static final IdentifierExpression INE_STR_S = new IdentifierNameExpression(0, 0, IDENT_STR_S);
    static final IdentifierExpression INE_ARR_I64_X = new IdentifierNameExpression(0, 0, IDENT_ARR_I64_X);

    static final ArrayDeclaration DECL_ARR_I64_X = new ArrayDeclaration(0, 0, IDENT_ARR_I64_X.name(), TYPE_ARR_I64_1, singletonList(IL_1));

    private final BasicTypeManager typeManager = new BasicTypeManager();
    // Test with empty symbol table instead of the pre-filled one
    protected final SymbolTable symbols = new SymbolTable();
    protected final BasicAstOptimizer optimizer = new BasicAstOptimizer(typeManager, symbols);
    protected final BasicCodeGenerator codeGenerator = new BasicCodeGenerator(typeManager, symbols, optimizer);

    /**
     * Defines a function in the current scope.
     */
    void defineFunction(Function function) {
        symbols.addFunction(function);
    }

    IntermediateProgram assembleProgram(List<Statement> statements) {
        final Program program = new Program(0, 0, statements).withSourcePath(SOURCE_PATH);
        return codeGenerator.generate(program);
    }

    /**
     * Assemble the program made up by the given list of statements, and optimize it using the given optimizer.
     */
    IntermediateProgram assembleProgram(List<Statement> statements, AstOptimizer optimizer) {
        final Program program = new Program(0, 0, statements).withSourcePath(SOURCE_PATH);
        return codeGenerator.generate(optimizer.program(program));
    }

    static void assertCodeLines(List<Line> lines, int libraries, int functions, int labels, int calls) {
        assertEquals("libraries", 1, countInstances(Library.class, lines)); // One library statement
        int numberOfImportedLibraries = lines.stream()
                .filter(code -> code instanceof Library)
                .map(code -> (Library) code)
                .mapToInt(lib -> lib.libraries().size())
                .sum();
        assertEquals("libraries", libraries, numberOfImportedLibraries); // Number of imported libraries
        int numberOfImportedFunctions = lines.stream()
            .filter(code -> code instanceof Import)
            .map(code -> (Import) code)
            .mapToInt(imp -> imp.getFunctions().size())
            .sum();
        assertEquals("functions", functions, numberOfImportedFunctions); // Number of imported functions
        assertEquals("labels", labels, countInstances(Label.class, lines));
        assertEquals("calls", calls, countInstances(Call.class, lines));
    }

    static long countInstances(Class<?> clazz, List<Line> lines) {
        return lines.stream().filter(clazz::isInstance).count();
    }
}
