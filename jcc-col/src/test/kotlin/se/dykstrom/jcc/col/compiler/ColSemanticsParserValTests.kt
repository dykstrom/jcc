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
import se.dykstrom.jcc.col.ColTests.Companion.FUN_I64_TO_I64
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.common.ast.CastToIntExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64

class ColSemanticsParserValTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldParseTypedVal() {
        val program = parse(
            """
            val x as i64 := 17
            call println(x)
            """.trimIndent()
        )
        val statement = program.statements[0] as ValDeclarationStatement
        assertEquals(I64.INSTANCE, statement.declaration().type())
    }

    @Test
    fun shouldInferTypeFromInitializer() {
        val program = parse(
            """
            val phi := 1.618
            call println(phi)
            """.trimIndent()
        )
        val statement = program.statements[0] as ValDeclarationStatement
        assertEquals(F64.INSTANCE, statement.declaration().type())
    }

    @Test
    fun shouldParseValWithAliasType() {
        val program = parse(
            """
            alias Number as i64
            val x as Number := 17
            call println(x)
            """.trimIndent()
        )
        val statement = program.statements[1] as ValDeclarationStatement
        assertEquals(I64.INSTANCE, statement.declaration().type())
    }

    @Test
    fun shouldWidenInitializerToDeclaredType() {
        val program = parse(
            """
            val x as i64 := i32(17)
            call println(x)
            """.trimIndent()
        )
        val statement = program.statements[0] as ValDeclarationStatement
        assertEquals(I64.INSTANCE, statement.declaration().type())
        assertEquals(CastToIntExpression::class.java, statement.declaration().expression().javaClass)
    }

    @Test
    fun shouldParseValInitializedFromFunctionDefinedLater() {
        // Functions are hoisted, vals are not
        parse(
            """
            val x := g()
            call println(x)
            fun g() -> i64 := 17
            """.trimIndent()
        )
    }

    @Test
    fun shouldParseValReferencingEarlierVal() {
        parse(
            """
            val a := 17
            val b := a + 1
            call println(b)
            """.trimIndent()
        )
    }

    @Test
    fun shouldParseValUsedAsFunctionArgument() {
        parse(
            """
            val a := 17
            call println(max(a, 18))
            """.trimIndent()
        )
    }

    @Test
    fun shouldResolveOverloadFromDeclaredFunctionType() {
        val program = parse(
            """
            fun inc(a as i64) -> i64 := a + 1
            fun inc(a as f64) -> f64 := a + 1.0
            val f as (i64) -> i64 := inc
            call println(f(17))
            """.trimIndent()
        )
        val statement = program.statements[2] as ValDeclarationStatement
        assertEquals(FUN_I64_TO_I64, statement.declaration().type())
        val expression = statement.declaration().expression() as IdentifierDerefExpression
        assertEquals(FUN_I64_TO_I64, expression.identifier.type())
    }

    @Test
    fun shouldParseUntypedFunctionValWithSingleOverload() {
        val program = parse(
            """
            fun inc(a as i64) -> i64 := a + 1
            val f := inc
            call println(f(17))
            """.trimIndent()
        )
        val statement = program.statements[1] as ValDeclarationStatement
        assertEquals(FUN_I64_TO_I64, statement.declaration().type())
    }

    @Test
    fun shouldAllowParameterToShadowVal() {
        parse(
            """
            val a := 17
            fun f(a as f64) -> f64 := a + 1.0
            call println(f(1.0))
            call println(a)
            """.trimIndent()
        )
    }

    @Test
    fun shouldWarnAboutUnusedVal() {
        parseAndExpectWarning("val a := 17", "unused variable: a", UNUSED_VARIABLE)
    }

    @Test
    fun shouldNotWarnAboutValUsedOnlyAfterFunctionDefinition() {
        // Issue #78: top-level val x is used after a function definition; it must not be
        // reported unused just because it is not used inside the function.
        parse(
            """
            val x := 17
            fun foo(y as i64) -> i64 := y
            call println(x)
            """.trimIndent()
        )
        assertTrue(errorListener.warnings.isEmpty())
    }

    @Test
    fun shouldNotParseValWithoutInitializer() {
        parseAndExpectError("val x as i64", "value 'x' must have an initializer")
    }

    @Test
    fun shouldNotParseValWithTypeMismatch() {
        parseAndExpectError(
            "val x as f64 := 1",
            "you cannot initialize value 'x' of type f64 with an expression of type i64"
        )
    }

    @Test
    fun shouldNotParseValWithNarrowingInitializer() {
        parseAndExpectError(
            "val x as i32 := 17",
            "you cannot initialize value 'x' of type i32 with an expression of type i64"
        )
    }

    @Test
    fun shouldNotParseDuplicateVal() {
        parseAndExpectError(
            """
            val x := 17
            val x := 18
            """.trimIndent(),
            "value 'x' is already defined, with type i64"
        )
    }

    @Test
    fun shouldNotParseValWithFunctionName() {
        parseAndExpectError(
            """
            fun foo() -> i64 := 17
            val foo := 18
            """.trimIndent(),
            "value 'foo' is already defined as a function"
        )
    }

    @Test
    fun shouldNotParseValWithBuiltInFunctionName() {
        parseAndExpectError("val millis := 17", "value 'millis' is already defined as a function")
    }

    @Test
    fun shouldNotParseValUsedBeforeDeclaration() {
        parseAndExpectError(
            """
            call println(x)
            val x := 17
            """.trimIndent(),
            "undefined variable: x"
        )
    }

    @Test
    fun shouldNotParseValUsedInFunctionBody() {
        parseAndExpectError(
            """
            val a := 17
            fun f() -> i64 := a
            """.trimIndent(),
            "undefined variable: a"
        )
    }

    @Test
    fun shouldNotParseAmbiguousUntypedFunctionVal() {
        parseAndExpectError(
            """
            fun inc(a as i64) -> i64 := a + 1
            fun inc(a as f64) -> f64 := a + 1.0
            val f := inc
            """.trimIndent(),
            "ambiguous function reference in initializer of value 'f'"
        )
    }

    @Test
    fun shouldNotParseFunctionValWithNonMatchingType() {
        parseAndExpectError(
            """
            fun inc(a as i64) -> i64 := a + 1
            fun inc(a as f64) -> f64 := a + 1.0
            val f as (i64) -> f64 := inc
            """.trimIndent(),
            "you cannot initialize value 'f' of type function(i64)->f64"
        )
    }

    @Test
    fun shouldNotParseValWithUndefinedType() {
        parseAndExpectError("val x as number := 17", "undefined type: number")
    }

    @Test
    fun shouldNotParseValBoundWithEquals() {
        parseAndExpectError(
            """
            val x = 5
            call println(x)
            """.trimIndent(),
            "COL uses ':=' for binding: write 'val x := 5'"
        )
    }
}
