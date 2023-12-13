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

import org.junit.Assert.assertThrows
import org.junit.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_10
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_2
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_4
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_5
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_M1
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_A
import se.dykstrom.jcc.basic.ast.EndStatement
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.common.ast.*

/**
 * Tests class `BasicSyntaxVisitor`, especially functionality related to IF statements.
 *
 * @author Johan Dykstrom
 * @see BasicSyntaxVisitor
 */
class BasicSyntaxVisitorIfTests : AbstractBasicSyntaxVisitorTests() {

    @Test
    fun shouldParseIfGotoNum() {
        val gs = GotoStatement(0, 0, "20")
        val ifs = IfStatement.builder(IL_5, gs).build()
        val expectedStatements = listOf(LabelledStatement("10", ifs))
        parseAndAssert("10 if 5 goto 20", expectedStatements)
    }

    @Test
    fun shouldParseIfThenNum() {
        val gs = GotoStatement(0, 0, "100")
        val ifs = IfStatement.builder(IL_M1, gs).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if -1 then 100", expectedStatements)
    }

    @Test
    fun shouldParseIfThenGoto() {
        val gs = GotoStatement(0, 0, "100")
        val ifs = IfStatement.builder(IL_M1, gs).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if -1 then goto 100", expectedStatements)
    }

    @Test
    fun shouldParseIfThenPrint() {
        val ps = PrintStatement(0, 0, listOf(IL_10))
        val ifs = IfStatement.builder(IL_0, ps).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if 0 then print 10", expectedStatements)
    }

    @Test
    fun shouldParseIfThenAssign() {
        val ast = AssignStatement(0, 0, INE_I64_A, IL_4)
        val ifs = IfStatement.builder(IL_0, ast).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if 0 then a% = 4", expectedStatements)
    }

    @Test
    fun shouldParseIfThenMultiple() {
        val ast = AssignStatement(0, 0, INE_I64_A, IL_4)
        val ps = PrintStatement(0, 0, listOf(IDE_I64_A))
        val gs = GotoStatement(0, 0, "10")
        val ifs = IfStatement.builder(IL_0, listOf(ast, ps, gs)).build()
        val expectedStatements = listOf(LabelledStatement("10", ifs))
        parseAndAssert("10 if 0 then a% = 4 : print a% : goto 10", expectedStatements)
    }

    @Test
    fun shouldParseNestedIfThen() {
        val ee = EqualExpression(0, 0, IL_1, IL_2)
        val ps = PrintStatement(0, 0, listOf(IL_5))
        val is1 = IfStatement.builder(IL_M1, ps).build()
        val is2 = IfStatement.builder(IL_0, is1).build()
        val is3 = IfStatement.builder(ee, is2).build()
        val expectedStatements = listOf(is3)
        parseAndAssert("if 1 = 2 then if 0 then if -1 then print 5", expectedStatements)
    }

    @Test
    fun shouldParseIfGotoNumElseNum() {
        val gs1 = GotoStatement(0, 0, "20")
        val gs2 = GotoStatement(0, 0, "30")
        val ifs = IfStatement.builder(IL_10, gs1).elseStatements(gs2).build()
        val expectedStatements = listOf(LabelledStatement("10", ifs))
        parseAndAssert("10 if 10 goto 20 else 30", expectedStatements)
    }

    @Test
    fun shouldParseIfThenNumElseNum() {
        val gs1 = GotoStatement(0, 0, "20")
        val gs2 = GotoStatement(0, 0, "30")
        val ifs = IfStatement.builder(IL_10, gs1).elseStatements(gs2).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if 10 then 20 else 30", expectedStatements)
    }

    @Test
    fun shouldParseIfThenNumElseGoto() {
        val gs1 = GotoStatement(0, 0, "20")
        val gs2 = GotoStatement(0, 0, "30")
        val ifs = IfStatement.builder(IL_10, gs1).elseStatements(gs2).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if 10 then 20 else goto 30", expectedStatements)
    }

    @Test
    fun shouldParseIfThenNumElsePrint() {
        val gs = GotoStatement(0, 0, "20")
        val ps = PrintStatement(0, 0, listOf(IL_4))
        val ifs = IfStatement.builder(IL_10, gs).elseStatements(ps).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if 10 then 20 else print 4", expectedStatements)
    }

    @Test
    fun shouldParseIfThenMultipleElseMultiple() {
        val as1 = AssignStatement(0, 0, INE_I64_A, IL_4)
        val ps1 = PrintStatement(0, 0, listOf(IDE_I64_A))
        val gs1 = GotoStatement(0, 0, "10")
        val ps2 = PrintStatement(0, 0, listOf(IL_2))
        val gs2 = GotoStatement(0, 0, "20")
        val ifs = IfStatement.builder(IL_0, listOf(as1, ps1, gs1)).elseStatements(listOf(ps2, gs2)).build()
        val expectedStatements = listOf(LabelledStatement("10", ifs))
        parseAndAssert("10 if 0 then a% = 4 : print a% : goto 10 else print 2 : goto 20", expectedStatements)
    }

