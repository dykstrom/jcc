package se.dykstrom.jcc.llvm.code

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class LabelStackTests {

    private val stack = LabelStack()

    @Test
    fun `stack is empty initially`() {
        assertFalse(stack.isNotEmpty)
    }

    @Test
    fun `push makes stack not empty`() {
        stack.push(FixedLabel("L1"))
        assertTrue(stack.isNotEmpty)
    }

    @Test
    fun `pop returns pushed label`() {
        val label = FixedLabel("L1")
        stack.push(label)
        
        val result = stack.pop()
        
        assertEquals(label, result)
        assertFalse(stack.isNotEmpty)
    }

    @Test
    fun `stack follows LIFO order`() {
        val l1 = FixedLabel("L1")
        val l2 = FixedLabel("L2")
        val l3 = FixedLabel("L3")

        stack.push(l1)
        stack.push(l2)
        stack.push(l3)

        assertEquals(l3, stack.pop())
        assertEquals(l2, stack.pop())
        assertEquals(l1, stack.pop())
    }

    @Test
    fun `replace updates the top label`() {
        val l1 = FixedLabel("L1")
        val l2 = FixedLabel("L2")
        
        stack.push(l1)
        stack.replace(l2)

        assertTrue(stack.isNotEmpty)
        assertEquals(l2, stack.pop())
    }

    @Test
    fun `replace maintains stack depth`() {
        val l1 = FixedLabel("L1")
        val l2 = FixedLabel("L2")
        val l3 = FixedLabel("L3")

        stack.push(l1)
        stack.push(l2) // Top is L2
        
        stack.replace(l3) // Top should become L3, L1 remains at bottom

        assertEquals(l3, stack.pop())
        assertEquals(l1, stack.pop())
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
        val label = FixedLabel("L1")
        assertThrows(NoSuchElementException::class.java) {
            stack.replace(label)
        }
    }
}