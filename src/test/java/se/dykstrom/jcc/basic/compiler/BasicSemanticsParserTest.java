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

import org.junit.Ignore;
import org.junit.Test;
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.error.InvalidException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

public class BasicSemanticsParserTest extends AbstractBasicSemanticsParserTest {

    @Test
    public void shouldPrintNothing() {
        parse("10 print");
    }

    @Test
    public void shouldPrintMaxI64() {
        parse("10 print 9223372036854775807");
        parse("20 print &H7FFFFFFFFFFFFFFF");
    }

    @Test
    public void shouldPrintMinI64() {
        parse("10 print -9223372036854775808");
    }

    @Test
    public void shouldPrintMaxF64() {
        parse("10 print 1.7976931348623157E308");
    }

    @Test
    public void shouldPrintMinF64() {
        parse("10 print 2.2250738585072014E-308");
    }

    @Test
    public void testPrintWithOneString() {
        parse("10 print \"One\"");
        parse("20 print \"\"");
    }

    @Test
    public void testPrintWithTwoStrings() {
        parse("10 print \"One\",\"Two\"");
        parse("20 print \"\",\"\"");
    }

    @Test
    public void testPrintWithOneIntegerExpression() {
        parse("10 print 5 + 6");
        parse("15 print &H5 + &O6");
        parse("20 print 1 - 3");
        parse("25 print &B10 - &B01");
        parse("30 print 4 * 5");
        parse("40 print 100 / 10");
        parse("50 print 100 \\ 10");
        parse("60 print 100 MOD 10");
    }

    @Test
    public void shouldPrintOneFloatExpression() {
        parse("print 3.14");
        parse("print .7D+10");
        parse("print 1.2 + 2.1");
        parse("print 7. - 8.8000");
        parse("print 10E+10 / 12.34");
        parse("print 0.33 * 3.0");
        parse("print 5.3 MOD 4.0");
    }

    @Test
    public void shouldPrintFloatIntegerExpression() {
        parse("print 1.2 + 2");
        parse("print 7 - 8.8000");
        parse("print 10E+10 / 12");
        parse("print 0.33 * 3");
        parse("print 5.3 MOD 4");
    }

    @Test
    public void shouldPrintRelationalConstantExpression() {
        parse("print TRUE");
        parse("print FALSE");
    }

    @Test
    public void shouldPrintRelationalIntegerExpression() {
        parse("10 print 5 = 6");
        parse("20 print 1 <> 3");
        parse("30 print 4 > 5");
        parse("40 print 100 >= 10");
        parse("50 print 100 < 100");
        parse("60 print 100 <= 10");
    }

    @Test
    public void shouldPrintRelationalFloatExpression() {
        parse("print 3.4 = 6.7");
        parse("print 1.0E10 <> -0.1D10");
        parse("print 0.1 > 100000.99");
        parse("print -.4 >= .7");
        parse("print 1e+33 < -6.7d-100");
        parse("print 3.4 <= 6.7");
    }

    @Test
    public void shouldPrintRelationalFloatIntegerExpression() {
        parse("print 3.4 = 6");
        parse("print .1 <> -4711");
    }

    @Test
    public void shouldPrintRelationalStringExpression() {
        parse("10 print \"foo\" = \"bar\"");
        parse("20 print \"one\" <> \"two\"");
        parse("30 print \"\" < \"three\"");
        parse("40 print \"50\" <= \"70\"");
        parse("50 print \"foo\" > \"bar\"");
        parse("60 print \"one\" >= \"two\"");
    }

    @Test
    public void testPrintWithOneConditionalExpression() {
        parse("10 print 5 = 6 AND 5 <> 6");
        parse("20 print true and 1 < 0");
        parse("30 print 4 > 5 AND FALSE");
        parse("40 print 100 >= 10 OR 100 <= 10");
        parse("50 print true or false");
        parse("60 print true xor false");
        parse("70 print NOT 1 < 1 AND NOT 1 > 1");
    }

