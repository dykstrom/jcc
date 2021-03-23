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
import java.util.Collections.singletonList

/**
 * Compile-and-run integration tests for Basic, specifically for testing garbage collection.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunGarbageCollectionIT : AbstractIntegrationTest() {

    @Test
    fun shouldCallFunctionThatAllocatesMemory() {
        val source = listOf(
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
        val source = listOf(
                "foo% = 17",
                "bar$ = \"bar\"",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "print msg$"
        )
        val expected = listOf(
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
        val source = listOf(
                "foo% = 17",
                "bar$ = \"bar\"",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "print msg$"
        )
        val expected = listOf(
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
        val source = listOf(
                "foo% = 17",
                "bar$ = \"bar\"",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "msg$ = ucase\$(\"Hello!\")",
                "print msg$"
        )
        val expected = listOf(
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
        val source = listOf(
                "str$ = \"foo\"",
                "msg$ = str$",
                "print msg$"
        )
        val expected = listOf(
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
        val source = listOf(
                "str$ = ucase$(\"foo\")",
                "msg$ = str$",
                "str$ = ucase$(\"bar\")",
                "str$ = ucase$(\"axe\")",
                "print msg$",
                "print str$"
        )
        val expected = listOf(
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
        val source = listOf(
                "str$ = ucase$(\"foo\")",
                "str$ = \"foo\"",
                "msg$ = ucase$(\"bar\")",
                "msg$ = ucase$(\"axe\")",
                "print msg$",
                "print str$"
        )
        val expected = listOf(
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

    /**
     * Tests the case where no garbage collection is done because we have not allocated enough memory.
     * In this case, all GC output we get is about registering new memory.
     */
    @Test
    fun shouldRegisterMemoryAfterLineInput() {
        val source = listOf(
                "line input msg$",
                "print msg$"
        )
        val expected = listOf(
                "GC: Registering new memory:",
                "HELLO!"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 10)
        runAndAssertSuccess(sourceFile, singletonList("HELLO!"), expected, 0)
    }

    @Test
    fun shouldFreeMemoryAfterStringAddition() {
        val source = listOf(
                "msg$ = ucase$(\"Hello, \") + ucase$(\"world!\")",
                "print msg$"
        )
        val expected = listOf(
                "HELLO, WORLD!"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, expected, 0)
    }

    /**
     * Tests the case where dynamic memory is assigned to one variable. This variable is then swapped with another
     * variable. A new string is assigned to the original variable, which should not trigger garbage collection.
     * Finally, a new string is assigned to the first variable, which _should_ trigger garbage collection.
     */
    @Test
    fun shouldGarbageCollectAfterSwappingStrings() {
        val source = listOf(
                "str$ = ucase$(\"foo\")",
                "msg$ = \"bar\"",
                "print str$;\"-\";msg$",
                "swap str$, msg$",
                "print str$;\"-\";msg$",
                "str$ = ucase$(\"axe\")",
                "print str$;\"-\";msg$",
                "msg$ = ucase$(\"tee\")",
                "tmp$ = ucase$(\"zap\")",
                "print str$;\"-\";msg$"
        )
        val expected = listOf(
                "GC: Registering new memory:",                     // Assignment to str$
                "FOO-bar",
                "bar-FOO",
                "GC: Registering new memory:",                     // Assignment to str$
                "GC: Allocation count reached limit: 2",
                "GC: Marking memory:",                             // String assigned to str$
                "GC: Marking memory:",                             // String assigned to msg$ (was str$ before swapping)
                "GC: Collection finished with new limit: 4",
                "AXE-FOO",
                "GC: Registering new memory:",                     // Assignment to msg$
                "GC: Registering new memory:",                     // Assignment to tmp$
                "GC: Allocation count reached limit: 4",
                "GC: Marking memory:",                             // String assigned to tmp$
                "GC: Marking memory:",                             // String assigned to msg$
                "GC: Marking memory:",                             // String assigned to str$
                "GC: Sweeping memory:",                            // String previously assigned to msg$
                "GC: Collection finished with new limit: 6",
                "AXE-TEE"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 2)
        runAndAssertSuccess(sourceFile, expected, 0)
    }
}
