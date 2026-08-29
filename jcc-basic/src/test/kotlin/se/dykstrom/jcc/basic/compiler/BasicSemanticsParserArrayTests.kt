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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_3_14
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_2
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_11
import se.dykstrom.jcc.basic.ast.statement.OptionBaseStatement
import se.dykstrom.jcc.basic.ast.statement.PrintStatement
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_CINT_F64
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_VAL_STR
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.error.Warning.UNDEFINED_VARIABLE
import se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE
import se.dykstrom.jcc.llvm.code.LlvmBuiltIns.LF_ROUNDEVEN_F64
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str

/**
 * Tests class `BasicSemanticsParser`, especially functionality related to arrays.
 *
 * @author Johan Dykstrom
 * @see BasicSemanticsParser
 */
class BasicSemanticsParserArrayTests : AbstractBasicSemanticsParserTests() {

    @BeforeEach
    fun setUp() {
        // Define some functions for testing
        defineFunction(BF_CINT_F64)
        defineFunction(BF_VAL_STR)
    }

    @Test
    fun shouldParseSingleDimensionStaticDim() {
        parse("dim a(10) as integer")
        parse("dim bar(999) as string")
        parse("dim axe(9) as double")
    }

    @Test
    fun shouldParseDimWithTypeSpecifier() {
        parse("dim a%(10) as integer")
        parse("dim bar$(999) as string")
        parse("dim foo#(99) as double")
    }

    /**
     * Without an AS clause, the element type comes from the type specifier, from a DEFtype
     * statement, or from the default type, which is double.
     */
    @Test
    fun shouldParseDimWithoutType() {
        assertEquals(Arr.from(1, F64.INSTANCE), arrayDeclaration(parse("dim a(10)")).type())
        assertEquals(Arr.from(1, I64.INSTANCE), arrayDeclaration(parse("dim b%(10)")).type())
        assertEquals(Arr.from(1, Str.INSTANCE), arrayDeclaration(parse("dim c$(10)")).type())
        assertEquals(Arr.from(1, F64.INSTANCE), arrayDeclaration(parse("dim d#(10)")).type())
        assertEquals(Arr.from(2, F64.INSTANCE), arrayDeclaration(parse("dim e(1, 2)")).type())
    }

    @Test
    fun shouldParseDimWithoutTypeAfterDefType() {
        assertEquals(Arr.from(1, I64.INSTANCE), arrayDeclaration(parse("defint b-c : dim b(5)"), 1).type())
        assertEquals(Arr.from(1, Str.INSTANCE), arrayDeclaration(parse("defstr s : dim s(5)"), 1).type())
    }

    @Test
    fun shouldParseScalarDimWithoutType() {
        val program = parse("dim a, b%, c$, d#")

        val statement = program.statements[0] as VariableDeclarationStatement
        val types = statement.declarations.map { it.type() }
        assertEquals(listOf(F64.INSTANCE, I64.INSTANCE, Str.INSTANCE, F64.INSTANCE), types)
    }

    /**
     * An array that is used without having been declared is defined implicitly, with the
     * inclusive upper bound 10 in every dimension. The declaration is added to the start
     * of the program, so that the array is allocated before it is used.
     */
    @Test
    fun shouldDefineUndefinedArrayImplicitly() {
        val program = parse("a(3) = 7")

        val declaration = arrayDeclaration(program)
        assertEquals("a", declaration.name())
        assertEquals(Arr.from(1, F64.INSTANCE), declaration.type())
        assertEquals(listOf(IL_11), declaration.subscripts)
    }

    @Test
    fun shouldDefineUndefinedArrayImplicitlyWhenRead() {
        val program = parse("print a(3)")

        val declaration = arrayDeclaration(program)
        assertEquals("a", declaration.name())
        assertEquals(Arr.from(1, F64.INSTANCE), declaration.type())
        assertEquals(listOf(IL_11), declaration.subscripts)

        val printStatement = program.statements[1] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals(F64.INSTANCE, arrayAccessExpression.type())
    }

