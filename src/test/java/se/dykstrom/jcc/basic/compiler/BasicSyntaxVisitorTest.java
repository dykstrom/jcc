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

import org.junit.Ignore;
import org.junit.Test;
import se.dykstrom.jcc.basic.ast.EndStatement;
import se.dykstrom.jcc.basic.ast.OnGotoStatement;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.common.ast.*;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

public class BasicSyntaxVisitorTest extends AbstractBasicSyntaxVisitorTest {

    @Test
    public void testEmptyProgram() {
        List<Statement> expectedStatements = emptyList();

        parseAndAssert("", expectedStatements);
    }

    @Test
    public void testGoto() {
        Statement gs = new GotoStatement(0, 0, "20", "10");
        List<Statement> expectedStatements = singletonList(gs);

        parseAndAssert("10 goto 20", expectedStatements);
    }

    @Test
    public void testOnGotoOneLabel() {
        Statement os = new OnGotoStatement(0, 0, IDE_A, singletonList("20"), "10");
        List<Statement> expectedStatements = singletonList(os);

        parseAndAssert("10 on a% goto 20", expectedStatements);
    }

    @Test
    public void testOnGotoMultipleLabels() {
        Statement os = new OnGotoStatement(0, 0, IDE_A, asList("20", "30", "40"), "10");
        List<Statement> expectedStatements = singletonList(os);

        parseAndAssert("10 on a% goto 20, 30, 40", expectedStatements);
    }

    @Test
    public void testEnd() {
        Statement es = new EndStatement(0, 0, "10");
        List<Statement> expectedStatements = singletonList(es);

        parseAndAssert("10 end", expectedStatements);
    }

    @Test
    public void testIntAssignment() {
        Statement as = new AssignStatement(0, 0, IDENT_INT_A, IL_3);
        List<Statement> expectedStatements = singletonList(as);

        parseAndAssert("10 let a% = 3", expectedStatements); // With LET
        parseAndAssert("10 a% = 3", expectedStatements); // Without LET
        parseAndAssert("10 a% = &H3", expectedStatements); // With hexadecimal
        parseAndAssert("10 a% = &O3", expectedStatements); // With octal
        parseAndAssert("10 a% = &B11", expectedStatements); // With binary
    }

    @Test
    public void testStringAssignment() {
        Statement as = new AssignStatement(0, 0, IDENT_STR_S, SL_A);
        List<Statement> expectedStatements = singletonList(as);

        parseAndAssert("10 let s$ = \"A\"", expectedStatements); // With LET
        parseAndAssert("10 s$ = \"A\"", expectedStatements); // Without LET
    }

    @Test
    public void testUnknownAssignment() {
        Statement as = new AssignStatement(0, 0, IDENT_UNK_U, SL_A);
        List<Statement> expectedStatements = singletonList(as);

        parseAndAssert("10 let u = \"A\"", expectedStatements); // With LET
        parseAndAssert("10 u = \"A\"", expectedStatements); // Without LET
    }

    @Test
    public void testTwoAssignments() {
        Statement as1 = new AssignStatement(0, 0, IDENT_INT_A, IL_3);
        Statement as2 = new AssignStatement(0, 0, IDENT_INT_B, IL_5);
        List<Statement> expectedStatements = asList(as1, as2);

        parseAndAssert("10 let a% = 3 : b% = 5", expectedStatements);
    }

    @Test
    public void testIntDereference() {
        Statement as = new AssignStatement(0, 0, IDENT_INT_B, IDE_A);
        List<Statement> expectedStatements = singletonList(as);

        parseAndAssert("10 let b% = a%", expectedStatements);
    }