    @Test
    public void testPrintWithTwoIntegerExpressions() {
        parse("10 print 5 + 6 + 7");
        parse("20 print 1 - 3 + 3");
        parse("30 print 4 * 5 + 8");
        parse("40 print 1 - 100 / 10");
        parse("50 print 1 - 100 MOD 10");
        parse("60 print 8 * 9 \\ 4");
    }

    @Test
    public void testPrintWithMultipleBooleanExpressions() {
        parse("10 print 5 = 6 AND 5 <> 6 AND 5 > 6 AND 5 < 6");
        parse("20 print true and 1 < 0 or false and 1 = 0");
        parse("30 print 0 > 1 and (7 < 8 or 8 < 7)");
        parse("40 print 0 > 1 and (\"A\" <> \"B\" or 8 < 7)");
        parse("50 print 0 > 1 or 0 < 1 xor 0 = 1 or 0 <> 1");
        parse("60 print not 0 > 1 or 0 < 1 xor not 0 = 1 or 0 <> 1");
    }

    @Test
    public void testPrintWithComplexIntegerExpressions() {
        parse("10 print (1 - 100) / (10 + 2)");
        parse("20 print 3 * (100 / 2) + (10 - 2) * (0 + 1 + 2)");
        parse("30 print (1 - 100) \\ (10 + 2)");
        parse("40 print -(&B1 - &O100) * -(10 + &H02)");
    }

    @Test
    public void testPrintWithMixedExpressions() {
        parse("10 print (1 - 100); true; \"foo\"");
        parse("20 print 2 - 1 > 3 - 4");
        parse("30 print \"\"; 2 - 1; 5 <> 6 AND 6 <> 5; &HFFFF");
        parse("40 print 2.0 * 1 = .3 - 4.01e+10");
        parse("50 print -12345.12345 / 1 * (7.77 - 1) <> 0 + 0.0 + 0 AND (1 \\ 1 <> 0.01d-100)");
    }

    @Test
    public void testPrintAndGoto() {
        parse("10 print \"One\"" + EOL + "20 goto 10");
    }

    @Test
    public void testMultiplePrintAndGotos() {
        parse("10 goto 40" + EOL
                + "20 print \"A\"" + EOL
                + "30 goto 60" + EOL
                + "40 print \"B\"" + EOL
                + "50 goto 20" + EOL
                + "60 print \"C\"");
    }

    @Test
    public void shouldParseOnGoto() {
        parse("10 on 1 goto 10");
    }

    @Test
    public void shouldParseOnGotoMultipleLabels() {
        parse("10 let a = 1 " + "20 on a goto 10, 20");
    }
    
    @Test
    public void testAssignment() {
        parse("10 let a = 5");
        parse("20 b = 5");
        parse("30 let a% = 5");
        parse("40 b% = 5");
        parse("50 let a$ = \"B\"");
        parse("60 b$ = \"B\"");
    }

    @Test
    public void testReAssignment() {
        parse("10 let a% = 5" + EOL + "20 let a% = 7");
        parse("30 let s$ = \"A\"" + EOL + "40 let s$ = \"B\"");
        parse("50 let foo = 5" + EOL + "60 let foo = 7");
        parse("70 let bar = \"C\"" + EOL + "80 let bar = \"D\"");
    }

    @Test
    public void testAssignmentWithDerivedTypeInteger() {
        Program program = parse("10 let a = 5");
        List<Statement> statements = program.getStatements();
        assertEquals(1, statements.size());
        AssignStatement statement = (AssignStatement) statements.get(0);
        assertEquals(IDENT_I64_A, statement.getIdentifier());
    }

    @Test
    public void testAssignmentWithDerivedTypeFloat() {
        Program program = parse("10 let f = 3.14");
        List<Statement> statements = program.getStatements();
        assertEquals(1, statements.size());
        AssignStatement statement = (AssignStatement) statements.get(0);
        assertEquals(IDENT_F64_F, statement.getIdentifier());
    }

