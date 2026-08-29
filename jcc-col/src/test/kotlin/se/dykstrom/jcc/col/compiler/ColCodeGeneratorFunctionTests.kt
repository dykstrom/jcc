package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.FL_2_0
import se.dykstrom.jcc.col.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.compiler.ColSymbols.*
import se.dykstrom.jcc.common.ast.FloatLiteral.FL_F32_0_0
import se.dykstrom.jcc.common.ast.FunctionCallExpression

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
    fun callFmuladdFunction() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_FMULADD_F64.identifier, listOf(FL_2_0, FL_2_0, FL_2_0), BF_FMULADD_F64))))
        assertContains(result, listOf(
            "declare double @llvm.fmuladd.f64(double, double, double)",
            "%0 = call double @llvm.fmuladd.f64(double 2.0, double 2.0, double 2.0)"
        ))
    }
}
