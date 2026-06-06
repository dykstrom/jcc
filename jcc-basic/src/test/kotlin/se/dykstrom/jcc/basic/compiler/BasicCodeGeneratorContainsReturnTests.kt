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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.ast.statement.EndStatement
import se.dykstrom.jcc.basic.ast.statement.ReturnFromGosubStatement
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator.containsReturn
import se.dykstrom.jcc.common.ast.IfStatement
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.LabelledStatement
import se.dykstrom.jcc.common.ast.ReturnStatement
import se.dykstrom.jcc.common.ast.WhileStatement

class BasicCodeGeneratorContainsReturnTests {

    @Test
    fun emptyProgramContainsNoReturn() {
        assertFalse(containsReturn(listOf()))
    }

    @Test
    fun programWithoutReturn() {
        assertFalse(containsReturn(listOf(EndStatement())))
    }

    @Test
    fun directReturnStatement() {
        assertTrue(containsReturn(listOf(ReturnStatement(0, 0))))
    }

    @Test
    fun directReturnFromGosubStatement() {
        assertTrue(containsReturn(listOf(ReturnFromGosubStatement())))
    }

    @Test
    fun returnInsideLabelledStatement() {
        assertTrue(containsReturn(listOf(LabelledStatement("10", ReturnFromGosubStatement()))))
    }

    @Test
    fun returnInsideWhileStatement() {
        val ws = WhileStatement(0, 0, IntegerLiteral.ONE, listOf(ReturnStatement(0, 0)))
        assertTrue(containsReturn(listOf(ws)))
    }

    @Test
    fun returnInsideIfThenBranch() {
        val ifs = IfStatement.builder(IntegerLiteral.ONE, ReturnStatement(0, 0)).build()
        assertTrue(containsReturn(listOf(ifs)))
    }

    @Test
    fun returnInsideIfElseBranch() {
        val ifs = IfStatement.builder(IntegerLiteral.ONE, EndStatement())
            .elseStatements(ReturnStatement(0, 0))
            .build()
        assertTrue(containsReturn(listOf(ifs)))
    }

    @Test
    fun deeplyNestedReturn() {
        val inner = WhileStatement(0, 0, IntegerLiteral.ONE, listOf(ReturnFromGosubStatement()))
        val ifs = IfStatement.builder(IntegerLiteral.ONE, inner).build()
        val labelled = LabelledStatement("20", ifs)
        assertTrue(containsReturn(listOf(labelled)))
    }

    @Test
    fun nestedStructuresWithoutReturn() {
        val ws = WhileStatement(0, 0, IntegerLiteral.ONE, listOf(EndStatement()))
        val ifs = IfStatement.builder(IntegerLiteral.ONE, EndStatement())
            .elseStatements(ws)
            .build()
        assertFalse(containsReturn(listOf(LabelledStatement("30", ifs))))
    }
}
