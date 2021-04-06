package se.dykstrom.jcc.basic.code.statement

import org.junit.Test
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.ast.AddAssignStatement
import se.dykstrom.jcc.common.ast.ArrayAccessExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.code.statement.AddAssignCodeGenerator
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.Identifier
import kotlin.test.assertEquals

/**
 * This class tests the common [AddAssignCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class AddAssignCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = AddAssignCodeGenerator(context)

    @Test
    fun generateAddAssignToScalarIdentifier() {
        // Given
        val identifierExpression = IdentifierNameExpression(0, 0, IDENT_I64_FOO)
        val statement = AddAssignStatement(0, 0, identifierExpression, IL_53)

        // When
        val lines = generator.generate(statement)

        // Then
        assertEquals(2, lines.size)
        assertEquals("add [${address(IDENT_I64_FOO)}], ${IL_53.value}", lines[1].toAsm())
    }

    @Test
    fun generateAddAssignToArrayIdentifier() {
        // Given
        val arrayIdentifier = Identifier(IDENT_I64_FOO.name, Arr.from(1, IDENT_I64_FOO.type))
        val identifierExpression = ArrayAccessExpression(0, 0, arrayIdentifier, listOf(IL_4))
        val statement = AddAssignStatement(0, 0, identifierExpression, IL_53)

        // When
        val lines = generator.generate(statement)

        // Then
        assertEquals(2, lines.size)
        val expected = """add \[${address(arrayIdentifier)}], ${IL_53.value}""".toRegex()
        assertRegexMatches(expected, lines[1].toAsm())
    }

    companion object {
        private val IL_4 = IntegerLiteral(0, 0, 4)
        private val IL_53 = IntegerLiteral(0, 0, 53)
    }
}
