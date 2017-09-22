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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Unknown;

public class BasicTypeManagerTest {

    private static final Expression STRING_LITERAL = new StringLiteral(0, 0, "value");
    private static final Expression INTEGER_LITERAL = new IntegerLiteral(0, 0, "5");
    private static final Expression BOOLEAN_LITERAL = new BooleanLiteral(0, 0, "true");

    private static final Expression STRING_IDENT = new IdentifierDerefExpression(0, 0, new Identifier("string", Str.INSTANCE));
    private static final Expression INTEGER_IDENT = new IdentifierDerefExpression(0, 0, new Identifier("integer", I64.INSTANCE));
    private static final Expression BOOLEAN_IDENT = new IdentifierDerefExpression(0, 0, new Identifier("boolean", Bool.INSTANCE));

    private static final Expression ADD_INTEGERS = new AddExpression(0, 0, INTEGER_LITERAL, INTEGER_IDENT);
    private static final Expression ADD_INTEGERS_COMPLEX = new AddExpression(0, 0, INTEGER_LITERAL, new AddExpression(0, 0, INTEGER_IDENT, INTEGER_IDENT));

    private static final Expression ADD_STRINGS = new AddExpression(0, 0, STRING_LITERAL, STRING_IDENT);

    private static final Expression ADD_STRING_INTEGER = new AddExpression(0, 0, STRING_LITERAL, INTEGER_LITERAL);
    private static final Expression ADD_INTEGER_STRING = new AddExpression(0, 0, INTEGER_LITERAL, STRING_LITERAL);

    private static final Expression SUB_INTEGERS = new SubExpression(0, 0, INTEGER_LITERAL, INTEGER_IDENT);
    private static final Expression SUB_INTEGERS_COMPLEX = new SubExpression(0, 0, INTEGER_LITERAL, new SubExpression(0, 0, INTEGER_IDENT, INTEGER_IDENT));

    private static final Expression SUB_STRINGS = new SubExpression(0, 0, STRING_IDENT, STRING_LITERAL);
    private static final Expression SUB_STRING_INTEGER = new SubExpression(0, 0, STRING_IDENT, INTEGER_IDENT);
    private static final Expression SUB_BOOLEAN_INTEGER = new SubExpression(0, 0, BOOLEAN_IDENT, INTEGER_IDENT);

    private static final Expression IDIV_INTEGERS = new IDivExpression(0, 0, INTEGER_LITERAL, INTEGER_IDENT);
    private static final Expression IDIV_STRING_INTEGER = new IDivExpression(0, 0, STRING_IDENT, INTEGER_IDENT);
    
    private static final Expression MOD_INTEGERS = new ModExpression(0, 0, INTEGER_LITERAL, INTEGER_IDENT);
    private static final Expression MOD_STRING_INTEGER = new ModExpression(0, 0, STRING_IDENT, INTEGER_IDENT);

    private static final Expression AND_BOOLEANS = new AndExpression(0, 0, BOOLEAN_LITERAL, BOOLEAN_IDENT);
    private static final Expression AND_BOOLEANS_COMPLEX = new AndExpression(0, 0, BOOLEAN_LITERAL, new AndExpression(0, 0, BOOLEAN_IDENT, BOOLEAN_IDENT));

    private static final Expression SIMPLE_RELATIONAL = new EqualExpression(0, 0, INTEGER_IDENT, INTEGER_LITERAL);
    private static final Expression COMPLEX_RELATIONAL = new AndExpression(0, 0, new EqualExpression(0, 0, INTEGER_IDENT, INTEGER_LITERAL), BOOLEAN_IDENT);
    private static final Expression INVALID_RELATIONAL = new EqualExpression(0, 0, INTEGER_IDENT, STRING_LITERAL);

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
        assertFalse(testee.isAssignableFrom(I64.INSTANCE, Bool.INSTANCE));
        assertFalse(testee.isAssignableFrom(I64.INSTANCE, Str.INSTANCE));
        assertFalse(testee.isAssignableFrom(Bool.INSTANCE, Str.INSTANCE));
        assertFalse(testee.isAssignableFrom(Str.INSTANCE, I64.INSTANCE));
        assertFalse(testee.isAssignableFrom(Unknown.INSTANCE, Unknown.INSTANCE));
    }

    // Literals:
    
    @Test
    public void testStringLiteral() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_LITERAL));
    }

    @Test
    public void testIntegerLiteral() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_LITERAL));
    }

    @Test
    public void testBooleanLiteral() {
        assertEquals(Bool.INSTANCE, testee.getType(BOOLEAN_LITERAL));
    }

    // Identifiers:
    
    @Test
    public void testStringIdentifierExpression() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_IDENT));
    }

    @Test
    public void testIntegerIdentifierExpression() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_IDENT));
    }

    @Test
    public void testBooleanIdentifierExpression() {
        assertEquals(Bool.INSTANCE, testee.getType(BOOLEAN_IDENT));
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
    public void shouldGetBooleanFromCompleAnd() {
        assertEquals(Bool.INSTANCE, testee.getType(AND_BOOLEANS_COMPLEX));
    }
    
    @Test
    public void shouldGetBooleanFromSimpleRelational() {
        assertEquals(Bool.INSTANCE, testee.getType(SIMPLE_RELATIONAL));
    }
    
    @Test
    public void shouldGetBooleanFromComplexRelational() {
        assertEquals(Bool.INSTANCE, testee.getType(COMPLEX_RELATIONAL));
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
    public void shouldGetExceptionFromInvalidRelational() {
        testee.getType(INVALID_RELATIONAL);
    }
}
