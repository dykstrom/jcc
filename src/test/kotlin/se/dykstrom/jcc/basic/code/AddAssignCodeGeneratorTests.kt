package se.dykstrom.jcc.basic.code

import org.junit.Test
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.ast.AddAssignStatement
import se.dykstrom.jcc.common.ast.ArrayAccessExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.code.AddAssignCodeGenerator
import se.dykstrom.jcc.common.code.Context
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import se.dykstrom.jcc.common.storage.StorageFactory
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * This class tests the common [AddAssignCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class AddAssignCodeGeneratorTests {

    private val identifier = Identifier("foo", I64.INSTANCE)

    private val types = BasicTypeManager()
    private val storageFactory = StorageFactory()
    private val astOptimizer = DefaultAstOptimizer(types)
    private val codeGenerator = BasicCodeGenerator(types, astOptimizer)
    private val context = Context(codeGenerator.symbols, types, storageFactory, codeGenerator)
    private val generator = AddAssignCodeGenerator(context)

    @Test
    fun generateAddAssignToScalarIdentifier() {
        // Given
        val identifierExpression = IdentifierNameExpression(0, 0, identifier)
        val statement = AddAssignStatement(0, 0, identifierExpression, IL_53)

        // When
        val lines = generator.generate(statement)

        // Then
        assertEquals(2, lines.size)
        assertEquals("add [${address(identifier)}], ${IL_53.value}", lines[1].toAsm())
    }

    @Test
    fun generateAddAssignToArrayIdentifier() {
        // Given
        val arrayIdentifier = Identifier(identifier.name, Arr.from(1, identifier.type))
        val identifierExpression = ArrayAccessExpression(0, 0, arrayIdentifier, listOf(IL_4))
        val statement = AddAssignStatement(0, 0, identifierExpression, IL_53)

        // When
        val lines = generator.generate(statement)

        // Then
        assertEquals(2, lines.size)
        val expected = """add \[${address(arrayIdentifier)}], ${IL_53.value}""".toRegex()
        assertRegexMatches(expected, lines[1].toAsm())
    }

    private fun assertRegexMatches(expected: Regex, actual : String) {
        assertTrue(expected.matches(actual), "\nExpected (regex) :${expected}\nActual (string)  :${actual}")
    }

    private fun address(identifier: Identifier) =
        if (identifier.type is Arr) """${identifier.mappedName}_arr\+8\*r[a-z0-9]+"""
        else identifier.mappedName

    companion object {
        private val IL_4 = IntegerLiteral(0, 0, 4)
        private val IL_53 = IntegerLiteral(0, 0, 53)
    }
}
