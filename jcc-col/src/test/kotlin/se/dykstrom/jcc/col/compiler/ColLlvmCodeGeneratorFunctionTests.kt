package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_F64
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_SQRT_F64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_2_0
import se.dykstrom.jcc.common.ast.FunctionCallExpression

internal class ColLlvmCodeGeneratorFunctionTests : AbstractColCodeGeneratorTests() {

    private val cg = ColLlvmCodeGenerator(typeManager, symbols, optimizer)

    @Test
    fun callIntrinsicLlvmFunction() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, FunctionCallExpression(BF_SQRT_F64.identifier, listOf(FL_2_0)))))
        println(result.toText())
        assertContains(result, listOf(
            "declare double @llvm.sqrt.f64(double)",
            "%0 = call double @llvm.sqrt.f64(double 2.0)"
        ))
    }
}
