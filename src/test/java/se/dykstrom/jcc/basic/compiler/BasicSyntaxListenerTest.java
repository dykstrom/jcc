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

package se.dykstrom.jcc.basic.compiler;

import org.antlr.v4.runtime.*;
import org.junit.Test;
import se.dykstrom.jcc.basic.ast.EndStatement;
import se.dykstrom.jcc.basic.ast.GotoStatement;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.basic.ast.RemStatement;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Unknown;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

public class BasicSyntaxListenerTest {

    private static final Identifier IDENT_INT_A = new Identifier("a%", I64.INSTANCE);
    private static final Identifier IDENT_INT_B = new Identifier("b%", I64.INSTANCE);
    private static final Identifier IDENT_STR_S = new Identifier("s$", Str.INSTANCE);
    private static final Identifier IDENT_UNK_U = new Identifier("u", Unknown.INSTANCE);

    private static final IntegerLiteral IL_1 = new IntegerLiteral(0, 0, "1");
    private static final IntegerLiteral IL_2 = new IntegerLiteral(0, 0, "2");
    private static final IntegerLiteral IL_3 = new IntegerLiteral(0, 0, "3");
    private static final IntegerLiteral IL_4 = new IntegerLiteral(0, 0, "4");
    private static final IntegerLiteral IL_5 = new IntegerLiteral(0, 0, "5");
    private static final IntegerLiteral IL_10 = new IntegerLiteral(0, 0, "10");

    private static final StringLiteral SL_A = new StringLiteral(0, 0, "A");
    private static final StringLiteral SL_B = new StringLiteral(0, 0, "B");
    private static final StringLiteral SL_C = new StringLiteral(0, 0, "C");

    @Test
    public void testOneGoto() throws Exception {
        GotoStatement gs = new GotoStatement(0, 0, "20", "10");
        List<Statement> expectedStatements = singletonList(gs);

        parseAndAssert("10 goto 20", expectedStatements);
    }

    @Test
    public void testOneEnd() throws Exception {
        EndStatement es = new EndStatement(0, 0, "10");
        List<Statement> expectedStatements = singletonList(es);

        parseAndAssert("10 end", expectedStatements);
    }

    @Test
    public void testOneIntAssignment() throws Exception {
        AssignStatement as = new AssignStatement(0, 0, IDENT_INT_A, IL_3);
        List<Statement> expectedStatements = singletonList(as);

        parseAndAssert("10 let a% = 3", expectedStatements); // With LET
        parseAndAssert("10 a% = 3", expectedStatements); // Without LET
    }

    @Test
    public void testOneStringAssignment() throws Exception {
        AssignStatement as = new AssignStatement(0, 0, IDENT_STR_S, SL_A);
        List<Statement> expectedStatements = singletonList(as);

        parseAndAssert("10 let s$ = \"A\"", expectedStatements); // With LET
        parseAndAssert("10 s$ = \"A\"", expectedStatements); // Without LET
    }

    @Test
    public void testOneUnknownAssignment() throws Exception {
        AssignStatement as = new AssignStatement(0, 0, IDENT_UNK_U, SL_A);
        List<Statement> expectedStatements = singletonList(as);

        parseAndAssert("10 let u = \"A\"", expectedStatements); // With LET
        parseAndAssert("10 u = \"A\"", expectedStatements); // Without LET
    }

    @Test
    public void testTwoAssignments() throws Exception {
        AssignStatement as1 = new AssignStatement(0, 0, IDENT_INT_A, IL_3);
        AssignStatement as2 = new AssignStatement(0, 0, IDENT_INT_B, IL_5);
        List<Statement> expectedStatements = asList(as1, as2);

        parseAndAssert("10 let a% = 3 : b% = 5", expectedStatements);
    }

    @Test
    public void testOneRem() throws Exception {
        RemStatement rs = new RemStatement(0, 0, "10");
        List<Statement> expectedStatements = singletonList(rs);

        parseAndAssert("10 rem", expectedStatements);
    }

    @Test
    public void testOneRemWithComment() throws Exception {
        RemStatement rs = new RemStatement(0, 0, "10");
        List<Statement> expectedStatements = singletonList(rs);

        parseAndAssert("10 rem foo", expectedStatements);
    }

