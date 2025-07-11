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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for Basic, specifically for testing garbage collection.
 *
 * @author Johan Dykstrom
 */
@EnabledOnOs(OS.WINDOWS)
class BasicCompileAndRunGarbageCollectionIT : AbstractIntegrationTests() {

    @Test
    fun shouldCallFunctionThatAllocatesMemory() {
        val source = listOf(
            "foo% = 17",
            "bar$ = \"bar\"",
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
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
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
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
        runAndAssertSuccess(sourceFile, expected)
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
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
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
        runAndAssertSuccess(sourceFile, expected)
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
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
            "msg$ = ucase$(\"Hello!\")",
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
        runAndAssertSuccess(sourceFile, expected)
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
        runAndAssertSuccess(sourceFile, expected)
    }

    /**
     * Tests the case of assigning one array element to another array element, and the first
     * array element points to a static string. In this case, no registration of dynamic memory
     * should be performed.
     */
    @Test
    fun shouldNotRegisterMemoryWhenAssigningStaticStringToArrayElement() {
        val source = listOf(
            "dim str$(1) as string, msg$(1) as string",
            "str$(1) = \"foo\"",
            "msg$(0) = str$(1)",
            "print msg$(0)"
        )
        val expected = listOf(
            "foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 10)
        runAndAssertSuccess(sourceFile, expected)
    }

    /**
     * Tests the case of assigning one array element to another array element, and the first
     * array element points to a dynamic string. Memory should be registered after the first
     * assignment and copied after the second assignment.
     */
    @Test
    fun shouldRegisterMemoryWhenAssigningDynamicStringToArrayElement() {
        val source = listOf(
            "dim str$(1) as string, msg$(7) as string",
            "str$(1) = ucase$(\"foo\")",
            "msg$(0) = str$(1)",
            "print msg$(0)"
        )
        val expected = listOf(
            "GC: Registering new memory:",
            "FOO"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 10)
        runAndAssertSuccess(sourceFile, expected)
    }

    /**
     * Tests the case of assigning dynamic memory to an array element, and then reassigning
     * it with a static string, throwing away the old memory reference.
     */
    @Test
    fun shouldRegisterAndThrowMemoryWhenAssigningDynamicStringToArrayElement() {
        val source = listOf(
            "dim str$(10) as string",
            "str$(5) = ucase$(\"foo\")",
            "str$(5) = \"bar\"",
            "print str$(5)"
        )
        val expected = listOf(
            "GC: Registering new memory:",
            "bar"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 10)
        runAndAssertSuccess(sourceFile, expected)
    }

    /**
     * Tests the case of assigning between string variables and string array elements.
     */
    @Test
    fun mixingStringsAndStringArrays() {
        val source = listOf(
            "dim str$(50) as string, msg$(50) as string",
            "str$(1) = ucase$(\"foo\")",
            "a.string$ = str$(1)",
            "msg$(50) = a.string$",
            "print str$(1) ; \"-\" ; msg$(50) ; \"-\" ; a.string$",
            "str$(1) = \"bar\"",
            "a.string$ = \"tee\"",
            "msg$(50) = ucase$(str$(1))",
            "print str$(1) ; \"-\" ; msg$(50) ; \"-\" ; a.string$"
        )
        val expected = listOf(
            "GC: Registering new memory:",
            "FOO-FOO-FOO",
            "GC: Registering new memory:",
            "bar-BAR-tee"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 10)
        runAndAssertSuccess(sourceFile, expected)
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
        runAndAssertSuccess(sourceFile, expected)
    }

    /**
     * Tests the case of assigning one array element to another array element, and the first
     * array element points to a dynamic string. In this case, they should point to the same
     * string, and the same node in the memory allocation list. They would have one type pointer
     * each, though, and the string will not be garbage collected even though the first array
     * element is reassigned with a new value.
     */
    @Test
    fun shouldRegisterAndReassignMemoryWithStringsArray() {
        val source = listOf(
            """
            dim arr(10) as string
            arr(9) = ucase$("foo")
            arr(5) = arr(9)
            arr(9) = ucase$("bar")
            arr(9) = ucase$("axe")
            print arr(5)
            print arr(9)
            """
        )
        val expected = listOf(
            "GC: Registering new memory:",
            "GC: Registering new memory:",
            "GC: Registering new memory:",
            "GC: Allocation count reached limit: 3",
            "GC: Marking memory:",                             // String assigned to arr(10)
            "GC: Marking memory:",                             // String assigned to arr(5)
            "GC: Sweeping memory:",                            // String previously assigned to arr(10)
            "GC: Collection finished with new limit: 4",
            "FOO",
            "AXE"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 3)
        runAndAssertSuccess(sourceFile, expected)
    }

    /**
     * Tests the case of assigning and reassigning dynamic memory to array elements
     * where the subscripts are function calls.
     */
    @Test
    fun arraySubscriptsCanBeFunctionCalls() {
        val source = listOf(
            """
            dim arr(10) as string
            arr(cint(val("9"))) = ucase$("foo")
            arr(cint(val("5"))) = arr(cint(9.1))
            print arr(5)
            print arr(9)
            arr(abs(9)) = ucase$("bar")
            arr(6 + 3) = ucase$("axe")
            print arr(5)
            print arr(9)
            """
        )
        val expected = listOf(
            "GC: Registering new memory:",
            "FOO",
            "FOO",
            "GC: Registering new memory:",
            "GC: Registering new memory:",
            "GC: Allocation count reached limit: 3",
            "GC: Marking memory:",                             // String assigned to arr(10)
            "GC: Marking memory:",                             // String assigned to arr(5)
            "GC: Sweeping memory:",                            // String previously assigned to arr(10)
            "GC: Collection finished with new limit: 4",
            "FOO",
            "AXE"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 3)
        runAndAssertSuccess(sourceFile, expected)
    }

    /**
     * Tests the case of assigning and reassigning dynamic memory to array elements
     * where the subscripts are array expressions. The case where the LHS and RHS of
     * an assignment refer to the same array element is of special interest.
     *
     * This does not work. The expression arr(val(arr(7))) is actually evaluated twice.
     * The first time is when the expression is assigned to the LHS as normal. The
     * second time is when we need the address of the array element arr(val(arr(7)))
     * points to, so we can copy the type pointer to __arr_type(7). But when evaluating
     * the expression the second time, the value of arr(7) has already changed, so we
     * copy the type pointer from the wrong array element (2 instead of 4), and thus
     * the string that originally is referenced by arr(4) and then later by arr(7) is
     * garbage collected when a new value is assigned to arr(4).
     */
    @Disabled("Fails because of bug in GC, see comment!")
    @Test
    fun arraySubscriptsCanBeArrayExpressions() {
        val source = listOf(
            "dim arr(10) as string",
            "arr(4) = ucase$(\"2\")",
            "arr(7) = \"4\"",
            "arr(7) = arr(val(arr(7)))", // arr(7) = "4" -> val(arr(7)) = 4 -> arr(val(arr(7))) = "2"
            "print arr(4)",
            "print arr(7)",
            "arr(4) = ucase$(\"1\")",
            "print arr(4)",
            "print arr(7)"
        )
        val expected = listOf(
            "GC: Registering new memory:",
            "2",
            "2",
            "GC: Registering new memory:",
            "GC: Allocation count reached limit: 2",
            "GC: Marking memory:",                             // String assigned to arr(4)
            "GC: Marking memory:",                             // String assigned to arr(7) and previously arr(4)
            "GC: Collection finished with new limit: 4",
            "1",
            "2"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 2)
        runAndAssertSuccess(sourceFile, expected)
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
        runAndAssertSuccess(sourceFile, expected)
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
        runAndAssertSuccess(sourceFile, listOf("HELLO!"), expected)
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
        runAndAssertSuccess(sourceFile, expected)
    }

    /**
     * Tests the case where dynamic memory is assigned to one variable. This variable is then swapped with a 
     * second variable. A new string is assigned to the first variable, which should not trigger garbage collection.
     * Finally, a new string is assigned to the second variable, which _should_ trigger garbage collection.
     */
    @Test
    fun shouldGarbageCollectAfterSwappingStrings() {
        val source = listOf(
            "first$ = ucase$(\"foo\")",
            "second$ = \"bar\"",
            "print first$;\"-\";second$",
            "swap first$, second$",
            "print first$;\"-\";second$",
            "first$ = ucase$(\"axe\")",
            "print first$;\"-\";second$",
            "second$ = ucase$(\"tee\")",
            "tmp$ = ucase$(\"zap\")",
            "print first$;\"-\";second$"
        )
        val expected = listOf(
            "GC: Registering new memory:",                     // Assignment to first$
            "FOO-bar",
            "bar-FOO",
            "GC: Registering new memory:",                     // Assignment to first$
            "GC: Allocation count reached limit: 2",
            "GC: Marking memory:",                             // String assigned to first$
            "GC: Marking memory:",                             // String assigned to second$ (was first$ before swapping)
            "GC: Collection finished with new limit: 4",
            "AXE-FOO",
            "GC: Registering new memory:",                     // Assignment to second$
            "GC: Registering new memory:",                     // Assignment to tmp$
            "GC: Allocation count reached limit: 4",
            "GC: Marking memory:",                             // String assigned to tmp$
            "GC: Marking memory:",                             // String assigned to second$
            "GC: Marking memory:",                             // String assigned to first$
            "GC: Sweeping memory:",                            // String previously assigned to second$
            "GC: Collection finished with new limit: 6",
            "AXE-TEE"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 2)
        runAndAssertSuccess(sourceFile, expected)
    }

    /**
     * Tests the case where dynamic memory is assigned to one string array element. This array element is then
     * swapped with a second string array element. A new string is assigned to the first array element, which should
     * not trigger garbage collection. Finally, a new string is assigned to the array element, which _should_ 
     * trigger garbage collection.
     */
    @Test
    fun shouldGarbageCollectAfterSwappingStringArrayElements() {
        val source = listOf(
            "dim arr$(10) as string",
            "arr$(2) = ucase$(\"foo\")",
            "arr$(3) = \"bar\"",
            "print arr$(2);\"-\";arr$(3)",
            "swap arr$(2), arr$(3)",
            "print arr$(2);\"-\";arr$(3)",
            "arr$(2) = ucase$(\"axe\")",
            "print arr$(2);\"-\";arr$(3)",
            "arr$(3) = ucase$(\"tee\")",
            "tmp$ = ucase$(\"zap\")",
            "print arr$(2);\"-\";arr$(3)"
        )
        val expected = listOf(
            "GC: Registering new memory:",                     // Assignment to arr$(2)
            "FOO-bar",
            "bar-FOO",
            "GC: Registering new memory:",                     // Assignment to arr$(2)
            "GC: Allocation count reached limit: 2",
            "GC: Marking memory:",                             // String assigned to arr$(2)
            "GC: Marking memory:",                             // String assigned to arr$(3) (was arr$(2) before swapping)
            "GC: Collection finished with new limit: 4",
            "AXE-FOO",
            "GC: Registering new memory:",                     // Assignment to arr$(3)
            "GC: Registering new memory:",                     // Assignment to tmp$
            "GC: Allocation count reached limit: 4",
            "GC: Marking memory:",                             // String assigned to tmp$
            "GC: Marking memory:",                             // String assigned to arr$(3)
            "GC: Marking memory:",                             // String assigned to arr$(2)
            "GC: Sweeping memory:",                            // String previously assigned to arr$(3)
            "GC: Collection finished with new limit: 6",
            "AXE-TEE"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, true, 2)
        runAndAssertSuccess(sourceFile, expected)
    }
}
