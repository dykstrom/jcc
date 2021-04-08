package se.dykstrom.jcc.basic.code.expression

import org.junit.Test
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.assembly.base.Instruction
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.ShiftLeftExpression
import se.dykstrom.jcc.common.code.expression.ShiftLeftCodeGenerator
import kotlin.test.assertEquals

/**
 * This class tests the common class [ShiftLeftCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class ShiftLeftCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = ShiftLeftCodeGenerator(context)

    @Test
    fun generateShiftLeftIdentifier() {
        // Given
        val identifierExpression = IdentifierDerefExpression(0, 0, IDENT_I64_FOO)
        val expression = ShiftLeftExpression(0, 0, identifierExpression, IL_4)
        val location = storageFactory.allocateNonVolatile()

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toAsm() }
        val legacyLines = codeGenerator.lines().filterIsInstance<Instruction>().map { it.toAsm() }

        // Then
        assertEquals(2, legacyLines.size)
        val moveLeft = """mov (r[a-z0-9]+), \[${identifierExpression.identifier.mappedName}]""".toRegex()
        val moveRight = """mov (r[a-z0-9]+), ${IL_4.value}""".toRegex()
        val left = assertRegexMatches(moveLeft, legacyLines[0])
        val right = assertRegexMatches(moveRight, legacyLines[1])
        assertEquals(2, lines.size)
        assertEquals("mov rcx, $right", lines[0])
        assertEquals("sal $left, cl", lines[1])
    }
}
