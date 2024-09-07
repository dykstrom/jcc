package se.dykstrom.jcc.basic.code.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_A
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_B
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_MID2
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_VAL
import se.dykstrom.jcc.common.assembly.instruction.Instruction
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.code.expression.FunctionCallCodeGenerator
import se.dykstrom.jcc.common.types.F64

/**
 * This class tests the common class [FunctionCallCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class FunctionCallCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = FunctionCallCodeGenerator(codeGenerator)

    @BeforeEach
    fun setUp() {
        symbols.addFunction(FUN_MID2)
        symbols.addFunction(FUN_VAL)
    }

    @Test
    fun generateFunctionCall() {
        // Given
        val expression = FunctionCallExpression(0, 0, FUN_VAL.identifier, listOf(SL_A))
        val location = codeGenerator.storageFactory().allocateNonVolatile(F64.INSTANCE)

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toText() }

        // Then
        assertEquals(5, lines.size)
        val moveArg = "mov r[a-z0-9]+, __string_0".toRegex()
        assertRegexMatches(moveArg, lines[0])
        assertEquals("call [${FUN_VAL.mappedName}]", lines[2])
        val moveResult = "movsd xmm[0-9], xmm0".toRegex()
        assertRegexMatches(moveResult, lines[4])
    }

    @Test
    fun generateFunctionCallWithFunctionCallArg() {
        // Given
        val valExpression = FunctionCallExpression(0, 0, FUN_VAL.identifier, listOf(SL_A))
        val midExpression = FunctionCallExpression(0, 0, FUN_MID2.identifier, listOf(SL_B, valExpression))
        val location = codeGenerator.storageFactory().allocateNonVolatile()

        // When
        val lines = generator.generate(midExpression, location).filterIsInstance<Instruction>().map { it.toText() }

        // Then
        assertEquals(11, lines.size)
        val moveValArg = "mov r[a-z0-9]+, __string_0".toRegex()
        assertRegexMatches(moveValArg, lines[0])
        assertEquals("call [${FUN_VAL.mappedName}]", lines[2])
        val moveValResult = "movsd (xmm[a-z0-9]), xmm0".toRegex()
        val midArg1 = assertRegexMatches(moveValResult, lines[4])
        val moveMidArg0 = "mov r[a-z0-9]+, __string_1".toRegex()
        assertRegexMatches(moveMidArg0, lines[5])
        // Round the return value from val to an integer
        assertRegexMatches("cvtsd2si rdx, $midArg1".toRegex(), lines[6])
        assertEquals("call _${FUN_MID2.mappedName}", lines[8])
        val moveMidResult = "mov r[a-z0-9]+, rax".toRegex()
        assertRegexMatches(moveMidResult, lines[10])
    }
}
