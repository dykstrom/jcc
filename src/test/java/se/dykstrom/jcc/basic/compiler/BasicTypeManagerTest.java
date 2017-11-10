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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.*;

public class BasicTypeManagerTest {

    private static final Identifier ID_BOOLEAN = new Identifier("boolean", Bool.INSTANCE);
    private static final Identifier ID_INTEGER = new Identifier("integer", I64.INSTANCE);
    private static final Identifier ID_STRING = new Identifier("string", Str.INSTANCE);

    private static final Identifier ID_FUN_BOOLEAN = new Identifier("booleanf", Fun.from(emptyList(), Bool.INSTANCE));
    private static final Identifier ID_FUN_INTEGER = new Identifier("integerf", Fun.from(singletonList(Str.INSTANCE), I64.INSTANCE));
    private static final Identifier ID_FUN_STRING = new Identifier("stringf", Fun.from(asList(I64.INSTANCE, Bool.INSTANCE), Str.INSTANCE));

    private static final Expression BOOLEAN_LITERAL = new BooleanLiteral(0, 0, "true");
    private static final Expression INTEGER_LITERAL = new IntegerLiteral(0, 0, "5");
    private static final Expression STRING_LITERAL = new StringLiteral(0, 0, "value");

    private static final Expression BOOLEAN_IDE = new IdentifierDerefExpression(0, 0, ID_BOOLEAN);
    private static final Expression INTEGER_IDE = new IdentifierDerefExpression(0, 0, ID_INTEGER);
    private static final Expression STRING_IDE = new IdentifierDerefExpression(0, 0, ID_STRING);

    private static final Expression BOOLEAN_FCE = new FunctionCallExpression(0, 0, ID_FUN_BOOLEAN, emptyList());
    private static final Expression INTEGER_FCE = new FunctionCallExpression(0, 0, ID_FUN_INTEGER, emptyList());
    private static final Expression STRING_FCE = new FunctionCallExpression(0, 0, ID_FUN_STRING, emptyList());

