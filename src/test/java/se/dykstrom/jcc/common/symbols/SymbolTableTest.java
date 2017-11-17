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

package se.dykstrom.jcc.common.symbols;

import org.junit.Test;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class SymbolTableTest {

    private static final String NAME_FOO = "foo";
    
    private static final Function FUN_INT = new LibraryFunction(NAME_FOO, singletonList(I64.INSTANCE), I64.INSTANCE, emptyMap(), "fooo");
    private static final Function FUN_STR = new LibraryFunction(NAME_FOO, singletonList(Str.INSTANCE), I64.INSTANCE, emptyMap(), "fooo");

    private static final String I64_VALUE = "17";
    private static final String STR_VALUE = "hello";

    private static final Identifier IDENT_FUN_INT = new Identifier(FUN_INT.getName(), Fun.from(FUN_INT.getArgTypes(), FUN_INT.getReturnType()));
    private static final Identifier IDENT_FUN_STR = new Identifier(FUN_STR.getName(), Fun.from(FUN_STR.getArgTypes(), FUN_STR.getReturnType()));
    
    private static final Identifier IDENT_I64_A = new Identifier("a", I64.INSTANCE);
    private static final Identifier IDENT_STR_B = new Identifier("b", Str.INSTANCE);

    private final SymbolTable testee = new SymbolTable();

    @Test
    public void shouldVerifyEmptySymbolTable() {
        assertTrue(testee.isEmpty());
        assertEquals(0, testee.size());
    }

    @Test
    public void shouldAddVariable() {
        testee.addVariable(IDENT_I64_A);
        assertEquals(1, testee.size());
        assertTrue(testee.contains(IDENT_I64_A.getName()));
        assertEquals(I64.INSTANCE, testee.getType(IDENT_I64_A.getName()));
        assertEquals(I64.INSTANCE.getDefaultValue(), testee.getValue(IDENT_I64_A.getName()));
        assertFalse(testee.isConstant(IDENT_I64_A.getName()));
        assertFalse(testee.contains(IDENT_STR_B.getName()));

        testee.addVariable(IDENT_STR_B);
        assertEquals(2, testee.size());
        assertTrue(testee.contains(IDENT_I64_A.getName()));
        assertTrue(testee.contains(IDENT_STR_B.getName()));
    }

    @Test
    public void shouldAddConstant() {
        testee.addConstant(IDENT_I64_A, I64_VALUE);
        testee.addConstant(IDENT_STR_B, STR_VALUE);

        assertEquals(2, testee.size());
        assertTrue(testee.contains(IDENT_I64_A.getName()));
        assertTrue(testee.contains(IDENT_STR_B.getName()));

        assertEquals(I64.INSTANCE, testee.getType(IDENT_I64_A.getName()));
        assertEquals(I64_VALUE, testee.getValue(IDENT_I64_A.getName()));
        assertTrue(testee.isConstant(IDENT_I64_A.getName()));

        assertEquals(Str.INSTANCE, testee.getType(IDENT_STR_B.getName()));
        assertEquals(STR_VALUE, testee.getValue(IDENT_STR_B.getName()));
        assertTrue(testee.isConstant(IDENT_STR_B.getName()));
    }

    @Test
    public void shouldGetConstantByTypeAndValue() {
        testee.addConstant(IDENT_I64_A, I64_VALUE);
        testee.addConstant(IDENT_STR_B, STR_VALUE);

        assertEquals(2, testee.size());
        assertTrue(testee.contains(IDENT_I64_A.getName()));
        assertTrue(testee.contains(IDENT_STR_B.getName()));
        
        assertEquals(IDENT_I64_A, testee.getConstantByTypeAndValue(IDENT_I64_A.getType(), I64_VALUE));
        assertEquals(IDENT_STR_B, testee.getConstantByTypeAndValue(IDENT_STR_B.getType(), STR_VALUE));
        
        // This combination does not exist
        assertNull(testee.getConstantByTypeAndValue(IDENT_I64_A.getType(), STR_VALUE));
    }

    @Test
    public void shouldAddFunction() {
        testee.addFunction(IDENT_FUN_INT, FUN_INT);
        testee.addFunction(IDENT_FUN_STR, FUN_STR);
        testee.addFunction(IDENT_FUN_STR, FUN_STR); // Add twice to verify that only one instance is saved

        assertThat(testee.size(), is(1));
        assertTrue(testee.containsFunction(NAME_FOO));
        assertThat(testee.functionIdentifiers().size(), is(2));
        assertThat(testee.functionIdentifiers(), hasItems(IDENT_FUN_INT, IDENT_FUN_STR));
        assertThat(testee.getFunctions(NAME_FOO).size(), is(2));
        assertThat(testee.getFunctions(NAME_FOO), hasItems(FUN_INT, FUN_STR));
        assertTrue(testee.containsFunction(NAME_FOO, FUN_INT.getArgTypes()));
        assertThat(testee.getFunction(NAME_FOO, FUN_INT.getArgTypes()), is(FUN_INT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotFindFunctionWithWrongArgTypes() {
        testee.getFunction(NAME_FOO, singletonList(Bool.INSTANCE));
    }
}