    @Test
    fun shouldDefineUndefinedMultiDimensionalArrayImplicitly() {
        val declaration = arrayDeclaration(parse("b%(2, 3) = 5"))
        assertEquals("b%", declaration.name())
        assertEquals(Arr.from(2, I64.INSTANCE), declaration.type())
        assertEquals(listOf(IL_11, IL_11), declaration.subscripts)
    }

    @Test
    fun shouldDefineUndefinedArrayImplicitlyWithTypeFromDefType() {
        val declaration = arrayDeclaration(parse("defint a-c : a(3) = 7"), 0)
        assertEquals(Arr.from(1, I64.INSTANCE), declaration.type())
    }

    @Test
    fun shouldDefineImplicitArrayOnlyOnce() {
        val program = parse("a(3) = 7 : print a(4) : a(5) = a(3)")

        val dimStatement = program.statements[0] as VariableDeclarationStatement
        assertEquals(1, dimStatement.declarations.size)
        assertEquals(1, errorListener.warnings.size)
    }

    /**
     * The implicit declarations of one program must not leak into the next one parsed by
     * the same parser instance.
     */
    @Test
    fun shouldNotDeclareImplicitArrayOfEarlierProgram() {
        parse("a(3) = 7")

        val program = parse("print 1")

        assertEquals(listOf(PrintStatement(0, 0, listOf(IL_1))), program.statements)
    }

    /**
     * The implicit declaration is added before any OPTION BASE, which source code may not do,
     * but only the semantics parser enforces that order, and it has already run.
     */
    @Test
    fun shouldAllowOptionBaseBeforeImplicitArray() {
        val program = parse("option base 1 : a(3) = 7")

        assertEquals(Arr.from(1, F64.INSTANCE), arrayDeclaration(program).type())
        assertEquals(OptionBaseStatement(0, 0, 1), program.statements[1])
    }

    @Test
    fun shouldWarnAboutUndefinedArray() {
        parseAndExpectWarning("a(3) = 7", "undefined array: a", UNDEFINED_VARIABLE)
        assertEquals(1, errorListener.warnings.size)
    }

    /**
     * An implicitly defined array is never reported as unused, just like an implicitly
     * defined scalar variable.
     */
    @Test
    fun shouldNotWarnAboutUnusedImplicitArray() {
        parseAndExpectWarning("a(3) = 7", "undefined array: a", UNDEFINED_VARIABLE)
        assertTrue(errorListener.warnings.none { it.warning == UNUSED_VARIABLE })
    }

    @Test
    fun shouldParseMultiDimensionStaticDim() {
        parse("dim two(3, 4) as string")
        parse("dim three(10, 10, 10) as integer")
        parse("dim ten(1, 2, 3, 4, 5, 6, 7, 8, 9, 10) as double")
    }

    @Test
    fun arraysAndScalarsShouldHaveDifferentNameSpaces() {
        parse("dim a(10) as integer : dim a as string")
        parse("dim foo(10) as integer, foo as double")
    }

    @Test
    fun shouldParseStaticDimWithConstant() {
        parse("CONST N = 23 : DIM a(N) AS INTEGER")
        parse("CONST MIN = 0, MAX = MIN + 50 : DIM b(MAX) AS STRING")
    }

    @Test
    fun shouldParseSingleDimensionArrayAccess() {
        val program = parse("dim a%(2) as integer : print a%(1)")

        val dimStatement = program.statements[0] as VariableDeclarationStatement
        val declaration = dimStatement.declarations[0] as ArrayDeclaration
        assertEquals("a%", declaration.name())
        assertEquals(Arr.from(1, I64.INSTANCE), declaration.type())
        assertEquals(listOf(AddExpression(0, 0, IL_2, IL_1)), declaration.subscripts)

        val printStatement = program.statements[1] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("a%", arrayAccessExpression.identifier.name())
        assertEquals(I64.INSTANCE, arrayAccessExpression.type())
        assertEquals(IL_1, arrayAccessExpression.subscripts[0])
    }

