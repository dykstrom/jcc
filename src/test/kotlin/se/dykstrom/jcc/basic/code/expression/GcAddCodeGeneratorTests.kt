package se.dykstrom.jcc.basic.code.expression

import org.junit.Test
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.assembly.base.Instruction
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.code.expression.GcAddCodeGenerator
import se.dykstrom.jcc.common.types.Str
import kotlin.test.assertEquals

/**
 * This class tests the common class [GcAddCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class GcAddCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = GcAddCodeGenerator(context)

    @Test
    fun generateAddStrings() {
        // Given
        val expression = AddExpression(0, 0, SL_A, SL_B)
        val location = storageFactory.allocateNonVolatile()

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toAsm() }

        // Then
        assertEquals(2, symbols.identifiers().count { it.type() == Str.INSTANCE })
        val moveLeft = """mov (r[a-z0-9]+), .*""".toRegex()
        assertRegexMatches(moveLeft, lines[0])
        val moveRight = """mov (r[a-z0-9]+), .*""".toRegex()
        assertRegexMatches(moveRight, lines[1])
        assertEquals(1, lines.count { it == "call [_strcat_lib]" })
    }
}