    @Test
    public void testOneApostropheWithComment() throws Exception {
        RemStatement rs = new RemStatement(0, 0, "10");
        List<Statement> expectedStatements = singletonList(rs);

        parseAndAssert("10 'foo", expectedStatements);
    }

    @Test
    public void testPrintAndRem() throws Exception {
        Statement ps = new PrintStatement(0, 0, singletonList(IL_1), "10");
        Statement rs = new RemStatement(0, 0, null);
        List<Statement> expectedStatements = asList(ps, rs);

        parseAndAssert("10 PRINT 1 : REM PRINT 1", expectedStatements);
    }

    @Test
    public void testTwoGotosOneLine() throws Exception {
        Statement gs0 = new GotoStatement(0, 0, "20", "10");
        Statement gs1 = new GotoStatement(0, 0, "10");
        List<Statement> expectedStatements = asList(gs0, gs1);

        parseAndAssert("10 goto 20 : goto 10", expectedStatements);
    }

    @Test
    public void testTwoGotosTwoLines() throws Exception {
        Statement gs0 = new GotoStatement(0, 0, "20", "10");
        Statement gs1 = new GotoStatement(0, 0, "10", "20");
        List<Statement> expectedStatements = asList(gs0, gs1);

        parseAndAssert("10 goto 20" + EOL + "20 goto 10", expectedStatements);
    }

    @Test
    public void testOnePrintWithoutExpression() throws Exception {
        List<Expression> expressions = Collections.emptyList();
        Statement ps = new PrintStatement(0, 0, expressions, "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print", expectedStatements);
    }

    @Test
    public void testOnePrintOneString() throws Exception {
        List<Expression> expressions = singletonList(SL_A);
        Statement ps = new PrintStatement(0, 0, expressions, "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print \"A\"", expectedStatements);
    }

    @Test
    public void testOnePrintOneInteger() throws Exception {
        List<Expression> expressions = singletonList(IL_3);
        Statement ps = new PrintStatement(0, 0, expressions, "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print 3", expectedStatements);
    }

