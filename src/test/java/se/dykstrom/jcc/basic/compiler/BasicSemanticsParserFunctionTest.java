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

import org.junit.Before;
import org.junit.Test;
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions;
import se.dykstrom.jcc.common.ast.*;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_ABS;

/**
 * Tests class {@code BasicSemanticsParser}, especially functionality related to function calls.
 * 
 * @author Johan Dykstrom
 * @see BasicSemanticsParser
 */
public class BasicSemanticsParserFunctionTest extends AbstractBasicSemanticsParserTest {

    @Before
    public void setUp() {
        // Define some functions for testing
        defineFunction(BasicBuiltInFunctions.FUN_ABS);
        defineFunction(BasicBuiltInFunctions.FUN_FMOD);
        defineFunction(FUN_COMMAND);
        // Function 'sum' is overloaded with different number of arguments 
        defineFunction(FUN_SUM1);
        defineFunction(FUN_SUM2);
        defineFunction(FUN_SUM3);
    }
    
    @Test
    public void shouldParseCall() {
        parse("let a% = abs(1)");
        parse("let c$ = command$()");
        parse("let f = fmod(1.0, 2.0)");
    }

    @Test
    public void shouldParseOverloadedFunctionCall() {
        parse("let a% = sum(1)");
        parse("let a% = sum(1, 2)");
        parse("let a% = sum(1, 2, 3)");
    }
    
    @Test
    public void shouldParseCallWithoutParens() {
        parse("let c$ = command$");
    }

    @Test
    public void shouldParseCallWithTypeCastArguments() {
        parse("let f = fmod(1.0, 2)");
        parse("let f = fmod(1, 2.0)");
        parse("let f = fmod(1, 2)");
    }

    @Test
    public void shouldParseCallAndFindType() {
        // Given
        Expression fe = new FunctionCallExpression(0, 0, FUN_ABS.getIdentifier(), singletonList(IL_1));
        Statement as = new AssignStatement(0, 0, IDENT_I64_A, fe);
        List<Statement> expectedStatements = singletonList(as);

        // When
        Program program = parse("a = abs(1)");
        
        // Then
        assertEquals(expectedStatements, program.getStatements());
    }
    
    @Test
    public void shouldParseCallWithFunCallArgs() {
        // Given
        Expression fe1 = new FunctionCallExpression(0, 0, FUN_ABS.getIdentifier(), singletonList(IL_1));
        Expression fe2 = new FunctionCallExpression(0, 0, FUN_ABS.getIdentifier(), singletonList(fe1));
        Expression fe3 = new FunctionCallExpression(0, 0, FUN_ABS.getIdentifier(), singletonList(fe2));
        Statement as = new AssignStatement(0, 0, IDENT_I64_A, fe3);
        List<Statement> expectedStatements = singletonList(as);

        // When
        Program program = parse("let a = abs(abs(abs(1)))");
        
        // Then
        assertEquals(expectedStatements, program.getStatements());
    }
    
    @Test
    public void shouldNotParseCallToUndefined() {
        parseAndExpectException("print foo(1, 2, 3)", "undefined function");
    }
    
    @Test
    public void shouldNotParseCallToVariable() {
        parseAndExpectException("let a = 5 print a()", "undefined function");
    }
    
    @Test
    public void shouldNotParseCallWithWrongReturnType() {
        parseAndExpectException("let number% = command$", "a value of type string");
        parseAndExpectException("let number% = fmod(1.0, 1.0)", "a value of type double");
    }
    
    @Test
    public void shouldNotParseCallWithWrongNumberOfArgs() {
        parseAndExpectException("print abs(1, 2)", "found no match for function call: abs(integer, integer)");
        parseAndExpectException("print command$(1)", "found no match for function call: command$(integer)");
        parseAndExpectException("print sum()", "found no match for function call: sum()");
    }
    
    @Test
    public void shouldNotParseCallWithWrongArgTypes() {
        parseAndExpectException("print abs(TRUE)", "found no match for function call: abs(boolean)");
        parseAndExpectException("print fmod(TRUE, 1.0)", "found no match for function call: fmod(boolean, double)");
        parseAndExpectException("print sum(1, \"\", FALSE)", "found no match for function call: sum(integer, string, boolean)");
    }
}
