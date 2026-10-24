package se.dykstrom.jcc.llvm.code

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L1
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L2
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L3

internal class LabelStackTests {

    private val stack = LabelStack()

    @Test
    fun `stack is empty initially`() {
        assertFalse(stack.isNotEmpty)
    }

    @Test
    fun `push makes stack not empty`() {
        stack.push(LABEL_L1)
        assertTrue(stack.isNotEmpty)
    }

    @Test
    fun `pop returns pushed label`() {
        stack.push(LABEL_L1)
        
        val result = stack.pop()
        
        assertEquals(LABEL_L1, result)
        assertFalse(stack.isNotEmpty)
    }

    @Test
    fun `stack follows LIFO order`() {
        stack.push(LABEL_L1)
        stack.push(LABEL_L2)
        stack.push(LABEL_L3)

        assertEquals(LABEL_L3, stack.pop())
        assertEquals(LABEL_L2, stack.pop())
        assertEquals(LABEL_L1, stack.pop())
    }

    @Test
    fun `replace updates the top label`() {
        stack.push(LABEL_L1)
        stack.replace(LABEL_L2)

        assertTrue(stack.isNotEmpty)
        assertEquals(LABEL_L2, stack.pop())
    }

    @Test
    fun `replace maintains stack depth`() {
        stack.push(LABEL_L1)
        stack.push(LABEL_L2) // Top is L2
        
        stack.replace(LABEL_L3) // Top should become L3, L1 remains at bottom

        assertEquals(LABEL_L3, stack.pop())
        assertEquals(LABEL_L1, stack.pop())
        assertFalse(stack.isNotEmpty)
    }
    
    @Test
    fun `pop on empty stack throws exception`() {
        assertThrows(NoSuchElementException::class.java) {
            stack.pop()
        }
    }

    @Test
    fun `replace on empty stack throws exception`() {
        assertThrows(NoSuchElementException::class.java) {
            stack.replace(LABEL_L1)
        }
    }
}