    @Test
    public void testOnePrintOneAdd() throws Exception {
        Expression e = new AddExpression(0, 0, IL_3, IL_4);
        Statement ps = new PrintStatement(0, 0, singletonList(e), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print 3 + 4", expectedStatements);
    }

    @Test
    public void testOnePrintOneSub() throws Exception {
        Expression e = new SubExpression(0, 0, IL_1, IL_4);
        Statement ps = new PrintStatement(0, 0, singletonList(e), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print 1-4", expectedStatements);
    }

    @Test
    public void testOnePrintOneMul() throws Exception {
        Expression e = new MulExpression(0, 0, IL_1, IL_2);
        Statement ps = new PrintStatement(0, 0, singletonList(e), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print 1*2", expectedStatements);
    }

    @Test
    public void testOnePrintOneDiv() throws Exception {
        Expression e = new DivExpression(0, 0, IL_10, IL_5);
        Statement ps = new PrintStatement(0, 0, singletonList(e), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print 10/5", expectedStatements);
    }

    @Test
    public void testAddAndSub() throws Exception {
        Expression ae = new AddExpression(0, 0, IL_1, IL_2);
        Expression se = new SubExpression(0, 0, ae, IL_3);
        Statement ps = new PrintStatement(0, 0, singletonList(se), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print 1 + 2 - 3", expectedStatements);
    }

    @Test
    public void testAddAndMul() throws Exception {
        Expression me = new MulExpression(0, 0, IL_10, IL_2);
        Expression ae = new AddExpression(0, 0, IL_5, me);
        Statement ps = new PrintStatement(0, 0, singletonList(ae), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print 5 + 10 * 2", expectedStatements);
    }

    @Test
    public void testAddAndMulWithPar() throws Exception {
        Expression ae = new AddExpression(0, 0, IL_5, IL_10);
        Expression me = new MulExpression(0, 0, ae, IL_2);
        Statement ps = new PrintStatement(0, 0, singletonList(me), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print (5 + 10) * 2", expectedStatements);
    }

    @Test
    public void testMulAndAddWithPar() throws Exception {
        Expression ae = new AddExpression(0, 0, IL_5, IL_10);
        Expression me = new MulExpression(0, 0, IL_2, ae);
        Statement ps = new PrintStatement(0, 0, singletonList(me), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print 2 * (5 + 10)", expectedStatements);
    }

    @Test
    public void testAddAndMulAndSubWithPar() throws Exception {
        Expression ae = new AddExpression(0, 0, IL_5, IL_10);
        Expression se = new SubExpression(0, 0, IL_1, IL_2);
        Expression me = new MulExpression(0, 0, ae, se);
        Statement ps = new PrintStatement(0, 0, singletonList(me), "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print (5 + 10) * (1 - 2)", expectedStatements);
    }

    @Test
    public void testOnePrintTwoStrings() throws Exception {
        List<Expression> expressions = asList(SL_A, SL_B);
        Statement ps = new PrintStatement(0, 0, expressions, "10");
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("10 print \"A\"; \"B\"", expectedStatements);
    }

    @Test
    public void testTwoPrintsOneLine() throws Exception {
        Statement ps0 = new PrintStatement(0, 0, singletonList(SL_A), "10");
        Statement ps1 = new PrintStatement(0, 0, singletonList(SL_B));
        List<Statement> expectedStatements = asList(ps0, ps1);

        parseAndAssert("10 print \"A\" : print \"B\"", expectedStatements);
    }

    @Test
    public void testTwoPrintsTwoLines() throws Exception {
        Statement ps0 = new PrintStatement(0, 0, singletonList(SL_A), "10");
        Statement ps1 = new PrintStatement(0, 0, singletonList(SL_B), "20");
        List<Statement> expectedStatements = asList(ps0, ps1);

        parseAndAssert("10 print \"A\"" + EOL + "20 print \"B\"", expectedStatements);
    }

    @Test
    public void testPrintAndGotoOneLine() throws Exception {
        Statement ps = new PrintStatement(0, 0, singletonList(SL_A), "10");
        Statement gs = new GotoStatement(0, 0, "10");
        List<Statement> expectedStatements = asList(ps, gs);

        parseAndAssert("10 print \"A\":goto 10", expectedStatements);
    }

    @Test
    public void testPrintAndGotoTwoLines() throws Exception {
        Statement ps = new PrintStatement(0, 0, singletonList(SL_A), "10");
        Statement gs = new GotoStatement(0, 0, "10", "20");
        List<Statement> expectedStatements = asList(ps, gs);

        parseAndAssert("10 print \"A\"" + EOL + "20 goto 10", expectedStatements);
    }

    @Test
    public void testMultiplePrintAndGotos() throws Exception {
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

    @Test(expected = IllegalStateException.class)
    public void testNoLineNumber() throws Exception {
        parse("goto 10");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoGotoLine() throws Exception {
        parse("10 goto");
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidGotoLine() throws Exception {
        parse("10 goto ten");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoStatementAfterColon() throws Exception {
        parse("10 print :");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoClosingQuotationMark() throws Exception {
        parse("10 print \"Hello!");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoTermAfterPlus() throws Exception {
        parse("10 print 5 +");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoFactorAfterMul() throws Exception {
        parse("10 print 5 *");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoExpressionInAssignment() throws Exception {
        parse("10 cool =");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoEqualsInAssignment() throws Exception {
        parse("10 let a 5");
    }

    private void parseAndAssert(String text, List<Statement> expectedStatements) {
        Program program = parse(text);
        List<Statement> actualStatements = program.getStatements();
        assertEquals(expectedStatements, actualStatements);
    }

    private Program parse(String text) {
        BasicLexer lexer = new BasicLexer(new ANTLRInputStream(text));
        lexer.addErrorListener(ERROR_LISTENER);

        BasicParser parser = new BasicParser(new CommonTokenStream(lexer));
        parser.addErrorListener(ERROR_LISTENER);

        BasicSyntaxListener listener = new BasicSyntaxListener();
        parser.addParseListener(listener);
        parser.program();
        return listener.getProgram();
    }

    private static final BaseErrorListener ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalStateException("Syntax error at " + line + ":" + charPositionInLine + ": " + msg, e);
        }
    };
}
