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
    public void shouldParseIfGotoNum() {
        Statement gs = new GotoStatement(0, 0, "20");
        Statement is = IfStatement.builder(IL_5, gs).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 5 goto 20", expectedStatements);
    }

    @Test
    public void shouldParseIfThenNum() {
        Statement gs = new GotoStatement(0, 0, "100");
        Statement is = IfStatement.builder(BL_TRUE, gs).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then 100", expectedStatements);
    }

    @Test
    public void shouldParseIfThenGoto() {
        Statement gs = new GotoStatement(0, 0, "100");
        Statement is = IfStatement.builder(BL_TRUE, gs).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then goto 100", expectedStatements);
    }

    @Test
    public void shouldParseIfThenPrint() {
        Statement ps = new PrintStatement(0, 0, singletonList(IL_10));
        Statement is = IfStatement.builder(BL_FALSE, ps).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if false then print 10", expectedStatements);
    }

    @Test
    public void shouldParseIfThenAssign() {
        Statement as = new AssignStatement(0, 0, IDENT_INT_A, IL_4);
        Statement is = IfStatement.builder(BL_FALSE, as).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if false then a% = 4", expectedStatements);
    }

    @Test
    public void shouldParseIfThenMultiple() {
        Statement as = new AssignStatement(0, 0, IDENT_INT_A, IL_4);
        Statement ps = new PrintStatement(0, 0, singletonList(IDE_A));
        Statement gs = new GotoStatement(0, 0, "10");
        Statement is = IfStatement.builder(BL_FALSE, asList(as, ps, gs)).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if false then a% = 4 : print a% : goto 10", expectedStatements);
    }

    @Test
    public void shouldParseNestedIfThen() {
        Expression ee = new EqualExpression(0, 0, IL_1, IL_2);
        Statement ps = new PrintStatement(0, 0, singletonList(IL_5));
        Statement is1 = IfStatement.builder(BL_TRUE, ps).build();
        Statement is2 = IfStatement.builder(BL_FALSE, is1).build();
        Statement is3 = IfStatement.builder(ee, is2).label("10").build();
        List<Statement> expectedStatements = singletonList(is3);

        parseAndAssert("10 if 1 = 2 then if false then if true then print 5", expectedStatements);
    }

    @Test
    public void shouldParseIfGotoNumElseNum() {
        Statement gs1 = new GotoStatement(0, 0, "20");
        Statement gs2 = new GotoStatement(0, 0, "30");
        Statement is = IfStatement.builder(IL_10, gs1).elseStatements(gs2).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 10 goto 20 else 30", expectedStatements);
    }

    @Test
    public void shouldParseIfThenNumElseNum() {
        Statement gs1 = new GotoStatement(0, 0, "20");
        Statement gs2 = new GotoStatement(0, 0, "30");
        Statement is = IfStatement.builder(IL_10, gs1).elseStatements(gs2).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 10 then 20 else 30", expectedStatements);
    }

    @Test
    public void shouldParseIfThenNumElseGoto() {
        Statement gs1 = new GotoStatement(0, 0, "20");
        Statement gs2 = new GotoStatement(0, 0, "30");
        Statement is = IfStatement.builder(IL_10, gs1).elseStatements(gs2).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 10 then 20 else goto 30", expectedStatements);
    }

    @Test
    public void shouldParseIfThenNumElsePrint() {
        Statement gs = new GotoStatement(0, 0, "20");
        Statement ps = new PrintStatement(0, 0, singletonList(IL_4));
        Statement is = IfStatement.builder(IL_10, gs).elseStatements(ps).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if 10 then 20 else print 4", expectedStatements);
    }

    @Test
    public void shouldParseIfThenMultipleElseMultiple() {
        Statement as1 = new AssignStatement(0, 0, IDENT_INT_A, IL_4);
        Statement ps1 = new PrintStatement(0, 0, singletonList(IDE_A));
        Statement gs1 = new GotoStatement(0, 0, "10");
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement gs2 = new GotoStatement(0, 0, "20");
        Statement is = IfStatement.builder(BL_FALSE, asList(as1, ps1, gs1)).elseStatements(asList(ps2, gs2)).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if false then a% = 4 : print a% : goto 10 else print 2 : goto 20", expectedStatements);
    }

    @Test
    public void shouldParseIfThenBlock() {
        Statement ps = new PrintStatement(0, 0, singletonList(IL_4));
        Statement is = IfStatement.builder(BL_TRUE, ps).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then print 4 end if", expectedStatements);
    }

    @Test
    public void shouldParseEmptyThenBlock() {
        Statement is = IfStatement.builder(BL_TRUE, emptyList()).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then end if", expectedStatements);
    }

    @Test
    public void shouldParseIfThenElseBlock() {
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_4));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_3));
        Statement as = new AssignStatement(0, 0, IDENT_INT_A, IL_1);
        Statement ps3 = new PrintStatement(0, 0, singletonList(IDE_A));
        Statement is = IfStatement.builder(BL_TRUE, asList(ps1, ps2)).elseStatements(asList(as, ps3)).label("10").build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("10 if true then print 4 print 3 else a% = 1 print a% end if", expectedStatements);
    }

    @Test
    public void shouldParseEmptyThenElseBlock() {
        Statement is = IfStatement.builder(BL_TRUE, emptyList()).elseStatements(emptyList()).build();
        List<Statement> expectedStatements = singletonList(is);

        parseAndAssert("if true then else endif", expectedStatements);
    }

    @Test
    public void shouldParseNestedIfThenElseBlock() {
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement ps3 = new PrintStatement(0, 0, singletonList(IL_3));
        Statement ps4 = new PrintStatement(0, 0, singletonList(IL_4));
        Expression ge = new GreaterExpression(0, 0, IL_1, IL_2);
        Expression le = new LessExpression(0, 0, IL_1, IL_2);
        Statement is1 = IfStatement.builder(ge, ps1).elseStatements(ps2).build();
        Statement is2 = IfStatement.builder(le, ps3).elseStatements(ps4).build();
        Statement is3 = IfStatement.builder(BL_TRUE, is1).elseStatements(is2).build();
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
    public void shouldParseEmptyElseIfBlock() {
        Statement is1 = IfStatement.builder(BL_FALSE, emptyList()).build();
        Statement is2 = IfStatement.builder(BL_TRUE, emptyList()).elseStatements(is1).build();
        List<Statement> expectedStatements = singletonList(is2);

        parseAndAssert("if true then elseif false then endif", expectedStatements);
    }

    @Test
    public void shouldParseElseIfBlock() {
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement secondIf = IfStatement.builder(BL_FALSE, ps1).build();
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement firstIf = IfStatement.builder(BL_TRUE, ps2).elseStatements(secondIf).build();
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if true then print 2 elseif false then print 1 endif", expectedStatements);
    }

    @Test
    public void shouldParseElseIfElseBlock() {
        Statement ps4 = new PrintStatement(0, 0, singletonList(IL_4));
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement secondIf = IfStatement.builder(BL_FALSE, ps1).elseStatements(ps4).build();
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement firstIf = IfStatement.builder(BL_TRUE, ps2).elseStatements(secondIf).build();
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if true then print 2 elseif false then print 1 else print 4 endif", expectedStatements);
    }

    @Test
    public void shouldParseElseIfElseIfBlock() {
        Statement ps4 = new PrintStatement(0, 0, singletonList(IL_4));
        Statement thirdIf = IfStatement.builder(BL_TRUE, ps4).build();
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement secondIf = IfStatement.builder(BL_FALSE, ps1).elseStatements(thirdIf).build();
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement firstIf = IfStatement.builder(BL_TRUE, ps2).elseStatements(secondIf).build();
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if true then print 2 elseif false then print 1 elseif true then print 4 endif", expectedStatements);
    }

    @Test
    public void shouldParseElseIfElseIfElseBlock() {
        Statement ps4 = new PrintStatement(0, 0, singletonList(IL_4));
        Statement ps3 = new PrintStatement(0, 0, singletonList(IL_3));
        Expression ee3 = new EqualExpression(0, 0, IDE_A, IL_3);
        Statement thirdIf = IfStatement.builder(ee3, ps3).elseStatements(ps4).build();
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Expression ee2 = new EqualExpression(0, 0, IDE_A, IL_2);
        Statement secondIf = IfStatement.builder(ee2, ps2).elseStatements(thirdIf).build();
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Expression ee1 = new EqualExpression(0, 0, IDE_A, IL_1);
        Statement firstIf = IfStatement.builder(ee1, ps1).elseStatements(secondIf).build();
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if a% = 1 then " +
                       "  print 1 " +
                       "elseif a% = 2 then " +
                       "  print 2 " +
                       "elseif a% = 3 then " +
                       "  print 3 " +
                       "else " +
                       "  print 4 " +
                       "endif", expectedStatements);
    }

    @Test
    public void shouldParseNestedElseIfBlock() {
        Statement ps3 = new PrintStatement(0, 0, singletonList(IL_3));
        Statement fourthIf = IfStatement.builder(BL_FALSE, ps3).build();
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement thirdIf = IfStatement.builder(BL_TRUE, ps2).elseStatements(fourthIf).build();
        Expression ee2 = new EqualExpression(0, 0, IDE_A, IL_2);
        Statement secondIf = IfStatement.builder(ee2, thirdIf).build();
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Expression ee1 = new EqualExpression(0, 0, IDE_A, IL_1);
        Statement firstIf = IfStatement.builder(ee1, ps1).elseStatements(secondIf).build();
        List<Statement> expectedStatements = singletonList(firstIf);

        parseAndAssert("if a% = 1 then " +
                       "  print 1 " +
                       "elseif a% = 2 then " +
                       "  if true then " +
                       "    print 2 " +
                       "  elseif false then " +
                       "    print 3 " +
                       "  endif " + 
                       "endif", expectedStatements);
    }

    // Negative tests:
    
    @Test(expected = IllegalStateException.class)
    public void shouldNotParseMissingThen() {
        parse("10 if true " +
              "20   print 1" +
              "30 end if");
    }
}
