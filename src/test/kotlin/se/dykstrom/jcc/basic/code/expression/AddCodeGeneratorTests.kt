package se.dykstrom.jcc.basic.code.expression

import org.junit.Test
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.assembly.base.Instruction
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.ArrayAccessExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.code.expression.AddCodeGenerator
import se.dykstrom.jcc.common.types.F64
import kotlin.test.assertEquals

/**
 * This class tests the common class [AddCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class AddCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = AddCodeGenerator(context)

    @Test
    fun generateAddIdentifierAndArrayElement() {
        // Given
        val identifierExpression = IdentifierDerefExpression(0, 0, IDENT_I64_FOO)
        val arrayAccessExpression = ArrayAccessExpression(0, 0, IDENT_ARR_I64_TWO, listOf(IL_4, IL_53))
        val expression = AddExpression(0, 0, identifierExpression, arrayAccessExpression)
        val location = storageFactory.allocateNonVolatile()

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toAsm() }
        val legacyLines = codeGenerator.lines().filterIsInstance<Instruction>().map { it.toAsm() }

        // Then
        assertEquals(7, legacyLines.size)
        val moveLeft = """mov (r[a-z0-9]+), \[${IDENT_I64_FOO.mappedName}]""".toRegex()
        val left = assertRegexMatches(moveLeft, legacyLines[0])
        val moveOffset = """mov (r[a-z0-9]+), ${IL_4.value}""".toRegex()
        val offset = assertRegexMatches(moveOffset, legacyLines[1])
        val mulOffset = """imul $offset, (r[a-z0-9]+)""".toRegex()
        assertRegexMatches(mulOffset, legacyLines[3])
        val moveRight = """mov (r[a-z0-9]+), \[${IDENT_ARR_I64_TWO.mappedName}_arr\+8\*${offset}]""".toRegex()
        val right = assertRegexMatches(moveRight, legacyLines[6])
        assertEquals(1, lines.size)
        assertEquals("add $left, $right", lines[0])
    }

    @Test
    fun generateAddFloats() {
        // Given
        val expression = AddExpression(0, 0, FL_0_5, FL_1_0)
        val location = storageFactory.allocateNonVolatile(F64.INSTANCE)

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toAsm() }
        val legacyLines = codeGenerator.lines().filterIsInstance<Instruction>().map { it.toAsm() }

        // Then
        assertEquals(2, symbols.identifiers().count { it.type == F64.INSTANCE })
        assertEquals(2, legacyLines.size)
        val moveLeft = """movsd (xmm[0-9]), \[.*]""".toRegex()
        val left = assertRegexMatches(moveLeft, legacyLines[0])
        val moveRight = """movsd (xmm[0-9]), \[.*]""".toRegex()
        val right = assertRegexMatches(moveRight, legacyLines[1])
        assertEquals(1, lines.size)
        assertEquals("addsd $left, $right", lines[0])
    }
}