    @Test
    fun shouldParseIfThenBlock() {
        val ps = PrintStatement(0, 0, listOf(IL_4))
        val ifs = IfStatement.builder(IL_M1, ps).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if -1 then print 4 end if", expectedStatements)
    }

    @Test
    fun shouldParseEmptyThenBlock() {
        val ifs = IfStatement.builder(IL_M1, emptyList()).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if -1 then end if", expectedStatements)
    }

    @Test
    fun shouldParseIfThenElseBlock() {
        val ps1 = PrintStatement(0, 0, listOf(IL_4))
        val ps2 = PrintStatement(0, 0, listOf(IL_3))
        val ast = AssignStatement(0, 0, INE_I64_A, IL_1)
        val ps3 = PrintStatement(0, 0, listOf(IDE_I64_A))
        val ifs = IfStatement.builder(IL_M1, listOf(ps1, ps2)).elseStatements(listOf(ast, ps3)).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if -1 then print 4 print 3 else a% = 1 print a% end if", expectedStatements)
    }

    @Test
    fun shouldParseEmptyThenElseBlock() {
        val ifs = IfStatement.builder(IL_M1, emptyList()).elseStatements(emptyList()).build()
        val expectedStatements = listOf(ifs)
        parseAndAssert("if -1 then else end if", expectedStatements)
    }

    @Test
    fun shouldParseNestedIfThenElseBlock() {
        val ps1 = PrintStatement(0, 0, listOf(IL_1))
        val ps2 = PrintStatement(0, 0, listOf(IL_2))
        val ps3 = PrintStatement(0, 0, listOf(IL_3))
        val ps4 = PrintStatement(0, 0, listOf(IL_4))
        val ge: Expression = GreaterExpression(0, 0, IL_1, IL_2)
        val le: Expression = LessExpression(0, 0, IL_1, IL_2)
        val is1 = IfStatement.builder(ge, ps1).elseStatements(ps2).build()
        val is2 = IfStatement.builder(le, ps3).elseStatements(ps4).build()
        val is3 = IfStatement.builder(IL_M1, is1).elseStatements(is2).build()
        val expectedStatements = listOf(is3)
        parseAndAssert(
            "if -1 then " +
                    "  if 1 > 2 then " +
                    "    print 1 " +
                    "  else " +
                    "    print 2 " +
                    "  end if " +
                    "else " +
                    "  if 1 < 2 then " +
                    "    print 3 " +
                    "  else " +
                    "    print 4 " +
                    "  end if " +
                    "end if", expectedStatements
        )
    }

    @Test
    fun shouldParseEmptyElseIfBlock() {
        val is1 = IfStatement.builder(IL_0, emptyList()).build()
        val is2 = IfStatement.builder(IL_M1, emptyList()).elseStatements(is1).build()
        val expectedStatements = listOf(is2)
        parseAndAssert("if -1 then elseif 0 then end if", expectedStatements)
    }

    @Test
    fun shouldParseElseIfBlock() {
        val ps1 = PrintStatement(0, 0, listOf(IL_1))
        val secondIf = IfStatement.builder(IL_0, ps1).build()
        val ps2 = PrintStatement(0, 0, listOf(IL_2))
        val firstIf = IfStatement.builder(IL_M1, ps2).elseStatements(secondIf).build()
        val expectedStatements = listOf(firstIf)
        parseAndAssert("if -1 then print 2 elseif 0 then print 1 end if", expectedStatements)
    }

    @Test
    fun shouldParseElseIfElseBlock() {
        val ps4 = PrintStatement(0, 0, listOf(IL_4))
        val ps1 = PrintStatement(0, 0, listOf(IL_1))
        val secondIf = IfStatement.builder(IL_0, ps1).elseStatements(ps4).build()
        val ps2 = PrintStatement(0, 0, listOf(IL_2))
        val firstIf = IfStatement.builder(IL_M1, ps2).elseStatements(secondIf).build()
        val expectedStatements = listOf(firstIf)
        parseAndAssert("if -1 then print 2 elseif 0 then print 1 else print 4 end if", expectedStatements)
    }

    @Test
    fun shouldParseElseIfElseIfBlock() {
        val ps4 = PrintStatement(0, 0, listOf(IL_4))
        val thirdIf = IfStatement.builder(IL_M1, ps4).build()
        val ps1 = PrintStatement(0, 0, listOf(IL_1))
        val secondIf = IfStatement.builder(IL_0, ps1).elseStatements(thirdIf).build()
        val ps2 = PrintStatement(0, 0, listOf(IL_2))
        val firstIf = IfStatement.builder(IL_M1, ps2).elseStatements(secondIf).build()
        val expectedStatements = listOf(firstIf)
        parseAndAssert("if -1 then print 2 elseif 0 then print 1 elseif -1 then print 4 end if", expectedStatements)
    }

