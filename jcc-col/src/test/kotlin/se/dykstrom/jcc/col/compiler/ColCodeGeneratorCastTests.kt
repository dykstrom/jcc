package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.compiler.ColSymbols.*
import se.dykstrom.jcc.col.ColTests.Companion.FL_2_0
import se.dykstrom.jcc.col.ColTests.Companion.IL_5
import se.dykstrom.jcc.common.ast.FloatLiteral.FL_F32_0_0
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO_I32
import se.dykstrom.jcc.common.types.I128
import se.dykstrom.jcc.common.ast.IntegerLiteral

internal class ColCodeGeneratorCastTests : AbstractColCodeGeneratorTests() {

    @Test
    fun castF64ToF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_F32_F64.identifier, listOf(FL_2_0), BF_F32_F64))))
        assertContains(result, listOf("%0 = fptrunc double 2.0 to float"))
    }

    @Test
    fun castI32ToF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_F32_I32.identifier, listOf(ZERO_I32), BF_F32_I32))))
        assertContains(result, listOf("%0 = sitofp i32 0 to float"))
    }

    @Test
    fun castI64ToF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_F32_I64.identifier, listOf(IL_5), BF_F32_I64))))
        assertContains(result, listOf("%0 = sitofp i64 5 to float"))
    }

    @Test
    fun castI128ToF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_F32_I128.identifier, listOf(IL_5_I128), BF_F32_I128))))
        assertContains(result, listOf("%0 = sitofp i128 5 to float"))
    }

    @Test
    fun castF32ToF64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_F64_F32.identifier, listOf(FL_F32_0_0), BF_F64_F32))))
        assertContains(result, listOf("%0 = fpext float 0.0 to double"))
    }

    @Test
    fun castI32ToF64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_F64_I32.identifier, listOf(ZERO_I32), BF_F64_I32))))
        assertContains(result, listOf("%0 = sitofp i32 0 to double"))
    }

    @Test
    fun castI64ToF64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_F64_I64.identifier, listOf(IL_5), BF_F64_I64))))
        assertContains(result, listOf("%0 = sitofp i64 5 to double"))
    }

    @Test
    fun castI128ToF64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_F64_I128.identifier, listOf(IL_5_I128), BF_F64_I128))))
        assertContains(result, listOf("%0 = sitofp i128 5 to double"))
    }

    @Test
    fun castF32ToI32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, FunctionCallExpression(BF_I32_F32.identifier, listOf(FL_F32_0_0), BF_I32_F32))))
        assertContains(result, listOf("%0 = fptosi float 0.0 to i32"))
    }

    @Test
    fun castF64ToI32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, FunctionCallExpression(BF_I32_F64.identifier, listOf(FL_2_0), BF_I32_F64))))
        assertContains(result, listOf("%0 = fptosi double 2.0 to i32"))
    }

    @Test
    fun castI64ToI32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, FunctionCallExpression(BF_I32_I64.identifier, listOf(IL_5), BF_I32_I64))))
        assertContains(result, listOf("%0 = trunc i64 5 to i32"))
    }

    @Test
    fun castI128ToI32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, FunctionCallExpression(BF_I32_I128.identifier, listOf(IL_5_I128), BF_I32_I128))))
        assertContains(result, listOf("%0 = trunc i128 5 to i32"))
    }

    @Test
    fun castF32ToI64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_F32.identifier, listOf(FL_F32_0_0), BF_I64_F32))))
        assertContains(result, listOf("%0 = fptosi float 0.0 to i64"))
    }

    @Test
    fun castF64ToI64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_F64.identifier, listOf(FL_2_0), BF_I64_F64))))
        assertContains(result, listOf("%0 = fptosi double 2.0 to i64"))
    }

    @Test
    fun castI32ToI64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_I32.identifier, listOf(ZERO_I32), BF_I64_I32))))
        assertContains(result, listOf("%0 = sext i32 0 to i64"))
    }

    @Test
    fun castI128ToI64() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_I128.identifier, listOf(IL_5_I128), BF_I64_I128))))
        assertContains(result, listOf("%0 = trunc i128 5 to i64"))
    }

    @Test
    fun castF32ToI128() {
        // The result is cast to i64, because there is no println(i128)
        val cast = FunctionCallExpression(BF_I128_F32.identifier, listOf(FL_F32_0_0), BF_I128_F32)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_I128.identifier, listOf(cast), BF_I64_I128))))
        assertContains(result, listOf("%0 = fptosi float 0.0 to i128", "%1 = trunc i128 %0 to i64"))
    }

    @Test
    fun castF64ToI128() {
        // The result is cast to i64, because there is no println(i128)
        val cast = FunctionCallExpression(BF_I128_F64.identifier, listOf(FL_2_0), BF_I128_F64)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_I128.identifier, listOf(cast), BF_I64_I128))))
        assertContains(result, listOf("%0 = fptosi double 2.0 to i128", "%1 = trunc i128 %0 to i64"))
    }

    @Test
    fun castI32ToI128() {
        // The result is cast to i64, because there is no println(i128)
        val cast = FunctionCallExpression(BF_I128_I32.identifier, listOf(ZERO_I32), BF_I128_I32)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_I128.identifier, listOf(cast), BF_I64_I128))))
        assertContains(result, listOf("%0 = sext i32 0 to i128", "%1 = trunc i128 %0 to i64"))
    }

    @Test
    fun castI64ToI128() {
        // The result is cast to i64, because there is no println(i128)
        val cast = FunctionCallExpression(BF_I128_I64.identifier, listOf(IL_5), BF_I128_I64)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_I128.identifier, listOf(cast), BF_I64_I128))))
        assertContains(result, listOf("%0 = sext i64 5 to i128", "%1 = trunc i128 %0 to i64"))
    }

    @Test
    fun castToSameType() {
        // A same-type cast emits nothing, so the outer cast to i64 reads the literal itself
        listOf(
            Triple(BF_I32_I32, ZERO_I32, "%0 = sext i32 0 to i64"),
            Triple(BF_I128_I128, IL_5_I128, "%0 = trunc i128 5 to i64"),
            Triple(BF_F32_F32, FL_F32_0_0, "%0 = fptosi float 0.0 to i64"),
            Triple(BF_F64_F64, FL_2_0, "%0 = fptosi double 2.0 to i64"),
        ).forEach { (function, literal, line) ->
            val toI64 = ColSymbols().getFunction("i64", listOf(function.returnType))
            val cast = FunctionCallExpression(function.identifier, listOf(literal), function)
            val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(toI64.identifier, listOf(cast), toI64))))
            assertContains(result, listOf(line))
        }
        val cast = FunctionCallExpression(BF_I64_I64.identifier, listOf(IL_5), BF_I64_I64)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, cast)))
        assertNotContains(result, listOf("sext", "trunc"))
    }

    companion object {
        private val IL_5_I128 = IntegerLiteral(0, 0, "5", I128.INSTANCE)
    }
}