    @Test
    fun shouldParseSingleDimensionArrayAccessWithOptionBase0() {
        val program = parse("option base 0 : dim a%(2) as integer : print a%(1)")

        val optionBaseStatement = program.statements[0] as OptionBaseStatement
        assertEquals(0, optionBaseStatement.base())

        val dimStatement = program.statements[1] as VariableDeclarationStatement
        val declaration = dimStatement.declarations[0] as ArrayDeclaration
        assertEquals("a%", declaration.name())
        assertEquals(Arr.from(1, I64.INSTANCE), declaration.type())
        assertEquals(listOf(AddExpression(0, 0, IL_2, IL_1)), declaration.subscripts)

        val printStatement = program.statements[2] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("a%", arrayAccessExpression.identifier.name())
        assertEquals(I64.INSTANCE, arrayAccessExpression.type())
        assertEquals(IL_1, arrayAccessExpression.subscripts[0])
    }

    @Test
    fun shouldParseSingleDimensionArrayAccessWithOptionBase1() {
        val program = parse("option base 1 : dim a%(2) as integer : print a%(2)")

        val optionBaseStatement = program.statements[0] as OptionBaseStatement
        assertEquals(1, optionBaseStatement.base())

        val dimStatement = program.statements[1] as VariableDeclarationStatement
        val declaration = dimStatement.declarations[0] as ArrayDeclaration
        assertEquals("a%", declaration.name())
        assertEquals(Arr.from(1, I64.INSTANCE), declaration.type())
        assertEquals(listOf(AddExpression(0, 0, IL_2, IL_1)), declaration.subscripts)

        val printStatement = program.statements[2] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("a%", arrayAccessExpression.identifier.name())
        assertEquals(I64.INSTANCE, arrayAccessExpression.type())
        assertEquals(listOf(IL_2), arrayAccessExpression.subscripts)
    }

    @Test
    fun shouldParseStringArrayAccess() {
        val program = parse("dim foo(100) as string : print foo(0)")

        val printStatement = program.statements[1] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("foo", arrayAccessExpression.identifier.name())
        assertEquals(Str.INSTANCE, arrayAccessExpression.type())
        assertEquals(IL_0, arrayAccessExpression.subscripts[0])
    }

    @Test
    fun shouldParseArrayAccessWithFloatSubscripts() {
        val program = parse("dim foo(100) as string : print foo(3.14)")

        val printStatement = program.statements[1] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("foo", arrayAccessExpression.identifier.name())
        assertEquals(Str.INSTANCE, arrayAccessExpression.type())
        // The float subscript is made an explicit round-then-truncate to integer (issue #52)
        assertEquals(
            CastToIntExpression(0, 0, RoundExpression(FL_3_14, LF_ROUNDEVEN_F64), I64.INSTANCE),
            arrayAccessExpression.subscripts[0]
        )
    }

    @Test
    fun shouldParseMultiDimensionArrayAccess() {
        val program = parse("dim foo(1, 2) as string : print foo(0, 1)")

        val dimStatement = program.statements[0] as VariableDeclarationStatement
        val declaration = dimStatement.declarations[0] as ArrayDeclaration
        assertEquals("foo", declaration.name())
        assertEquals(Arr.from(2, Str.INSTANCE), declaration.type())
        assertEquals(listOf(
            AddExpression(0, 0, IL_1, IL_1),
            AddExpression(0, 0, IL_2, IL_1)
        ), declaration.subscripts)

        val printStatement = program.statements[1] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("foo", arrayAccessExpression.identifier.name())
        assertEquals(Str.INSTANCE, arrayAccessExpression.type())
        assertEquals(2, arrayAccessExpression.subscripts.size)
        assertEquals(listOf(IL_0, IL_1), arrayAccessExpression.subscripts)
    }

