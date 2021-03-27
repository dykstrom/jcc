/*
 * Copyright (C) 2017 Johan Dykstrom
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

import org.junit.Test
import se.dykstrom.jcc.basic.ast.*
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.types.Bool
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str
import se.dykstrom.jcc.common.utils.FormatUtils.EOL
import java.util.Collections.emptyList

class BasicSyntaxVisitorTests : AbstractBasicSyntaxVisitorTest() {

    @Test
    fun testEmptyProgram() {
        val expectedStatements = emptyList<Statement>()
        parseAndAssert("", expectedStatements)
    }

    @Test
    fun testGosub() {
        val expectedStatements = listOf(GosubStatement(0, 0, "20", "10"))
        parseAndAssert("10 gosub 20", expectedStatements)
    }

    @Test
    fun testGosubLabel() {
        val expectedStatements = listOf(GosubStatement(0, 0, "sub", "10"))
        parseAndAssert("10 gosub sub", expectedStatements)
    }

    @Test
    fun testReturn() {
        val expectedStatements = listOf(ReturnStatement(0, 0, "10"))
        parseAndAssert("10 return", expectedStatements)
    }

    @Test
    fun testGoto() {
        val expectedStatements = listOf(GotoStatement(0, 0, "20", "10"))
        parseAndAssert("10 goto 20", expectedStatements)
    }

    @Test
    fun testGotoLabel() {
        val expectedStatements = listOf(GotoStatement(0, 0, "loop", "loop"))
        parseAndAssert("loop: goto loop", expectedStatements)
    }

    @Test
    fun testOnGosubOneNumber() {
        val expectedStatements = listOf(OnGosubStatement(0, 0, IDE_A, listOf("20"), "10"))
        parseAndAssert("10 on a% gosub 20", expectedStatements)
    }

    @Test
    fun testOnGosubMultipleNumbers() {
        val expectedStatements = listOf(OnGosubStatement(0, 0, IDE_A, listOf("20", "30", "40"), "10"))
        parseAndAssert("10 on a% gosub 20, 30, 40", expectedStatements)
    }

    @Test
    fun testOnGosubMultipleLabels() {
        val expectedStatements = listOf(OnGosubStatement(0, 0, IDE_A, listOf("foo", "bar", "axe"), "10"))
        parseAndAssert("10 on a% gosub foo, bar, axe", expectedStatements)
    }

    @Test
    fun testOnGotoOneNumber() {
        val expectedStatements = listOf(OnGotoStatement(0, 0, IDE_A, listOf("20"), "my.line"))
        parseAndAssert("my.line: on a% goto 20", expectedStatements)
    }

    @Test
    fun testOnGotoMultipleNumbers() {
        val expectedStatements = listOf(OnGotoStatement(0, 0, IDE_A, listOf("20", "30", "40"), "10"))
        parseAndAssert("10 on a% goto 20, 30, 40", expectedStatements)
    }

    @Test
    fun testOnGotoMultipleLabels() {
        val expectedStatements = listOf(OnGotoStatement(0, 0, IDE_A, listOf("one", "two", "three"), "10"))
        parseAndAssert("10 on a% goto one, two, three", expectedStatements)
    }

    @Test
    fun shouldParseLabel() {
        val expectedStatements = listOf(OnGotoStatement(0, 0, IDE_A, listOf("1", "two", "3"), "loop"))
        parseAndAssert("loop: on a% goto 1, two, 3", expectedStatements)
    }

    @Test
    fun testEnd() {
        val expectedStatements = listOf(EndStatement(0, 0, "10"))
        parseAndAssert("10 end", expectedStatements)
    }

    @Test
    fun testDefBoolOneLetter() {
        val expectedStatements = listOf(DefBoolStatement(0, 0, setOf('a')))
        parseAndAssert("defbool a", expectedStatements)
    }

    @Test
    fun testDefBoolTwoLetters() {
        val expectedStatements = listOf(DefBoolStatement(0, 0, setOf('a', 'f')))
        parseAndAssert("defbool a,f", expectedStatements)
    }

    @Test
    fun testDefBoolOneInterval() {
        val expectedStatements = listOf(DefBoolStatement(0, 0, setOf('a', 'b', 'c')))
        parseAndAssert("defbool a-c", expectedStatements)
    }

    @Test
    fun testDefBoolMixed() {
        val expectedStatements = listOf(DefBoolStatement(0, 0, setOf('a', 'b', 'c', 'f')))
        parseAndAssert("defbool a-c,f", expectedStatements)
    }

    @Test
    fun testDefDbl() {
        val expectedStatements = listOf(DefDblStatement(0, 0, setOf('a', 'b', 'c', 'f')))
        parseAndAssert("defdbl a, b, c, f", expectedStatements)
    }

    @Test
    fun testDefInt() {
        val expectedStatements = listOf(DefIntStatement(0, 0, setOf('a', 'b', 'c', 'f')))
        parseAndAssert("defint f,a-c", expectedStatements)
    }

    @Test
    fun testDefStr() {
        val expectedStatements = listOf(DefIntStatement(0, 0, setOf('a', 'b', 'c', 'f')))
        parseAndAssert("defint a-b,c,f", expectedStatements)
    }

    @Test
    fun testDimSingle() {
        val declaration = Declaration(0, 0, "count", I64.INSTANCE)
        val expectedStatements = listOf(VariableDeclarationStatement(0, 0, listOf(declaration)))
        parseAndAssert("dim count as integer", expectedStatements)
    }

    @Test
    fun testDimMultiple() {
        val declaration1 = Declaration(0, 0, "int", I64.INSTANCE)
        val declaration2 = Declaration(0, 0, "boo", Bool.INSTANCE)
        val expectedStatements = listOf(VariableDeclarationStatement(0, 0, listOf(declaration1, declaration2)))
        parseAndAssert("Dim int As Integer, boo As Boolean", expectedStatements)
    }

    @Test
    fun testDimAll() {
        val declaration1 = Declaration(0, 0, "a", I64.INSTANCE)
        val declaration2 = Declaration(0, 0, "b", Bool.INSTANCE)
        val declaration3 = Declaration(0, 0, "c", F64.INSTANCE)
        val declaration4 = Declaration(0, 0, "d", Str.INSTANCE)
        val expectedStatements = listOf(VariableDeclarationStatement(0, 0, listOf(declaration1, declaration2, declaration3, declaration4)))
        parseAndAssert("Dim a As Integer, b As Boolean, c As Double, d As String", expectedStatements)
    }

    @Test
    fun testIntAssignment() {
        val assignStatement = AssignStatement(0, 0, NAME_A, IL_3)
        val expectedStatements = listOf(assignStatement)

        parseAndAssert("10 let a% = 3", expectedStatements) // With LET
        parseAndAssert("10 a% = 3", expectedStatements) // Without LET
        parseAndAssert("10 a% = &H3", expectedStatements) // With hexadecimal
        parseAndAssert("10 a% = &O3", expectedStatements) // With octal
        parseAndAssert("10 a% = &B11", expectedStatements) // With binary
    }

    @Test
    fun testStringAssignment() {
        val assignStatement = AssignStatement(0, 0, NAME_S, SL_A)
        val expectedStatements = listOf(assignStatement)

        parseAndAssert("10 let s$ = \"A\"", expectedStatements) // With LET
        parseAndAssert("10 s$ = \"A\"", expectedStatements) // Without LET
    }

    @Test
    fun testFloatAssignment() {
        val assignStatement = AssignStatement(0, 0, NAME_F, FL_0_3)
        val expectedStatements = listOf(assignStatement)

        parseAndAssert("10 let f# = 0.3", expectedStatements) // With LET
        parseAndAssert("10 f# = 0.3", expectedStatements) // Without LET
    }

    @Test
    fun testTwoAssignments() {
        val as1 = AssignStatement(0, 0, NAME_A, IL_3)
        val as2 = AssignStatement(0, 0, NAME_B, IL_5)
        val expectedStatements = listOf(as1, as2)

        parseAndAssert("10 let a% = 3 : b% = 5", expectedStatements)
    }

    @Test
    fun testFloatDereference() {
        val assignStatement = AssignStatement(0, 0, NAME_G, IDE_F)
        val expectedStatements = listOf(assignStatement)

        parseAndAssert("10 let g# = f#", expectedStatements)
    }

    @Test
    fun testIntDereference() {
        val assignStatement = AssignStatement(0, 0, NAME_B, IDE_A)
        val expectedStatements = listOf(assignStatement)

        parseAndAssert("10 let b% = a%", expectedStatements)
    }

    @Test
    fun testStringDereference() {
        val ps = PrintStatement(0, 0, listOf(IDE_S), "10")
        val expectedStatements = listOf(ps)

        parseAndAssert("10 print s$", expectedStatements)
    }

    @Test
    fun testTwoDereferences() {
        val ps = PrintStatement(0, 0, listOf(IDE_A, IL_10, IDE_S), "10")
        val expectedStatements = listOf(ps)

        parseAndAssert("10 print a%; 10; s$", expectedStatements)
    }

    @Test
    fun testTwoDereferencesInExpression() {
        val ae = AddExpression(0, 0, IDE_A, IDE_B)
        val assignStatement = AssignStatement(0, 0, NAME_F, ae)
        val expectedStatements = listOf(assignStatement)

        parseAndAssert("10 let f# = a% + b%", expectedStatements)
    }

    @Test
    fun testRandomize() {
        val randomizeStatement = RandomizeStatement(0, 0, "10")
        val expectedStatements = listOf(randomizeStatement)

        parseAndAssert("10 randomize", expectedStatements)
    }

    @Test
    fun testRandomizeWithExpression() {
        val randomizeStatement = RandomizeStatement(0, 0, IL_NEG_3, "10")
        val expectedStatements = listOf(randomizeStatement)

        parseAndAssert("10 randomize -3", expectedStatements)
    }

    @Test
    fun testSwap() {
        val swapStatement = SwapStatement(0, 0, IDENT_INT_A, IDENT_INT_B, "10")
        val expectedStatements = listOf(swapStatement)

        parseAndAssert("10 swap a%, b%", expectedStatements)
    }

    @Test
    fun testRem() {
        val cs = CommentStatement(0, 0, "10")
        val expectedStatements = listOf(cs)

        parseAndAssert("10 rem", expectedStatements)
    }

    @Test
    fun testRemWithComment() {
        val cs = CommentStatement(0, 0, "10")
        val expectedStatements = listOf(cs)

        parseAndAssert("10 rem foo", expectedStatements)
    }

    @Test
    fun testApostropheWithComment() {
        val cs = CommentStatement(0, 0, "10")
        val expectedStatements = listOf(cs)

        parseAndAssert("10 'foo", expectedStatements)
    }

    @Test
    fun testPrintAndRem() {
        val ps = PrintStatement(0, 0, listOf(IL_1), "10")
        val cs = CommentStatement(0, 0, null)
        val expectedStatements = listOf(ps, cs)

        parseAndAssert("10 PRINT 1 : REM PRINT 1", expectedStatements)
    }

    @Test
    fun testTwoGotosOneLine() {
        val gs0 = GotoStatement(0, 0, "20", "10")
        val gs1 = GotoStatement(0, 0, "10")
        val expectedStatements = listOf(gs0, gs1)

        parseAndAssert("10 goto 20 : goto 10", expectedStatements)
    }

    @Test
    fun testTwoGotosTwoLines() {
        val gs0 = GotoStatement(0, 0, "20", "10")
        val gs1 = GotoStatement(0, 0, "10", "20")
        val expectedStatements = listOf(gs0, gs1)

        parseAndAssert("10 goto 20" + EOL + "20 goto 10", expectedStatements)
    }

    @Test
    fun testPrintWithoutExpression() {
        val expressions = emptyList<Expression>()
        val ps = PrintStatement(0, 0, expressions, "10")
        val expectedStatements = listOf(ps)

        parseAndAssert("10 print", expectedStatements)
    }

    @Test
    fun testString() = testPrintOneExpression("\"A\"", SL_A)

    @Test
    fun testInteger() = testPrintOneExpression("3", IL_3)

    @Test
    fun testHexadecimalInteger() = testPrintOneExpression("&HFF", IL_255)

    @Test
    fun testOctalInteger() = testPrintOneExpression("&O12", IL_10)

    @Test
    fun testBinaryInteger() = testPrintOneExpression("&B1010", IL_10)

    @Test
    fun testNegativeInteger() = testPrintOneExpression("-3", IL_NEG_3)

    @Test
    fun testFloats() {
        testPrintOneExpression("1.2", FL_1_2)
        testPrintOneExpression(".3", FL_0_3)
        testPrintOneExpression("0.3", FL_0_3)
        testPrintOneExpression("17.", FloatLiteral(0, 0, "17.0"))
        testPrintOneExpression("-17.5", FloatLiteral(0, 0, "-17.5"))

        testPrintOneExpression("7.5e+10", FL_7_5_EXP)
        testPrintOneExpression("7.5d10", FL_7_5_EXP)
        testPrintOneExpression("7.5D+10", FL_7_5_EXP)
        testPrintOneExpression("7.5E10", FL_7_5_EXP)

        testPrintOneExpression("1.2#", FL_1_2)
        testPrintOneExpression(".3#", FL_0_3)
        testPrintOneExpression("7.5e10#", FL_7_5_EXP)
    }

    @Test
    fun testNegativeDereference() = testPrintOneExpression("-a%", SubExpression(0, 0, IL_0, IDE_A))

    @Test
    fun testNegativeSubExpr() = testPrintOneExpression("-(1+a%)", SubExpression(0, 0, IL_0, AddExpression(0, 0, IL_1, IDE_A)))

    @Test
    fun testNegativeHexadecimalExpr() = testPrintOneExpression("-(&HFF + -&H3)", SubExpression(0, 0, IL_0, AddExpression(0, 0, IL_255, IL_NEG_3)))

    @Test
    fun testAdd() = testPrintOneExpression("3 + 4", AddExpression(0, 0, IL_3, IL_4))

    @Test
    fun testAddWithStrings() = testPrintOneExpression("\"A\" + \"B\"", AddExpression(0, 0, SL_A, SL_B))

    @Test
    fun testAddWithHexadecimal() = testPrintOneExpression("3 + &H04", AddExpression(0, 0, IL_3, IL_4))

    @Test
    fun testAddWithOctal() = testPrintOneExpression("3 + &H03", AddExpression(0, 0, IL_3, IL_3))

    @Test
    fun testSub() = testPrintOneExpression("1-4", SubExpression(0, 0, IL_1, IL_4))

    @Test
    fun testMul() = testPrintOneExpression("1*2", MulExpression(0, 0, IL_1, IL_2))

    @Test
    fun testMulWithBinary() = testPrintOneExpression("1*&B10", MulExpression(0, 0, IL_1, IL_2))

    @Test
    fun testMulWithFloat() = testPrintOneExpression("1*.3", MulExpression(0, 0, IL_1, FL_0_3))

    @Test
    fun testDiv() = testPrintOneExpression("10/5", DivExpression(0, 0, IL_10, IL_5))

    @Test
    fun testDivWithFloat() = testPrintOneExpression("10/0.3", DivExpression(0, 0, IL_10, FL_0_3))

    @Test
    fun testIDiv() = testPrintOneExpression("10\\5", IDivExpression(0, 0, IL_10, IL_5))

    @Test
    fun testMod() = testPrintOneExpression("10 MOD 5", ModExpression(0, 0, IL_10, IL_5))

    @Test
    fun testAddAndSub() {
        val ae = AddExpression(0, 0, IL_1, IL_2)
        val se = SubExpression(0, 0, ae, IL_3)
        testPrintOneExpression("1 + 2 - 3", se)
    }

    @Test
    fun testAddAndMul() {
        val me = MulExpression(0, 0, IL_10, IL_2)
        val ae = AddExpression(0, 0, IL_5, me)
        testPrintOneExpression("5 + 10 * 2", ae)
    }

    @Test
    fun testAddAndMulWitFloat() {
        val me = MulExpression(0, 0, IL_10, IL_2)
        val ae = AddExpression(0, 0, FL_7_5_EXP, me)
        testPrintOneExpression("7.5E10 + 10 * 2", ae)
    }

    @Test
    fun testAddAndMod() {
        val me = ModExpression(0, 0, IL_10, IL_2)
        val ae = AddExpression(0, 0, IL_5, me)
        testPrintOneExpression("5 + 10 MOD 2", ae)
    }

    @Test
    fun testMulAndIDiv() {
        val me = MulExpression(0, 0, IL_5, IL_10)
        val ie = IDivExpression(0, 0, me, IL_2)
        testPrintOneExpression("5 * 10 \\ 2", ie)
    }

    @Test
    fun testAddAndMulWithPar() {
        val ae = AddExpression(0, 0, IL_5, IL_10)
        val me = MulExpression(0, 0, ae, IL_2)
        testPrintOneExpression("(5 + 10) * 2", me)
    }

    @Test
    fun testMulAndAddWithPar() {
        val ae = AddExpression(0, 0, IL_5, IL_10)
        val me = MulExpression(0, 0, IL_2, ae)
        testPrintOneExpression("2 * (5 + 10)", me)
    }

    @Test
    fun testAddAndMulAndSubWithPar() {
        val ae = AddExpression(0, 0, IL_5, IL_10)
        val se = SubExpression(0, 0, IL_1, IL_2)
        val me = MulExpression(0, 0, ae, se)
        testPrintOneExpression("(5 + 10) * (1 - 2)", me)
    }

    @Test
    fun testOnePrintTwoStrings() {
        val expressions = listOf(SL_A, SL_B)
        val ps = PrintStatement(0, 0, expressions, "10")
        val expectedStatements = listOf(ps)

        parseAndAssert("10 print \"A\"; \"B\"", expectedStatements)
    }

    @Test
    fun testTwoPrintsOneLine() {
        val ps0 = PrintStatement(0, 0, listOf(SL_A), "10")
        val ps1 = PrintStatement(0, 0, listOf(SL_B))
        val expectedStatements = listOf(ps0, ps1)

        parseAndAssert("10 print \"A\" : print \"B\"", expectedStatements)
    }

    @Test
    fun testTwoPrintsTwoLines() {
        val ps0 = PrintStatement(0, 0, listOf(SL_A), "10")
        val ps1 = PrintStatement(0, 0, listOf(SL_B), "20")
        val expectedStatements = listOf(ps0, ps1)

        parseAndAssert("10 print \"A\"" + EOL + "20 print \"B\"", expectedStatements)
    }

    @Test
    fun testPrintAndGotoOneLine() {
        val ps = PrintStatement(0, 0, listOf(SL_A), "10")
        val gs = GotoStatement(0, 0, "10")
        val expectedStatements = listOf(ps, gs)

        parseAndAssert("10 print \"A\":goto 10", expectedStatements)
    }

    @Test
    fun testPrintAndGotoTwoLines() {
        val ps = PrintStatement(0, 0, listOf(SL_A), "10")
        val gs = GotoStatement(0, 0, "10", "20")
        val expectedStatements = listOf(ps, gs)

        parseAndAssert("10 print \"A\"" + EOL + "20 goto 10", expectedStatements)
    }

    @Test
    fun testMultiplePrintAndGotos() {
        val gs10 = GotoStatement(0, 0, "40", "10")
        val ps20 = PrintStatement(0, 0, listOf(SL_A), "20")
        val gs30 = GotoStatement(0, 0, "60", "30")
        val ps40 = PrintStatement(0, 0, listOf(SL_B), "40")
        val gs50 = GotoStatement(0, 0, "20", "50")
        val ps60 = PrintStatement(0, 0, listOf(SL_C), "60")
        val expectedStatements = listOf(gs10, ps20, gs30, ps40, gs50, ps60)

        parseAndAssert("10 goto 40" + EOL
                + "20 print \"A\"" + EOL
                + "30 goto 60" + EOL
                + "40 print \"B\"" + EOL
                + "50 goto 20" + EOL
                + "60 print \"C\"", expectedStatements)
    }

    @Test
    fun testTrueAndFalse() {
        val expressions = listOf(BL_TRUE, BL_FALSE)
        val ps = PrintStatement(0, 0, expressions, "10")
        val expectedStatements = listOf(ps)

        parseAndAssert("10 print true; false", expectedStatements)
    }

    @Test
    fun testAssignBoolean() {
        val ee = EqualExpression(0, 0, IL_5, IL_10)
        val assignStatement = AssignStatement(0, 0, NAME_B, ee)
        val expectedStatements = listOf(assignStatement)

        parseAndAssert("30 let b% = 5 = 10", expectedStatements)
    }

    @Test
    fun testEqual() = testPrintOneExpression("1 = 4", EqualExpression(0, 0, IL_1, IL_4))

    @Test
    fun testNotEqual() = testPrintOneExpression("1 <> 4", NotEqualExpression(0, 0, IL_1, IL_4))

    @Test
    fun testGreaterThan() = testPrintOneExpression("1 > 4", GreaterExpression(0, 0, IL_1, IL_4))

    @Test
    fun testGreaterThanOrEqual() = testPrintOneExpression("1 >= 4", GreaterOrEqualExpression(0, 0, IL_1, IL_4))

    @Test
    fun testLessThan() = testPrintOneExpression("1 < 4", LessExpression(0, 0, IL_1, IL_4))

    @Test
    fun testLessThanOrEqual() = testPrintOneExpression("1 <= 4", LessOrEqualExpression(0, 0, IL_1, IL_4))

    @Test
    fun testEqualStrings() = testPrintOneExpression("\"A\" = \"B\"", EqualExpression(0, 0, SL_A, SL_B))

    @Test
    fun testNotEqualStrings() = testPrintOneExpression("\"A\" <> \"C\"", NotEqualExpression(0, 0, SL_A, SL_C))

    @Test
    fun testAnd() {
        val e1 = EqualExpression(0, 0, IL_1, IL_1)
        val e2 = EqualExpression(0, 0, IL_1, IL_2)
        testPrintOneExpression("1 = 1 AND 1 = 2", AndExpression(0, 0, e1, e2))
    }

    @Test
    fun testOr() {
        val e1 = EqualExpression(0, 0, IL_1, IL_1)
        val e2 = EqualExpression(0, 0, IL_1, IL_2)
        testPrintOneExpression("1 = 1 OR 1 = 2", OrExpression(0, 0, e1, e2))
    }

    @Test
    fun testXor() {
        val e1 = EqualExpression(0, 0, IL_1, IL_1)
        val e2 = EqualExpression(0, 0, IL_1, IL_2)
        testPrintOneExpression("1 = 1 XOR 1 = 2", XorExpression(0, 0, e1, e2))
    }

    @Test
    fun testNot() {
        val e1 = EqualExpression(0, 0, IL_1, IL_1)
        testPrintOneExpression("NOT 1 = 1", NotExpression(0, 0, e1))
    }

    @Test
    fun testOrXor() {
        val e1 = EqualExpression(0, 0, IL_1, IL_1)
        val e2 = EqualExpression(0, 0, IL_1, IL_2)
        val e3 = NotEqualExpression(0, 0, IL_1, IL_3)
        testPrintOneExpression("1 = 1 OR 1 = 2 XOR 1 <> 3", XorExpression(0, 0, OrExpression(0, 0, e1, e2), e3))
    }

    @Test
    fun testOrAnd() {
        val e1 = EqualExpression(0, 0, SL_A, SL_B)
        val e2 = EqualExpression(0, 0, IL_1, IL_2)
        val e3 = NotEqualExpression(0, 0, IL_1, IL_3)
        testPrintOneExpression("\"A\" = \"B\" OR 1 = 2 AND 1 <> 3", OrExpression(0, 0, e1, AndExpression(0, 0, e2, e3)))
    }

    @Test
    fun testNotAnd() {
        val e1 = EqualExpression(0, 0, IL_1, IL_2)
        val e2 = NotEqualExpression(0, 0, IL_1, IL_3)
        testPrintOneExpression("NOT 1 = 2 AND 1 <> 3", AndExpression(0, 0, NotExpression(0, 0, e1), e2))
    }

    @Test
    fun testAndNot() {
        val e1 = EqualExpression(0, 0, IL_1, IL_2)
        val e2 = NotEqualExpression(0, 0, IL_1, IL_3)
        testPrintOneExpression("1 = 2 AND NOT 1 <> 3", AndExpression(0, 0, e1, NotExpression(0, 0, e2)))
    }

    @Test
    fun testOrOr() {
        val e1 = EqualExpression(0, 0, IL_1, IL_2)
        val e2 = EqualExpression(0, 0, IL_1, IL_3)
        val e3 = EqualExpression(0, 0, IL_1, IL_1)
        testPrintOneExpression("1 = 2 OR 1 = 3 OR 1 = 1", OrExpression(0, 0, OrExpression(0, 0, e1, e2), e3))
    }

    @Test
    fun testOrAndWithPar() {
        val e1 = EqualExpression(0, 0, IL_1, IL_1)
        val e2 = EqualExpression(0, 0, IL_1, IL_2)
        val e3 = NotEqualExpression(0, 0, IL_1, IL_3)
        testPrintOneExpression("(1 = 1 OR 1 = 2) AND 1 <> 3", AndExpression(0, 0, OrExpression(0, 0, e1, e2), e3))
    }

    @Test
    fun testMultipleOrAndAnd() {
        val le = LessExpression(0, 0, IDE_B, IL_1)
        val ge = GreaterExpression(0, 0, IDE_B, IL_4)
        val oe1 = OrExpression(0, 0, le, ge)
        val ae1 = AndExpression(0, 0, BL_TRUE, oe1)
        val ee = EqualExpression(0, 0, IL_2, IL_2)
        val ae2 = AndExpression(0, 0, ee, BL_FALSE)
        val oe2 = OrExpression(0, 0, ae1, ae2)
        testPrintOneExpression("true AND (b% < 1 OR b% > 4) OR (2 = 2 AND false)", oe2)
    }

    @Test
    fun testMultipleAnd() {
        val le = LessExpression(0, 0, IDE_A, IL_1)
        val ge = GreaterExpression(0, 0, IDE_A, IL_4)
        val ae1 = AndExpression(0, 0, BL_TRUE, le)
        val ae2 = AndExpression(0, 0, ae1, ge)
        val ae3 = AndExpression(0, 0, ae2, BL_FALSE)
        testPrintOneExpression("true AND a% < 1 AND a% > 4 AND false", ae3)
    }

    @Test
    fun testMultipleOr() {
        val le = LessExpression(0, 0, IDE_B, IL_1)
        val ge = GreaterExpression(0, 0, IDE_B, IL_4)
        val oe1 = OrExpression(0, 0, BL_TRUE, le)
        val oe2 = OrExpression(0, 0, oe1, ge)
        val oe3 = OrExpression(0, 0, oe2, BL_FALSE)
        testPrintOneExpression("true OR b% < 1 OR b% > 4 OR false", oe3)
    }

    @Test
    fun testMixedExpressions() {
        val ae1 = AddExpression(0, 0, IDE_B, IL_1)
        val ae2 = AddExpression(0, 0, IDE_B, IL_2)
        val ge = GreaterExpression(0, 0, ae1, ae2)
        val se = SubExpression(0, 0, IL_5, FL_1_2)
        val nee = NotEqualExpression(0, 0, se, IL_1)
        val ande = AndExpression(0, 0, ge, nee)
        testPrintOneExpression("b% + 1 > b% + 2 and 5 - 1.2 <> 1", ande)
    }

    @Test(expected = IllegalStateException::class)
    fun testNoStatementAfterColon() {
        parse("10 print :")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoClosingQuotationMark() {
        parse("10 print \"Hello!")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoTermAfterPlus() {
        parse("10 print 5 +")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoFactorAfterMul() {
        parse("10 print 5 *")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoExpressionInAssignment() {
        parse("10 cool =")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoEqualsInAssignment() {
        parse("10 let a 5")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoExpressionAfterOr() {
        parse("10 print true or")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoExpressionAfterAnd() {
        parse("10 PRINT FALSE AND")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoExpressionBeforeAnd() {
        parse("10 PRINT AND FALSE")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoExpressionBetweenAnds() {
        parse("10 PRINT TRUE AND AND FALSE")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoRelationalOperator() {
        parse("10 print 5 6")
    }

    @Test(expected = IllegalStateException::class)
    fun testNoLetterList() {
        parse("defbool")
    }

    @Test(expected = IllegalStateException::class)
    fun testInvalidLetters() {
        parse("defbool 1-2")
    }

    @Test(expected = IllegalStateException::class)
    fun testMultipleLetters() {
        parse("defbool abc")
    }

    @Test(expected = IllegalStateException::class)
    fun testMultipleLettersInInterval() {
        parse("defbool abc-d")
    }
}