    @Test
    public void testAssignmentWithDerivedTypeBoolean() {
        Program program = parse("10 let b = 5 > 0");
        List<Statement> statements = program.getStatements();
        assertEquals(1, statements.size());
        AssignStatement statement = (AssignStatement) statements.get(0);
        assertEquals(IDENT_BOOL_B, statement.getIdentifier());
    }

    @Test
    public void testReAssignmentWithDerivedType() {
        Program program = parse("10 let a = 5" + EOL + "20 let a = 8");
        List<Statement> statements = program.getStatements();
        assertEquals(2, statements.size());
        AssignStatement as0 = (AssignStatement) statements.get(0);
        AssignStatement as1 = (AssignStatement) statements.get(1);
        assertEquals(IDENT_I64_A, as0.getIdentifier());
        assertEquals(IDENT_I64_A, as1.getIdentifier());
    }

    @Test
    public void testAssignmentWithExpression() {
        parse("10 let a = 5 + 2");
        parse("20 let a% = 10 * 10");
        parse("30 let number% = 10 / (10 - 5)");
        parse("40 let bool = 10 > 10 or 5 < 5");
        parse("50 let bool = 1 + 1 = 2 AND 1 + 1 > 1");
        parse("60 let bool = 42 >= 17 AND (1 <> 0 OR 17 <= 4711)");
    }

    @Test
    public void testDereference() {
        parse("10 let a = 5" + EOL + "20 print a");
        parse("30 let a% = 17" + EOL + "40 print -a% + 1");
        parse("50 let s$ = \"foo\"" + EOL + "60 print s$");
        parse("70 let bool = 0 = 0" + EOL + "80 print bool");
        parse("90 let float = 1.2 / 7.8" + EOL + "100 print float");
    }

    @Test
    public void testDereferenceInExpression() {
        parse("10 let a = 5" + EOL + "20 let b = a * a");
        parse("30 let a% = 17" + EOL + "40 print a% + 1; a% / a%; a% \\ a%");
        parse("50 let s$ = \"foo\"" + EOL + "60 print s$; s$; s$; s$ = s$; s$ <> \"bar\"");
        parse("70 a = 23 : a = a + 1");
        parse("80 bool = true : bool = bool or 1 = 0");
        parse("90 a = 17 : bool = a > 21");
        parse("100 f = 1.7 : float = f + f");
    }

    @Test
    public void testTwoDereferences() {
        parse("10 a = 1 : b = 2" + EOL + "20 c = a + b : d = a + b + c" + EOL + "30 print d");
    }

    @Test
    public void shouldParseIfThenElse() {
        parse("10 if 5 > 0 then 10 else 20" + EOL + "20 end");
        parse("30 if 4711 then print 4711");
        parse("50 if false then print \"false\" : goto 60 else print \"true\" : goto 60" + EOL + "60 end");
    }

    @Test
    public void shouldParseIfThenGotoEndif() {
        parse("10 if 5 > 0 then " +
              "15   goto 20 " +
              "20 else " +
              "25   goto 30 " +
              "30 endif");
    }

    @Test
    public void shouldParseWhile() {
        parse("10 while true" + EOL + "20 wend");
        parse("30 while 0 = 0" + EOL + "40 print 0" + EOL + "50 wend");
        parse("while 1 > 0" + EOL + "a% = 0" + EOL + "print a%" + EOL + "wend");
    }

    @Test
    public void shouldParseWhileGotoWend() {
        parse("10 while false " +
              "15   goto 20 " +
              "20 wend");
    }

    @Test
    public void shouldNotParseWhileWithStringExpression() {
        parseAndExpectException("while \"foo\" wend", "expression of type string");
    }

    @Test
    public void shouldNotParseWhileWithSemanticsError() {
        parseAndExpectException("10 while 5 > 0 " +
                                "20   print 17 + \"17\" " +
                                "30 wend", "illegal expression");
    }

