package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_0_5
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_1_2
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_2_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_STR_S
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_5
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_BAR
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_FOO
import se.dykstrom.jcc.basic.ast.expression.EqvExpression
import se.dykstrom.jcc.basic.ast.expression.ImpExpression
import se.dykstrom.jcc.basic.ast.statement.PrintStatement
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_CINT_F64
import se.dykstrom.jcc.common.ast.*

internal class BasicLlvmCodeGeneratorTests : AbstractBasicCodeGeneratorTests() {

    private val cg = BasicLlvmCodeGenerator(typeManager, symbols, optimizer)

    @BeforeEach
    fun setUp() {
        symbols.addFunction(BF_CINT_F64)
    }

    @Test
    fun emptyProgram() {
        val result = assembleProgram(cg, listOf())
        assertContains(result, listOf("ret i32 0"))
    }

    @Test
    fun printLiteral() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(IL_5))))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64, i64 5)"))
    }

    @Test
    fun printTwoLiterals() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(IL_5, FL_2_0))))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64.F64, i64 5, double 2.0)"))
    }

    @Test
    fun printStringLiterals() {
        val result = assembleProgram(cg, listOf(
            PrintStatement(listOf(SL_FOO)),
            PrintStatement(listOf(SL_BAR)),
            PrintStatement(listOf(SL_FOO)),
        ))
        assertContains(result, listOf(
            "@_.str.0 = private constant [4 x i8] c\"foo\\00\"",
            "@_.str.1 = private constant [4 x i8] c\"bar\\00\"",
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str, ptr @_.str.0)",
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str, ptr @_.str.1)",
            "%2 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str, ptr @_.str.0)",
        ))
    }

    @Test
    fun arithmeticIntExpressions() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            AddExpression(IL_5, IL_3),
            SubExpression(IL_5, IL_3),
            MulExpression(IL_5, IL_3),
            IDivExpression(IL_5, IL_3),
            ModExpression(IL_5, IL_3),
            NegateExpression(ModExpression(IL_5, IL_3)),
        ))))
        assertContains(result, listOf(
            "%0 = add i64 5, 3",
            "%1 = sub i64 5, 3",
            "%2 = mul i64 5, 3",
            "%3 = sdiv i64 5, 3",
            "%4 = srem i64 5, 3",
            "%5 = srem i64 5, 3",
            "%6 = sub i64 0, %5",
        ))
    }

    @Test
    fun bitwiseIntExpressions() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            AndExpression(IL_5, IL_3),
            EqvExpression(IL_5, IL_3),
            ImpExpression(IL_5, IL_3),
            OrExpression(IL_5, IL_3),
            XorExpression(IL_5, IL_3),
            NotExpression(IL_5),
        ))))
        assertContains(result, listOf(
            // a AND b
            "%0 = and i64 5, 3",
            // a EQV b == NOT(a XOR b)
            "%1 = xor i64 5, 3",
            "%2 = xor i64 %1, -1",
            // a IMP b == NOT(a) OR b
            "%3 = xor i64 5, -1",
            "%4 = or i64 %3, 3",
            // a OR b
            "%5 = or i64 5, 3",
            // a XOR b
            "%6 = xor i64 5, 3",
            // NOT a
            "%7 = xor i64 5, -1",
        ))
    }

    @Test
    fun relationalIntExpressions() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            EqualExpression(IL_5, IL_3),
            NotEqualExpression(IL_5, IL_3),
            LessExpression(IL_5, IL_3),
            LessOrEqualExpression(IL_5, IL_3),
            GreaterExpression(IL_5, IL_3),
            GreaterOrEqualExpression(IL_5, IL_3),
        ))))
        assertContains(result, listOf(
            "%0 = icmp eq i64 5, 3",
            "%1 = zext i1 %0 to i64",
            "%2 = sub i64 0, %1",
            "%3 = icmp ne i64 5, 3",
            "%4 = zext i1 %3 to i64",
            "%5 = sub i64 0, %4",
            "%6 = icmp slt i64 5, 3",
            "%7 = zext i1 %6 to i64",
            "%8 = sub i64 0, %7",
            "%9 = icmp sle i64 5, 3",
            "%10 = zext i1 %9 to i64",
            "%11 = sub i64 0, %10",
            "%12 = icmp sgt i64 5, 3",
            "%13 = zext i1 %12 to i64",
            "%14 = sub i64 0, %13",
            "%15 = icmp sge i64 5, 3",
            "%16 = zext i1 %15 to i64",
            "%17 = sub i64 0, %16",
        ))
    }

    @Test
    fun arithmeticFloatExpressions() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            AddExpression(FL_0_5, FL_1_2),
            SubExpression(FL_0_5, FL_1_2),
            MulExpression(FL_0_5, FL_1_2),
            DivExpression(FL_0_5, FL_1_2),
            ModExpression(FL_0_5, FL_1_2),
            PowExpression(FL_0_5, FL_1_2),
            NegateExpression(AddExpression(FL_0_5, FL_1_2)),
        ))))
        assertContains(result, listOf(
            "%0 = fadd double 0.5, 1.2",
            "%1 = fsub double 0.5, 1.2",
            "%2 = fmul double 0.5, 1.2",
            "%3 = fdiv double 0.5, 1.2",
            "%4 = frem double 0.5, 1.2",
            "%5 = call double @llvm.pow.f64(double 0.5, double 1.2)",
            "%6 = fadd double 0.5, 1.2",
            "%7 = fneg double %6",
        ))
    }

    @Test
    fun callFunctionCint() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            FunctionCallExpression(BF_CINT_F64.identifier, listOf(FL_0_5)),
        ))))
        assertContains(result, listOf(
            "%0 = call double @llvm.round.f64(double 0.5)",
            "%1 = fptosi double %0 to i64",
        ))
    }

    @Test
    fun printUndefinedVariables() {
        val result = assembleProgram(cg, listOf(
            PrintStatement(listOf(IDE_I64_A)),
            PrintStatement(listOf(IDE_F64_F)),
            PrintStatement(listOf(IDE_STR_S)),
        ))
        assertContains(result, listOf(
            "@_a.pe = private global i64 0",
            "@_f.ha = private global double 0.0",
            "%0 = load i64, ptr @_a.pe",
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64, i64 %0)",
            "%2 = load double, ptr @_f.ha",
            "%3 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.F64, double %2)",
            "%4 = load ptr, ptr @_s.do",
            "%5 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str, ptr %4)",
        ))
    }

    @Test
    fun callCls() {
        val result = assembleProgram(cg, listOf(ClsStatement()))
        assertContains(result, listOf(
            "@_.cls.ansi.codes = private constant",
        ))
    }

    @Test
    fun gotoLabel() {
        val result = assembleProgram(cg, listOf(
            LabelledStatement("foo", GotoStatement("bar")))
        )
        assertContains(result, listOf(
            "_foo:",
            "br label %_bar",
        ))
    }
}