    private static final Expression ADD_INTEGERS = new AddExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE);
    private static final Expression ADD_INTEGERS_COMPLEX = new AddExpression(0, 0, INTEGER_LITERAL, new AddExpression(0, 0, INTEGER_IDE, INTEGER_IDE));
    private static final Expression ADD_INTEGERS_FUNCTION = new AddExpression(0, 0, INTEGER_FCE, new AddExpression(0, 0, INTEGER_IDE, INTEGER_FCE));

    private static final Expression ADD_STRINGS = new AddExpression(0, 0, STRING_LITERAL, STRING_IDE);

    private static final Expression ADD_STRING_INTEGER = new AddExpression(0, 0, STRING_LITERAL, INTEGER_LITERAL);
    private static final Expression ADD_INTEGER_STRING = new AddExpression(0, 0, INTEGER_LITERAL, STRING_LITERAL);

    private static final Expression SUB_INTEGERS = new SubExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE);
    private static final Expression SUB_INTEGERS_COMPLEX = new SubExpression(0, 0, INTEGER_LITERAL, new SubExpression(0, 0, INTEGER_IDE, INTEGER_IDE));

    private static final Expression SUB_STRINGS = new SubExpression(0, 0, STRING_IDE, STRING_LITERAL);
    private static final Expression SUB_STRING_INTEGER = new SubExpression(0, 0, STRING_IDE, INTEGER_IDE);
    private static final Expression SUB_BOOLEAN_INTEGER = new SubExpression(0, 0, BOOLEAN_IDE, INTEGER_IDE);

    private static final Expression IDIV_INTEGERS = new IDivExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE);
    private static final Expression IDIV_STRING_INTEGER = new IDivExpression(0, 0, STRING_IDE, INTEGER_IDE);
    
    private static final Expression MOD_INTEGERS = new ModExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE);
    private static final Expression MOD_STRING_INTEGER = new ModExpression(0, 0, STRING_IDE, INTEGER_IDE);

    private static final Expression AND_BOOLEANS = new AndExpression(0, 0, BOOLEAN_LITERAL, BOOLEAN_IDE);
    private static final Expression AND_BOOLEANS_COMPLEX = new AndExpression(0, 0, BOOLEAN_LITERAL, new AndExpression(0, 0, BOOLEAN_IDE, BOOLEAN_IDE));

    private static final Expression REL_INTEGERS = new EqualExpression(0, 0, INTEGER_IDE, INTEGER_LITERAL);
    private static final Expression REL_STRINGS = new NotEqualExpression(0, 0, STRING_IDE, STRING_LITERAL);
    private static final Expression REL_COMPLEX = new AndExpression(0, 0, new EqualExpression(0, 0, INTEGER_IDE, INTEGER_LITERAL), BOOLEAN_IDE);
    private static final Expression REL_INTEGER_STRING = new EqualExpression(0, 0, INTEGER_IDE, STRING_LITERAL);

    private final TypeManager testee = new BasicTypeManager();

    @Test
    public void shouldBeAssignableFrom() {
        assertTrue(testee.isAssignableFrom(I64.INSTANCE, I64.INSTANCE));
        assertTrue(testee.isAssignableFrom(Str.INSTANCE, Str.INSTANCE));
        assertTrue(testee.isAssignableFrom(Bool.INSTANCE, Bool.INSTANCE));
        assertTrue(testee.isAssignableFrom(Unknown.INSTANCE, I64.INSTANCE));
        assertTrue(testee.isAssignableFrom(Unknown.INSTANCE, Str.INSTANCE));
        assertTrue(testee.isAssignableFrom(Unknown.INSTANCE, Bool.INSTANCE));
    }

    @Test
    public void shouldNotBeAssignableFrom() {
        assertFalse(testee.isAssignableFrom(Bool.INSTANCE, ID_FUN_BOOLEAN.getType()));
        assertFalse(testee.isAssignableFrom(Bool.INSTANCE, I64.INSTANCE));
        assertFalse(testee.isAssignableFrom(Bool.INSTANCE, Str.INSTANCE));
        assertFalse(testee.isAssignableFrom(I64.INSTANCE, Bool.INSTANCE));
        assertFalse(testee.isAssignableFrom(I64.INSTANCE, ID_FUN_INTEGER.getType()));
        assertFalse(testee.isAssignableFrom(I64.INSTANCE, Str.INSTANCE));
        assertFalse(testee.isAssignableFrom(Str.INSTANCE, Bool.INSTANCE));
        assertFalse(testee.isAssignableFrom(Str.INSTANCE, ID_FUN_STRING.getType()));
        assertFalse(testee.isAssignableFrom(Str.INSTANCE, I64.INSTANCE));
        assertFalse(testee.isAssignableFrom(Unknown.INSTANCE, Unknown.INSTANCE));
        assertFalse(testee.isAssignableFrom(Unknown.INSTANCE, ID_FUN_INTEGER.getType()));
    }

    // Type names:
    
    @Test
    public void shouldGetTypeName() {
        assertEquals("boolean", testee.getTypeName(Bool.INSTANCE));
        assertEquals("integer", testee.getTypeName(I64.INSTANCE));
        assertEquals("string", testee.getTypeName(Str.INSTANCE));
        // Functions
        assertEquals("function()->boolean", testee.getTypeName(ID_FUN_BOOLEAN.getType()));
        assertEquals("function(string)->integer", testee.getTypeName(ID_FUN_INTEGER.getType()));
        assertEquals("function(integer, boolean)->string", testee.getTypeName(ID_FUN_STRING.getType()));
    }
    
    // Literals:
    
    @Test
    public void shouldGetStringFromStringLiteral() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_LITERAL));
    }

    @Test
    public void shouldGetIntegerFromIntegerLiteral() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_LITERAL));
    }

    @Test
    public void shouldGetBooleanFromBooleanLiteral() {
        assertEquals(Bool.INSTANCE, testee.getType(BOOLEAN_LITERAL));
    }

    // Identifiers:
    
    @Test
    public void testStringIdentifierExpression() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_IDE));
    }

    @Test
    public void testIntegerIdentifierExpression() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_IDE));
    }

    @Test
    public void testBooleanIdentifierExpression() {
        assertEquals(Bool.INSTANCE, testee.getType(BOOLEAN_IDE));
    }

    // Expressions:
    
    @Test
    public void testAddIntegers() {
        assertEquals(I64.INSTANCE, testee.getType(ADD_INTEGERS));
    }

    @Test
    public void testAddIntegersComplex() {
        assertEquals(I64.INSTANCE, testee.getType(ADD_INTEGERS_COMPLEX));
    }

    @Test
    public void testSubIntegers() {
        assertEquals(I64.INSTANCE, testee.getType(SUB_INTEGERS));
    }

    @Test
    public void testSubIntegersComplex() {
        assertEquals(I64.INSTANCE, testee.getType(SUB_INTEGERS_COMPLEX));
    }

    @Test
    public void shouldGetIntegerFromIntegerDivision() {
        assertEquals(I64.INSTANCE, testee.getType(IDIV_INTEGERS));
    }

    @Test
    public void shouldGetIntegerFromModulo() {
        assertEquals(I64.INSTANCE, testee.getType(MOD_INTEGERS));
    }
    
    @Test
    public void shouldGetBooleanFromAnd() {
        assertEquals(Bool.INSTANCE, testee.getType(AND_BOOLEANS));
    }

    @Test
    public void shouldGetBooleanFromComplexAnd() {
        assertEquals(Bool.INSTANCE, testee.getType(AND_BOOLEANS_COMPLEX));
    }
    
    @Test
    public void shouldGetBooleanFromIntegerRelational() {
        assertEquals(Bool.INSTANCE, testee.getType(REL_INTEGERS));
    }
    
    @Test
    public void shouldGetBooleanFromStringRelational() {
        assertEquals(Bool.INSTANCE, testee.getType(REL_STRINGS));
    }
    
    @Test
    public void shouldGetBooleanFromComplexRelational() {
        assertEquals(Bool.INSTANCE, testee.getType(REL_COMPLEX));
    }
    
    @Test
    public void shouldGetBooleanFromBooleanFunction() {
        assertEquals(Bool.INSTANCE, testee.getType(BOOLEAN_FCE));
    }
    
    @Test
    public void shouldGetIntegerFromIntegerFunction() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_FCE));
    }
    
    @Test
    public void shouldGetStringFromStringFunction() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_FCE));
    }
    
    @Test
    public void shouldGetIntegerFromAddIntegerWithFunction() {
        assertEquals(I64.INSTANCE, testee.getType(ADD_INTEGERS_FUNCTION));
    }

    // Negative tests:
    
    @Test(expected = SemanticsException.class)
    public void testAddStrings() {
        assertEquals(Str.INSTANCE, testee.getType(ADD_STRINGS));
    }

    @Test(expected = SemanticsException.class)
    public void testAddStringInteger() {
        assertEquals(Str.INSTANCE, testee.getType(ADD_STRING_INTEGER));
    }

    @Test(expected = SemanticsException.class)
    public void testAddIntegerString() {
        assertEquals(Str.INSTANCE, testee.getType(ADD_INTEGER_STRING));
    }

    @Test(expected = SemanticsException.class)
    public void testSubString() {
        testee.getType(SUB_STRINGS);
    }

    @Test(expected = SemanticsException.class)
    public void testSubStringInteger() {
        testee.getType(SUB_STRING_INTEGER);
    }

    @Test(expected = SemanticsException.class)
    public void testSubBooleanInteger() {
        testee.getType(SUB_BOOLEAN_INTEGER);
    }

    @Test(expected = SemanticsException.class)
    public void shouldGetExceptionFromIDivStringInteger() {
        testee.getType(IDIV_STRING_INTEGER);
    }

    @Test(expected = SemanticsException.class)
    public void shouldGetExceptionFromModStringInteger() {
        testee.getType(MOD_STRING_INTEGER);
    }
    
    @Test(expected = SemanticsException.class)
    public void shouldGetExceptionFromRelIntegerString() {
        testee.getType(REL_INTEGER_STRING);
    }
}
