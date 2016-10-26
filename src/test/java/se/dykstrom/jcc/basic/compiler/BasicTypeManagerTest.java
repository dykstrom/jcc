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

import org.junit.Test;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import static org.junit.Assert.assertEquals;

public class BasicTypeManagerTest {

    private static final Expression STRING_LITERAL = new StringLiteral(0, 0, "value");
    private static final Expression INTEGER_LITERAL = new IntegerLiteral(0, 0, "5");

    private static final Expression STRING_IDENT = new IdentifierReferenceExpression(0, 0, new Identifier("string", Str.INSTANCE));
    private static final Expression INTEGER_IDENT = new IdentifierReferenceExpression(0, 0, new Identifier("integer", I64.INSTANCE));

    private static final Expression ADD_INTEGERS = new AddExpression(0, 0, INTEGER_LITERAL, INTEGER_IDENT);
    private static final Expression ADD_INTEGERS_COMPLEX = new AddExpression(0, 0, INTEGER_LITERAL, new AddExpression(0, 0, INTEGER_IDENT, INTEGER_IDENT));

    private static final Expression ADD_STRINGS = new AddExpression(0, 0, STRING_LITERAL, STRING_IDENT);

    private static final Expression ADD_STRING_INTEGER = new AddExpression(0, 0, STRING_LITERAL, INTEGER_LITERAL);
    private static final Expression ADD_INTEGER_STRING = new AddExpression(0, 0, INTEGER_LITERAL, STRING_LITERAL);

    private static final Expression SUB_INTEGERS = new SubExpression(0, 0, INTEGER_LITERAL, INTEGER_IDENT);
    private static final Expression SUB_INTEGERS_COMPLEX = new SubExpression(0, 0, INTEGER_LITERAL, new SubExpression(0, 0, INTEGER_IDENT, INTEGER_IDENT));

    private static final Expression SUB_STRINGS = new SubExpression(0, 0, STRING_IDENT, STRING_LITERAL);
    private static final Expression SUB_STRING_INTEGER = new SubExpression(0, 0, STRING_IDENT, INTEGER_IDENT);

    private final TypeManager testee = new BasicTypeManager();

    @Test
    public void testStringLiteral() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_LITERAL));
    }

    @Test
    public void testIntegerLiteral() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_LITERAL));
    }

    @Test
    public void testStringIdentifierExpression() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_IDENT));
    }

    @Test
    public void testIntegerIdentifierExpression() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_IDENT));
    }

    @Test
    public void testAddIntegers() {
        assertEquals(I64.INSTANCE, testee.getType(ADD_INTEGERS));
    }

    @Test
    public void testAddIntegersComplex() {
        assertEquals(I64.INSTANCE, testee.getType(ADD_INTEGERS_COMPLEX));
    }

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

    @Test
    public void testSubIntegers() {
        assertEquals(I64.INSTANCE, testee.getType(SUB_INTEGERS));
    }

    @Test
    public void testSubIntegersComplex() {
        assertEquals(I64.INSTANCE, testee.getType(SUB_INTEGERS_COMPLEX));
    }

    @Test(expected = SemanticsException.class)
    public void testSubString() {
        testee.getType(SUB_STRINGS);
    }

    @Test(expected = SemanticsException.class)
    public void testSubStringInteger() {
        testee.getType(SUB_STRING_INTEGER);
    }
}
