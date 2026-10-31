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

package se.dykstrom.jcc.common.ast

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.BaseTests.Companion.IL_5
import se.dykstrom.jcc.common.BaseTests.Companion.IL_7
import se.dykstrom.jcc.common.BaseTests.Companion.IL_9

class BinaryExpressionTests {

    @Test
    fun shouldUpdateLeftExpression() {
        val originalExpression = AddExpression(IL_5, IL_7)
        val expectedExpression = AddExpression(IL_9, IL_7)
        assertEquals(expectedExpression, originalExpression.withLeft(IL_9))
    }

    @Test
    fun shouldUpdateRightExpression() {
        val originalExpression = SubExpression(IL_5, IL_7)
        val expectedExpression = SubExpression(IL_5, IL_9)
        assertEquals(expectedExpression, originalExpression.withRight(IL_9))
    }
}