    @Test
    fun shouldParseMultiDimensionArrayAccessWithOptionBase1() {
        val program = parse("option base 1 : dim foo(1, 2) as string : print foo(2, 1)")

        val optionBaseStatement = program.statements[0] as OptionBaseStatement
        assertEquals(1, optionBaseStatement.base())

        val dimStatement = program.statements[1] as VariableDeclarationStatement
        val declaration = dimStatement.declarations[0] as ArrayDeclaration
        assertEquals("foo", declaration.name())
        assertEquals(Arr.from(2, Str.INSTANCE), declaration.type())
        assertEquals(listOf(
            AddExpression(0, 0, IL_1, IL_1),
            AddExpression(0, 0, IL_2, IL_1)
        ), declaration.subscripts)

        val printStatement = program.statements[2] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("foo", arrayAccessExpression.identifier.name())
        assertEquals(Str.INSTANCE, arrayAccessExpression.type())
        assertEquals(2, arrayAccessExpression.subscripts.size)
        assertEquals(listOf(IL_2, IL_1), arrayAccessExpression.subscripts)
    }

    @Test
    fun shouldParseArrayAccessWithExpressionSubscripts() {
        val program = parse("dim index as integer : dim array(10, 10) as double : print array(index - 3, 0 * 8 + 1)")
        val printStatement = program.statements[2] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("array", arrayAccessExpression.identifier.name())
        assertEquals(F64.INSTANCE, arrayAccessExpression.type())
        assertEquals(2, arrayAccessExpression.subscripts.size)
        assertEquals(I64.INSTANCE, typeManager.getType(arrayAccessExpression.subscripts[0]))
        assertEquals(I64.INSTANCE, typeManager.getType(arrayAccessExpression.subscripts[1]))
    }

    @Test
    fun arraySubscriptsCanBeFunctionCalls() {
        val program = parse(
            """
            dim a%(10, 5) as integer
            print a%(cint(1.7 + 1.3), cint(val("2")))
            """
        )
        val printStatement = program.statements[1] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("a%", arrayAccessExpression.identifier.name())
    }

    @Test
    fun shouldParseNestedArrayAccess() {
        val program = parse("dim index as integer : dim values(10) as integer : print values(values(index))")

        val printStatement = program.statements[2] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("values", arrayAccessExpression.identifier.name())
        assertEquals(I64.INSTANCE, arrayAccessExpression.type())
        assertEquals(1, arrayAccessExpression.subscripts.size)

        val nestedExpression = arrayAccessExpression.subscripts[0] as ArrayAccessExpression
        assertEquals("values", nestedExpression.identifier.name())
        assertEquals(I64.INSTANCE, nestedExpression.type())
        assertEquals(1, nestedExpression.subscripts.size)
        assertTrue(nestedExpression.subscripts[0] is IdentifierDerefExpression)
    }

    @Test
    fun shouldParseNestedArrayAccessWithOptionBase1() {
        val program = parse(
            """
            option base 1
            dim index as integer
            dim values(10) as integer
            print values(values(index))
            """
        )

        val printStatement = program.statements[3] as PrintStatement
        val outerArrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("values", outerArrayAccessExpression.identifier.name())
        assertEquals(I64.INSTANCE, outerArrayAccessExpression.type())
        assertEquals(1, outerArrayAccessExpression.subscripts.size)

        val innerArrayAccessExpression = outerArrayAccessExpression.subscripts[0] as ArrayAccessExpression
        assertEquals("values", innerArrayAccessExpression.identifier.name())
        assertEquals(I64.INSTANCE, innerArrayAccessExpression.type())
        assertEquals(1, innerArrayAccessExpression.subscripts.size)
        val ide = innerArrayAccessExpression.subscripts[0] as IdentifierDerefExpression
        assertEquals("index", ide.identifier.name())
    }

    @Test
    fun shouldParseOptionBase0() {
        val program = parse("option base 0")
        val statement = OptionBaseStatement(0, 0, 0)
        assertEquals(statement, program.statements[0])
    }

    @Test
    fun shouldParseOptionBase1() {
        val program = parse("option base 1")
        val statement = OptionBaseStatement(0, 0, 1)
        assertEquals(statement, program.statements[0])
    }