    @Test
    public void shouldNotParseWhileWithDuplicateLineNumbers() {
        parseAndExpectException("10 while 5 > 0 " +
                                "10   print 17 " +
                                "30 wend", "duplicate line");
    }

    @Test
    public void shouldNotParseIfWithStringExpression() {
        parseAndExpectException("10 if \"foo\" then 10", "expression of type string");
    }

    @Test
    public void shouldNotParseIfThenWithSemanticsError() {
        parseAndExpectException("10 if 5 > 0 then " +
                                "20   print 17 + \"17\" " +
                                "30 endif", "illegal expression");
    }

    @Test
    public void shouldNotParseIfThenWithDuplicateLineNumbers() {
        parseAndExpectException("10 if 5 > 0 then " +
                                "10   print 17 " +
                                "30 endif", "duplicate line");
    }

    @Test
    public void testAssignmentWithInvalidExpression() {
        parseAndExpectException("10 let a = \"A\" + 7", "illegal expression");
        parseAndExpectException("20 let a = \"A\" + true", "illegal expression");
        parseAndExpectException("25 let a = &HFF + true", "illegal expression");
        parseAndExpectException("30 let b = true > 0", "cannot compare boolean and integer");
        parseAndExpectException("40 let b = true and 5 + 2", "expected subexpressions of type boolean");
        parseAndExpectException("50 let b = \"A\" OR 5 > 2", "expected subexpressions of type boolean");
        parseAndExpectException("60 let c = -TRUE", "illegal expression");
        parseAndExpectException("70 let c = -\"A\"", "illegal expression");
        parseAndExpectException("80 let c = -(true or false)", "illegal expression");
        parseAndExpectException("90 let b = NOT 0", "expected subexpression of type boolean");
        parseAndExpectException("100 let b = NOT \"\"", "expected subexpression of type boolean");
        parseAndExpectException("110 let b = NOT 90.1", "expected subexpression of type boolean");
        parseAndExpectException("120 let f = 0.1 + true", "illegal expression");
    }

    @Test
    public void testAssignmentWithTypeError() {
        parseAndExpectException("10 let a% = \"A\"", "you cannot assign a value of type string");
        parseAndExpectException("20 let b$ = 0", "you cannot assign a value of type integer");
        parseAndExpectException("30 c$ = 7 * 13", "you cannot assign a value of type integer");
        parseAndExpectException("40 let a% = 1 > 0", "you cannot assign a value of type boolean");
        parseAndExpectException("50 let b$ = false", "you cannot assign a value of type boolean");
        parseAndExpectException("60 let b$ = &O123", "you cannot assign a value of type integer");
        parseAndExpectException("70 let b$ = 1.", "you cannot assign a value of type float");
    }

    @Test
    public void testReAssignmentWithDifferentType() {
        parseAndExpectException("10 let a = 5" + EOL + "20 let a = \"foo\"", "a value of type string");
        parseAndExpectException("30 let b = \"foo\"" + EOL + "40 let b = 17", "a value of type integer");
        parseAndExpectException("50 let b = \"foo\"" + EOL + "60 let b = true", "a value of type boolean");
        parseAndExpectException("70 let a = 0" + EOL + "80 let a = 0 <> 1", "a value of type boolean");
        parseAndExpectException("90 let bool = true" + EOL + "100 let bool = 17", "a value of type integer");
    }

    @Test
    public void testDereferenceOfUndefined() {
        parseAndExpectException("10 let a = b", "undefined identifier: b");
        parseAndExpectException("20 print foo", "undefined identifier: foo");
        parseAndExpectException("30 let a = 1 : print a + b", "undefined identifier: b");
    }

    @Test
    public void testDuplicateLineNumber() {
        parseAndExpectException("10 goto 10" + EOL + "10 print", "duplicate line");
    }

