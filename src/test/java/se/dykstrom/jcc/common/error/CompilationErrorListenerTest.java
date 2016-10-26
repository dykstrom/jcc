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

package se.dykstrom.jcc.common.error;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class CompilationErrorListenerTest {

    private static final String MSG = "error message";

    private final CompilationErrorListener testee = new CompilationErrorListener();

    @Test
    public void testSemanticsError() {
        int line = 1;
        int column = 2;

        testee.semanticsError(line, column, MSG, new SemanticsException(MSG));

        List<CompilationError> errors = testee.getErrors();
        assertEquals(1, errors.size());
        CompilationError error = errors.get(0);
        assertEquals(line, error.getLine());
        assertEquals(column, error.getColumn());
        assertEquals(MSG, error.getMsg());
    }
}
