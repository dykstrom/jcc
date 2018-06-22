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

import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.Call;
import se.dykstrom.jcc.common.assembly.other.Import;
import se.dykstrom.jcc.common.assembly.other.Library;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.*;

import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class AbstractBasicCodeGeneratorTest {

    private static final String FILENAME = "file.bas";

    static final IntegerLiteral IL_1 = new IntegerLiteral(0, 0, "1");
    static final IntegerLiteral IL_2 = new IntegerLiteral(0, 0, "2");
    static final IntegerLiteral IL_3 = new IntegerLiteral(0, 0, "3");
    static final IntegerLiteral IL_4 = new IntegerLiteral(0, 0, "4");

    static final FloatLiteral FL_3_14 = new FloatLiteral(0, 0, "3.14");
    static final FloatLiteral FL_17_E4 = new FloatLiteral(0, 0, "17E+4");

    static final StringLiteral SL_FOO = new StringLiteral(0, 0, "foo");
    static final StringLiteral SL_ONE = new StringLiteral(0, 0, "One");
    static final StringLiteral SL_TWO = new StringLiteral(0, 0, "Two");

    static final BooleanLiteral BL_TRUE = new BooleanLiteral(0, 0, "-1");
    static final BooleanLiteral BL_FALSE = new BooleanLiteral(0, 0, "0");

    static final Identifier IDENT_F64_F = new Identifier("f", F64.INSTANCE);
    static final Identifier IDENT_I64_A = new Identifier("a%", I64.INSTANCE);
    static final Identifier IDENT_I64_H = new Identifier("h%", I64.INSTANCE);
    static final Identifier IDENT_STR_B = new Identifier("b$", Str.INSTANCE);
    static final Identifier IDENT_BOOL_C = new Identifier("c", Bool.INSTANCE);
    static final Identifier IDENT_UNK_U = new Identifier("u", Unknown.INSTANCE);

    static final Expression IDE_I64_A = new IdentifierDerefExpression(0, 0, IDENT_I64_A);
    static final Expression IDE_I64_H = new IdentifierDerefExpression(0, 0, IDENT_I64_H);
    static final Expression IDE_UNK_U = new IdentifierDerefExpression(0, 0, IDENT_UNK_U);

    private final BasicCodeGenerator codeGenerator = new BasicCodeGenerator();

    /**
     * Defines a function in the current scope.
     */
    void defineFunction(Function function) {
        codeGenerator.getSymbols().addFunction(function);
    }

    AsmProgram assembleProgram(List<Statement> statements) {
        Program program = new Program(0, 0, statements);
        program.setSourceFilename(FILENAME);
        return codeGenerator.program(program);
    }

    static void assertCodes(List<Code> codes, int libraries, int functions, int labels, int calls) {
        assertEquals("libraries", 1, countInstances(Library.class, codes)); // One library statement
        int numberOfImportedLibraries = codes.stream()
                .filter(code -> code instanceof Library)
                .map(code -> (Library) code)
                .mapToInt(lib -> lib.getLibraries().size())
                .sum();
        assertEquals("libraries", libraries, numberOfImportedLibraries); // Number of imported libraries
        assertEquals("functions", 1, countInstances(Import.class, codes)); // One import statement
        int numberOfImportedFunctions = codes.stream()
            .filter(code -> code instanceof Import)
            .map(code -> (Import) code)
            .mapToInt(imp -> imp.getFunctions().size())
            .sum();
        assertEquals("functions", functions, numberOfImportedFunctions); // Number of imported functions
        assertEquals("labels", labels, countInstances(Label.class, codes));
        assertEquals("calls", calls, countInstances(Call.class, codes));
    }

    static long countInstances(Class<?> clazz, List<Code> codes) {
        return codes.stream().filter(clazz::isInstance).count();
    }
}