    @Test
    public void testUndefinedGotoLine() {
        parseAndExpectException("10 goto 20", "undefined line");
    }

    @Test
    public void shouldNotParseOnGotoInvalidExpression() {
        parseAndExpectException("10 on \"foo\" goto 10", "expression of type string");
        parseAndExpectException("20 on 1 = 1 goto 20", "expression of type boolean");
    }

    @Test
    public void shouldNotParseOnGotoUnknownLabel() {
        parseAndExpectException("10 on 5 goto 100", "undefined line number: 100");
        parseAndExpectException("20 a = 1 30 on a goto 20, 30, 40", "undefined line number: 40");
    }

    /**
     * Invalid integer -> overflow.
     */
    @Test(expected = InvalidException.class)
    public void testOverflowI64() {
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
    public void testUnderflowI64() {
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
    public void testAddingStrings() {
        parseAndExpectException("10 print \"A\" + \"B\"", "illegal expression");
    }

    @Test
    public void testSubtractingStrings() {
        parseAndExpectException("10 print \"A\" - \"B\"", "illegal expression");
    }

    @Test
    public void testMultiplyingStrings() {
        parseAndExpectException("10 print \"A\" * \"B\"", "illegal expression");
    }

    @Test
    public void testDividingStrings() {
        parseAndExpectException("10 print \"A\" / \"B\"", "illegal expression");
    }

    @Test
    public void testIntegerDivisionOnStrings() {
        parseAndExpectException("10 print \"A\" \\ \"B\"", "expected subexpressions of type integer");
    }

    @Test
    public void testModuloOnStrings() {
        parseAndExpectException("10 print \"A\" MOD \"B\"", "illegal expression");
    }

    @Test
    public void testAddingBooleans() {
        parseAndExpectException("10 print true + false", "illegal expression");
    }

    @Test
    public void testAddingStringAndInteger() {
        parseAndExpectException("10 print \"A\" + 17", "illegal expression");
    }

    @Test
    public void testAddingStringAndFloat() {
        parseAndExpectException("10 print \"A\" + 17.17", "illegal expression");
    }

    @Test
    public void testAddingIntegerAndBoolean() {
        parseAndExpectException("10 print 17 + (1 <> 0)", "illegal expression");
    }

    @Test
    public void testSimpleDivisionByZero() {
        parseAndExpectException("10 print 1 / 0", "division by zero");
    }

    @Ignore("requires constant folding")
    @Test
    public void testComplexDivisionByZero() {
        parseAndExpectException("10 print 1 / (5 * 2 - 10)", "division by zero");
    }

    @Test
    public void testComparingBooleans() {
        parseAndExpectException("10 print true <> false", "cannot compare boolean and boolean");
        parseAndExpectException("20 print true = false", "cannot compare boolean and boolean");
    }

    @Test
    public void testComparingDifferentTypes() {
        parseAndExpectException("10 print 1 <> \"one\"", "cannot compare integer and string");
        parseAndExpectException("20 print \"two\" = 2", "cannot compare string and integer");
        parseAndExpectException("30 print true = 2", "cannot compare boolean and integer");
    }

    @Test
    public void testAndingFloats() {
        parseAndExpectException("10 print 1.5 AND 5.1", "expected subexpressions of type boolean");
    }

    @Test
    public void testAndingStrings() {
        parseAndExpectException("10 print \"A\" AND \"B\"", "expected subexpressions of type boolean");
    }

    @Test
    public void testNottingString() {
        parseAndExpectException("10 print NOT \"B\"", "expected subexpression of type boolean");
    }

    @Test
    public void shouldNotParseIntegerDivisionWithFloat() {
        parseAndExpectException("print 1.5 \\ 2", "expected subexpressions of type integer");
        parseAndExpectException("print 1 \\ 0.7", "expected subexpressions of type integer");
        parseAndExpectException("print 1 \\ (0.7 + 1)", "expected subexpressions of type integer");
    }
}
