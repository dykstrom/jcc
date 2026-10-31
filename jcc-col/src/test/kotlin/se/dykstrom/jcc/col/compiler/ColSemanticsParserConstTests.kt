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
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.ConstDeclarationStatement
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.LiteralExpression
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE
import se.dykstrom.jcc.common.types.Bool
import se.dykstrom.jcc.common.types.F32
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Opaque
import se.dykstrom.jcc.common.types.Str
import se.dykstrom.jcc.common.types.Type

class ColSemanticsParserConstTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldDefineConstWithOpaqueType() {
        val program = parse(
            """
            type Meters as f64
            const LENGTH := Meters(7.3) + Meters(2.2)
            fun twice() -> Meters := LENGTH + LENGTH
            call println(string(twice()))
            """.trimIndent()
        )
        assertConst(program.statements[1], "LENGTH", Opaque("Meters", F64.INSTANCE), "9.5")
        assertEquals("9.5", symbolTable.getValue("LENGTH"))
        assertTrue(symbolTable.isConstant("LENGTH"))
    }

    @Test
    fun shouldInferTypeFromInitializer() {
        val program = parse(
            """
            type Name as string
            const A := 17i32
            const B := 17
            const C := 1.5f32
            const D := 1.5
            const E := true
            const F := "hello"
            const G := Name("Bob")
            call println(i64(A) + B)
            call println(f64(C) + D)
            call println(E)
            call println(F + string(G))
            """.trimIndent()
        )
        assertConst(program.statements[1], "A", I32.INSTANCE, "17")
        assertConst(program.statements[2], "B", I64.INSTANCE, "17")
        assertConst(program.statements[3], "C", F32.INSTANCE, "1.5")
        assertConst(program.statements[4], "D", F64.INSTANCE, "1.5")
        assertConst(program.statements[5], "E", Bool.INSTANCE, "-1")
        assertConst(program.statements[6], "F", Str.INSTANCE, "hello")
        assertConst(program.statements[7], "G", Opaque("Name", Str.INSTANCE), "Bob")
    }

    @Test
    fun shouldWidenInitializerToDeclaredType() {
        val program = parse(
            """
            const N as i64 := 17i32
            call println(N)
            """.trimIndent()
        )
        assertConst(program.statements[0], "N", I64.INSTANCE, "17")
    }

    @Test
    fun shouldUseConstDeclaredBelow() {
        // Consts are hoisted, like functions and types
        val program = parse(
            """
            call println(f())
            call println(N)
            fun f() -> i64 := N + 1
            const N := 17
            """.trimIndent()
        )
        val body = (program.statements[2] as FunctionDefinitionStatement).expression().toString()
        assertEquals("N + 1", body)
    }

    @Test
    fun shouldUseEarlierConstInInitializer() {
        val program = parse(
            """
            const A := 17
            const B := A * 2
            call println(B)
            """.trimIndent()
        )
        assertConst(program.statements[1], "B", I64.INSTANCE, "34")
    }

    @Test
    fun shouldNotEvaluateBranchNotTaken() {
        val program = parse(
            """
            const A := 17
            const B := 0
            const C := if B == 0 then 0 else A div B
            call println(C)
            """.trimIndent()
        )
        assertConst(program.statements[2], "C", I64.INSTANCE, "0")
    }

    @Test
    fun shouldAllowParameterToShadowConst() {
        val program = parse(
            """
            const A := 17
            fun f(A as f64) -> f64 := A + 1.0
            call println(f(1.0))
            call println(A)
            """.trimIndent()
        )
        val body = (program.statements[1] as FunctionDefinitionStatement).expression() as AddExpression
        assertEquals(F64.INSTANCE, ((body.left) as IdentifierDerefExpression).identifier.type())
    }

    @Test
    fun shouldWarnAboutUnusedConst() {
        parseAndExpectWarning("const A := 17", "unused variable: A", UNUSED_VARIABLE)
    }

    @Test
    fun shouldNotWarnAboutConstUsedOnlyInFunction() {
        parse(
            """
            const A := 17
            fun f() -> i64 := A
            call println(f())
            """.trimIndent()
        )
        assertTrue(errorListener.warnings.isEmpty(), errorListener.warnings.toString())
    }

    // Errors

    @Test
    fun shouldNotParseConstWithoutInitializer() {
        parseAndExpectError("const X as i64", "constant 'X' must have an initializer")
    }

    @Test
    fun shouldNotParseConstWithTypeMismatch() {
        parseAndExpectError(
            "const X as f64 := 1",
            "you cannot initialize constant 'X' of type f64 with an expression of type i64"
        )
    }

    @Test
    fun shouldNotParseConstUsingLaterConst() {
        parseAndExpectError(
            """
            const A := B + 1
            const B := 17
            """.trimIndent(),
            "undefined variable: B"
        )
    }

    @Test
    fun shouldReportTypedConstUsingLaterConstOnce() {
        parseAndExpectOneError(
            """
            const A as i64 := B + 1
            const B := 17
            """.trimIndent(),
            "undefined variable: B"
        )
    }

    @Test
    fun shouldReportTypedConstUsingUndefinedNameOnce() {
        parseAndExpectOneError("const X as i64 := y", "undefined variable: y")
    }

    @Test
    fun shouldReportNegatedBoolOnce() {
        // The negation still has a type, so the evaluator sees the operand that was reported
        parseAndExpectOneError("const X := -true", "cannot negate bool")
    }

    @Test
    fun shouldReportBitwiseNotOfFloatOnce() {
        parseAndExpectOneError("const X := ~1.5", "cannot bitwise-not f64")
    }

    @Test
    fun shouldReportOrderedStringComparisonOnce() {
        // A comparison is always bool, so the evaluator sees the strings that were reported
        parseAndExpectOneError("const X := \"a\" < \"b\"", "cannot order strings")
    }

    @Test
    fun shouldNotParseConstCallingUserFunction() {
        parseAndExpectError(
            """
            fun f() -> i64 := 17
            const X := f()
            """.trimIndent(),
            "constant 'X' is defined with non-constant expression: f()"
        )
    }

    @Test
    fun shouldNotParseConstCallingMillis() {
        parseAndExpectError("const X := millis()", "constant 'X' is defined with non-constant expression: millis()")
    }

    @Test
    fun shouldNotParseConstCallingString() {
        parseAndExpectError("const X := string(17)", "constant 'X' is defined with non-constant expression")
    }

    @Test
    fun shouldNotParseConstCallingAbs() {
        parseAndExpectError("const X := abs(-17)", "constant 'X' is defined with non-constant expression")
    }

    @Test
    fun shouldNotParseConstUsingVal() {
        // Vals are defined after consts, so a val is not visible in a const initializer
        parseAndExpectError(
            """
            val V := 17
            const X := V + 1
            """.trimIndent(),
            "undefined variable: V"
        )
    }

    @Test
    fun shouldNotParseDuplicateConst() {
        parseAndExpectError(
            """
            const X := 17
            const X := 18
            """.trimIndent(),
            "constant 'X' is already defined, with type i64"
        )
    }

    @Test
    fun shouldNotParseValWithConstName() {
        parseAndExpectError(
            """
            const X := 17
            val X := 18
            """.trimIndent(),
            "value 'X' is already defined, with type i64"
        )
    }

    @Test
    fun shouldNotParseConstWithValName() {
        // The const is defined first, whatever the order in the source
        parseAndExpectError(
            """
            val X := 18
            const X := 17
            """.trimIndent(),
            "value 'X' is already defined, with type i64"
        )
    }

    @Test
    fun shouldNotParseConstWithNameOfEarlierFunction() {
        parseAndExpectError(
            """
            fun foo() -> i64 := 17
            const foo := 18
            """.trimIndent(),
            "constant 'foo' is already defined as a function"
        )
    }

    @Test
    fun shouldNotParseConstWithNameOfLaterFunction() {
        parseAndExpectError(
            """
            const foo := 18
            fun foo() -> i64 := 17
            """.trimIndent(),
            "constant 'foo' is already defined as a function"
        )
    }

    @Test
    fun shouldNotParseConstWithTypeName() {
        parseAndExpectError(
            """
            type Meters as f64
            const Meters := 1.0
            """.trimIndent(),
            "constant 'Meters' is already defined as a function"
        )
    }

    @Test
    fun shouldNotParseFunctionTypedConst() {
        parseAndExpectError(
            """
            fun inc(a as i64) -> i64 := a + 1
            const F as (i64) -> i64 := inc
            """.trimIndent(),
            "you cannot declare constant 'F' of type"
        )
    }

    @Test
    fun shouldNotParseConstInWhileBody() {
        parseAndExpectError(
            """
            while false do
              const X := 17
            end
            """.trimIndent(),
            "statement not allowed in while body"
        )
    }

    @Test
    fun shouldNotParseConstDividingByZero() {
        parseAndExpectError(
            """
            const Z := 0
            const X := 17 div Z
            """.trimIndent(),
            "cannot evaluate constant 'X': division by zero"
        )
    }

    @Test
    fun shouldNotParseConstWithOverflowingDivision() {
        parseAndExpectError(
            "const X := -2147483648i32 div -1i32",
            "cannot evaluate constant 'X': integer overflow"
        )
    }

    @Test
    fun shouldNotParseConstWithOutOfRangeCast() {
        parseAndExpectError("const X := i32(1e10)", "cannot evaluate constant 'X': value 1.0E10 does not fit in i32")
    }

    @Test
    fun shouldReportOneErrorForFailedConstUsedLater() {
        // The failed const is still defined, so neither its use in another const, nor in a
        // function, is reported again
        parseAndExpectOneError(
            """
            const A := millis()
            const B := A + 1
            fun f() -> i64 := B
            call println(f())
            """.trimIndent(),
            "constant 'A' is defined with non-constant expression: millis()"
        )
    }

    @Test
    fun shouldNotParseConstBoundWithEquals() {
        parseAndExpectOneError(
            """
            const X = 5
            call println(X)
            """.trimIndent(),
            "COL uses ':=' for binding: write 'const X := 5'"
        )
    }

    private fun assertConst(statement: Statement, name: String, type: Type, value: String) {
        val declaration = (statement as ConstDeclarationStatement).declarations.single()
        assertEquals(name, declaration.name())
        assertEquals(type, declaration.type())
        assertEquals(value, (declaration.expression() as LiteralExpression).value)
    }
}
