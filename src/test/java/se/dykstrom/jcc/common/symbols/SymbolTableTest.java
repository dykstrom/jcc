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
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SymbolTableTest {

    private static final Identifier IDENT_I64_A = new Identifier("a", I64.INSTANCE);
    private static final Identifier IDENT_STR_B = new Identifier("b", Str.INSTANCE);

    private static final String I64_VALUE = "17";
    private static final String STR_VALUE = "hello";

    private final SymbolTable testee = new SymbolTable();

    @Test
    public void emptyTable() {
        assertTrue(testee.isEmpty());
        assertEquals(0, testee.size());
    }

    @Test
    public void addVariable() {
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
    public void addConstant() {
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
}
