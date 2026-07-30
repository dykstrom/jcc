/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.FUN_F64_TO_F64
import se.dykstrom.jcc.col.ColTests.Companion.FUN_I32_TO_I64
import se.dykstrom.jcc.col.ColTests.Companion.FUN_I64_TO_I64
import se.dykstrom.jcc.col.ColTests.Companion.FUN_TO_I64
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE

class ColSemanticsParserAnonymousFunctionTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldLiftAnonymousFunctionBoundToVal() {
        // When
        val program = parse("val f := fun() -> i64 := 5")

        // Then
        val lifted = liftedFunctions(program)
        assertEquals(1, lifted.size)
        assertEquals("lambda.0", lifted[0].identifier().name())
        assertEquals(FUN_TO_I64, lifted[0].identifier().type())
        // The val is initialized with a reference to the lifted function
        val reference = program.statements
            .filterIsInstance<ValDeclarationStatement>()
            .first()
            .declaration().expression() as IdentifierDerefExpression
        assertEquals(lifted[0].identifier(), reference.identifier)
    }

    @Test
    fun shouldInferReturnTypeFromBody() {
        // When
        val program = parse("val f := fun(a as i64) := a + 1")

        // Then
        val lifted = liftedFunctions(program)
        assertEquals(FUN_I64_TO_I64, lifted[0].identifier().type())
    }

    @Test
    fun shouldInferFloatReturnTypeFromBody() {
        // When
        val program = parse("val f := fun(a as f64) := a * 2.0")

        // Then
        val lifted = liftedFunctions(program)
        assertEquals(FUN_F64_TO_F64, lifted[0].identifier().type())
    }

    @Test
    fun shouldWidenBodyToDeclaredReturnType() {
        // When
        val program = parse("val f := fun(a as i32) -> i64 := a")

        // Then
        val lifted = liftedFunctions(program)
        assertEquals(FUN_I32_TO_I64, lifted[0].identifier().type())
    }

    @Test
    fun shouldLiftAnonymousFunctionPassedAsArgument() {
        // When
        val program = parse(
            """
            fun apply(f as (i64) -> i64, x as i64) -> i64 := f(x)
            call println(apply(fun(a as i64) := a + 1, 5))
            """
        )

        // Then
        val lifted = liftedFunctions(program)
        assertEquals(1, lifted.size)
        assertEquals(FUN_I64_TO_I64, lifted[0].identifier().type())
    }

    @Test
    fun shouldLiftAnonymousFunctionReturnedFromFunction() {
        // When
        val program = parse("fun adder() -> (i64) -> i64 := fun(x as i64) := x + 1")

        // Then
        val lifted = liftedFunctions(program)
        assertEquals(1, lifted.size)
        assertEquals(FUN_I64_TO_I64, lifted[0].identifier().type())
    }

    @Test
    fun shouldLiftNestedAnonymousFunctions() {
        // When
        val program = parse("val f := fun() -> (i64) -> i64 := fun(b as i64) := b + 1")

        // Then
        // The inner lambda is lifted first, since it is checked while checking the outer body
        val lifted = liftedFunctions(program)
        assertEquals(listOf("lambda.0", "lambda.1"), lifted.map { it.identifier().name() })
    }

    @Test
    fun shouldLiftSeveralAnonymousFunctionsWithUniqueNames() {
        // When
        val program = parse(
            """
            val f := fun(a as i64) := a + 1
            val g := fun(a as i64) := a + 2
            """
        )

        // Then
        assertEquals(listOf("lambda.0", "lambda.1"), liftedFunctions(program).map { it.identifier().name() })
    }

    @Test
    fun shouldNotSeeEnclosingFunctionParameter() {
        parseAndExpectError(
            "fun adder(n as i64) -> () -> i64 := fun() -> i64 := n",
            "undefined variable: n"
        )
    }

    @Test
    fun shouldNotSeeTopLevelVal() {
        parseAndExpectError(
            """
            val limit := 10
            val f := fun() -> i64 := limit
            """,
            "undefined variable: limit"
        )
    }

    @Test
    fun shouldNotSeeEnclosingAnonymousFunctionParameter() {
        parseAndExpectError(
            "val f := fun(a as i64) -> () -> i64 := fun() -> i64 := a",
            "undefined variable: a"
        )
    }

    @Test
    fun shouldNotAllowMissingParameterType() {
        parseAndExpectError("val f := fun(a) -> i64 := 0", "parameter 'a' must declare a type")
    }

    @Test
    fun shouldNotAllowDuplicateParameterNames() {
        parseAndExpectError(
            "val f := fun(a as i64, a as i64) -> i64 := a",
            "parameter 'a' is already defined"
        )
    }

    @Test
    fun shouldNotAllowBodyOfWrongType() {
        parseAndExpectError(
            "val f := fun(a as i64) -> f64 := a",
            "you cannot return a value of type i64 from an anonymous function with return type f64"
        )
    }

    @Test
    fun shouldNotAllowUndefinedReturnType() {
        parseAndExpectError("val f := fun(a as i64) -> foo := a", "undefined type: foo")
    }

    @Test
    fun shouldNotAllowVoidReturnType() {
        parseAndExpectError(
            "val f := fun(a as i64) -> void := a",
            "an anonymous function cannot have return type void"
        )
    }

    @Test
    fun shouldWarnAboutUnusedParameter() {
        parseAndExpectWarning("val f := fun(a as i64) -> i64 := 0", "unused variable: a", UNUSED_VARIABLE)
    }

    @Test
    fun shouldNotAllowBecomeToBuiltInFunction() {
        parseAndExpectError(
            "val f := fun(a as i64) -> i32 := become println(a)",
            "become can only tail-call a user-defined function"
        )
    }

    @Test
    fun shouldNotAllowBecomeOutsideTailPosition() {
        parseAndExpectError(
            """
            fun foo(a as i64) -> i64 := a
            val f := fun(a as i64) -> i64 := 1 + become foo(a)
            """,
            "become is not in tail position"
        )
    }

    @Test
    fun shouldNotAllowBecomeWithWideningReturnType() {
        parseAndExpectError(
            """
            fun foo(a as i64) -> i32 := i32(a)
            val f := fun(a as i64) -> i64 := become foo(a)
            """,
            "tail call returns i32 but the anonymous function returns i64"
        )
    }

    @Test
    fun shouldAllowBecomeInTailPositionOfAnonymousFunction() {
        // When
        val program = parse(
            """
            fun foo(a as i64) -> i64 := a
            val f := fun(a as i64) -> i64 := become foo(a)
            """
        )

        // Then
        assertEquals(1, liftedFunctions(program).size)
    }

    /**
     * Returns the function definitions the semantics parser prepended to the program, that is,
     * the ones lifted from anonymous functions. They come before all statements from the source.
     */
    private fun liftedFunctions(program: AstProgram): List<FunctionDefinitionStatement> {
        val statements = program.statements
        val lifted = statements
            .takeWhile { it is FunctionDefinitionStatement && it.identifier().name().startsWith("lambda.") }
            .map { it as FunctionDefinitionStatement }
        assertTrue(lifted.isNotEmpty(), "no lifted functions in: $statements")
        return lifted
    }
}
