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

import org.antlr.v4.runtime.ANTLRInputStream;
import org.junit.Before;
import org.junit.Test;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.Cmp;
import se.dykstrom.jcc.common.assembly.instruction.DecReg;
import se.dykstrom.jcc.common.assembly.instruction.IncReg;
import se.dykstrom.jcc.common.assembly.instruction.Jne;
import se.dykstrom.jcc.common.error.CompilationErrorListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssembunnyCompilerTest {

    private static final String FILENAME = "file.asmb";

    private final CompilationErrorListener errorListener = new CompilationErrorListener();

    private final AssembunnyCompiler testee = new AssembunnyCompiler();

    @Before
    public void setUp() {
        testee.setSourceFilename(FILENAME);
        testee.setErrorListener(errorListener);
    }

    @Test
    public void testCompile_Ok() {
        ANTLRInputStream inputStream = new ANTLRInputStream("inc a cpy a d dec a jnz a -2");
        testee.setInputStream(inputStream);

        AsmProgram result = testee.compile();
        assertTrue(errorListener.getErrors().isEmpty());
        assertEquals(6, result.codes().stream().filter(code -> code instanceof Label).count());
        assertEquals(1, result.codes().stream().filter(code -> code instanceof IncReg).count());
        assertEquals(1, result.codes().stream().filter(code -> code instanceof DecReg).count());
        assertEquals(1, result.codes().stream().filter(code -> code instanceof Cmp).count());
        assertEquals(1, result.codes().stream().filter(code -> code instanceof Jne).count());
    }

    @Test
    public void testCompile_SyntaxErrorInc() {
        ANTLRInputStream inputStream = new ANTLRInputStream("inc e");
        testee.setInputStream(inputStream);
        testee.compile();
        assertEquals(2, errorListener.getErrors().size());
    }

    @Test
    public void testCompile_SyntaxErrorCpy() {
        ANTLRInputStream inputStream = new ANTLRInputStream("cpy a 1");
        testee.setInputStream(inputStream);
        testee.compile();
        assertEquals(1, errorListener.getErrors().size());
    }
}
