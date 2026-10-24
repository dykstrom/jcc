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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.ast.BlockStatement
import se.dykstrom.jcc.common.ast.ForStatement
import se.dykstrom.jcc.common.ast.WhileStatement
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L1
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L2
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L3

class LoopStackTests {

    private val stack = LoopStack()

    @Test
    fun shouldFindNothingInEmptyStack() {
        assertTrue(stack.innermost(ForStatement::class.java).isEmpty)
    }

    @Test
    fun shouldFindInnermostLoopOfKind() {
        stack.withLoop(ForStatement::class.java, LABEL_L1) {
            stack.withLoop(ForStatement::class.java, LABEL_L2) {
                stack.withLoop(WhileStatement::class.java, LABEL_L3) {
                    assertEquals(LABEL_L2, stack.innermost(ForStatement::class.java).get())
                    assertEquals(LABEL_L3, stack.innermost(WhileStatement::class.java).get())
                }
            }
        }
    }

    @Test
    fun shouldFindLoopThroughInterface() {
        stack.withLoop(WhileStatement::class.java, LABEL_L1) {
            stack.withLoop(ForStatement::class.java, LABEL_L2) {
                assertEquals(LABEL_L2, stack.innermost(BlockStatement::class.java).get())
            }
        }
    }

    @Test
    fun shouldPopLoopWhenBodyReturns() {
        stack.withLoop(ForStatement::class.java, LABEL_L1) {
            stack.withLoop(ForStatement::class.java, LABEL_L2) {}
            assertEquals(LABEL_L1, stack.innermost(ForStatement::class.java).get())
        }
        assertTrue(stack.innermost(ForStatement::class.java).isEmpty)
    }

    @Test
    fun shouldPopLoopWhenBodyThrows() {
        assertThrows<IllegalStateException> {
            stack.withLoop(ForStatement::class.java, LABEL_L1) { throw IllegalStateException() }
        }
        assertTrue(stack.innermost(ForStatement::class.java).isEmpty)
    }
}
