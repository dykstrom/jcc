package se.dykstrom.jcc.common.semantics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.types.I32

class VariableUsageTrackerTests {

    private lateinit var tracker: VariableUsageTracker
    private lateinit var warnings: MutableList<String>

    @BeforeEach
    fun setUp() {
        tracker = VariableUsageTracker()
        warnings = mutableListOf()
    }

    @Test
    fun `should report unused variable when declared but not used`() {
        // Given
        tracker.declare("x", Declaration("x", I32.INSTANCE))

        // When
        tracker.check { _, msg -> warnings.add(msg) }

        // Then
        assertEquals(1, warnings.size)
        assertEquals("unused variable: x", warnings[0])
    }

    @Test
    fun `should not report warning when variable is used`() {
        // Given
        tracker.declare("x", Declaration("x", I32.INSTANCE))
        tracker.use("x")

        // When
        tracker.check { _, msg -> warnings.add(msg) }

        // Then
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `should handle multiple variables correctly`() {
        // Given
        tracker.declare("used", Declaration("used", I32.INSTANCE))
        tracker.declare("unused", Declaration("unused", I32.INSTANCE))
        tracker.use("used")

        // When
        tracker.check { _, msg -> warnings.add(msg) }

        // Then
        assertEquals(1, warnings.size)
        assertEquals("unused variable: unused", warnings[0])
    }

    @Test
    fun `should propagate usage from inner scope to outer scope via restore`() {
        // Given: A global variable 'g'
        tracker.declare("g", Declaration("g", I32.INSTANCE))

        // When: We enter a function scope, use 'g', and restore
        tracker.save()
        tracker.use("g") // Used inside the function
        tracker.restore(emptySet()) // Restore to global scope

        // Then: 'g' should be considered used in the global scope
        tracker.check { _, msg -> warnings.add(msg) }
        assertTrue(warnings.isEmpty(), "Global variable 'g' should be marked as used")
    }

    @Test
    fun `should not propagate usage if variable is shadowed by a parameter`() {
        // Given: A global variable 'x'
        tracker.declare("x", Declaration("x", I32.INSTANCE))

        // When: We enter a function scope
        tracker.save()
        // We use 'x', but 'x' is also passed as a parameter name to restore
        tracker.use("x")
        tracker.restore(setOf("x"))

        // Then: The usage of 'x' inside the function refers to the parameter,
        // so the global 'x' should remain unused.
        tracker.check { _, msg -> warnings.add(msg) }

        assertEquals(1, warnings.size)
        assertEquals("unused variable: x", warnings[0])
    }

    @Test
    fun `should restore previous declaration state`() {
        // Given: Global 'x'
        tracker.declare("x", Declaration("x", I32.INSTANCE))
        tracker.save()

        // When: Declare local 'y' and restore
        tracker.declare("y", Declaration("y", I32.INSTANCE))
        tracker.restore(emptySet())

        // Then: 'y' should no longer exist in the tracker (it was popped),
        // checking implies only 'x' is checked.
        tracker.check { _, msg -> warnings.add(msg) }

        assertEquals(1, warnings.size)
        assertEquals("unused variable: x", warnings[0])
    }

    @Test
    fun `should track nested usages correctly`() {
        // Scenario:
        // Global: 'a' (unused), 'b' (used in func)
        // Function: uses 'b'

        tracker.declare("a", Declaration("a", I32.INSTANCE))
        tracker.declare("b", Declaration("b", I32.INSTANCE))

        tracker.save()
        tracker.use("b")
        tracker.restore(emptySet())

        tracker.check { _, msg -> warnings.add(msg) }

        assertEquals(1, warnings.size)
        assertEquals("unused variable: a", warnings[0])
    }
}
