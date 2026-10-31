/*
 * Copyright (C) 2026 Johan Dykstrom
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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.BaseTests.Companion.IL_1
import se.dykstrom.jcc.common.BaseTests.Companion.IL_2
import se.dykstrom.jcc.common.BaseTests.Companion.IL_5
import se.dykstrom.jcc.common.BaseTests.Companion.INE_I64_A

class BlockStatementTests {

    @Test
    fun ifShouldReturnThenAndElseBlocks() {
        val statement = IfStatement.builder(IL_5, CS_A).elseStatements(CS_B).build()
        assertEquals(listOf(listOf(CS_A), listOf(CS_B)), statement.blocks())
    }

    @Test
    fun ifWithoutElseShouldReturnEmptyElseBlock() {
        val statement = IfStatement.builder(IL_5, CS_A).build()
        assertEquals(listOf(listOf(CS_A), listOf()), statement.blocks())
    }

    @Test
    fun ifShouldReturnCondition() {
        val statement = IfStatement.builder(IL_5, CS_A).build()
        assertEquals(listOf(IL_5), statement.expressions())
    }

    @Test
    fun ifShouldReplaceBlocks() {
        val statement = IfStatement.builder(IL_5, CS_A).line(3).column(4).build()
        val expected = IfStatement.builder(IL_5, CS_B).elseStatements(CS_C).build()

        val actual = statement.withBlocks(listOf(listOf(CS_B), listOf(CS_C)))

        assertEquals(expected, actual)
        assertEquals(3, actual.line())
        assertEquals(4, actual.column())
    }

    @Test
    fun ifShouldMapBlocks() {
        val statement = IfStatement.builder(IL_5, CS_A).elseStatements(CS_B).build()
        val expected = IfStatement.builder(IL_5, CS_A, CS_C).elseStatements(CS_B, CS_C).build()
        assertEquals(expected, statement.mapBlocks { it + CS_C })
    }

    @Test
    fun ifShouldRejectWrongNumberOfBlocks() {
        val statement = IfStatement.builder(IL_5, CS_A).build()
        assertThrows<IllegalArgumentException> { statement.withBlocks(listOf(listOf(CS_B))) }
    }

    @Test
    fun whileShouldReturnBody() {
        val statement = WhileStatement(IL_5, listOf(CS_A, CS_B))
        assertEquals(listOf(listOf(CS_A, CS_B)), statement.blocks())
    }

    @Test
    fun whileShouldReturnCondition() {
        val statement = WhileStatement(IL_5, listOf(CS_A))
        assertEquals(listOf(IL_5), statement.expressions())
    }

    @Test
    fun whileShouldReplaceBlocks() {
        val statement = WhileStatement(3, 4, IL_5, listOf(CS_A))
        val expected = WhileStatement(IL_5, listOf(CS_B))

        val actual = statement.withBlocks(listOf(listOf(CS_B)))

        assertEquals(expected, actual)
        assertEquals(3, actual.line())
        assertEquals(4, actual.column())
    }

    @Test
    fun whileShouldMapBlocks() {
        val statement = WhileStatement(IL_5, listOf(CS_A))
        val expected = WhileStatement(IL_5, listOf(CS_A, CS_C))
        assertEquals(expected, statement.mapBlocks { it + CS_C })
    }

    @Test
    fun whileShouldRejectWrongNumberOfBlocks() {
        val statement = WhileStatement(IL_5, listOf(CS_A))
        assertThrows<IllegalArgumentException> { statement.withBlocks(listOf(listOf(CS_A), listOf(CS_B))) }
    }

    @Test
    fun forShouldReturnBody() {
        val statement = ForStatement(INE_I64_A, IL_1, IL_5, IL_2, listOf(CS_A, CS_B))
        assertEquals(listOf(listOf(CS_A, CS_B)), statement.blocks())
    }

    @Test
    fun forShouldReturnCounterStartEndAndStep() {
        val statement = ForStatement(INE_I64_A, IL_1, IL_5, IL_2, listOf(CS_A))
        assertEquals(listOf(INE_I64_A, IL_1, IL_5, IL_2), statement.expressions())
    }

    @Test
    fun forWithoutStepShouldReturnCounterStartAndEnd() {
        val statement = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(CS_A))
        assertEquals(listOf(INE_I64_A, IL_1, IL_5), statement.expressions())
    }

    @Test
    fun forShouldReplaceBlocks() {
        val statement = ForStatement(3, 4, INE_I64_A, IL_1, IL_5, IL_2, false, listOf(CS_A))
        val expected = ForStatement(0, 0, INE_I64_A, IL_1, IL_5, IL_2, false, listOf(CS_B))

        val actual = statement.withBlocks(listOf(listOf(CS_B)))

        assertEquals(expected, actual)
        assertEquals(3, actual.line())
        assertEquals(4, actual.column())
    }

    @Test
    fun forShouldMapBlocks() {
        val statement = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(CS_A))
        val expected = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(CS_A, CS_C))
        assertEquals(expected, statement.mapBlocks { it + CS_C })
    }

    @Test
    fun forShouldRejectWrongNumberOfBlocks() {
        val statement = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(CS_A))
        assertThrows<IllegalArgumentException> { statement.withBlocks(listOf()) }
    }

    @Test
    fun forShouldCompareInclusiveFlag() {
        val inclusive = ForStatement(0, 0, INE_I64_A, IL_1, IL_5, null, true, listOf(CS_A))
        val exclusive = ForStatement(0, 0, INE_I64_A, IL_1, IL_5, null, false, listOf(CS_A))
        assertNotEquals(inclusive, exclusive)
    }

    companion object {
        private val CS_A = CommentStatement(0, 0, "a")
        private val CS_B = CommentStatement(0, 0, "b")
        private val CS_C = CommentStatement(0, 0, "c")
    }
}
