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
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.error.InvalidException;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static org.junit.Assert.*;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

public class BasicSemanticsParserTest {

    private final BasicSemanticsParser testee = new BasicSemanticsParser();

    @Test
    public void testOnePrint() throws Exception {
        parse("10 print");
    }

    @Test
    public void testOnePrintWithMaxI64() throws Exception {
        parse("10 print 9223372036854775807");
    }

    @Test
    public void testOnePrintWithMinI64() throws Exception {
        parse("10 print -9223372036854775808");
    }

    @Test
    public void testOnePrintWithOneString() throws Exception {
        parse("10 print \"One\"");
    }

    @Test
    public void testOnePrintWithTwoStrings() throws Exception {
        parse("10 print \"One\",\"Two\"");
    }

    @Test
    public void testOnePrintWithOneIntegerExpression() throws Exception {
        parse("10 print 5 + 6");
        parse("20 print 1 - 3");
        parse("30 print 4 * 5");
        parse("40 print 100 / 10");
    }

    @Test
    public void testOnePrintWithTwoIntegerExpressions() throws Exception {
        parse("10 print 5 + 6 + 7");
        parse("20 print 1 - 3 + 3");
        parse("30 print 4 * 5 + 8");
        parse("40 print 1 - 100 / 10");
    }

    @Test
    public void testOnePrintWithComplexExpressions() throws Exception {
        parse("10 print (1 - 100) / (10 + 2)");
        parse("20 print 3 * (100 / 2) + (10 - 2) * (0 + 1 + 2)");
    }

    @Test
    public void testPrintAndGoto() throws Exception {
        parse("10 print \"One\"" + EOL + "20 goto 10");
    }

    @Test
    public void testMultiplePrintAndGotos() throws Exception {
        parse("10 goto 40" + EOL
                + "20 print \"A\"" + EOL
                + "30 goto 60" + EOL
                + "40 print \"B\"" + EOL
                + "50 goto 20" + EOL
                + "60 print \"C\"");
    }

    @Test
    public void testOneAssignment() throws Exception {
        parse("10 let a = 5");
        parse("10 a = 5");
        parse("10 let a% = 5");
        parse("10 a% = 5");
        parse("10 let b$ = \"B\"");
        parse("10 b$ = \"B\"");
    }

    @Test
    public void testOneAssignmentWithDerivedType() throws Exception {
        Program program = parse("10 let a = 5");
        List<Statement> statements = program.getStatements();
        assertEquals(1, statements.size());
        AssignStatement statement = (AssignStatement) statements.get(0);
        assertEquals(new Identifier("a", I64.INSTANCE), statement.getIdentifier());
    }

    @Test
    public void testOneAssignmentWithExpression() throws Exception {
        parse("10 let a = 5 + 2");
        parse("10 let a% = 10 * 10");
        parse("10 let number% = 10 / (10 - 5)");
    }

    @Test
    public void testAssignmentWithInvalidExpression() throws Exception {
        parseAndExpectException("10 let a = \"A\" + 7", "illegal expression");
    }

    @Test
    public void testAssignmentWithTypeError() throws Exception {
        parseAndExpectException("10 let a% = \"A\"", "you cannot assign a value of type string");
        parseAndExpectException("10 let a$ = 0", "you cannot assign a value of type integer");
        parseAndExpectException("10 a$ = 7 * 13", "you cannot assign a value of type integer");
    }

    @Test
    public void testDuplicateLineNumber() throws Exception {
        parseAndExpectException("10 goto 10" + EOL + "10 print", "duplicate line");
    }

    @Test
    public void testUndefinedGotoLine() throws Exception {
        parseAndExpectException("10 goto 20", "undefined line");
    }

    @Test
    public void testNoGotoLine() throws Exception {
        parseAndExpectException("10 goto", "missing NUMBER");
    }

    /**
     * Invalid integer -> overflow.
     */
    @Test(expected = InvalidException.class)
    public void testOverflowI64() throws Exception {
        String value = "9223372036854775808";
        try {
            parse("10 print " + value);
        } catch (IllegalStateException ise) {
            InvalidException ie = (InvalidException) ise.getCause();
            assertEquals(value, ie.getValue());
            throw ie;
        }
    }

    /**
     * Invalid integer -> underflow.
     */
    @Test(expected = InvalidException.class)
    public void testUnderflowI64() throws Exception {
        String value = "-9223372036854775809";
        try {
            parse("10 print " + value);
        } catch (IllegalStateException ise) {
            InvalidException ie = (InvalidException) ise.getCause();
            assertEquals(value, ie.getValue());
            throw ie;
        }
    }

    @Test
    public void testAddingStrings() throws Exception {
        parseAndExpectException("10 print \"A\" + \"B\"", "illegal expression");
    }

    @Test
    public void testSubtractingStrings() throws Exception {
        parseAndExpectException("10 print \"A\" - \"B\"", "illegal expression");
    }

    @Test
    public void testMultiplyingStrings() throws Exception {
        parseAndExpectException("10 print \"A\" * \"B\"", "illegal expression");
    }

    @Test
    public void testDividingStrings() throws Exception {
        parseAndExpectException("10 print \"A\" / \"B\"", "illegal expression");
    }

    @Test
    public void testAddingStringAndInteger() throws Exception {
        parseAndExpectException("10 print \"A\" + 17", "illegal expression");
    }

    private void parseAndExpectException(String text, String message) {
        try {
            parse(text);
            fail("Expected exception containing '" + message + "'");
        } catch (Exception e) {
            assertTrue("\nExpected: '" + message + "'\nActual:   '" + e.getMessage() + "'",
                    e.getMessage().contains(message));
        }
    }

    private Program parse(String text) {
        BasicLexer lexer = new BasicLexer(new ANTLRInputStream(text));
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER);

        BasicParser parser = new BasicParser(new CommonTokenStream(lexer));
        parser.addErrorListener(SYNTAX_ERROR_LISTENER);

        BasicSyntaxListener listener = new BasicSyntaxListener();
        parser.addParseListener(listener);
        parser.program();

        testee.addErrorListener((line, column, msg, e) -> {
            throw new IllegalStateException("Semantics error at " + line + ":" + column + ": " + msg, e);
        });
        return testee.program(listener.getProgram());
    }

    private static final BaseErrorListener SYNTAX_ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalStateException("Syntax error at " + line + ":" + charPositionInLine + ": " + msg, e);
        }
    };
}
