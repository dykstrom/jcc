package se.dykstrom.jcc.basic.code.expression

import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_MID2
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
        symbols.addFunction(FUN_MID2)
        symbols.addFunction(FUN_VAL)
    }

    @Test
    fun generateFunctionCall() {
        // Given
        val expression = FunctionCallExpression(0, 0, FUN_VAL.identifier, listOf(SL_A))
        val location = storageFactory.allocateNonVolatile()

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toAsm() }

        // Then
        assertEquals(6, lines.size)
        val moveArg = """mov (r[a-z0-9]+), __string_0""".toRegex()
        assertRegexMatches(moveArg, lines[0])
        assertEquals("call [${FUN_VAL.mappedName}]", lines[3])
        val moveResult = """mov (r[a-z0-9]+), rax""".toRegex()
        assertRegexMatches(moveResult, lines[5])
    }

    @Test
    fun generateFunctionCallWithFunctionCallArg() {
        // Given
        val valExpression = FunctionCallExpression(0, 0, FUN_VAL.identifier, listOf(SL_A))
        val midExpression = FunctionCallExpression(0, 0, FUN_MID2.identifier, listOf(SL_B, valExpression))
        val location = storageFactory.allocateNonVolatile()

        // When
        val lines = generator.generate(midExpression, location).filterIsInstance<Instruction>().map { it.toAsm() }

        // Then
        assertEquals(13, lines.size)
        val moveMidArg0 = """mov (r[a-z0-9]+), __string_0""".toRegex()
        val midArg0 = assertRegexMatches(moveMidArg0, lines[0])
        val moveValArg = """mov (r[a-z0-9]+), __string_1""".toRegex()
        assertRegexMatches(moveValArg, lines[1])
        assertEquals("call [${FUN_VAL.mappedName}]", lines[4])
        val moveValResult = """mov (r[a-z0-9]+), rax""".toRegex()
        val midArg1 = assertRegexMatches(moveValResult, lines[6])
        assertRegexMatches("mov rcx, $midArg0".toRegex(), lines[7])
        assertRegexMatches("mov rdx, $midArg1".toRegex(), lines[8])
        assertEquals("call _${FUN_MID2.mappedName}", lines[10])
        val moveMidResult = """mov (r[a-z0-9]+), rax""".toRegex()
        assertRegexMatches(moveMidResult, lines[12])
    }
}
