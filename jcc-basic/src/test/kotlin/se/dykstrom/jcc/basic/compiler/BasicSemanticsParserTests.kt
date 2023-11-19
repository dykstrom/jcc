/*
 * Copyright (C) 2016 Johan Dykstrom
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

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FL_2_0
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FL_3_14
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_FMOD
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.LabelledStatement
import se.dykstrom.jcc.common.error.InvalidValueException
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str
import se.dykstrom.jcc.common.types.Type
import se.dykstrom.jcc.common.utils.FormatUtils.EOL
import kotlin.test.assertTrue
import kotlin.test.fail

class BasicSemanticsParserTests : AbstractBasicSemanticsParserTests() {

    @Before
    fun setUp() {
        // Function fmod is used for modulo operations on floats
        defineFunction(FUN_FMOD)
        defineFunction(FUN_SUM1)
    }

    @Test
    fun shouldPrintNothing() {
        parse("10 print")
    }

    @Test
    fun shouldPrintMaxI64() {
        parse("10 print 9223372036854775807")
        parse("20 print &H7FFFFFFFFFFFFFFF")
    }

    @Test
    fun shouldPrintMinI64() {
        parse("10 print -9223372036854775808")
    }

    @Test
    fun shouldPrintMaxF64() {
        parse("10 print 1.7976931348623157E308")
    }

    @Test
    fun shouldPrintMinF64() {
        parse("10 print 2.2250738585072014E-308")
    }

    @Test
    fun shouldPrintOneString() {
        parse("10 print \"One\"")
        parse("20 print \"\"")
    }

    @Test
    fun shouldPrintTwoStrings() {
        parse("10 print \"One\",\"Two\"")
        parse("20 print \"\",\"\"")
    }

    @Test
    fun shouldPrintStringAddition() {
        parse("print \"A\" + \"B\"")
        parse("print \"\" + \"\"")
        parse("print \"1234567890\" + \"abcdefghijk\"")
    }

    @Test
    fun shouldPrintOneIntegerExpression() {
        parse("10 print 5 + 6")
        parse("15 print &H5 + &O6")
        parse("20 print 1 - 3")
        parse("25 print &B10 - &B01")
        parse("30 print 4 * 5")
        parse("40 print 100 / 10")
        parse("50 print 100 \\ 10")
        parse("60 print 100 MOD 10")
    }

    @Test
    fun shouldPrintOneFloatExpression() {
        parse("print 3.14")
        parse("print .7D+10")
        parse("print 1.2 + 2.1#")
        parse("print 7. - 8.8000")
        parse("print 10E+10 / 12.34")
        parse("print 0.33# * 3.0")
        parse("print 5.3 MOD 4.0")
    }

    @Test
    fun shouldPrintFloatIntegerExpression() {
        parse("print 1.2 + 2")
        parse("print 1 + 2#")
        parse("print 7 - 8.8000")
        parse("print 10E+10 / 12")
        parse("print 0.33 * 3")
        parse("print 5.3 MOD 4")
        parse("print 4 MOD 5.3")
    }

    @Test
    fun shouldPrintRelationalIntegerExpression() {
        parse("10 print 5 = 6")
        parse("20 print 1 <> 3")
        parse("30 print 4 > 5")
        parse("40 print 100 >= 10")
        parse("50 print 100 < 100")
        parse("60 print 100 <= 10")
    }

    @Test
    fun shouldPrintRelationalFloatExpression() {
        parse("print 3.4 = 6.7")
        parse("print 1.0E10 <> -0.1D10")
        parse("print 0.1 > 100000.99")
        parse("print -.4 >= .7")
        parse("print 1e+33 < -6.7d-100")
        parse("print 3.4 <= 6.7")
    }

    @Test
    fun shouldPrintRelationalFloatIntegerExpression() {
        parse("print 3.4 = 6")
        parse("print .1 <> -4711")
    }

    @Test
    fun shouldPrintRelationalStringExpression() {
        parse("10 print \"foo\" = \"bar\"")
        parse("20 print \"one\" <> \"two\"")
        parse("30 print \"\" < \"three\"")
        parse("40 print \"50\" <= \"70\"")
        parse("50 print \"foo\" > \"bar\"")
        parse("60 print \"one\" >= \"two\"")
    }

    @Test
    fun testPrintWithOneConditionalExpression() {
        parse("10 print 5 AND 5")
        parse("20 print 1 and 0")
        parse("30 print 4 AND 0")
        parse("40 print 10 OR 100")
        parse("50 print 1 or 0")
        parse("60 print 1 xor 0")
        parse("70 print NOT 1 AND NOT 1")
        parse("80 dim foo as integer : let foo = 1 : print foo xor not foo")
    }

    @Test
    fun testPrintWithTwoIntegerExpressions() {
        parse("10 print 5 + 6 + 7")
        parse("20 print 1 - 3 + 3")
        parse("30 print 4 * 5 + 8")
        parse("40 print 1 - 100 / 10")
        parse("50 print 1 - 100 MOD 10")
        parse("60 print 8 * 9 \\ 4")
    }

    @Test
    fun testPrintWithMultipleConditionalExpressions() {
        parse("10 print 5 AND 5 AND 6 AND 6")
        parse("20 print 1 and 0 or 0 and 1")
        parse("30 print 0 and (7 or 8)")
        parse("40 print 0 and (0 or 1)")
        parse("50 print 1 or 1 xor 0 or 0")
        parse("60 print not 0 or 1 xor not 0 or 0")
    }

    @Test
    fun testPrintWithComplexIntegerExpressions() {
        parse("10 print (1 - 100) / (10 + 2)")
        parse("20 print 3 * (100 / 2) + (10 - 2) * (0 + 1 + 2)")
        parse("30 print (1 - 100) \\ (10 + 2)")
        parse("40 print -(&B1 - &O100) * -(10 + &H02)")
    }

    @Test
    fun testPrintWithMixedExpressions() {
        parse("10 print (1 - 100); -3.14; \"foo\"")
        parse("20 print 2 - 1 > 3 - 4")
        parse("30 print \"\"; 2 - 1; 5 AND 6; &HFFFF")
        parse("40 print 2.0 * 1 = .3 - 4.01e+10")
        parse("50 print 1 + 2 + 3 + 4 AND (1 \\ 1)")
    }

    @Test
    fun shouldPrintAndGosub() {
        parse("10 print \"One\"" + EOL + "20 gosub 10")
    }

    @Test
    fun shouldPrintAndGoto() {
        parse("10 print \"One\"" + EOL + "20 goto 10")
    }

    @Test
    fun shouldParseMultiplePrintAndGotos() {
        parse("10 goto 40" + EOL
                + "20 print \"A\"" + EOL
                + "30 goto 60" + EOL
                + "40 print \"B\"" + EOL
                + "50 goto 20" + EOL
                + "60 print \"C\"")
    }

    @Test
    fun shouldParseVariableDefinedInSubroutine() {
        parse("10 gosub 100 "
                + "20 print x "
                + "30 end "
                + "100 let x = 1 "
                + "110 return"
        )
    }

    @Test
    fun shouldGotoLabel() {
        parse("line10: goto loop "
                + "loop: goto foo.bar "
                + "foo.bar: goto line10")
    }

    @Test
    fun shouldGosubLabel() {
        parse("line10: gosub line20 line20: gosub line10")
    }

    @Test
    fun shouldParseOnGosub() {
        parse("10 on 1 gosub 10")
    }

    @Test
    fun shouldParseOnGosubMultipleLabels() {
        parse("10 let a% = 1 " + "20 on a% gosub 10, 20")
    }

    @Test
    fun shouldParseOnGoto() {
        parse("10 on 1 goto 10")
    }

    @Test
    fun shouldParseOnGotoMultipleLabels() {
        parse("10 let a% = 1 " + "20 on a% goto 10, 20")
    }

    @Test
    fun shouldParseOnGotoMixedLabels() {
        parse("10 let a% = 1 "
                + "loop: on a% goto 10, loop "
                + "last.line: on a% goto loop, last.line")
    }

    @Test
    fun shouldDefineDblVariable() {
        parse("defdbl d : d = 4.5")
        parse("dim x as double : x = 0.0")
        parse("p# = 1.23")
        parse("s = 0.01")
    }

    @Test
    fun shouldDefineIntVariable() {
        parse("defint i : i = 4")
        parse("dim x as integer : x = 0")
        parse("j% = 1")
        parse("s = 0")
    }

    @Test
    fun shouldDefineStrVariable() {
        parse("defstr h : h = \"string\"")
        parse("dim x as string : x = \"string\"")
        parse("j$ = \"string\"")
    }

    @Test
    fun shouldDimVariables() {
        parse("dim foo as integer, boo as double, moo as string, zoo as string : foo = 0 : boo = 0.0 : moo = zoo")
    }

    @Test
    fun shouldDimVariablesWithTypeSpecifier() {
        parse("dim foo% as integer, boo# as double, moo$ as string, zoo$ as string : foo% = 0 : boo# = 0.0 : moo$ = zoo$")
    }

    @Test
    fun shouldRespectTypePrecedence() {
        parse("defint a-c "                    // Define variables starting with a-c to be integers
                + "dim amount as double "          // Define variable amount to be a float
                + "let amount = 1.1 "
                + "let account = 17 "
                + "let a$ = \"string\" "           // Variables with $ suffix should still be strings
                + "let b% = 0 "                    // Variables with % suffix should still be integers
                + "let c# = 1.2")                  // Variables with # suffix should still be floats
        parse("defstr f, g "
                + "let go = \"go\" "
                + "let f% = 4711 "
                + "let g# = 3.14")
    }

    @Test
    fun shouldDifferBetweenVariablesWithTypeSpecifiers() {
        parse("""
            let i% = 5
            let i# = 8.9
            let i$ = "foo"
            print i% ; i# ; i$
            """)
    }

    @Test
    fun shouldAssignIntegerToFloatVariable() {
        parse("let a# = 17")
        parse("dim b as double : let b = 17")
    }

    @Test
    fun shouldAssignFloatToIntegerVariable() {
        parse("let a% = 17.3")
        parse("dim b as integer : let b = -0.1")
    }

    @Test
    fun shouldAssignToUntypedVariable() {
        parse("let a = 17.3")
        parse("let b = 89")
    }

    @Test
    fun shouldAssignFromUntypedVariable() {
        parse("let a# = foo")
        parse("let b% = bar")
    }

    @Test
    fun testAssignment() {
        parse("10 let a = 5")
        parse("20 b = 5")
        parse("30 let a% = 5")
        parse("40 b% = 5")
        parse("50 let a$ = \"B\"")
        parse("60 b$ = \"B\"")
        parse("70 f = 3.14")
        parse("80 f# = 3.14")
    }

    @Test
    fun testReAssignment() {
        parse("10 let a% = 5" + EOL + "20 let a% = 7")
        parse("30 let s$ = \"A\"" + EOL + "40 let s$ = \"B\"")
        parse("50 let foo = 5" + EOL + "60 let foo = 7")
        parse("90 let float# = 1.0" + EOL + "100 let float# = 2.0")
    }

    @Test
    fun shouldParseAssignmentOfModWithFloats() {
        val program = parse("10 let f = 3.14 MOD 2.0")
        val statements = program.statements
        assertEquals(1, statements.size)
        val labelledStatement = statements[0] as LabelledStatement
        val assignStatement = labelledStatement.statement() as AssignStatement
        val lhsExpression = assignStatement.lhsExpression as IdentifierNameExpression
        assertEquals(INE_F64_F, lhsExpression)
        val rhsExpression = assignStatement.rhsExpression as FunctionCallExpression
        assertEquals(FUN_FMOD.identifier, rhsExpression.identifier)
        assertEquals(2, rhsExpression.args.size)
        assertEquals(FL_3_14, rhsExpression.args[0])
        assertEquals(FL_2_0, rhsExpression.args[1])
    }

    @Test
    fun testAssignmentWithExpression() {
        parse("10 let a = 5 + 2")
        parse("20 let a% = 10 * 10")
        parse("30 let float = 10 / (10 - 5)")
        parse("35 let float# = 10 / (10 - 5)")
    }

    @Test
    fun shouldParseSingleConstAssignment() {
        parse("CONST foo = 1")
        parse("CONST bar = 1 + 99 \\ 23 * 1")
        parse("CONST tee = 3.14 / 1.0")
        parse("CONST moo = \"hello\" + \"world\"")
    }

    @Test
    fun shouldParseSingleConstAssignmentWithTypeSpecifier() {
        parse("CONST foo% = 1")
        parse("CONST bar% = 1 + 99 \\ 23 * 1")
        parse("CONST tee# = 3.14 / 1.0")
        parse("CONST moo$ = \"hello\" + \"world\"")
    }

    @Test
    fun shouldParseMultiConstAssignment() {
        parse("CONST foo = 1, bar = 9.99 + 99.9, tee = 1 AND 0 OR 1 XOR 0, moo = \"moo\"")
        parse("CONST foo% = 1, bar# = 9.99 + 99.9, tee% = 1 AND 0 OR 1 XOR 0, moo$ = \"moo\"")
    }

    @Test
    fun shouldParseConstAssignmentFromIntegerConstant() {
        // When
        parse("CONST foo = 1, bar = foo + 1")

        // Then
        assertConstant("foo", I64.INSTANCE, "1")
        assertConstant("bar", I64.INSTANCE, "2")
    }

    @Test
    fun shouldParseConstAssignmentFromFloatConstant() {
        // When
        parse("CONST foo = 1.0, bar = foo + 1")

        // Then
        assertConstant("foo", F64.INSTANCE, "1.0")
        assertConstant("bar", F64.INSTANCE, "2.0")
    }

    @Test
    fun shouldParseConstAssignmentFromStringConstant() {
        // When
        parse("CONST foo = \"foo\", bar = foo + \"bar\"")

        // Then
        assertConstant("foo", Str.INSTANCE, "foo")
        assertConstant("bar", Str.INSTANCE, "foobar")
    }

    private fun assertConstant(name: String, type: Type, value: String) {
        assertTrue(symbolTable.contains(name))
        assertTrue(symbolTable.isConstant(name))
        assertEquals(type, symbolTable.getIdentifier(name).type())
        assertEquals(value, symbolTable.getValue(name))
    }

    @Test
    fun testDereference() {
        parse("10 let a = 5" + EOL + "20 print a")
        parse("30 let a% = 17" + EOL + "40 print -a% + 1")
        parse("50 let s$ = \"foo\"" + EOL + "60 print s$")
        parse("90 let float = 1.2 / 7.8" + EOL + "100 print float")
        // CONST dereference
        parse("110 const foo = 9" + EOL + "120 print foo")
        parse("130 const bar = \"hello\" + \"world\"" + EOL + "140 print bar")
    }

    @Test
    fun testDereferenceInExpression() {
        parse("10 let a = 5" + EOL + "20 let b = a * a")
        parse("30 let a% = 17" + EOL + "40 print a% + 1; a% / a%; a% \\ a%")
        parse("50 let s$ = \"foo\"" + EOL + "60 print s$; s$; s$; s$ = s$; s$ <> \"bar\"")
        parse("70 a = 23 : a = a + 1")
        parse("100 f = 1.7 : float = f + f")
        parse("110 f# = 1.7# : float# = f# + f#")
    }

    @Test
    fun shouldParseTwoDereferences() {
        parse("10 a = 1 : b = 2" + EOL + "20 c = a + b : d = a + b + c" + EOL + "30 print d")
    }

    @Test
    fun shouldParseDereferenceOfUndefined() {
        parse("print x")
        parse("print y$")
        parse("let a = b")
        parse("let a# = b") // The default type of an undefined identifier is F64
        parse("defstr z : let b$ = zoo") // Identifier 'zoo' is defined to have type string
        parse("dim cool as double : let b# = cool") // Identifier 'cool' is defined to have type F64
    }

    @Test
    fun shouldRandomizeWithoutExpression() {
        parse("randomize")
    }

    @Test
    fun shouldRandomizeWithExpression() {
        parse("randomize 1")
        parse("randomize a%")
        parse("randomize f# * 3.14 - a%")
        parse("randomize \"Hello!\"")
    }

    @Test
    fun shouldSwapIntegers() {
        parse("swap a%, b%")
        parse("swap a%, u")
        parse("swap u, b%")
    }

    @Test
    fun shouldSwapFloats() {
        parse("swap a#, b#")
        parse("swap a#, u")
        parse("swap u, b#")
    }

    @Test
    fun shouldSwapStrings() {
        parse("swap a$, b$")
        parse("dim u as string : swap u, b$")
    }

    @Test
    fun shouldSwapIntegerAndFloat() {
        parse("swap a#, b%")
        parse("swap b%, a#")
    }

    @Test
    fun shouldParseIfThenElse() {
        parse("10 if 5 > 0 then 10 else 20" + EOL + "20 end")
        parse("30 if 4711 then print 4711")
        parse("50 if 0 then print \"false\" : goto 60 else print \"true\" : goto 60" + EOL + "60 end")
    }

    @Test
    fun shouldParseIfElseAndGoto() {
        parse("""
            10 if 5 > 0 then
            15   goto 20
            20 else
            25   goto 30
            30 end if
            """)
    }

    @Test
    fun shouldParseIfWithElseIfAndGoto() {
        parse("""
            10 if 5 > 0 then
            15   goto 20
            20 elseif 0 > 5 then
            25   goto 40
            30 else
            35   goto 30
            40 end if
            """)
    }

    @Test
    fun shouldParseWhile() {
        parse("10 while -1" + EOL + "20 wend")
        parse("30 while 0 = 0" + EOL + "40 print 0" + EOL + "50 wend")
        parse("while 1 > 0" + EOL + "a% = 0" + EOL + "print a%" + EOL + "wend")
    }

    @Test
    fun shouldParseWhileGotoWend() {
        parse("10 while 0 15 goto 20 20 wend")
    }

    @Test
    fun shouldNotParseWhileWithStringExpression() {
        parseAndExpectException("while \"foo\" wend", "expression of type string")
    }

    @Test
    fun shouldNotParseWhileWithSemanticsError() {
        parseAndExpectException("10 while 5 > 0 " +
                "20   print 17 + \"17\" " +
                "30 wend", "illegal expression")
    }

    @Test
    fun shouldNotParseWhileWithDuplicateLineNumbers() {
        parseAndExpectException("10 while 5 > 0 " +
                "10   print 17 " +
                "30 wend", "duplicate line")
    }

    @Test
    fun shouldNotParseIfWithStringExpression() {
        parseAndExpectException("10 if \"foo\" then 10", "expression of type string")
    }

    @Test
    fun shouldNotParseIfThenWithSemanticsError() {
        parseAndExpectException("10 if 5 > 0 then " +
                "20   print 17 + \"17\" " +
                "30 end if", "illegal expression")
    }

    @Test
    fun shouldNotParseIfThenWithDuplicateLineNumbers() {
        parseAndExpectException("""
            10 if 5 > 0 then
            10   print 17
            30 end if
            """,
            "duplicate line")
    }

    @Test
    fun testAssignmentWithInvalidExpression() {
        parseAndExpectException("10 let a = \"A\" + 7", "illegal expression")
        parseAndExpectException("20 let a = \"A\" + 0.1", "illegal expression")
        parseAndExpectException("30 let a = &HFF + \"A\"", "illegal expression")
        parseAndExpectException("50 let b = 3.14 and 5 + 2", "expected subexpressions of type integer")
        parseAndExpectException("60 let b = \"A\" OR 5 > 2", "expected subexpressions of type integer")
        parseAndExpectException("70 print -\"A\"", "expected numeric subexpression")
        parseAndExpectException("100 let b = NOT \"\"", "expected subexpression of type integer")
        parseAndExpectException("110 let b = NOT 90.1", "expected subexpression of type integer")
    }

    @Test
    fun testAssignmentWithTypeError() {
        parseAndExpectException("10 let a% = \"A\"", "you cannot assign a value of type string")
        parseAndExpectException("20 let f# = \"A\"", "you cannot assign a value of type string")
        parseAndExpectException("30 let b$ = 0", "you cannot assign a value of type integer")
        parseAndExpectException("40 c$ = 7 * 13", "you cannot assign a value of type integer")
        parseAndExpectException("50 let b$ = &O123", "you cannot assign a value of type integer")
        parseAndExpectException("60 let b$ = 1.", "you cannot assign a value of type double")
    }

    @Test
    fun testReAssignmentWithDifferentType() {
        parseAndExpectException("10 let a% = 5" + EOL + "20 let a% = \"foo\"", "a value of type string")
        parseAndExpectException("70 let f$ = \"foo\"" + EOL + "80 let f$ = 0 <> 1", "a value of type integer")
    }

    @Test
    fun testDuplicateLineNumber() {
        parseAndExpectException("10 goto 10" + EOL + "10 print", "duplicate line")
    }

    @Test
    fun testDuplicateLineLabel() {
        parseAndExpectException("foo: goto foo" + EOL + "foo: print", "duplicate line")
    }

    @Test
    fun shouldNotParseUndefinedGosubLine() {
        parseAndExpectException("10 gosub 20", "undefined line number: 20")
    }

    @Test
    fun shouldNotParseUndefinedGotoLine() {
        parseAndExpectException("10 goto 20", "undefined line number: 20")
    }

    @Test
    fun shouldNotParseUndefinedGotoLabel() {
        parseAndExpectException("10 goto foo", "undefined line number: foo")
    }

    @Test
    fun shouldNotParseOnGosubInvalidExpression() {
        parseAndExpectException("10 on \"foo\" gosub 10", "expression of type string")
        parseAndExpectException("20 on 1.0 gosub 20", "expression of type double")
    }

    @Test
    fun shouldNotParseOnGotoInvalidExpression() {
        parseAndExpectException("10 on \"foo\" goto 10", "expression of type string")
        parseAndExpectException("20 on 1.0 goto 20", "expression of type double")
    }

    @Test
    fun shouldNotParseOnGosubUnknownLabel() {
        parseAndExpectException("10 on 5 gosub 100", "undefined line number/label: 100")
        parseAndExpectException("20 a% = 1 30 on a% gosub 20, 30, 40", "undefined line number/label: 40")
    }

    @Test
    fun shouldNotParseOnGotoUnknownLabel() {
        parseAndExpectException("10 on 5 goto 100", "undefined line number/label: 100")
        parseAndExpectException("20 a% = 1 30 on a% goto 20, 30, 40", "undefined line number/label: 40")
    }

    /**
     * Invalid integer -> overflow.
     */
    @Test
    fun testOverflowI64() {
        val value = "9223372036854775808"
        try {
            parse("10 print $value")
            fail("Expected SemanticsException")
        } catch (se: SemanticsException) {
            val ive = errorListener.errors[0].exception as InvalidValueException
            assertEquals(value, ive.value())
        }
    }

    /**
     * Invalid integer -> underflow.
     */
    @Test
    fun testUnderflowI64() {
        val value = "-9223372036854775809"
        try {
            parse("10 print $value")
            fail("Expected SemanticsException")
        } catch (se: SemanticsException) {
            val ive = errorListener.errors[0].exception as InvalidValueException
            assertEquals(value, ive.value())
        }
    }

    /**
     * Invalid float -> overflow.
     */
    @Test
    fun testOverflowF64() {
        val value = "1.7976931348623157e+309"
        try {
            parse("10 print $value")
            fail("Expected SemanticsException")
        } catch (se: SemanticsException) {
            val ive = errorListener.errors[0].exception as InvalidValueException
            assertEquals(value, ive.value())
        }
    }

    @Test
    fun testSubtractingStrings() {
        parseAndExpectException("10 print \"A\" - \"B\"", "illegal expression")
    }

    @Test
    fun testMultiplyingStrings() {
        parseAndExpectException("10 print \"A\" * \"B\"", "illegal expression")
    }

    @Test
    fun testDividingStrings() {
        parseAndExpectException("10 print \"A\" / \"B\"", "illegal expression")
    }

    @Test
    fun testIntegerDivisionOnStrings() {
        parseAndExpectException("10 print \"A\" \\ \"B\"", "expected subexpressions of type integer")
    }

    @Test
    fun testModuloOnStrings() {
        parseAndExpectException("10 print \"A\" MOD \"B\"", "illegal expression")
    }

    @Test
    fun testAddingStringAndInteger() {
        parseAndExpectException("10 print \"A\" + 17", "illegal expression")
    }

    @Test
    fun testAddingStringAndFloat() {
        parseAndExpectException("10 print \"A\" + 17.17", "illegal expression")
    }

    @Test
    fun testSimpleDivisionByZero() {
        parseAndExpectException("print 1 / 0", "division by zero")
        parseAndExpectException("print 1 / 0.", "division by zero")
        parseAndExpectException("print 1 / 0.0", "division by zero")
        parseAndExpectException("print 1 / 0.00", "division by zero")
        parseAndExpectException("print 1 / 0.000", "division by zero")
        parseAndExpectException("print 1 / .0", "division by zero")
        parseAndExpectException("print 1 / .00", "division by zero")
        parseAndExpectException("print 1 / .000", "division by zero")
    }

    @Test
    fun testComparingDifferentTypes() {
        parseAndExpectException("10 print 1 <> \"one\"", "cannot compare integer and string")
        parseAndExpectException("20 print \"two\" = 2", "cannot compare string and integer")
        parseAndExpectException("30 print \"three\" = 2.2", "cannot compare string and double")
    }

    @Test
    fun testAndingFloats() {
        parseAndExpectException("10 print 1.5 AND 5.1", "expected subexpressions of type integer")
    }

    @Test
    fun testAndingStrings() {
        parseAndExpectException("10 print \"A\" AND \"B\"", "expected subexpressions of type integer")
    }

    @Test
    fun testNottingString() {
        parseAndExpectException("10 print NOT \"B\"", "expected subexpression of type integer")
    }

    @Test
    fun shouldNotParseIntegerDivisionWithFloat() {
        parseAndExpectException("print 1.5 \\ 2", "expected subexpressions of type integer")
        parseAndExpectException("print 1 \\ 0.7", "expected subexpressions of type integer")
        parseAndExpectException("print 1 \\ (0.7 + 1)", "expected subexpressions of type integer")
    }

    @Test
    fun shouldNotParseInvalidLetterInterval() {
        parseAndExpectException("defstr c-a", "invalid letter interval")
    }

    @Test
    fun shouldNotParseInvalidAssignmentToDefinedType() {
        parseAndExpectException("defstr s : s = 5", "a value of type integer")
        parseAndExpectException("defstr s : s = 1.0", "a value of type double")
        parseAndExpectException("defint i : i = \"...\"", "a value of type string")
        parseAndExpectException("defdbl d : d = \"...\"", "a value of type string")
    }

    @Test
    fun shouldNotParseInvalidAssignmentToDefaultType() {
        // The default type is Double
        parseAndExpectException("let d = \"...\"", "a value of type string")
    }

    @Test
    fun shouldNotParseInvalidAssignmentFromDefaultType() {
        // The default type is Double
        parseAndExpectException("defstr s : s = foo", "a value of type double")
    }

    @Test
    fun shouldNotParseInvalidAssignmentToDimmedType() {
        parseAndExpectException("dim s1 as string : s1 = 5", "a value of type integer")
        parseAndExpectException("dim s2 as string : s2 = 1.0", "a value of type double")
        parseAndExpectException("dim i2 as integer : i2 = \"...\"", "a value of type string")
        parseAndExpectException("dim d2 as double : d2 = \"...\"", "a value of type string")
    }

    @Test
    fun shouldNotParseDimOfDefinedVariable() {
        parseAndExpectException("let a = 0 : dim a as integer", "variable 'a' is already defined")
        parseAndExpectException("let foo = bar : dim bar as integer", "variable 'bar' is already defined")
        parseAndExpectException("let foo = bar : dim tee as integer, bar as string", "variable 'bar' is already defined")
        parseAndExpectException("dim a as integer : dim a as integer", "variable 'a' is already defined")
        parseAndExpectException("dim a as integer, a as integer", "variable 'a' is already defined")
    }

    @Test
    fun shouldNotParseDimOfVariableWithNonMatchingTypeSpecifier() {
        parseAndExpectException("dim bar# as integer", "variable 'bar#' is defined")
        parseAndExpectException("dim tee$ as integer, bar$ as string", "variable 'tee$' is defined")
        parseAndExpectException("dim foo% as double", "variable 'foo%' is defined")
    }

    @Test
    fun shouldNotSwapIntegerAndString() {
        parseAndExpectException("swap a%, b$", "variables with types I64 and Str")
    }

    @Test
    fun shouldNotSwapFloatAndString() {
        parseAndExpectException("swap a#, b$", "variables with types F64 and Str")
    }

    @Test
    fun shouldNotRandomizeWithIllegalExpression() {
        parseAndExpectException("randomize 1 + \"2\"", "illegal expression")
    }

    @Test
    fun shouldNotInitializeConstantWithFunctionCall() {
        parseAndExpectException("CONST a = sum(1)", "non-constant expression: sum(1)")
    }

    @Test
    fun shouldNotInitializeConstantWithVariableDereference() {
        parseAndExpectException("DIM foo AS INTEGER : CONST a = foo", "non-constant expression: foo")
    }

    @Test
    fun shouldNotParseConstantWithInvalidTypeSpecifier() {
        parseAndExpectException("CONST a$ = 1", "type specifier string and an expression of type integer")
    }

    @Test
    fun shouldNotInitializeConstantWithUnknownIdentifier() {
        parseAndExpectException("CONST a = foo", "non-constant expression: foo")
    }

    @Test
    fun shouldNotAssignToConstant() {
        parseAndExpectException("CONST a = 1 : LET a = 2", "cannot assign a new value to constant")
    }
}