    @Test
    fun shouldParseElseIfElseIfElseBlock() {
        val ps4 = PrintStatement(0, 0, listOf(IL_4))
        val ps3 = PrintStatement(0, 0, listOf(IL_3))
        val ee3 = EqualExpression(0, 0, IDE_I64_A, IL_3)
        val thirdIf = IfStatement.builder(ee3, ps3).elseStatements(ps4).build()
        val ps2 = PrintStatement(0, 0, listOf(IL_2))
        val ee2 = EqualExpression(0, 0, IDE_I64_A, IL_2)
        val secondIf = IfStatement.builder(ee2, ps2).elseStatements(thirdIf).build()
        val ps1 = PrintStatement(0, 0, listOf(IL_1))
        val ee1 = EqualExpression(0, 0, IDE_I64_A, IL_1)
        val firstIf = IfStatement.builder(ee1, ps1).elseStatements(secondIf).build()
        val expectedStatements = listOf(firstIf)
        parseAndAssert(
            "if a% = 1 then " +
                    "  print 1 " +
                    "elseif a% = 2 then " +
                    "  print 2 " +
                    "elseif a% = 3 then " +
                    "  print 3 " +
                    "else " +
                    "  print 4 " +
                    "end if", expectedStatements
        )
    }

    @Test
    fun shouldParseNestedElseIfBlock() {
        val ps3 = PrintStatement(0, 0, listOf(IL_3))
        val fourthIf = IfStatement.builder(IL_0, ps3).build()
        val ps2 = PrintStatement(0, 0, listOf(IL_2))
        val thirdIf = IfStatement.builder(IL_M1, ps2).elseStatements(fourthIf).build()
        val ee2 = EqualExpression(0, 0, IDE_I64_A, IL_2)
        val secondIf = IfStatement.builder(ee2, thirdIf).build()
        val ps1 = PrintStatement(0, 0, listOf(IL_1))
        val ee1 = EqualExpression(0, 0, IDE_I64_A, IL_1)
        val firstIf = IfStatement.builder(ee1, ps1).elseStatements(secondIf).build()
        val expectedStatements = listOf(firstIf)
        parseAndAssert(
            "if a% = 1 then " +
                    "  print 1 " +
                    "elseif a% = 2 then " +
                    "  if -1 then " +
                    "    print 2 " +
                    "  elseif 0 then " +
                    "    print 3 " +
                    "  end if " +
                    "end if", expectedStatements
        )
    }

    @Test
    fun shouldParseThenBlockWithEndStatement() {
        val ps = PrintStatement(0, 0, listOf(IL_1))
        val es = EndStatement(0, 0)
        val expected = IfStatement.builder(IL_M1, ps, es).build()
        parseAndAssert(
            """
                if -1 then
                    print 1
                    end
                end if
                """,
            expected
        )
    }

    @Test
    fun shouldParseElseBlockWithEndStatement() {
        val ps = PrintStatement(0, 0, listOf(IL_1))
        val es = EndStatement(0, 0)
        val expected = IfStatement.builder(IL_M1, ps).elseStatements(es).build()
        parseAndAssert(
            """
                if -1 then
                    print 1
                else
                    end
                end if
                """,
            expected
        )
    }

    @Test
    fun shouldParseElseIfBlockWithEndStatement() {
        val ps = PrintStatement(0, 0, listOf(IL_1))
        val es = EndStatement(0, 0)
        val eis = IfStatement.builder(IL_0, es).build()
        val expected = IfStatement.builder(IL_M1, ps).elseStatements(eis).build()
        parseAndAssert(
            """
                if -1 then
                    print 1
                elseif 0 then
                    end
                end if
                """,
            expected
        )
    }

    @Test
    fun shouldParseNestedIfBlockWithEndStatement() {
        val ps = PrintStatement(0, 0, listOf(IL_1))
        val es = EndStatement(0, 0)
        val nis = IfStatement.builder(IL_M1, es).build()
        val eis = IfStatement.builder(IL_0, nis).build()
        val expected = IfStatement.builder(IL_M1, ps).elseStatements(eis).build()
        parseAndAssert(
            """
                if -1 then
                    print 1
                elseif 0 then
                    if -1 then
                        end
                    end if
                end if
                """,
            expected
        )
    }

    // Negative tests:
    @Test
    fun shouldNotParseMissingThen() {
        assertThrows(IllegalStateException::class.java) {
            parse(
                """
              10 if -1
              20   print 1
              30 end if
              """
            )
        }
    }
}
