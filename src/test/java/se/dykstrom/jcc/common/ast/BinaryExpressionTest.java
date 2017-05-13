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

package se.dykstrom.jcc.common.ast;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BinaryExpressionTest {

    private static final IntegerLiteral IL_5 = new IntegerLiteral(0, 0, "5");
    private static final IntegerLiteral IL_7 = new IntegerLiteral(0, 0, "7");
    private static final IntegerLiteral IL_9 = new IntegerLiteral(0, 0, "9");

    @Test
    public void testWithLeft() {
        AddExpression originalExpression = new AddExpression(0, 0, IL_5, IL_7);
        AddExpression expectedExpression = new AddExpression(0, 0, IL_9, IL_7);
        assertThat(originalExpression.withLeft(IL_9), is(expectedExpression));
    }

    @Test
    public void testWithRight() {
        SubExpression originalExpression = new SubExpression(0, 0, IL_5, IL_7);
        SubExpression expectedExpression = new SubExpression(0, 0, IL_5, IL_9);
        assertThat(originalExpression.withRight(IL_9), is(expectedExpression));
    }
}
