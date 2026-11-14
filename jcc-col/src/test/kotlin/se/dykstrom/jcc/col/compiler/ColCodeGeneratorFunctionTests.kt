package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.FL_2_0
import se.dykstrom.jcc.col.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.compiler.ColSymbols.*
import se.dykstrom.jcc.common.ast.FloatLiteral.FL_F32_0_0
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.types.I128
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO_I32

internal class ColCodeGeneratorFunctionTests : AbstractColCodeGeneratorTests() {

    // COL now wires the real collector, so the GC plumbing these programs emit is asserted in
    // ColCodeGeneratorGcTests instead of ruled out here.

    @Test
    fun callIntrinsicLlvmFunction() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_SQRT_F64.identifier, listOf(FL_2_0), BF_SQRT_F64))))
        assertContains(result, listOf(
            "declare double @llvm.sqrt.f64(double)",
            "%0 = call double @llvm.sqrt.f64(double 2.0)"
        ))
    }

    @Test
    fun callIntrinsicLlvmFunctionOfTypeF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_SQRT_F32.identifier, listOf(FL_F32_0_0), BF_SQRT_F32))))
        assertContains(result, listOf(
            "declare float @llvm.sqrt.f32(float)",
            "%0 = call float @llvm.sqrt.f32(float 0.0)"
        ))
    }

    @Test
    fun callIntrinsicAbsFunction() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_ABS_I64.identifier, listOf(IL_5), BF_ABS_I64))))
        assertContains(result, listOf(
            "declare i64 @llvm.abs.i64(i64, i1)",
            "%0 = call i64 @llvm.abs.i64(i64 5, i1 1)"
        ))
    }

    @Test
    fun callIntrinsicAbsFunctionOfTypeI128() {
        // The result is cast to i64, because there is no println(i128)
        val abs = FunctionCallExpression(BF_ABS_I128.identifier, listOf(IL_5_I128), BF_ABS_I128)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(BF_I64_I128.identifier, listOf(abs), BF_I64_I128))))
        assertContains(result, listOf(
            "declare i128 @llvm.abs.i128(i128, i1)",
            "%0 = call i128 @llvm.abs.i128(i128 5, i1 1)"
        ))
    }

    @Test
    fun callIntrinsicMinAndMaxFunctions() {
        listOf(
            Triple(BF_MIN_I32_I32, ZERO_I32, "i32 @llvm.smin.i32(i32"),
            Triple(BF_MAX_I32_I32, ZERO_I32, "i32 @llvm.smax.i32(i32"),
            Triple(BF_MIN_I64_I64, IL_5, "i64 @llvm.smin.i64(i64"),
            Triple(BF_MAX_I64_I64, IL_5, "i64 @llvm.smax.i64(i64"),
            Triple(BF_MIN_I128_I128, IL_5_I128, "i128 @llvm.smin.i128(i128"),
            Triple(BF_MAX_I128_I128, IL_5_I128, "i128 @llvm.smax.i128(i128"),
            Triple(BF_MIN_F32_F32, FL_F32_0_0, "float @llvm.minimum.f32(float"),
            Triple(BF_MAX_F32_F32, FL_F32_0_0, "float @llvm.maximum.f32(float"),
            Triple(BF_MIN_F64_F64, FL_2_0, "double @llvm.minimum.f64(double"),
            Triple(BF_MAX_F64_F64, FL_2_0, "double @llvm.maximum.f64(double"),
        ).forEach { (function, literal, text) ->
            // The result is cast to i64, because println does not take every type
            val toI64 = ColSymbols().getFunction("i64", listOf(function.returnType))
            val call = FunctionCallExpression(function.identifier, listOf(literal, literal), function)
            val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, FunctionCallExpression(toI64.identifier, listOf(call), toI64))))
            assertContains(result, listOf("declare $text", "%0 = call $text"))
        }
    }

    @Test
    fun callIntrinsicSinFunction() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_SIN_F64.identifier, listOf(FL_2_0), BF_SIN_F64))))
        assertContains(result, listOf(
            "declare double @llvm.sin.f64(double)",
            "%0 = call double @llvm.sin.f64(double 2.0)"
        ))
    }

    @Test
    fun callIntrinsicSinFunctionOfTypeF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_SIN_F32.identifier, listOf(FL_F32_0_0), BF_SIN_F32))))
        assertContains(result, listOf(
            "declare float @llvm.sin.f32(float)",
            "%0 = call float @llvm.sin.f32(float 0.0)"
        ))
    }

    @Test
    fun callIntrinsicLog2Function() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_LOG2_F64.identifier, listOf(FL_2_0), BF_LOG2_F64))))
        assertContains(result, listOf(
            "declare double @llvm.log2.f64(double)",
            "%0 = call double @llvm.log2.f64(double 2.0)"
        ))
    }

    @Test
    fun callLibmCbrtFunction() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_CBRT_F64.identifier, listOf(FL_2_0), BF_CBRT_F64))))
        assertContains(result, listOf(
            "declare double @cbrt(double)",
            "%0 = call double @cbrt(double 2.0)"
        ))
    }

    @Test
    fun callLibmCbrtFunctionOfTypeF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FunctionCallExpression(BF_CBRT_F32.identifier, listOf(FL_F32_0_0), BF_CBRT_F32))))
        assertContains(result, listOf(
            "declare float @cbrtf(float)",
            "%0 = call float @cbrtf(float 0.0)"
        ))
    }

    @Test
    fun callFmodFunction() {
        // fmod is inlined to frem, which is defined to produce the same value (issue #99)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_FMOD_F64_F64.identifier, listOf(FL_2_0, FL_2_0), BF_FMOD_F64_F64))))
        assertContains(result, listOf(
            "%0 = frem double 2.0, 2.0"
        ))
    }

    @Test
    fun callMuladdFunction() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_MULADD_F64.identifier, listOf(FL_2_0, FL_2_0, FL_2_0), BF_MULADD_F64))))
        assertContains(result, listOf(
            "declare double @llvm.fmuladd.f64(double, double, double)",
            "%0 = call double @llvm.fmuladd.f64(double 2.0, double 2.0, double 2.0)"
        ))
    }

    companion object {
        private val IL_5_I128 = IntegerLiteral(0, 0, "5", I128.INSTANCE)
    }
}
