package se.dykstrom.jcc.basic.code.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_FOO
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_53
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.assembly.instruction.Instruction
import se.dykstrom.jcc.common.ast.AndExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.code.expression.AndCodeGenerator

/**
 * This class tests the common class [AndCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class AndCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = AndCodeGenerator(codeGenerator)

    @Test
    fun generateAndLiteralAndIdentifier() {
        // Given
        val identifierExpression = IdentifierDerefExpression(0, 0, IDENT_I64_FOO)
        val expression = AndExpression(0, 0, identifierExpression, IL_53)
        val location = codeGenerator.storageFactory().allocateNonVolatile()

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toText() }

        // Then
        assertEquals(3, lines.size)
        val moveLeft = """mov (r[a-z0-9]+), \[${identifierExpression.identifier.mappedName}]""".toRegex()
        val moveRight = """mov (r[a-z0-9]+), ${IL_53.value}""".toRegex()
        val left = assertRegexMatches(moveLeft, lines[0])
        val right = assertRegexMatches(moveRight, lines[1])
        assertEquals("and $left, $right", lines[2])
    }
}
