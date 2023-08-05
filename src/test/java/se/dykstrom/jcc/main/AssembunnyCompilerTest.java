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

package se.dykstrom.jcc.main;

import org.junit.After;
import org.junit.Test;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.Cmp;
import se.dykstrom.jcc.common.assembly.instruction.DecReg;
import se.dykstrom.jcc.common.assembly.instruction.IncReg;
import se.dykstrom.jcc.common.assembly.instruction.Jne;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.SyntaxException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class AssembunnyCompilerTest {

    private static final Path SOURCE_PATH = Path.of("file.asmb");
    private static final Path OUTPUT_PATH = Path.of("file.asm");

    private final CompilationErrorListener errorListener = new CompilationErrorListener();

    private final CompilerFactory factory = CompilerFactory.builder()
            .compileOnly(true)
            .errorListener(errorListener)
            .build();

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(OUTPUT_PATH);
    }

    @Test
    public void shouldCompileOk() {
        // Given
        final var compiler = factory.create("inc a cpy a d dec a jnz a -2", SOURCE_PATH, OUTPUT_PATH);

        // When
        final var lines = compiler.compile().lines();

        // Then
        assertTrue(errorListener.getErrors().isEmpty());
        assertEquals(6, lines.stream().filter(code -> code instanceof Label).count());
        assertEquals(1, lines.stream().filter(code -> code instanceof IncReg).count());
        assertEquals(1, lines.stream().filter(code -> code instanceof DecReg).count());
        assertEquals(1, lines.stream().filter(code -> code instanceof Cmp).count());
        assertEquals(1, lines.stream().filter(code -> code instanceof Jne).count());
    }

    @Test
    public void shouldFailWithSyntaxErrorInc() {
        final Compiler compiler = factory.create("inc e", SOURCE_PATH, OUTPUT_PATH);
        assertThrows(SyntaxException.class, compiler::compile);
        assertEquals(2, errorListener.getErrors().size());
    }

    @Test
    public void shouldFailWithSyntaxErrorCpy() {
        final Compiler compiler = factory.create("cpy a 1", SOURCE_PATH, OUTPUT_PATH);
        assertThrows(SyntaxException.class, compiler::compile);
        assertEquals(1, errorListener.getErrors().size());
    }
}
