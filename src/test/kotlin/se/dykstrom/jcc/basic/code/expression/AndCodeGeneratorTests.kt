package se.dykstrom.jcc.basic.code.expression

import org.junit.Test
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.assembly.base.Instruction
import se.dykstrom.jcc.common.ast.AndExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.code.expression.AndCodeGenerator
import kotlin.test.assertEquals

/**
 * This class tests the common class [AndCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class AndCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = AndCodeGenerator(context)

    @Test
    fun generateAndLiteralAndIdentifier() {
        // Given
        val identifierExpression = IdentifierDerefExpression(0, 0, IDENT_I64_FOO)
        val expression = AndExpression(0, 0, identifierExpression, IL_53)
        val location = storageFactory.allocateNonVolatile()

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toAsm() }
        val legacyLines = codeGenerator.lines().filterIsInstance<Instruction>().map { it.toAsm() }

        // Then
        assertEquals(2, legacyLines.size)
        val moveLeft = """mov (r[a-z0-9]+), \[${identifierExpression.identifier.mappedName}]""".toRegex()
        val moveRight = """mov (r[a-z0-9]+), ${IL_53.value}""".toRegex()
        val left = assertRegexMatches(moveLeft, legacyLines[0])
        val right = assertRegexMatches(moveRight, legacyLines[1])
        assertEquals(1, lines.size)
        assertEquals("and $left, $right", lines[0])
    }
}
