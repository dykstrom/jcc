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

package se.dykstrom.jcc.llvm.code

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.utils.FormatUtils.EOL

class TargetProgramTests {

    companion object {
        private val ADD = Text("add i64 %0, %1")
        private val MOVE = Text("store i64 %0, ptr %1")
        private val RET = Text("ret i64 0")
    }

    @Test
    fun shouldKeepLinesInOrder() {
        val program = TargetProgram(listOf(ADD, MOVE, RET))
        assertEquals(listOf(ADD, MOVE, RET), program.lines())
    }

    @Test
    fun shouldJoinLinesWithEol() {
        val program = TargetProgram(listOf(ADD, MOVE, RET))
        assertEquals("add i64 %0, %1${EOL}store i64 %0, ptr %1${EOL}ret i64 0", program.toText())
    }

    @Test
    fun shouldCopyTheGivenList() {
        val mutable: MutableList<Line> = mutableListOf(ADD)
        val program = TargetProgram(mutable)
        mutable.add(RET)
        assertEquals(listOf(ADD), program.lines())
    }

    @Test
    fun shouldNotAllowModifyingLines() {
        val program = TargetProgram(listOf(ADD))
        assertThrows<UnsupportedOperationException> { program.lines().add(RET) }
    }
}