    @Test
    fun shouldNotParseInvalidOptionBase() {
        parseAndExpectException("option base 2", "invalid option base")
    }

    @Test
    fun shouldNotParseDuplicateOptionBase() {
        parseAndExpectException("option base 1 : option base 1", "option base already set")
    }

    @Test
    fun shouldNotParseOptionBaseAfterArrayDeclaration() {
        parseAndExpectException("dim a(10) as integer : option base 1", "option base not allowed after")
    }

    @Test
    fun shouldNotParseDimWithInvalidType() {
        parseAndExpectException("dim a$(1) as integer", "type specifier string and type integer")
        parseAndExpectException("dim a%(1) as double", "type specifier integer and type double")
        parseAndExpectException("dim a#(1) as string", "type specifier double and type string")
    }

    @Test
    fun shouldNotParseRedefinitionOfArray() {
        parseAndExpectException("dim a(1) as integer : dim a(1) as integer", "variable 'a' is already defined")
    }

    @Test
    fun arrayDeclarationMustUseIntegerSubscripts() {
        parseAndExpectException("dim a(1.7) as integer", "array 'a' has non-integer")
        parseAndExpectException("dim c(\"foo\") as integer", "array 'c' has non-integer")
        parseAndExpectException("dim d(1 / 7) as integer", "array 'd' has non-integer")
    }

    @Test
    fun arrayAccessMustHaveSameDimensionsAsArrayDefinition() {
        parseAndExpectException("dim a(1) as integer : print a(1, 7)", "array 'a' has 1 dimension, not 2")
        parseAndExpectException("dim b(1, 2) as integer : print b(8)", "array 'b' has 2 dimensions, not 1")
        parseAndExpectException("dim a(1) as integer : a(1, 7) = 0", "array 'a' has 1 dimension, not 2")
        parseAndExpectException("dim b(1, 2) as integer : b(8) = 0", "array 'b' has 2 dimensions, not 1")
    }

    @Test
    fun arrayAccessMustUseNumericSubscripts() {
        parseAndExpectException("dim a(1) as integer : print a(\"0\")", "array 'a' has non-integer subscript")
        parseAndExpectException("dim b(1, 2) as integer : print b(\"5\")", "array 'b' has non-integer subscript")
        parseAndExpectException("dim a(1) as integer : a(\"0\") = 1", "array 'a' has non-integer subscript")
        parseAndExpectException("b$(\"0\") = \"x\"", "array 'b$' has non-integer subscript")
    }

    /**
     * A floating point subscript is legal, on both sides of an assignment. It is rounded
     * (half-to-even) to an integer, as in QuickBASIC.
     *
     * @see shouldParseArrayAccessWithFloatSubscripts
     */
    @Test
    fun shouldRoundFloatSubscriptInAssignmentTarget() {
        val program = parse("dim a%(10) as integer : a%(1.7) = 5")

        val assignStatement = program.statements[1] as AssignStatement
        val arrayAccessExpression = assignStatement.lhsExpression as ArrayAccessExpression
        assertEquals(
            CastToIntExpression(0, 0, RoundExpression(FloatLiteral(0, 0, "1.7"), LF_ROUNDEVEN_F64), I64.INSTANCE),
            arrayAccessExpression.subscripts[0]
        )
    }

    /**
     * $DYNAMIC arrays are not implemented yet.
     */
    @Test
    fun shouldNotAcceptDynamicArrays() {
        parseAndExpectException("dim a as integer : dim foo(a) as integer", $$"$DYNAMIC arrays not supported yet")
        parseAndExpectException("dim b as integer : dim foo(7, 1 + b) as integer", $$"$DYNAMIC arrays not supported yet")
    }

    /**
     * Returns the first array declaration of the variable declaration statement at [index].
     */
    private fun arrayDeclaration(program: AstProgram, index: Int = 0): ArrayDeclaration =
        (program.statements[index] as VariableDeclarationStatement).declarations[0] as ArrayDeclaration
}
