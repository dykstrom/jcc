package se.dykstrom.jcc.basic.code.asm.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_FOO
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_4
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.assembly.instruction.Instruction
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.ShiftLeftExpression
import se.dykstrom.jcc.common.code.expression.ShiftLeftCodeGenerator

/**
 * This class tests the common class [ShiftLeftCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class ShiftLeftCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = ShiftLeftCodeGenerator(codeGenerator)

    @Test
    fun generateShiftLeftIdentifier() {
        // Given
        val identifierExpression = IdentifierDerefExpression(0, 0, IDENT_I64_FOO)
        val expression = ShiftLeftExpression(0, 0, identifierExpression, IL_4)
        val location = codeGenerator.storageFactory().allocateNonVolatile()

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toText() }

        // Then
        assertEquals(4, lines.size)
        val moveLeft = """mov (r[a-z0-9]+), \[${identifierExpression.identifier.mappedName}]""".toRegex()
        val moveRight = """mov (r[a-z0-9]+), ${IL_4.value}""".toRegex()
        val left = assertRegexMatches(moveLeft, lines[0])
        val right = assertRegexMatches(moveRight, lines[1])
        assertEquals("mov rcx, $right", lines[2])
        assertEquals("sal $left, cl", lines[3])
    }
}