    @Test
    public void testStringDereference() {
        Statement ps = new PrintStatement(0, 0, singletonList(IDE_S), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print s$", expectedStatements);
    }

    @Test
    public void testTwoDereferences() {
        Statement ps = new PrintStatement(0, 0, asList(IDE_A, IL_10, IDE_S), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print a%; 10; s$", expectedStatements);
    }

    @Test
    public void testTwoDereferenceInExpression() {
        Expression ae = new AddExpression(0, 0, IDE_A, IDE_B);
        Statement as = new AssignStatement(0, 0, IDENT_UNK_U, ae);
        List<Statement> expectedStatements = singletonList(as);

        parseAndAssert("10 let u = a% + b%", expectedStatements);
    }

    @Test
    public void testRem() {
        Statement cs = new CommentStatement(0, 0, "10");
        List<Statement> expectedStatements = singletonList(cs);

        parseAndAssert("10 rem", expectedStatements);
    }

    @Test
    public void testRemWithComment() {
        Statement cs = new CommentStatement(0, 0, "10");
        List<Statement> expectedStatements = singletonList(cs);

        parseAndAssert("10 rem foo", expectedStatements);
    }

    @Test
    public void testApostropheWithComment() {
        Statement cs = new CommentStatement(0, 0, "10");
        List<Statement> expectedStatements = singletonList(cs);

        parseAndAssert("10 'foo", expectedStatements);
    }

    @Test
    public void testPrintAndRem() {
        Statement ps = new PrintStatement(0, 0, singletonList(IL_1), "10");
        Statement cs = new CommentStatement(0, 0, null);
        List<Statement> expectedStatements = asList(ps, cs);

        parseAndAssert("10 PRINT 1 : REM PRINT 1", expectedStatements);
    }

    @Test
    public void testTwoGotosOneLine() {
        Statement gs0 = new GotoStatement(0, 0, "20", "10");
        Statement gs1 = new GotoStatement(0, 0, "10");
        List<Statement> expectedStatements = asList(gs0, gs1);

        parseAndAssert("10 goto 20 : goto 10", expectedStatements);
    }

    @Test
    public void testTwoGotosTwoLines() {
        Statement gs0 = new GotoStatement(0, 0, "20", "10");
        Statement gs1 = new GotoStatement(0, 0, "10", "20");
        List<Statement> expectedStatements = asList(gs0, gs1);

        parseAndAssert("10 goto 20" + EOL + "20 goto 10", expectedStatements);
    }

    @Test
    public void testPrintWithoutExpression() {
        List<Expression> expressions = Collections.emptyList();
        Statement ps = new PrintStatement(0, 0, expressions, "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print", expectedStatements);
    }

    @Test
    public void testString() {
        testPrintOneExpression("\"A\"", SL_A);
    }

    @Test
    public void testInteger() {
        testPrintOneExpression("3", IL_3);
    }

    @Test
    public void testHexadecimalInteger() {
        testPrintOneExpression("&HFF", IL_255);
    }

    @Test
    public void testOctalInteger() {
        testPrintOneExpression("&O12", IL_10);
    }

    @Test
    public void testBinaryInteger() {
        testPrintOneExpression("&B1010", IL_10);
    }

    @Test
    public void testNegativeInteger() {
        testPrintOneExpression("-3", IL_NEG_3);
    }

    @Test
    public void testFloats() {
        testPrintOneExpression("1.2", FL_1_2);
        testPrintOneExpression(".3", FL_0_3);
        testPrintOneExpression("0.3", FL_0_3);
        testPrintOneExpression("17.", new FloatLiteral(0, 0, "17.0"));
        testPrintOneExpression("-17.5", new FloatLiteral(0, 0, "-17.5"));

        testPrintOneExpression("7.5e+10", FL_7_5_EXP);
        testPrintOneExpression("7.5d10", FL_7_5_EXP);
        testPrintOneExpression("7.5D+10", FL_7_5_EXP);
        testPrintOneExpression("7.5E10", FL_7_5_EXP);

        testPrintOneExpression("1.2#", FL_1_2);
        testPrintOneExpression(".3#", FL_0_3);
        testPrintOneExpression("7.5e10#", FL_7_5_EXP);
    }

    @Test
    public void testNegativeDereference() {
        testPrintOneExpression("-a%", new SubExpression(0, 0, IL_0, IDE_A));
    }

    @Test
    public void testNegativeSubExpr() {
        testPrintOneExpression("-(1+a%)", new SubExpression(0, 0, IL_0, new AddExpression(0, 0, IL_1, IDE_A)));
    }

    @Test
    public void testNegativeHexadecimalExpr() {
        testPrintOneExpression("-(&HFF + -&H3)", new SubExpression(0, 0, IL_0, new AddExpression(0, 0, IL_255, IL_NEG_3)));
    }

    @Test
    public void testAdd() {
        testPrintOneExpression("3 + 4", new AddExpression(0, 0, IL_3, IL_4));
    }

    @Test
    public void testAddWithHexdecimal() {
        testPrintOneExpression("3 + &H04", new AddExpression(0, 0, IL_3, IL_4));
    }

    @Test
    public void testAddWithOctal() {
        testPrintOneExpression("3 + &H03", new AddExpression(0, 0, IL_3, IL_3));
    }

    @Test
    public void testSub() {
        testPrintOneExpression("1-4", new SubExpression(0, 0, IL_1, IL_4));
    }

    @Test
    public void testMul() {
        testPrintOneExpression("1*2", new MulExpression(0, 0, IL_1, IL_2));
    }

    @Test
    public void testMulWithBinary() {
        testPrintOneExpression("1*&B10", new MulExpression(0, 0, IL_1, IL_2));
    }

    @Test
    public void testMulWithFloat() {
        testPrintOneExpression("1*.3", new MulExpression(0, 0, IL_1, FL_0_3));
    }

    @Test
    public void testDiv() {
        testPrintOneExpression("10/5", new DivExpression(0, 0, IL_10, IL_5));
    }

    @Test
    public void testIDiv() {
        testPrintOneExpression("10\\5", new IDivExpression(0, 0, IL_10, IL_5));
    }

    @Test
    public void testMod() {
        testPrintOneExpression("10 MOD 5", new ModExpression(0, 0, IL_10, IL_5));
    }

    @Test
    public void testAddAndSub() {
        Expression ae = new AddExpression(0, 0, IL_1, IL_2);
        Expression se = new SubExpression(0, 0, ae, IL_3);
        testPrintOneExpression("1 + 2 - 3", se);
    }

    @Test
    public void testAddAndMul() {
        Expression me = new MulExpression(0, 0, IL_10, IL_2);
        Expression ae = new AddExpression(0, 0, IL_5, me);
        testPrintOneExpression("5 + 10 * 2", ae);
    }

    @Test
    public void testAddAndMulWitFloat() {
        Expression me = new MulExpression(0, 0, IL_10, IL_2);
        Expression ae = new AddExpression(0, 0, FL_7_5_EXP, me);
        testPrintOneExpression("7.5E10 + 10 * 2", ae);
    }

    @Test
    public void testAddAndMod() {
        Expression me = new ModExpression(0, 0, IL_10, IL_2);
        Expression ae = new AddExpression(0, 0, IL_5, me);
        testPrintOneExpression("5 + 10 MOD 2", ae);
    }

    @Test
    public void testMulAndIDiv() {
        Expression me = new MulExpression(0, 0, IL_5, IL_10);
        Expression ie = new IDivExpression(0, 0, me, IL_2);
        testPrintOneExpression("5 * 10 \\ 2", ie);
    }

    @Test
    public void testAddAndMulWithPar() {
        Expression ae = new AddExpression(0, 0, IL_5, IL_10);
        Expression me = new MulExpression(0, 0, ae, IL_2);
        testPrintOneExpression("(5 + 10) * 2", me);
    }

    @Test
    public void testMulAndAddWithPar() {
        Expression ae = new AddExpression(0, 0, IL_5, IL_10);
        Expression me = new MulExpression(0, 0, IL_2, ae);
        testPrintOneExpression("2 * (5 + 10)", me);
    }

    @Test
    public void testAddAndMulAndSubWithPar() {
        Expression ae = new AddExpression(0, 0, IL_5, IL_10);
        Expression se = new SubExpression(0, 0, IL_1, IL_2);
        Expression me = new MulExpression(0, 0, ae, se);
        testPrintOneExpression("(5 + 10) * (1 - 2)", me);
    }

    @Test
    public void testOnePrintTwoStrings() {
        List<Expression> expressions = asList(SL_A, SL_B);
        Statement ps = new PrintStatement(0, 0, expressions, "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print \"A\"; \"B\"", expectedStatements);
    }

    @Test
    public void testTwoPrintsOneLine() {
        Statement ps0 = new PrintStatement(0, 0, singletonList(SL_A), "10");
        Statement ps1 = new PrintStatement(0, 0, singletonList(SL_B));
        List<Statement> expectedStatements = asList(ps0, ps1);

        parseAndAssert("10 print \"A\" : print \"B\"", expectedStatements);
    }

    @Test
    public void testTwoPrintsTwoLines() {
        Statement ps0 = new PrintStatement(0, 0, singletonList(SL_A), "10");
        Statement ps1 = new PrintStatement(0, 0, singletonList(SL_B), "20");
        List<Statement> expectedStatements = asList(ps0, ps1);

        parseAndAssert("10 print \"A\"" + EOL + "20 print \"B\"", expectedStatements);
    }

    @Test
    public void testPrintAndGotoOneLine() {
        Statement ps = new PrintStatement(0, 0, singletonList(SL_A), "10");
        Statement gs = new GotoStatement(0, 0, "10");
        List<Statement> expectedStatements = asList(ps, gs);

        parseAndAssert("10 print \"A\":goto 10", expectedStatements);
    }

    @Test
    public void testPrintAndGotoTwoLines() {
        Statement ps = new PrintStatement(0, 0, singletonList(SL_A), "10");
        Statement gs = new GotoStatement(0, 0, "10", "20");
        List<Statement> expectedStatements = asList(ps, gs);

        parseAndAssert("10 print \"A\"" + EOL + "20 goto 10", expectedStatements);
    }

    @Test
    public void testMultiplePrintAndGotos() {
        Statement gs10 = new GotoStatement(0, 0, "40", "10");
        Statement ps20 = new PrintStatement(0, 0, singletonList(SL_A), "20");
        Statement gs30 = new GotoStatement(0, 0, "60", "30");
        Statement ps40 = new PrintStatement(0, 0, singletonList(SL_B), "40");
        Statement gs50 = new GotoStatement(0, 0, "20", "50");
        Statement ps60 = new PrintStatement(0, 0, singletonList(SL_C), "60");
        List<Statement> expectedStatements = asList(gs10, ps20, gs30, ps40, gs50, ps60);

        parseAndAssert("10 goto 40" + EOL
                + "20 print \"A\"" + EOL
                + "30 goto 60" + EOL
                + "40 print \"B\"" + EOL
                + "50 goto 20" + EOL
                + "60 print \"C\"", expectedStatements);
    }

    @Test
    public void testTrueAndFalse() {
        List<Expression> expressions = asList(BL_TRUE, BL_FALSE);
        Statement ps = new PrintStatement(0, 0, expressions, "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print true; false", expectedStatements);
    }

    @Test
    public void testAssignBoolean() {
        Expression ee = new EqualExpression(0, 0, IL_5, IL_10);
        Statement as = new AssignStatement(0, 0, IDENT_UNK_U, ee);
        List<Statement> expectedStatements = singletonList(as);

        parseAndAssert("30 let u = 5 = 10", expectedStatements);
    }

    @Test
    public void testEqual() {
        testPrintOneExpression("1 = 4", new EqualExpression(0, 0, IL_1, IL_4));
    }

    @Test
    public void testNotEqual() {
        testPrintOneExpression("1 <> 4", new NotEqualExpression(0, 0, IL_1, IL_4));
    }

    @Test
    public void testGreaterThan() {
        testPrintOneExpression("1 > 4", new GreaterExpression(0, 0, IL_1, IL_4));
    }

    @Test
    public void testGreaterThanOrEqual() {
        testPrintOneExpression("1 >= 4", new GreaterOrEqualExpression(0, 0, IL_1, IL_4));
    }

    @Test
    public void testLessThan() {
        testPrintOneExpression("1 < 4", new LessExpression(0, 0, IL_1, IL_4));
    }

    @Test
    public void testLessThanOrEqual() {
        testPrintOneExpression("1 <= 4", new LessOrEqualExpression(0, 0, IL_1, IL_4));
    }

    @Test
    public void testEqualStrings() {
        testPrintOneExpression("\"A\" = \"B\"", new EqualExpression(0, 0, SL_A, SL_B));
    }

    @Test
    public void testNotEqualStrings() {
        testPrintOneExpression("\"A\" <> \"C\"", new NotEqualExpression(0, 0, SL_A, SL_C));
    }

    @Test
    public void testAnd() {
        Expression e1 = new EqualExpression(0, 0, IL_1, IL_1);
        Expression e2 = new EqualExpression(0, 0, IL_1, IL_2);
        testPrintOneExpression("1 = 1 AND 1 = 2", new AndExpression(0, 0, e1, e2));
    }

    @Test
    public void testOr() {
        Expression e1 = new EqualExpression(0, 0, IL_1, IL_1);
        Expression e2 = new EqualExpression(0, 0, IL_1, IL_2);
        testPrintOneExpression("1 = 1 OR 1 = 2", new OrExpression(0, 0, e1, e2));
    }

    @Test
    public void testXor() {
        Expression e1 = new EqualExpression(0, 0, IL_1, IL_1);
        Expression e2 = new EqualExpression(0, 0, IL_1, IL_2);
        testPrintOneExpression("1 = 1 XOR 1 = 2", new XorExpression(0, 0, e1, e2));
    }

    @Test
    public void testNot() {
        Expression e1 = new EqualExpression(0, 0, IL_1, IL_1);
        testPrintOneExpression("NOT 1 = 1", new NotExpression(0, 0, e1));
    }

    @Test
    public void testOrXor() {
        Expression e1 = new EqualExpression(0, 0, IL_1, IL_1);
        Expression e2 = new EqualExpression(0, 0, IL_1, IL_2);
        Expression e3 = new NotEqualExpression(0, 0, IL_1, IL_3);
        testPrintOneExpression("1 = 1 OR 1 = 2 XOR 1 <> 3", new XorExpression(0, 0, new OrExpression(0, 0, e1, e2), e3));
    }

    @Test
    public void testOrAnd() {
        Expression e1 = new EqualExpression(0, 0, SL_A, SL_B);
        Expression e2 = new EqualExpression(0, 0, IL_1, IL_2);
        Expression e3 = new NotEqualExpression(0, 0, IL_1, IL_3);
        testPrintOneExpression("\"A\" = \"B\" OR 1 = 2 AND 1 <> 3", new OrExpression(0, 0, e1, new AndExpression(0, 0, e2, e3)));
    }

    @Test
    public void testNotAnd() {
        Expression e1 = new EqualExpression(0, 0, IL_1, IL_2);
        Expression e2 = new NotEqualExpression(0, 0, IL_1, IL_3);
        testPrintOneExpression("NOT 1 = 2 AND 1 <> 3", new AndExpression(0, 0, new NotExpression(0, 0, e1), e2));
    }

    @Test
    public void testAndNot() {
        Expression e1 = new EqualExpression(0, 0, IL_1, IL_2);
        Expression e2 = new NotEqualExpression(0, 0, IL_1, IL_3);
        testPrintOneExpression("1 = 2 AND NOT 1 <> 3", new AndExpression(0, 0, e1, new NotExpression(0, 0, e2)));
    }

    @Test
    public void testOrOr() {
        Expression e1 = new EqualExpression(0, 0, IL_1, IL_2);
        Expression e2 = new EqualExpression(0, 0, IL_1, IL_3);
        Expression e3 = new EqualExpression(0, 0, IL_1, IL_1);
        testPrintOneExpression("1 = 2 OR 1 = 3 OR 1 = 1", new OrExpression(0, 0, new OrExpression(0, 0, e1, e2), e3));
    }

    @Test
    public void testOrAndWithPar() {
        Expression e1 = new EqualExpression(0, 0, IL_1, IL_1);
        Expression e2 = new EqualExpression(0, 0, IL_1, IL_2);
        Expression e3 = new NotEqualExpression(0, 0, IL_1, IL_3);
        testPrintOneExpression("(1 = 1 OR 1 = 2) AND 1 <> 3", new AndExpression(0, 0, new OrExpression(0, 0, e1, e2), e3));
    }

    @Test
    public void testMultipleOrAndAnd() {
        Expression le = new LessExpression(0, 0, IDE_U, IL_1);
        Expression ge = new GreaterExpression(0, 0, IDE_U, IL_4);
        Expression oe1 = new OrExpression(0, 0, le, ge);
        Expression ae1 = new AndExpression(0, 0, BL_TRUE, oe1);
        Expression ee = new EqualExpression(0, 0, IL_2, IL_2);
        Expression ae2 = new AndExpression(0, 0, ee, BL_FALSE);
        Expression oe2 = new OrExpression(0, 0, ae1, ae2);
        testPrintOneExpression("true AND (u < 1 OR u > 4) OR (2 = 2 AND false)", oe2);
    }

    @Test
    public void testMultipleAnd() {
        Expression le = new LessExpression(0, 0, IDE_U, IL_1);
        Expression ge = new GreaterExpression(0, 0, IDE_U, IL_4);
        Expression ae1 = new AndExpression(0, 0, BL_TRUE, le);
        Expression ae2 = new AndExpression(0, 0, ae1, ge);
        Expression ae3 = new AndExpression(0, 0, ae2, BL_FALSE);
        testPrintOneExpression("true AND u < 1 AND u > 4 AND false", ae3);
    }

    @Test
    public void testMultipleOr() {
        Expression le = new LessExpression(0, 0, IDE_U, IL_1);
        Expression ge = new GreaterExpression(0, 0, IDE_U, IL_4);
        Expression oe1 = new OrExpression(0, 0, BL_TRUE, le);
        Expression oe2 = new OrExpression(0, 0, oe1, ge);
        Expression oe3 = new OrExpression(0, 0, oe2, BL_FALSE);
        testPrintOneExpression("true OR u < 1 OR u > 4 OR false", oe3);
    }

    @Test
    public void testMixedExpressions() {
        Expression ae1 = new AddExpression(0, 0, IDE_U, IL_1);
        Expression ae2 = new AddExpression(0, 0, IDE_U, IL_2);
        Expression ge = new GreaterExpression(0, 0, ae1, ae2);
        Expression se = new SubExpression(0, 0, IL_5, FL_1_2);
        Expression nee = new NotEqualExpression(0, 0, se, IL_1);
        Expression ande = new AndExpression(0, 0, ge, nee);
        testPrintOneExpression("u + 1 > u + 2 and 5 - 1.2 <> 1", ande);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoStatementAfterColon() {
        parse("10 print :");
    }

    @Ignore("issue #1949 in ANTLR 4.7")
    @Test(expected = IllegalStateException.class)
    public void testNoClosingQuotationMark() {
        parse("10 print \"Hello!");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoTermAfterPlus() {
        parse("10 print 5 +");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoFactorAfterMul() {
        parse("10 print 5 *");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoExpressionInAssignment() {
        parse("10 cool =");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoEqualsInAssignment() {
        parse("10 let a 5");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoExpressionAfterOr() {
        parse("10 print true or");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoExpressionAfterAnd() {
        parse("10 PRINT FALSE AND");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoExpressionBeforeAnd() {
        parse("10 PRINT AND FALSE");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoExpressionBetweenAnds() {
        parse("10 PRINT TRUE AND AND FALSE");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoRelationalOperator() {
        parse("10 print 5 6");
    }
}
