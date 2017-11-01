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

import org.junit.Test;

import java.nio.file.Path;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Compile-and-run integration tests for Basic, specifically for testing functions.
 *
 * @author Johan Dykstrom
 */
public class BasicCompileAndRunFunctionsIT extends AbstractIntegrationTest {

    @Test
    public void shouldCallAbs() throws Exception {
        List<String> source = asList(
                "print abs(1)",
                "print abs(-1)",
                "print abs(4 * 1000 + 7 * 100 + 11)",
                "print abs(-(4 * 1000 + 7 * 100 + 11))",
                "let a = 17 : print abs(a)",
                "let b = -17 : print abs(b)",
                "print abs(2147483650)",
                "print abs(-2147483650)",  // Does not fit in a 32-bit integer
                "print abs(abs(abs(-5)))"  // Nested calls
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "1\n1\n4711\n4711\n17\n17\n2147483650\n2147483650\n5\n", 0);
    }

    @Test
    public void shouldCallLen() throws Exception {
        List<String> source = asList(
                "print len(\"\")",
                "print len(\"a\")",
                "print len(\"abc\")",
                "print len(\"12345678901234567890123456789012345678901234567890\")"
        );
        Path sourceFile = createSourceFile(source, BASIC);
        compileAndAssertSuccess(sourceFile);
        runAndAssertSuccess(sourceFile, "0\n1\n3\n50\n", 0);
    }
}
