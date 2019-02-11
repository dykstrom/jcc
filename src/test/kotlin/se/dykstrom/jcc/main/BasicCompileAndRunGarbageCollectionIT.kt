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
     * Tests the case where no garbage collection is done. In this case,
     * all GC output we get will get is about registering new memory.
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
     * allocation list. The again, registering memory, marking, and sweeping.
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
}
