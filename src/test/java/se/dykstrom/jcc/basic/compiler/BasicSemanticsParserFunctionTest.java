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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

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
        defineFunction(IDENT_FUN_ABS, BasicBuiltInFunctions.FUN_ABS);
        defineFunction(IDENT_FUN_COMMAND, new LibraryFunction(IDENT_FUN_COMMAND.getName(), emptyList(), Str.INSTANCE, null, null));
        // Function 'sum' is overloaded with different number of arguments 
        defineFunction(IDENT_FUN_SUM, new LibraryFunction(IDENT_FUN_SUM.getName(), asList(I64.INSTANCE), I64.INSTANCE, null, null));
        defineFunction(IDENT_FUN_SUM, new LibraryFunction(IDENT_FUN_SUM.getName(), asList(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, null, null));
        defineFunction(IDENT_FUN_SUM, new LibraryFunction(IDENT_FUN_SUM.getName(), asList(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, null, null));
    }
    
    @Test
    public void shouldParseCall() throws Exception {
        parse("let a% = abs(1)");
        parse("let c$ = command$()");
    }
    
    @Test
    public void shouldParseOverloadedFunctionCall() throws Exception {
        parse("let a% = sum(1)");
        parse("let a% = sum(1, 2)");
        parse("let a% = sum(1, 2, 3)");
    }
    
    @Test
    public void shouldParseCallWithoutParens() throws Exception {
        parse("let c$ = command$");
    }
    
    @Test
    public void shouldParseCallAndFindType() throws Exception {
        // Given
        Expression fe = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(IL_1));
        Statement as = new AssignStatement(0, 0, IDENT_I64_A, fe);
        List<Statement> expectedStatements = singletonList(as);

        // When
        Program program = parse("a = abs(1)");
        
        // Then
        assertEquals(expectedStatements, program.getStatements());
    }
    
    @Test
    public void shouldParseCallWithFunCallArgs() throws Exception {
        // Given
        Expression fe1 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(IL_1));
        Expression fe2 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(fe1));
        Expression fe3 = new FunctionCallExpression(0, 0, IDENT_FUN_ABS, singletonList(fe2));
        Statement as = new AssignStatement(0, 0, IDENT_I64_A, fe3);
        List<Statement> expectedStatements = singletonList(as);

        // When
        Program program = parse("let a = abs(abs(abs(1)))");
        
        // Then
        assertEquals(expectedStatements, program.getStatements());
    }
    
    @Test
    public void shouldNotParseCallToUndefined() throws Exception {
        parseAndExpectException("print foo(1, 2, 3)", "undefined function");
    }
    
    @Test
    public void shouldNotParseCallToVariable() throws Exception {
        parseAndExpectException("let a = 5 print a()", "undefined function");
    }
    
    @Test
    public void shouldNotParseCallWithWrongReturnType() throws Exception {
        parseAndExpectException("let number% = command$", "a value of type string");
    }
    
    @Test
    public void shouldNotParseCallWithWrongNumberOfArgs() throws Exception {
        parseAndExpectException("print abs(1, 2)", "no match for function 'abs' with arguments (integer, integer)");
        parseAndExpectException("print command$(1)", "no match for function 'command$' with arguments (integer)");
        parseAndExpectException("print sum()", "no match for function 'sum' with arguments ()");
    }
    
    @Test
    public void shouldNotParseCallWithWrongArgTypes() throws Exception {
        parseAndExpectException("print abs(TRUE)", "no match for function 'abs' with arguments (boolean)");
        parseAndExpectException("print sum(1, \"\", FALSE)", "no match for function 'sum' with arguments (integer, string, boolean)");
    }
}
