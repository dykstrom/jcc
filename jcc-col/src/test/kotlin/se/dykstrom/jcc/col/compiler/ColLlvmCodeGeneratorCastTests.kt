package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.compiler.ColSymbols.*
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_2_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_5
import se.dykstrom.jcc.common.ast.FloatLiteral.FL_F32_0_0
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO_I32

internal class ColLlvmCodeGeneratorCastTests : AbstractColCodeGeneratorTests() {

    private val cg = ColLlvmCodeGenerator(typeManager, symbols, optimizer)

    @Test
    fun castF64ToF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_F32_F64.identifier, listOf(FL_2_0)))))
        assertContains(result, listOf("%0 = fptrunc double 2.0 to float"))
    }

    @Test
    fun castI32ToF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_F32_I32.identifier, listOf(ZERO_I32)))))
        assertContains(result, listOf("%0 = sitofp i32 0 to float"))
    }

    @Test
    fun castI64ToF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_F32_I64.identifier, listOf(IL_5)))))
        assertContains(result, listOf("%0 = sitofp i64 5 to float"))
    }

    @Test
    fun castF32ToF64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_F64_F32.identifier, listOf(FL_F32_0_0)))))
        assertContains(result, listOf("%0 = fpext float 0.0 to double"))
    }

    @Test
    fun castI32ToF64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_F64_I32.identifier, listOf(ZERO_I32)))))
        assertContains(result, listOf("%0 = sitofp i32 0 to double"))
    }

    @Test
    fun castI64ToF64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_F64_I64.identifier, listOf(IL_5)))))
        assertContains(result, listOf("%0 = sitofp i64 5 to double"))
    }

    @Test
    fun castF32ToI32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, FunctionCallExpression(BF_I32_F32.identifier, listOf(FL_F32_0_0)))))
        assertContains(result, listOf("%0 = fptosi float 0.0 to i32"))
    }

    @Test
    fun castF64ToI32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, FunctionCallExpression(BF_I32_F64.identifier, listOf(FL_2_0)))))
        assertContains(result, listOf("%0 = fptosi double 2.0 to i32"))
    }

    @Test
    fun castI64ToI32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, FunctionCallExpression(BF_I32_I64.identifier, listOf(IL_5)))))
        assertContains(result, listOf("%0 = trunc i64 5 to i32"))
    }

    @Test
    fun castF32ToI64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_F32.identifier, listOf(FL_F32_0_0)))))
        assertContains(result, listOf("%0 = fptosi float 0.0 to i64"))
    }

    @Test
    fun castF64ToI64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_F64.identifier, listOf(FL_2_0)))))
        assertContains(result, listOf("%0 = fptosi double 2.0 to i64"))
    }

    @Test
    fun castI32ToI64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_I32.identifier, listOf(ZERO_I32)))))
        assertContains(result, listOf("%0 = sext i32 0 to i64"))
    }
}
