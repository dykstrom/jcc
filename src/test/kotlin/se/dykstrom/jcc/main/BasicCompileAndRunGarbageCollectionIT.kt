/*
 * Copyright (C) 2019 Johan Dykstrom
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

package se.dykstrom.jcc.main

import org.junit.Test
import java.util.Arrays.asList

/**
 * Compile-and-run integration tests for Basic, specifically for testing garbage collection.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunGarbageCollectionIT : AbstractIntegrationTest() {

    @Test
    fun shouldCallFunctionThatAllocatesMemory() {
        val source = asList(
                "foo% = 17",
                "bar$ = \"bar\"",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "print msg$"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "HELLO!\n", 0)
    }

    /**
     * Tests the case where no garbage collection is done because we have not allocated enough memory.
     * In this case, all GC output we get is about registering new memory.
     */
    @Test
    fun shouldNotGarbageCollect() {
        val source = asList(
                "foo% = 17",
                "bar$ = \"bar\"",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "print msg$"
        )
        val expected = asList(
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "HELLO!"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 10)
        runAndAssertSuccess(sourceFile, expected, 0)
    }

    /**
     * Tests the case where GC is run once. In this case, we will get GC output from
     * registering memory, from marking the stack, and from sweeping the memory
     * allocation list.
     */
    @Test
    fun shouldGarbageCollectOnce() {
        val source = asList(
                "foo% = 17",
                "bar$ = \"bar\"",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "print msg$"
        )
        val expected = asList(
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Allocation count reached limit: 3",
                "GC: Marking memory:",
                "GC: Sweeping memory:",
                "GC: Sweeping memory:",
                "GC: Collection finished with new limit: 2",
                "HELLO!"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 3)
        runAndAssertSuccess(sourceFile, expected, 0)
    }

    /**
     * Tests the case where GC is run twice. In this case, we will get GC output from
     * registering memory, from marking the stack, and from sweeping the memory
     * allocation list. Then again, registering memory, marking, and sweeping.
     */
    @Test
    fun shouldGarbageCollectTwice() {
        val source = asList(
                "foo% = 17",
                "bar$ = \"bar\"",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "print msg$"
        )
        val expected = asList(
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Allocation count reached limit: 3",
                "GC: Marking memory:",
                "GC: Sweeping memory:",
                "GC: Sweeping memory:",
                "GC: Collection finished with new limit: 2",
                "GC: Registering new memory:",
                "GC: Allocation count reached limit: 2",
                "GC: Marking memory:",
                "GC: Sweeping memory:",
                "GC: Collection finished with new limit: 2",
                "HELLO!"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 3)
        runAndAssertSuccess(sourceFile, expected, 0)
    }

    /**
     * Tests the case of assigning one variable to another variable, and the first variable
     * points to a static string. In this case, no registration of dynamic memory should be
     * performed.
     */
    @Test
    fun shouldNotRegisterMemory() {
        val source = asList(
                "str$ = \"foo\"",
                "msg$ = str$",
                "print msg$"
        )
        val expected = asList(
                "foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 10)
        runAndAssertSuccess(sourceFile, expected, 0)
    }

    /**
     * Tests the case of assigning one variable to another variable, and the first variable
     * points to a dynamic string. In this case, they should point to the same string, and
     * the same node in the memory allocation list. They would have one type pointer each,
     * though, and the string will not be garbage collected even though the first variable
     * is reassigned with a new value.
     */
    @Test
    fun shouldRegisterAndReassignMemory() {
        val source = asList(
                "str$ = ucase$(\"foo\")",
                "msg$ = str$",
                "str$ = ucase$(\"bar\")",
                "str$ = ucase$(\"axe\")",
                "print msg$",
                "print str$"
        )
        val expected = asList(
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Allocation count reached limit: 3",
                "GC: Marking memory:",                             // String assigned to str$
                "GC: Marking memory:",                             // String assigned to msg$
                "GC: Sweeping memory:",                            // String previously assigned to str$
                "GC: Collection finished with new limit: 4",
                "FOO",
                "AXE"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 3)
        runAndAssertSuccess(sourceFile, expected, 0)
    }

    /**
     * Tests the case of assigning a dynamic string to a variable, and then assigning a static string to it later.
     * In this case, after allocating some more memory to force a garbage collection, the dynamic memory first
     * assigned to the variable should be swept.
     */
    @Test
    fun shouldRegisterAndThrowMemory() {
        val source = asList(
                "str$ = ucase$(\"foo\")",
                "str$ = \"foo\"",
                "msg$ = ucase$(\"bar\")",
                "msg$ = ucase$(\"axe\")",
                "print msg$",
                "print str$"
        )
        val expected = asList(
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Registering new memory:",
                "GC: Allocation count reached limit: 3",
                "GC: Marking memory:",                             // String assigned to msg$
                "GC: Sweeping memory:",                            // String previously assigned to msg$
                "GC: Sweeping memory:",                            // String previously assigned to str$
                "GC: Collection finished with new limit: 2",
                "AXE",
                "foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 3)
        runAndAssertSuccess(sourceFile, expected, 0)
    }

    @Test
    fun shouldFreeMemoryAfterStringAddition() {
        val source = asList(
                "msg$ = ucase$(\"Hello, \") + ucase$(\"world!\")",
                "print msg$"
        )
        val expected = asList(
                "HELLO, WORLD!"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, expected, 0)
    }
}
