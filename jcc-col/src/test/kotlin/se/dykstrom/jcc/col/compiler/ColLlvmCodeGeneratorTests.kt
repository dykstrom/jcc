package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.compiler.ColSymbols.*
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_2_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_5
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.BooleanLiteral.FALSE
import se.dykstrom.jcc.common.ast.BooleanLiteral.TRUE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE_I32

internal class ColLlvmCodeGeneratorTests : AbstractColCodeGeneratorTests() {

    private val cg = ColLlvmCodeGenerator(typeManager, symbols, optimizer)

    @Test
    fun emptyProgram() {
        val result = assembleProgram(cg, listOf())
        assertContains(result, listOf("ret i32 0"))
    }

    @Test
    fun printlnLiteral() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, IL_5)))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64, i64 5)"))
    }

    @Test
    fun addIntLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, AddExpression(IL_5, IL_17))))
        assertContains(result, listOf("%0 = add i64 5, 17"))
    }

    @Test
    fun subIntLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, SubExpression(IL_5, IL_17))))
        assertContains(result, listOf("%0 = sub i64 5, 17"))
    }

    @Test
    fun mulIntLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, MulExpression(IL_5, IL_17))))
        assertContains(result, listOf("%0 = mul i64 5, 17"))
    }

    @Test
    fun divIntLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, IDivExpression(IL_5, IL_17))))
        assertContains(result, listOf("%0 = sdiv i64 5, 17"))
    }

    @Test
    fun modIntLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, ModExpression(IL_5, IL_17))))
        assertContains(result, listOf("%0 = srem i64 5, 17"))
    }

    @Test
    fun negIntLiteral() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, NegateExpression(ONE_I32))))
        assertContains(result, listOf("%0 = sub i32 0, 1"))
    }

    @Test
    fun eqIntLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, EqualExpression(IL_5, IL_17))))
        assertContains(result, listOf("%0 = icmp eq i64 5, 17"))
    }

    @Test
    fun addFloatLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, AddExpression(FL_1_0, FL_2_0))))
        assertContains(result, listOf(
            "%0 = fadd double 1.0, 2.0",
            "%1 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.F64, double %0)"
        ))
    }

    @Test
    fun subFloatLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, SubExpression(FL_1_0, FL_2_0))))
        assertContains(result, listOf("%0 = fsub double 1.0, 2.0"))
    }

    @Test
    fun mulFloatLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, MulExpression(FL_1_0, FL_2_0))))
        assertContains(result, listOf("%0 = fmul double 1.0, 2.0"))
    }

    @Test
    fun divFloatLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, DivExpression(FL_1_0, FL_2_0))))
        assertContains(result, listOf("%0 = fdiv double 1.0, 2.0"))
    }

    @Test
    fun negFloatLiteral() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F64, NegateExpression(FL_2_0))))
        assertContains(result, listOf("%0 = fneg double 2.0"))
    }

    @Test
    fun eqFloatLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, EqualExpression(FL_1_0, FL_1_0))))
        assertContains(result, listOf("%0 = fcmp oeq double 1.0, 1.0"))
    }

    @Test
    fun geFloatLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, GreaterOrEqualExpression(FL_1_0, FL_1_0))))
        assertContains(result, listOf("%0 = fcmp oge double 1.0, 1.0"))
    }

    @Test
    fun logicAndLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, LogicalAndExpression(TRUE, FALSE))))
        assertContains(result, listOf(
            "br i1 1, label %L1, label %L2",
            "br label %L2",
            "%0 = phi i1 [ 0, %L0 ], [ 0, %L1 ]",
        ))
    }

    @Test
    fun logicAndExpressions() {
        val ee = EqualExpression(IL_5, IL_17)
        val gee = GreaterOrEqualExpression(FL_1_0, FL_2_0)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, LogicalAndExpression(ee, gee))))
        assertContains(result, listOf(
            "%0 = icmp eq i64 5, 17",
            "br i1 %0, label %L1, label %L2",
            "%1 = fcmp oge double 1.0, 2.0",
            "br label %L2",
            "%2 = phi i1 [ 0, %L0 ], [ %1, %L1 ]",
        ))
    }
}
