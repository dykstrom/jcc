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
import org.junit.Ignore;
import org.junit.Test;
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.error.InvalidException;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static org.junit.Assert.*;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

public class BasicSemanticsParserTest {

    private static final Identifier IDENT_I64_A = new Identifier("a", I64.INSTANCE);
    private static final Identifier IDENT_BOOL_A = new Identifier("a", Bool.INSTANCE);

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
        parse("20 print \"\"");
    }

    @Test
    public void testOnePrintWithTwoStrings() throws Exception {
        parse("10 print \"One\",\"Two\"");
        parse("20 print \"\",\"\"");
    }

    @Test
    public void testOnePrintWithOneIntegerExpression() throws Exception {
        parse("10 print 5 + 6");
        parse("20 print 1 - 3");
        parse("30 print 4 * 5");
        parse("40 print 100 / 10");
    }

    @Test
    public void testOnePrintWithOneBooleanExpression() throws Exception {
        parse("10 print 5 = 6");
        parse("20 print 1 <> 3");
        parse("30 print 4 > 5");
        parse("40 print 100 >= 10");
        parse("50 print 100 < 100");
        parse("60 print 100 <= 10");
        parse("70 print TRUE");
        parse("80 print FALSE");
    }

    @Test
    public void testOnePrintWithOneAndOr() throws Exception {
        parse("10 print 5 = 6 AND 5 <> 6");
        parse("20 print true and 1 < 0");
        parse("30 print 4 > 5 AND FALSE");
        parse("40 print 100 >= 10 OR 100 <= 10");
        parse("50 print true or false");
    }

    @Test
    public void testOnePrintWithTwoIntegerExpressions() throws Exception {
        parse("10 print 5 + 6 + 7");
        parse("20 print 1 - 3 + 3");
        parse("30 print 4 * 5 + 8");
        parse("40 print 1 - 100 / 10");
    }

    @Test
    public void testOnePrintWithMultipleBooleanExpressions() throws Exception {
        parse("10 print 5 = 6 AND 5 <> 6 AND 5 > 6 AND 5 < 6");
        parse("20 print true and 1 < 0 or false and 1 = 0");
        parse("30 print 0 > 1 and (7 < 8 or 8 < 7)");
    }

    @Test
    public void testOnePrintWithComplexIntegerExpressions() throws Exception {
        parse("10 print (1 - 100) / (10 + 2)");
        parse("20 print 3 * (100 / 2) + (10 - 2) * (0 + 1 + 2)");
    }

    @Test
    public void testOnePrintWithMixedExpressions() throws Exception {
        parse("10 print (1 - 100); true; \"foo\"");
        parse("20 print 2 - 1 > 3 - 4");
        parse("30 print \"\"; 2 - 1; 5 <> 6 AND 6 <> 5");
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
        parse("20 b = 5");
        parse("30 let a% = 5");
        parse("40 b% = 5");
        parse("50 let a$ = \"B\"");
        parse("60 b$ = \"B\"");
    }

    @Test
    public void testReAssignment() throws Exception {
        parse("10 let a% = 5" + EOL + "20 let a% = 7");
        parse("30 let s$ = \"A\"" + EOL + "40 let s$ = \"B\"");
        parse("50 let foo = 5" + EOL + "60 let foo = 7");
        parse("70 let bar = \"C\"" + EOL + "80 let bar = \"D\"");
    }

    @Test
    public void testOneAssignmentWithDerivedTypeInteger() throws Exception {
        Program program = parse("10 let a = 5");
        List<Statement> statements = program.getStatements();
        assertEquals(1, statements.size());
        AssignStatement statement = (AssignStatement) statements.get(0);
        assertEquals(IDENT_I64_A, statement.getIdentifier());
    }

    @Test
    public void testOneAssignmentWithDerivedTypeBoolean() throws Exception {
        Program program = parse("10 let a = 5 > 0");
        List<Statement> statements = program.getStatements();
        assertEquals(1, statements.size());
        AssignStatement statement = (AssignStatement) statements.get(0);
        assertEquals(IDENT_BOOL_A, statement.getIdentifier());
    }

    @Test
    public void testReAssignmentWithDerivedType() throws Exception {
        Program program = parse("10 let a = 5" + EOL + "20 let a = 8");
        List<Statement> statements = program.getStatements();
        assertEquals(2, statements.size());
        AssignStatement as0 = (AssignStatement) statements.get(0);
        AssignStatement as1 = (AssignStatement) statements.get(1);
        assertEquals(IDENT_I64_A, as0.getIdentifier());
        assertEquals(IDENT_I64_A, as1.getIdentifier());
    }

    @Test
    public void testOneAssignmentWithExpression() throws Exception {
        parse("10 let a = 5 + 2");
        parse("20 let a% = 10 * 10");
        parse("30 let number% = 10 / (10 - 5)");
        parse("40 let bool = 10 > 10 or 5 < 5");
        parse("50 let bool = 1 + 1 = 2 AND 1 + 1 > 1");
        parse("60 let bool = 42 >= 17 AND (1 <> 0 OR 17 <= 4711)");
    }

    @Test
    public void testOneDereference() throws Exception {
        parse("10 let a = 5" + EOL + "20 print a");
        parse("30 let a% = 17" + EOL + "40 print a% + 1");
        parse("50 let s$ = \"foo\"" + EOL + "60 print s$");
        parse("70 let bool = 0 = 0" + EOL + "80 print bool");
    }

    @Test
    public void testDereferenceInExpression() throws Exception {
        parse("10 let a = 5" + EOL + "20 let b = a * a");
        parse("30 let a% = 17" + EOL + "40 print a% + 1; a% / a%");
        parse("50 let s$ = \"foo\"" + EOL + "60 print s$; s$; s$");
        parse("70 a = 23 : a = a + 1");
        parse("80 bool = true : bool = bool or 1 = 0");
        parse("90 a = 17 : bool = a > 21");
    }

    @Test
    public void testTwoDereferences() throws Exception {
        parse("10 a = 1 : b = 2" + EOL + "20 c = a + b : d = a + b + c" + EOL + "30 print d");
    }

    @Test
    public void testAssignmentWithInvalidExpression() throws Exception {
        parseAndExpectException("10 let a = \"A\" + 7", "illegal expression");
        parseAndExpectException("20 let a = \"A\" + true", "illegal expression");
        parseAndExpectException("30 let b = true > 0", "illegal expression");
        parseAndExpectException("40 let b = true and 5 + 2", "illegal expression");
        parseAndExpectException("50 let b = \"A\" OR 5 > 2", "illegal expression");
    }

    @Test
    public void testAssignmentWithTypeError() throws Exception {
        parseAndExpectException("10 let a% = \"A\"", "you cannot assign a value of type string");
        parseAndExpectException("20 let b$ = 0", "you cannot assign a value of type integer");
        parseAndExpectException("30 c$ = 7 * 13", "you cannot assign a value of type integer");
        parseAndExpectException("40 let a% = 1 > 0", "you cannot assign a value of type boolean");
        parseAndExpectException("50 let b$ = false", "you cannot assign a value of type boolean");
    }

    @Test
    public void testReAssignmentWithDifferentType() throws Exception {
        parseAndExpectException("10 let a = 5" + EOL + "20 let a = \"foo\"", "a value of type string");
        parseAndExpectException("30 let b = \"foo\"" + EOL + "40 let b = 17", "a value of type integer");
        parseAndExpectException("50 let b = \"foo\"" + EOL + "60 let b = true", "a value of type boolean");
        parseAndExpectException("70 let a = 0" + EOL + "80 let a = 0 <> 1", "a value of type boolean");
        parseAndExpectException("90 let bool = true" + EOL + "100 let bool = 17", "a value of type integer");
    }

    @Test
    public void testDereferenceOfUndefined() throws Exception {
        parseAndExpectException("10 let a = b", "undefined identifier: b");
        parseAndExpectException("20 print foo", "undefined identifier: foo");
        parseAndExpectException("30 let a = 1 : print a + b", "undefined identifier: b");
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
    public void testAddingBooleans() throws Exception {
        parseAndExpectException("10 print true + false", "illegal expression");
    }

    @Test
    public void testAddingStringAndInteger() throws Exception {
        parseAndExpectException("10 print \"A\" + 17", "illegal expression");
    }

    @Test
    public void testAddingIntegerAndBoolean() throws Exception {
        parseAndExpectException("10 print 17 + (1 <> 0)", "illegal expression");
    }

    @Test
    public void testSimpleDivisionByZero() throws Exception {
        parseAndExpectException("10 print 1 / 0", "division by zero");
    }

    @Ignore("requires constant folding")
    @Test
    public void testComplexDivisionByZero() throws Exception {
        parseAndExpectException("10 print 1 / (5 * 2 - 10)", "division by zero");
    }

    @Test
    public void testComparingStrings() throws Exception {
        parseAndExpectException("10 print \"A\" <> \"B\"", "illegal expression");
    }

    @Test
    public void testAndingStrings() throws Exception {
        parseAndExpectException("10 print \"A\" AND \"B\"", "illegal expression");
    }

    private void parseAndExpectException(String text, String message) {
        try {
            parse(text);
            fail("\nExpected: '" + message + "'\nActual:   ''");
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
