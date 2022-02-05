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

package se.dykstrom.jcc.assembunny.compiler;

import org.junit.Test;
import se.dykstrom.jcc.assembunny.ast.*;
import se.dykstrom.jcc.assembunny.ast.DecStatement;
import se.dykstrom.jcc.assembunny.ast.IncStatement;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Import;
import se.dykstrom.jcc.common.assembly.other.Library;
import se.dykstrom.jcc.common.ast.*;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class AssembunnyCodeGeneratorTest {

    private static final Expression IL_1 = new IntegerLiteral(0, 0, "1");
    private static final Expression RE_B = new RegisterExpression(0, 0, AssembunnyRegister.B);

    @Test
    public void shouldGenerateEmptyProgram() {
        AsmProgram result = assembleProgram(emptyList());
        assertCodeLines(result.lines(), 1, 1, 2, 1);
        // Initialize registers to 0
        assertEquals(4, countInstances(MoveImmToReg.class, result.lines()));
    }

    @Test
    public void shouldGenerateInc() {
        Statement is = new IncStatement(0, 0, AssembunnyRegister.D);
        AsmProgram result = assembleProgram(singletonList(new LabelledStatement("0", is)));
        assertCodeLines(result.lines(), 1, 1, 3, 1);
        assertEquals(1, countInstances(IncReg.class, result.lines()));
    }

    @Test
    public void shouldGenerateDec() {
        Statement ds = new DecStatement(0, 0, AssembunnyRegister.D);
        AsmProgram result = assembleProgram(singletonList(new LabelledStatement("0", ds)));
        assertCodeLines(result.lines(), 1, 1, 3, 1);
        assertEquals(1, countInstances(DecReg.class, result.lines()));
    }

    @Test
    public void shouldGenerateCpyFromInt() {
        Statement cs = new CpyStatement(0, 0, IL_1, AssembunnyRegister.D);
        AsmProgram result = assembleProgram(singletonList(new LabelledStatement("0", cs)));
        assertCodeLines(result.lines(), 1, 1, 3, 1);
        // Four for initializing, and one for the cpy statement
        assertEquals(5, countInstances(MoveImmToReg.class, result.lines()));
    }

    @Test
    public void shouldGenerateCpyFromReg() {
        Statement cs = new CpyStatement(0, 0, RE_B, AssembunnyRegister.D);
        AsmProgram result = assembleProgram(singletonList(new LabelledStatement("0", cs)));
        assertCodeLines(result.lines(), 1, 1, 3, 1);
        // Four for initializing
        assertEquals(4, countInstances(MoveImmToReg.class, result.lines()));
        // One for the cpy statement, and two for the exit statement
        assertEquals(3, countInstances(MoveRegToReg.class, result.lines()));
    }

    @Test
    public void shouldGenerateJnzOnInt() {
        Statement js = new JnzStatement(0, 0, IL_1, AssembunnyUtils.END_JUMP_TARGET);
        AsmProgram result = assembleProgram(singletonList(new LabelledStatement("0", js)));
        assertCodeLines(result.lines(), 1, 1, 3, 1);
        // Four for initializing, one for the integer literal
        assertEquals(5, countInstances(MoveImmToReg.class, result.lines()));
        // One for the jnz statement
        assertEquals(1, countInstances(CmpRegWithImm.class, result.lines()));
        // One for the jnz statement
        assertEquals(1, countInstances(Jne.class, result.lines()));
    }

    @Test
    public void shouldGenerateJnzOnReg() {
        Statement js = new JnzStatement(0, 0, RE_B, AssembunnyUtils.END_JUMP_TARGET);
        AsmProgram result = assembleProgram(singletonList(new LabelledStatement("0", js)));
        assertCodeLines(result.lines(), 1, 1, 3, 1);
        // Four for initializing
        assertEquals(4, countInstances(MoveImmToReg.class, result.lines()));
        // One for the register expression, and two for the exit statement
        assertEquals(3, countInstances(MoveRegToReg.class, result.lines()));
        // One for the jnz statement
        assertEquals(1, countInstances(CmpRegWithImm.class, result.lines()));
        // One for the jnz statement
        assertEquals(1, countInstances(Jne.class, result.lines()));
    }

    @Test
    public void shouldGenerateOutn() {
        Statement os = new OutnStatement(0, 0, RE_B);
        AsmProgram result = assembleProgram(singletonList(new LabelledStatement("0", os)));
        assertCodeLines(result.lines(), 1, 2, 3, 2);
    }

    private AsmProgram assembleProgram(List<Statement> statements) {
        Program program = new Program(0, 0, statements);
        program.setSourceFilename("file.asmb");
        AssembunnyCodeGenerator codeGenerator = new AssembunnyCodeGenerator();
        return codeGenerator.program(program);
    }

    /**
     * Asserts certain properties about the code fragments in {@code lines}. This method asserts that
     * the number of imported libraries and functions, the number of defined labels, and the number
     * of function calls are as specified.
     */
    private static void assertCodeLines(List<Line> lines, int libraries, int functions, int labels, int calls) {
        assertEquals("libraries", 1, countInstances(Library.class, lines)); // One library statement
        int numberOfImportedLibraries = lines.stream()
                .filter(code -> code instanceof Library)
                .map(code -> (Library) code)
                .mapToInt(lib -> lib.libraries().size())
                .sum();
        assertEquals("libraries", libraries, numberOfImportedLibraries); // Number of imported libraries
        assertEquals("functions", 1, countInstances(Import.class, lines)); // One import statement
        int numberOfImportedFunctions = lines.stream()
            .filter(code -> code instanceof Import)
            .map(code -> (Import) code)
            .mapToInt(imp -> imp.getFunctions().size())
            .sum();
        assertEquals("functions", functions, numberOfImportedFunctions); // Number of imported functions
        assertEquals("labels", labels, countInstances(Label.class, lines));
        assertEquals("calls", calls, countInstances(Call.class, lines));
    }

    /**
     * Returns the number of instances of {@code clazz} found in the list {@code lines}.
     */
    private static long countInstances(Class<?> clazz, List<Line> lines) {
        return lines.stream().filter(clazz::isInstance).count();
    }
}
