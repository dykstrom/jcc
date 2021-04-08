package se.dykstrom.jcc.basic.code.expression

import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_VAL
import se.dykstrom.jcc.common.assembly.base.Instruction
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.code.expression.FunctionCallCodeGenerator
import kotlin.test.assertEquals

/**
 * This class tests the common class [FunctionCallCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class FunctionCallCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = FunctionCallCodeGenerator(context)

    @Before
    fun setUp() {
        symbols.addFunction(FUN_VAL)
    }

    @Test
    fun generateFunctionCall() {
        // Given
        val expression = FunctionCallExpression(0, 0, FUN_VAL.identifier, listOf(SL_A))
        val location = storageFactory.allocateNonVolatile()

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toAsm() }
        val legacyLines = codeGenerator.lines().filterIsInstance<Instruction>().map { it.toAsm() }

        // Then
        assertEquals(6, legacyLines.size)
        val moveArg = """mov (r[a-z0-9]+), __string_0""".toRegex()
        assertRegexMatches(moveArg, legacyLines[0])
        assertEquals("call [${FUN_VAL.mappedName}]", legacyLines[3])
        val moveResult = """mov (r[a-z0-9]+), rax""".toRegex()
        assertRegexMatches(moveResult, legacyLines[5])
    }
}
