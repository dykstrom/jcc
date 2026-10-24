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
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.types.Bool
import se.dykstrom.jcc.common.types.F32
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Opaque
import se.dykstrom.jcc.common.types.Str

class ColSemanticsParserOpaqueTypeTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldDefineOpaqueTypeOverEveryScalarType() {
        parse(
            """
            type A as i32
            type B as i64
            type C as f32
            type D as f64
            type E as bool
            type F as string
            """.trimIndent()
        )
        assertEquals(Opaque("D", F64.INSTANCE), typeManager.getTypeFromName("D").get())
    }

    @Test
    fun shouldConvertToAndFromOpaqueType() {
        parse(
            """
            type Meters as f64
            type Id as i64
            type Name as string
            type Flag as bool
            val m := Meters(3.0)
            val n := Name("x")
            val b := Flag(true)
            call println(f64(m))
            call println(i64(Id(3)))
            call println(i64(Id(3i32)))
            call println(string(n))
            call println(bool(b))
            """.trimIndent()
        )
    }

    @Test
    fun shouldConvertOpaqueScalarToString() {
        val program = parse(
            """
            type A as i32
            type B as i64
            type C as f32
            type D as f64
            type E as bool
            call println(string(A(1i32)))
            call println(string(B(1)))
            call println(string(C(1.0f32)))
            call println(string(D(1.0)))
            call println(string(E(true)))
            """.trimIndent()
        )
        val opaques = listOf(
            Opaque("A", I32.INSTANCE),
            Opaque("B", I64.INSTANCE),
            Opaque("C", F32.INSTANCE),
            Opaque("D", F64.INSTANCE),
            Opaque("E", Bool.INSTANCE)
        )
        opaques.forEachIndexed { index, opaque ->
            val function = calledFunction(program.statements[5 + index])
            assertEquals(OpaqueStringFunction(opaque), function)
            assertEquals(Str.INSTANCE, function.returnType)
        }
    }

    @Test
    fun shouldConvertOpaqueStringToStringWithUnwrapConversion() {
        val program = parse(
            """
            type Name as string
            call println(string(Name("x")))
            """.trimIndent()
        )
        val name = Opaque("Name", Str.INSTANCE)
        assertEquals(OpaqueConversionFunction("string", name, Str.INSTANCE), calledFunction(program.statements[1]))
    }

    @Test
    fun shouldNotPrintOpaqueValue() {
        parseAndExpectError(
            "type Meters as f64\ncall println(Meters(1.0))",
            "found no match for function call: println(Meters)"
        )
    }

    @Test
    fun shouldNotDefineStringFunctionThatClashesWithGeneratedOne() {
        parseAndExpectError(
            "type Meters as f64\nfun string(m as Meters) -> string := \"m\"",
            "function 'string(Meters) -> string' has already been defined"
        )
    }

    @Test
    fun shouldInferValTypeFromConversion() {
        val program = parse(
            """
            type Meters as f64
            val m := Meters(3.0)
            call println(f64(m))
            """.trimIndent()
        )
        val statement = program.statements[1] as ValDeclarationStatement
        assertEquals(Opaque("Meters", F64.INSTANCE), statement.declaration().type())
    }

    @Test
    fun shouldParseTypedVal() {
        val program = parse(
            """
            type Meters as f64
            val m as Meters := Meters(3.0)
            call println(f64(m))
            """.trimIndent()
        )
        val statement = program.statements[1] as ValDeclarationStatement
        assertEquals(Opaque("Meters", F64.INSTANCE), statement.declaration().type())
    }

    @Test
    fun shouldUseOpaqueTypeAsParameterAndReturnType() {
        parse(
            """
            type Meters as f64
            type Feet as f64
            fun ascend(from as Meters, by as Feet) -> Meters := Meters(f64(from) + f64(by) * 0.3048)
            call println(f64(ascend(Meters(3.0), Feet(4.0))))
            """.trimIndent()
        )
    }

    @Test
    fun shouldResolveOverloadsOnOpaqueAndUnderlyingType() {
        val program = parse(
            """
            type Meters as f64
            fun f(x as Meters) -> i64 := 1
            fun f(x as f64) -> i64 := 2
            call println(f(Meters(1.0)))
            call println(f(1.0))
            """.trimIndent()
        )
        val first = calledFunction(program.statements[3])
        val second = calledFunction(program.statements[4])
        assertEquals(listOf(Opaque("Meters", F64.INSTANCE)), first.argTypes)
        assertEquals(listOf(F64.INSTANCE), second.argTypes)
    }

    @Test
    fun shouldMangleOpaqueTypeNamedLikeBuiltInTypeDistinctly() {
        val program = parse(
            """
            type I64 as f64
            fun f(x as I64) -> i64 := 1
            fun f(x as i64) -> i64 := 2
            call println(f(I64(1.0)))
            """.trimIndent()
        )
        val first = calledFunction(program.statements[3]) as UserDefinedFunction
        val names = symbolTable.getFunctions("f").map { (it as UserDefinedFunction).mangledName() }
        assertEquals(2, names.toSet().size)
        assertEquals(listOf(Opaque("I64", F64.INSTANCE)), first.argTypes)
    }

    @Test
    fun shouldNotAcceptTransposedArguments() {
        parseAndExpectError(
            """
            type Meters as f64
            type Feet as f64
            fun ascend(from as Meters, by as Feet) -> Meters := from
            val base := Meters(3.0)
            val climb := Feet(4.0)
            call println(f64(ascend(climb, base)))
            """.trimIndent(),
            "found no match for function call: ascend(Feet, Meters)\npossible matches:\n  -> ascend(Meters, Feet)"
        )
    }

    @Test
    fun shouldNotAcceptUnderlyingTypeForOpaqueType() {
        parseAndExpectError(
            """
            type Meters as f64
            fun f(m as Meters) -> i64 := 1
            call println(f(3.0))
            """.trimIndent(),
            "found no match for function call: f(f64)"
        )
    }

    @Test
    fun shouldNotAcceptOpaqueTypeForUnderlyingType() {
        parseAndExpectError(
            """
            type Meters as f64
            call println(sqrt(Meters(3.0)))
            """.trimIndent(),
            "found no match for function call: sqrt(Meters)"
        )
    }

    @Test
    fun shouldNotConvertIntegerToOpaqueFloat() {
        parseAndExpectError(
            """
            type Meters as f64
            call println(f64(Meters(3)))
            """.trimIndent(),
            "found no match for function call: Meters(i64)"
        )
    }

    @Test
    fun shouldNotInitializeOpaqueValWithUnderlyingType() {
        parseAndExpectError(
            """
            type Meters as f64
            val m as Meters := 3.0
            """.trimIndent(),
            "you cannot initialize value 'm' of type Meters with an expression of type f64"
        )
    }

    @Test
    fun shouldNotRedefineType() {
        parseAndExpectError(
            """
            type Meters as f64
            type Meters as i64
            """.trimIndent(),
            "cannot redefine type: Meters"
        )
    }

    @Test
    fun shouldNotRedefineBuiltInType() {
        parseAndExpectError("type i64 as f64", "cannot redefine type: i64")
    }

    @Test
    fun shouldNotDefineTypeOverUndefinedType() {
        parseAndExpectOneError("type Meters as Foo", "undefined type: Foo")
    }

    @Test
    fun shouldNotDefineOpaqueTypeOverOpaqueType() {
        parseAndExpectError(
            """
            type Meters as f64
            type Feet as Meters
            """.trimIndent(),
            "cannot define type Feet as Meters: the underlying type must be i32, i64, f32, f64, bool, string or a function type"
        )
    }

    @Test
    fun shouldDefineOpaqueTypeOverFunctionTypeWithWrapConversionOnly() {
        parse("type Cmp as (i64, i64) -> i64")
        val underlying = Fun.from(listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE)
        val cmp = Opaque("Cmp", underlying)
        assertEquals(cmp, typeManager.getTypeFromName("Cmp").get())
        assertEquals(listOf(OpaqueConversionFunction("Cmp", underlying, cmp)), symbolTable.getFunctions("Cmp").toList())
        assertTrue(symbolTable.getFunctions("string").none { it.argTypes == listOf(cmp) })
    }

    @Test
    fun shouldNotDefineOpaqueTypeOverFunctionTypeWithUndefinedType() {
        parseAndExpectOneError("type F as (Foo) -> i64", "undefined type: Foo")
    }

    @Test
    fun shouldWrapFunctionInOpaqueFunctionType() {
        parse(
            """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun apply(c as Cmp, a as i64, b as i64) -> i64 := c(a, b)
            call println(apply(Cmp(larger), 1, 2))
            call println(apply(Cmp(fun(a as i64, b as i64) := a + b), 1, 2))
            """.trimIndent()
        )
    }

    @Test
    fun shouldNarrowOverloadedFunctionWhenWrapping() {
        val program = parse(
            """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun larger(a as f64, b as f64) -> f64 := if a > b then a else b
            val c := Cmp(larger)
            """.trimIndent()
        )
        val wrap = (program.statements[3] as ValDeclarationStatement).declaration().expression() as FunctionCallExpression
        val arg = wrap.args[0] as IdentifierDerefExpression
        assertEquals(Fun.from(listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE), arg.identifier.type())
    }

    @Test
    fun shouldNotWrapBuiltInFunction() {
        parseAndExpectOneError(
            "type Cmp as (i64, i64) -> i64\nval c := Cmp(max)",
            "cannot use 'max' as a function reference: only user-defined functions can be referenced, not built-in or library functions"
        )
    }

    @Test
    fun shouldCallOpaqueFunctionValue() {
        val program = parse(
            """
            type Cmp as (i64, i64) -> i64
            type Meters as f64
            type Scale as (Meters) -> Meters
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun double(m as Meters) -> Meters := m + m
            fun apply(s as Scale, m as Meters) -> Meters := s(m)
            val c := Cmp(larger)
            val x := c(1, 2)
            val y := apply(Scale(double), Meters(1.0))
            """.trimIndent()
        )
        assertEquals(Opaque("Cmp", Fun.from(listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE)), valType(program.statements[6]))
        assertEquals(I64.INSTANCE, valType(program.statements[7]))
        assertEquals(Opaque("Meters", F64.INSTANCE), valType(program.statements[8]))
    }

    @Test
    fun shouldNotAcceptBareFunctionForOpaqueFunctionType() {
        parseAndExpectOneError(
            """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun apply(c as Cmp, a as i64, b as i64) -> i64 := c(a, b)
            call println(apply(larger, 1, 2))
            """.trimIndent(),
            "found no match for function call: apply(function(i64, i64)->i64, i64, i64)\npossible matches:\n  -> apply(Cmp, i64, i64)"
        )
    }

    @Test
    fun shouldNotAcceptOpaqueFunctionTypeForUnderlyingType() {
        parseAndExpectOneError(
            """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun apply(f as (i64, i64) -> i64, a as i64, b as i64) -> i64 := f(a, b)
            call println(apply(Cmp(larger), 1, 2))
            """.trimIndent(),
            "found no match for function call: apply(Cmp, i64, i64)"
        )
        expectOneError(
            """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            val c := Cmp(larger)
            val f as (i64, i64) -> i64 := c
            """.trimIndent(),
            "you cannot initialize value 'f' of type function(i64, i64)->i64 with an expression of type Cmp"
        )
    }

    @Test
    fun shouldNotReturnBareFunctionAsOpaqueFunctionType() {
        parseAndExpectOneError(
            """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun pick() -> Cmp := larger
            """.trimIndent(),
            "you cannot return a value of type function(i64, i64)->i64 from function 'pick' with return type Cmp"
        )
    }

    @Test
    fun shouldBecomeFunctionReturningOpaqueFunctionType() {
        parse(
            """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun pick(n as i64) -> Cmp := if n <= 0 then Cmp(larger) else become pick(n - 1)
            """.trimIndent()
        )
    }

    @Test
    fun shouldNotBecomeFunctionReturningUnderlyingFunctionType() {
        parseAndExpectError(
            """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun bare() -> (i64, i64) -> i64 := larger
            fun pick() -> Cmp := become bare()
            """.trimIndent(),
            "tail call returns function(i64, i64)->i64 but function 'pick' returns Cmp"
        )
    }

    @Test
    fun shouldNotApplyOperatorsToOpaqueFunctionType() {
        val prefix = """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            val c := Cmp(larger)
            """.trimIndent()
        val cases = listOf(
            "call println(c == c)" to "cannot compare Cmp and Cmp",
            "call println(c != c)" to "cannot compare Cmp and Cmp",
            "call println(c < c)" to "cannot compare Cmp and Cmp",
            "call println(c == larger)" to "cannot compare Cmp and function(i64, i64)->i64",
            "call println(c + c)" to "cannot add Cmp and Cmp",
            "call println(c * c)" to "cannot multiply Cmp and Cmp",
            "call println(-c)" to "cannot negate Cmp",
        )
        for ((source, message) in cases) {
            expectOneError("$prefix\n$source", "$message: an opaque function type supports no operators")
        }
    }

    @Test
    fun shouldNotDefineConversionThatClashesWithExistingFunction() {
        parseAndExpectError("type sqrt as f64", "function 'sqrt(f64) -> sqrt' has already been defined")
    }

    @Test
    fun shouldNotDefineTypeInWhileBody() {
        parseAndExpectError(
            """
            while false do
                type Meters as f64
            end
            """.trimIndent(),
            "statement not allowed in while body: type Meters as f64"
        )
    }

    @Test
    fun shouldInheritOperatorsFromNumericType() {
        val program = parse(
            """
            type Meters as f64
            type Id as i32
            val a := Meters(1.0)
            val b := Meters(2.0)
            val sum := a + b
            val difference := a - b
            val next := Id(1i32) + Id(2i32)
            call println(a == b)
            call println(a != b)
            call println(a < b)
            call println(a <= b)
            call println(a > b)
            call println(a >= b)
            call println(Id(1i32) < Id(2i32))
            """.trimIndent()
        )
        assertEquals(Opaque("Meters", F64.INSTANCE), valType(program.statements[4]))
        assertEquals(Opaque("Meters", F64.INSTANCE), valType(program.statements[5]))
        assertEquals(Opaque("Id", I32.INSTANCE), valType(program.statements[6]))
    }

    @Test
    fun shouldInheritOperatorsFromStringType() {
        val program = parse(
            """
            type Name as string
            val n := Name("a") + Name("b")
            call println(Name("a") == Name("b"))
            call println(Name("a") != Name("b"))
            """.trimIndent()
        )
        assertEquals(Opaque("Name", Str.INSTANCE), valType(program.statements[1]))
    }

    @Test
    fun shouldInheritOperatorsFromBoolType() {
        parse(
            """
            type Flag as bool
            call println(Flag(true) == Flag(false))
            call println(Flag(true) != Flag(false))
            """.trimIndent()
        )
    }

    @Test
    fun shouldNotMixOpaqueTypeWithOtherType() {
        val hint = "both operands must be Meters, wrap with Meters(...) or unwrap with f64(...)"
        expectOneError(
            "type Meters as f64\ncall println(f64(Meters(1.0) + 1.0))",
            "cannot add Meters and f64: $hint"
        )
        expectOneError(
            "type Meters as f64\ncall println(f64(1.0 - Meters(1.0)))",
            "cannot subtract f64 and Meters: $hint"
        )
        expectOneError(
            "type Meters as f64\ncall println(Meters(1.0) == 1.0)",
            "cannot compare Meters and f64: $hint"
        )
        expectOneError(
            "type Meters as f64\ncall println(Meters(1.0) < 1)",
            "cannot compare Meters and i64: $hint"
        )
    }

    @Test
    fun shouldNotMixTwoOpaqueTypes() {
        expectOneError(
            "type Meters as f64\ntype Feet as f64\ncall println(f64(Meters(1.0) + Feet(1.0)))",
            "cannot add Meters and Feet: both operands must be the same opaque type"
        )
        expectOneError(
            "type Meters as f64\ntype Feet as f64\ncall println(Meters(1.0) != Feet(1.0))",
            "cannot compare Meters and Feet: both operands must be the same opaque type"
        )
    }

    @Test
    fun shouldNotInheritOperatorNotSupportedByUnderlyingType() {
        val unsupported = "does not support this operator"
        expectOneError(
            "type Name as string\ncall println(string(Name(\"a\") - Name(\"b\")))",
            "cannot subtract Name and Name: its underlying type string $unsupported"
        )
        expectOneError(
            "type Name as string\ncall println(Name(\"a\") < Name(\"b\"))",
            "cannot compare Name and Name: its underlying type string $unsupported"
        )
        expectOneError(
            "type Flag as bool\ncall println(bool(Flag(true) + Flag(false)))",
            "cannot add Flag and Flag: its underlying type bool $unsupported"
        )
        expectOneError(
            "type Flag as bool\ncall println(Flag(true) >= Flag(false))",
            "cannot compare Flag and Flag: its underlying type bool $unsupported"
        )
    }

    @Test
    fun shouldNotInheritOtherOperators() {
        val inherited = "an opaque type inherits only ==, !=, ordering, + and -"
        val cases = listOf(
            "type M as f64\ncall println(f64(M(1.0) * M(1.0)))" to "cannot multiply M and M",
            "type M as f64\ncall println(f64(M(1.0) * 2.0))" to "cannot multiply M and f64",
            "type M as f64\ncall println(f64(M(1.0) / M(1.0)))" to "cannot divide M and M",
            "type I as i64\ncall println(i64(I(1) div I(1)))" to "cannot divide I and I",
            "type I as i64\ncall println(i64(I(1) mod I(1)))" to "cannot mod I and I",
            "type I as i64\ncall println(i64(I(1) & I(1)))" to "cannot bitwise-and I and I",
            "type I as i64\ncall println(i64(I(1) | I(1)))" to "cannot bitwise-or I and I",
            "type I as i64\ncall println(i64(I(1) ^ I(1)))" to "cannot bitwise-xor I and I",
            "type B as bool\ncall println(B(true) and B(true))" to "cannot logical-and B and B",
            "type B as bool\ncall println(B(true) or B(true))" to "cannot logical-or B and B",
            "type B as bool\ncall println(B(true) xor B(true))" to "cannot logical-xor B and B",
            "type M as f64\ncall println(f64(-M(1.0)))" to "cannot negate M",
            "type I as i64\ncall println(i64(~I(1)))" to "cannot bitwise-not I",
            "type B as bool\ncall println(not B(true))" to "cannot logical-not B",
        )
        for ((source, message) in cases) {
            expectOneError(source, "$message: $inherited")
        }
    }

    @Test
    fun shouldReportOneErrorForRejectedOperatorInArgument() {
        // The rejected expression has no type, so the call it is passed to reports nothing more
        expectOneError(
            "type M as f64\nval m := M(1.0)\ncall println(m * m)",
            "cannot multiply M and M"
        )
        expectOneError(
            "type M as f64\nval m := M(1.0)\ncall println(-m)",
            "cannot negate M"
        )
    }

    @Test
    fun shouldStillReportOneErrorForStringSubtraction() {
        parseAndExpectOneError("call println(\"a\" - \"b\")", "cannot subtract string and string")
    }

    /** Parses [source] with a fresh parser, so that several cases in one test do not see each other's errors. */
    private fun expectOneError(source: String, message: String) =
        ColSemanticsParserOpaqueTypeTests().parseAndExpectOneError(source, message)

    private fun valType(statement: Statement) = (statement as ValDeclarationStatement).declaration().type()

    private fun calledFunction(statement: Statement) =
        ((statement as FunCallStatement).expression().args[0] as FunctionCallExpression).function()
}
