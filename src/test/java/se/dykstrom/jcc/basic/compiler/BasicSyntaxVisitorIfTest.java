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

package se.dykstrom.jcc.basic.compiler;

import org.junit.Test;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.common.ast.*;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Tests class {@code BasicSyntaxVisitor}, especially functionality related to IF statements.
 * 
 * @author Johan Dykstrom
 * @see BasicSyntaxVisitor
 */
public class BasicSyntaxVisitorIfTest extends AbstractBasicSyntaxVisitorTest {

    @Test
    public void shouldParseIfGotoNum() throws Exception {
        Statement gs = new GotoStatement(0, 0, "20");
        Statement is = new IfStatement(0, 0, IL_5, singletonList(gs), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 5 goto 20", expectedStatements);
    }

    @Test
    public void shouldParseIfThenNum() throws Exception {
        Statement gs = new GotoStatement(0, 0, "100");
        Statement is = new IfStatement(0, 0, BL_TRUE, singletonList(gs), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then 100", expectedStatements);
    }

    @Test
    public void shouldParseIfThenGoto() throws Exception {
        Statement gs = new GotoStatement(0, 0, "100");
        Statement is = new IfStatement(0, 0, BL_TRUE, singletonList(gs), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then goto 100", expectedStatements);
    }

    @Test
    public void shouldParseIfThenPrint() throws Exception {
        Statement ps = new PrintStatement(0, 0, singletonList(IL_10));
        Statement is = new IfStatement(0, 0, BL_FALSE, singletonList(ps), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if false then print 10", expectedStatements);
    }

    @Test
    public void shouldParseIfThenAssign() throws Exception {
        Statement as = new AssignStatement(0, 0, IDENT_INT_A, IL_4);
        Statement is = new IfStatement(0, 0, BL_FALSE, singletonList(as), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if false then a% = 4", expectedStatements);
    }

    @Test
    public void shouldParseIfThenMultiple() throws Exception {
        Statement as = new AssignStatement(0, 0, IDENT_INT_A, IL_4);
        Statement ps = new PrintStatement(0, 0, singletonList(IDE_A));
        Statement gs = new GotoStatement(0, 0, "10");
        Statement is = new IfStatement(0, 0, BL_FALSE, asList(as, ps, gs), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if false then a% = 4 : print a% : goto 10", expectedStatements);
    }

    @Test
    public void shouldParseNestedIfThen() throws Exception {
        Expression ee = new EqualExpression(0, 0, IL_1, IL_2);
        Statement ps = new PrintStatement(0, 0, singletonList(IL_5));
        Statement is1 = new IfStatement(0, 0, BL_TRUE, singletonList(ps));
        Statement is2 = new IfStatement(0, 0, BL_FALSE, singletonList(is1));
        Statement is3 = new IfStatement(0, 0, ee, singletonList(is2), "10");
        List<Statement> expectedStatements = singletonList(is3);

        parseAndAssert("10 if 1 = 2 then if false then if true then print 5", expectedStatements);
    }

    @Test
    public void shouldParseIfGotoNumElseNum() throws Exception {
        Statement gs1 = new GotoStatement(0, 0, "20");
        Statement gs2 = new GotoStatement(0, 0, "30");
        Statement is = new IfStatement(0, 0, IL_10, singletonList(gs1), singletonList(gs2), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 10 goto 20 else 30", expectedStatements);
    }

    @Test
    public void shouldParseIfThenNumElseNum() throws Exception {
        Statement gs1 = new GotoStatement(0, 0, "20");
        Statement gs2 = new GotoStatement(0, 0, "30");
        Statement is = new IfStatement(0, 0, IL_10, singletonList(gs1), singletonList(gs2), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 10 then 20 else 30", expectedStatements);
    }

    @Test
    public void shouldParseIfThenNumElseGoto() throws Exception {
        Statement gs1 = new GotoStatement(0, 0, "20");
        Statement gs2 = new GotoStatement(0, 0, "30");
        Statement is = new IfStatement(0, 0, IL_10, singletonList(gs1), singletonList(gs2), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 10 then 20 else goto 30", expectedStatements);
    }

    @Test
    public void shouldParseIfThenNumElsePrint() throws Exception {
        Statement gs = new GotoStatement(0, 0, "20");
        Statement ps = new PrintStatement(0, 0, singletonList(IL_4));
        Statement is = new IfStatement(0, 0, IL_10, singletonList(gs), singletonList(ps), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 10 then 20 else print 4", expectedStatements);
    }

    @Test
    public void shouldParseIfThenMultipleElseMultiple() throws Exception {
        Statement as1 = new AssignStatement(0, 0, IDENT_INT_A, IL_4);
        Statement ps1 = new PrintStatement(0, 0, singletonList(IDE_A));
        Statement gs1 = new GotoStatement(0, 0, "10");
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement gs2 = new GotoStatement(0, 0, "20");
        Statement is = new IfStatement(0, 0, BL_FALSE, asList(as1, ps1, gs1), asList(ps2, gs2), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if false then a% = 4 : print a% : goto 10 else print 2 : goto 20", expectedStatements);
    }

    @Test
    public void shouldParseIfThenBlock() throws Exception {
        Statement ps = new PrintStatement(0, 0, singletonList(IL_4));
        Statement is = new IfStatement(0, 0, BL_TRUE, singletonList(ps), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then print 4 end if", expectedStatements);
    }

    @Test
    public void shouldParseEmptyThenBlock() throws Exception {
        Statement is = new IfStatement(0, 0, BL_TRUE, emptyList(), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then end if", expectedStatements);
    }

    @Test
    public void shouldParseIfThenElseBlock() throws Exception {
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_4));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_3));
        Statement as = new AssignStatement(0, 0, IDENT_INT_A, IL_1);
        Statement ps3 = new PrintStatement(0, 0, singletonList(IDE_A));
        Statement is = new IfStatement(0, 0, BL_TRUE, asList(ps1, ps2), asList(as, ps3), "10");
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then print 4 print 3 else a% = 1 print a% end if", expectedStatements);
    }

    @Test
    public void shouldParseEmptyThenElseBlock() throws Exception {
        Statement is = new IfStatement(0, 0, BL_TRUE, emptyList(), emptyList());
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("if true then else endif", expectedStatements);
    }

    @Test
    public void shouldParseNestedIfThenElseBlock() throws Exception {
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement ps3 = new PrintStatement(0, 0, singletonList(IL_3));
        Statement ps4 = new PrintStatement(0, 0, singletonList(IL_4));
        Expression ge = new GreaterExpression(0, 0, IL_1, IL_2);
        Expression le = new LessExpression(0, 0, IL_1, IL_2);
        Statement is1 = new IfStatement(0, 0, ge, singletonList(ps1), singletonList(ps2));
        Statement is2 = new IfStatement(0, 0, le, singletonList(ps3), singletonList(ps4));
        Statement is3 = new IfStatement(0, 0, BL_TRUE, singletonList(is1), singletonList(is2));
        List<Statement> expectedStatements = singletonList(is3);

        parseAndAssert(
                "if true then " +
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
                "end if", expectedStatements);
    }

    @Test
    public void shouldParseEmptyElseIfBlock() throws Exception {
        Statement is1 = new IfStatement(0, 0, BL_FALSE, emptyList());
        Statement is2 = new IfStatement(0, 0, BL_TRUE, emptyList(), singletonList(is1));
        List<Statement> expectedStatements = singletonList(is2);

        parseAndAssert("if true then elseif false then endif", expectedStatements);
    }

    @Test
    public void shouldParseElseIfBlock() throws Exception {
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement secondIf = new IfStatement(0, 0, BL_FALSE, singletonList(ps1));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement firstIf = new IfStatement(0, 0, BL_TRUE, singletonList(ps2), singletonList(secondIf));
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if true then print 2 elseif false then print 1 endif", expectedStatements);
    }

    @Test
    public void shouldParseElseIfElseBlock() throws Exception {
        Statement ps4 = new PrintStatement(0, 0, singletonList(IL_4));
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement secondIf = new IfStatement(0, 0, BL_FALSE, singletonList(ps1), singletonList(ps4));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement firstIf = new IfStatement(0, 0, BL_TRUE, singletonList(ps2), singletonList(secondIf));
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if true then print 2 elseif false then print 1 else print 4 endif", expectedStatements);
    }

    @Test
    public void shouldParseElseIfElseIfBlock() throws Exception {
        Statement ps4 = new PrintStatement(0, 0, singletonList(IL_4));
        Statement thirdIf = new IfStatement(0, 0, BL_TRUE, singletonList(ps4));
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement secondIf = new IfStatement(0, 0, BL_FALSE, singletonList(ps1), singletonList(thirdIf));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement firstIf = new IfStatement(0, 0, BL_TRUE, singletonList(ps2), singletonList(secondIf));
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if true then print 2 elseif false then print 1 elseif true then print 4 endif", expectedStatements);
    }

    @Test
    public void shouldParseElseIfElseIfElseBlock() throws Exception {
        Statement ps4 = new PrintStatement(0, 0, singletonList(IL_4));
        Statement ps3 = new PrintStatement(0, 0, singletonList(IL_3));
        Expression ee3 = new EqualExpression(0, 0, IDE_U, IL_3);
        Statement thirdIf = new IfStatement(0, 0, ee3, singletonList(ps3), singletonList(ps4));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Expression ee2 = new EqualExpression(0, 0, IDE_U, IL_2);
        Statement secondIf = new IfStatement(0, 0, ee2, singletonList(ps2), singletonList(thirdIf));
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Expression ee1 = new EqualExpression(0, 0, IDE_U, IL_1);
        Statement firstIf = new IfStatement(0, 0, ee1, singletonList(ps1), singletonList(secondIf));
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if u = 1 then " +
                       "  print 1 " +
                       "elseif u = 2 then " +
                       "  print 2 " +
                       "elseif u = 3 then " +
                       "  print 3 " +
                       "else " +
                       "  print 4 " +
                       "endif", expectedStatements);
    }

    @Test
    public void shouldParseNestedElseIfBlock() throws Exception {
        Statement ps3 = new PrintStatement(0, 0, singletonList(IL_3));
        Statement fourthIf = new IfStatement(0, 0, BL_FALSE, singletonList(ps3));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement thirdIf = new IfStatement(0, 0, BL_TRUE, singletonList(ps2), singletonList(fourthIf));
        Expression ee2 = new EqualExpression(0, 0, IDE_U, IL_2);
        Statement secondIf = new IfStatement(0, 0, ee2, singletonList(thirdIf));
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Expression ee1 = new EqualExpression(0, 0, IDE_U, IL_1);
        Statement firstIf = new IfStatement(0, 0, ee1, singletonList(ps1), singletonList(secondIf));
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if u = 1 then " +
                       "  print 1 " +
                       "elseif u = 2 then " +
                       "  if true then " +
                       "    print 2 " +
                       "  elseif false then " +
                       "    print 3 " +
                       "  endif " + 
                       "endif", expectedStatements);
    }

    // Negative tests:
    
    @Test(expected = IllegalStateException.class)
    public void shouldNotParseMissingThen() throws Exception {
        parse("10 if true " +
              "20   print 1" +
              "30 end if");
    }
}
