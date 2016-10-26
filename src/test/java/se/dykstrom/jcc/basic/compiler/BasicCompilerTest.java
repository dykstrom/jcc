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

package se.dykstrom.jcc.basic.compiler;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.junit.Before;
import org.junit.Test;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect;
import se.dykstrom.jcc.common.assembly.instruction.Jmp;
import se.dykstrom.jcc.common.error.CompilationErrorListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

public class BasicCompilerTest {

    private static final String FILENAME = "file.basic";

    private final CompilationErrorListener errorListener = new CompilationErrorListener();

    private final BasicCompiler testee = new BasicCompiler();

    @Before
    public void setUp() {
        testee.setSourceFilename(FILENAME);
        testee.setErrorListener(errorListener);
    }

    @Test
    public void testCompile_Ok() {
        ANTLRInputStream inputStream = new ANTLRInputStream("10 PRINT \"Hi!\"" + EOL + "20 GOTO 10");
        testee.setInputStream(inputStream);

        AsmProgram result = testee.compile();
        assertTrue(errorListener.getErrors().isEmpty());
        assertEquals(1, result
                .codes()
                .stream()
                .filter(code -> code instanceof CallIndirect)
                .map(code -> ((CallIndirect) code).getTarget())
                .filter(target -> target.equals("[printf]"))
                .count());
        assertEquals(1, result
                .codes()
                .stream()
                .filter(code -> code instanceof Jmp)
                .map(code -> ((Jmp) code).getTarget().getMappedName())
                .filter(target -> target.equals("__line_10"))
                .count());
    }

    @Test
    public void testCompile_SyntaxError() {
        ANTLRInputStream inputStream = new ANTLRInputStream("10 GOTO");
        testee.setInputStream(inputStream);

        testee.compile();
        assertEquals(1, errorListener.getErrors().size());
    }

    @Test
    public void testCompile_SemanticsError() {
        ANTLRInputStream inputStream = new ANTLRInputStream("10 GOTO 20");
        testee.setInputStream(inputStream);

        testee.compile();
        assertEquals(1, errorListener.getErrors().size());
    }
}
