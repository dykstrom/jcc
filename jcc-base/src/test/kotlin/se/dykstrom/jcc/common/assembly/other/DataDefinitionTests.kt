package se.dykstrom.jcc.common.assembly.other

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str

class DataDefinitionTests {

    private val identifier = Identifier("bar", Str.INSTANCE)

    @Test
    fun shouldGenerateConstantString() {
        val dataDefinition = DataDefinition(identifier, "\"foo\"", true)
        assertEquals("_bar db \"foo\"", dataDefinition.toText())
    }

    @Test
    fun shouldGenerateVariableString() {
        val dataDefinition = DataDefinition(identifier, "\"foo\"", false)
        assertEquals("_bar dq \"foo\"", dataDefinition.toText())
    }
}
