package se.dykstrom.jcc.basic.code.asm.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_A
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_B
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_MID_STR_I64
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_VAL_STR
import se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_MID_STR_I64
import se.dykstrom.jcc.basic.type.BasicTypeManager
import se.dykstrom.jcc.common.assembly.instruction.Instruction
import se.dykstrom.jcc.common.ast.CastToI64Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.RoundExpression
import se.dykstrom.jcc.common.code.expression.FunctionCallCodeGenerator
import se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_ATOF_STR
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.llvm.code.LlvmBuiltIns.LF_ROUNDEVEN_F64

/**
 * This class tests the common class [FunctionCallCodeGenerator] but it uses Basic classes,
 * for example the [BasicTypeManager] so it needs to be part of the Basic tests.
 */
class FunctionCallCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = BasicFunctionCallCodeGenerator(codeGenerator)

    @BeforeEach
    fun setUp() {
        symbols.addFunction(BF_MID_STR_I64)
        symbols.addFunction(BF_VAL_STR)
    }

    @Test
    fun generateFunctionCall() {
        // Given
        val expression = FunctionCallExpression(0, 0, BF_VAL_STR.identifier, listOf(SL_A))
        val location = codeGenerator.storageFactory().allocateNonVolatile(F64.INSTANCE)

        // When
        val lines = generator.generate(expression, location).filterIsInstance<Instruction>().map { it.toText() }

        // Then
        assertEquals(5, lines.size)
        val moveArg = "mov r[a-z0-9]+, __string_0".toRegex()
        assertRegexMatches(moveArg, lines[0])
        assertEquals("call [${CF_ATOF_STR.mappedName}]", lines[2])
        val moveResult = "movsd xmm[0-9], xmm0".toRegex()
        assertRegexMatches(moveResult, lines[4])
    }

    @Test
    fun generateFunctionCallWithFunctionCallArg() {
        // Given
        val valExpression = FunctionCallExpression(0, 0, BF_VAL_STR.identifier, listOf(SL_A))
        // VAL returns a double but MID's second parameter is an integer, so semantic analysis would
        // wrap it in a rounding float->int cast (issue #52); supply that cast here.
        val roundedVal = CastToI64Expression(0, 0, RoundExpression(valExpression, LF_ROUNDEVEN_F64))
        val midExpression = FunctionCallExpression(0, 0, BF_MID_STR_I64.identifier, listOf(SL_B, roundedVal))
        val location = codeGenerator.storageFactory().allocateNonVolatile()

        // When
        val lines = generator.generate(midExpression, location).filterIsInstance<Instruction>().map { it.toText() }

        // Then
        assertEquals(13, lines.size)
        val moveValArg = "mov r[a-z0-9]+, __string_0".toRegex()
        assertRegexMatches(moveValArg, lines[0])
        assertEquals("call [${CF_ATOF_STR.mappedName}]", lines[2])
        val moveValResult = "movsd (xmm[a-z0-9]+), xmm0".toRegex()
        val valResult = assertRegexMatches(moveValResult, lines[4])
        // Round the double return value from val half-to-even, then truncate it to an integer (issue #52)
        assertRegexMatches("roundsd $valResult, $valResult, 1000b".toRegex(), lines[5])
        val midArg1 = assertRegexMatches("cvttsd2si (r[a-z0-9]+), $valResult".toRegex(), lines[6])
        val moveMidArg0 = "mov r[a-z0-9]+, __string_1".toRegex()
        assertRegexMatches(moveMidArg0, lines[7])
        assertEquals("mov rdx, $midArg1", lines[8])
        assertEquals("call [${JF_MID_STR_I64.mappedName}]", lines[10])
        val moveMidResult = "mov r[a-z0-9]+, rax".toRegex()
        assertRegexMatches(moveMidResult, lines[12])
    }
}
