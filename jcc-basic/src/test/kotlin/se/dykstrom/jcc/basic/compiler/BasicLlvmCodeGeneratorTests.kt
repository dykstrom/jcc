package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_0_5
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_1_2
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_2_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_F64_G
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_B
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_STR_S
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_STR_X
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_STR_S
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_5
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_F64_G
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_B
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_STR_S
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_STR_X
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_BAR
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_FOO
import se.dykstrom.jcc.basic.ast.expression.EqvExpression
import se.dykstrom.jcc.basic.ast.expression.ImpExpression
import se.dykstrom.jcc.basic.ast.statement.LineInputStatement
import se.dykstrom.jcc.basic.ast.statement.PrintStatement
import se.dykstrom.jcc.basic.ast.statement.RandomizeStatement
import se.dykstrom.jcc.basic.ast.statement.SleepStatement
import se.dykstrom.jcc.basic.ast.statement.SwapStatement
import se.dykstrom.jcc.basic.compiler.BasicSymbols.*
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.symbols.Scope.GLOBAL

internal class BasicLlvmCodeGeneratorTests : AbstractBasicCodeGeneratorTests() {

    private val cg = BasicLlvmCodeGenerator(typeManager, symbols, optimizer)

    @BeforeEach
    fun setUp() {
        symbols.addFunction(BF_ASC_STR)
        symbols.addFunction(BF_CINT_F64)
        symbols.addFunction(BF_LEN_STR)
    }

    // ------------------------------------------------------------------------
    // Implicit cast sites (issue #52). The LLVM backend assumes casts are made
    // explicit in the AST (as COL does) and does NOT re-derive them; given a
    // *bare* mixed-type AST it emits INVALID IR (an integer where a double is
    // required, and vice versa). These tests construct bare ASTs directly,
    // WITHOUT running semantic analysis, to document that backend contract.
    //
    // As of Phase 3, BASIC semantic analysis inserts explicit cast nodes
    // (CastToF64Expression / CastToI64Expression(RoundExpression(...))), so a
    // real compilation never feeds the backend a bare mixed-type AST and the
    // invalid IR below is no longer reachable in practice. The backend itself
    // is unchanged, so these tests still pass; correct lowering of the inserted
    // casts is covered by the semantics tests, the FASM codegen tests, and the
    // compile-and-run ITs. Contrast the FASM backend, which re-derives the
    // conversions itself (see BasicCodeGeneratorFloatTests).
    // ------------------------------------------------------------------------

    @Test
    fun assignIntToFloatEmitsInvalidStoreToday() {
        // LET f# = a% : should sitofp, but currently stores the raw i64 into the double variable
        val result = assembleProgram(cg, listOf(AssignStatement(INE_F64_F, IDE_I64_A)))
        assertContains(result, listOf(
            "%0 = load i64, ptr @_a.pe",
            "store i64 %0, ptr @_f.ha", // INVALID: i64 stored into a double global; Phase 3 -> sitofp
        ))
        assertNotContains(result, listOf("sitofp"))
    }

    @Test
    fun assignFloatToIntEmitsInvalidStoreToday() {
        // LET a% = f# : should round + fptosi, but currently stores the raw double into the i64 variable
        val result = assembleProgram(cg, listOf(AssignStatement(INE_I64_A, IDE_F64_F)))
        assertContains(result, listOf(
            "%0 = load double, ptr @_f.ha",
            "store double %0, ptr @_a.pe", // INVALID: double stored into an i64 global; Phase 3 -> fptosi
        ))
        assertNotContains(result, listOf("fptosi"))
    }

    @Test
    fun binaryIntPlusFloatEmitsInvalidFaddToday() {
        // a% + 2.0 : the integer operand should be promoted (sitofp) to a double before the fadd
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(AddExpression(IDE_I64_A, FL_2_0)))))
        assertContains(result, listOf(
            "%0 = load i64, ptr @_a.pe",
            "%1 = fadd i64 %0, 2.0", // INVALID: fadd on i64 mixing an i64 and a double; Phase 3 -> fadd double after sitofp
        ))
        assertNotContains(result, listOf("sitofp"))
    }

    @Test
    fun relationalIntVsFloatEmitsInvalidIcmpToday() {
        // a% > 2.0 : the integer operand should be promoted to a double and compared with fcmp
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(GreaterExpression(IDE_I64_A, FL_2_0)))))
        assertContains(result, listOf(
            "%0 = load i64, ptr @_a.pe",
            "%1 = icmp sgt i64 %0, 2.0", // INVALID: icmp on i64 against a double literal; Phase 3 -> fcmp after sitofp
        ))
        assertNotContains(result, listOf("fcmp"))
    }

    @Test
    fun functionArgIntToFloatEmitsInvalidCallToday() {
        // sin(5) : the integer argument should be promoted (sitofp) to match the double parameter
        symbols.addFunction(BF_SIN_F64)
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            FunctionCallExpression(BF_SIN_F64.identifier, listOf(IL_5), BF_SIN_F64),
        ))))
        assertContains(result, listOf(
            "%0 = call double @llvm.sin.f64(i64 5)", // INVALID: i64 passed to a double parameter; Phase 3 -> sitofp
        ))
        assertNotContains(result, listOf("sitofp"))
    }

    @Test
    fun functionArgFloatToIntEmitsInvalidCallToday() {
        // abs(2.0) : the double argument should be rounded + fptosi to match the i64 parameter
        symbols.addFunction(BF_ABS_I64)
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            FunctionCallExpression(BF_ABS_I64.identifier, listOf(FL_2_0), BF_ABS_I64),
        ))))
        assertContains(result, listOf(
            "%0 = call i64 @llvm.abs.i64(double 2.0, i1 1)", // INVALID: double passed to an i64 parameter; Phase 3 -> fptosi
        ))
        assertNotContains(result, listOf("fptosi"))
    }

    @Test
    fun sleepIntArgumentEmitsInvalidCallToday() {
        // SLEEP 5 : SLEEP takes a double, so the integer argument should be promoted (sitofp)
        val result = assembleProgram(cg, listOf(SleepStatement(0, 0, IL_5)))
        assertContains(result, listOf(
            "call void @sleep_F64(i64 5)", // INVALID: i64 passed to a double parameter; Phase 3 -> sitofp
        ))
        assertNotContains(result, listOf("sitofp"))
    }

    @Test
    fun emptyProgram() {
        val result = assembleProgram(cg, listOf())
        assertContains(result, listOf("ret i32 0"))
    }

    @Test
    fun printLiteral() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(IL_5))))
        assertContains(result, listOf(
            "@_.printf.fmt.I64.nl = private constant [6 x i8] c\"%lld\\0A\\00\"", // With EOL
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64.nl, i64 5)"),
        )
    }

    @Test
    fun printLiteralFollowedBySeparator() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(IL_5, null))))
        assertContains(result, listOf(
            "@_.printf.fmt.I64 = private constant [5 x i8] c\"%lld\\00\"", // Without EOL
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64, i64 5)",
        ))
    }

    @Test
    fun printTwoLiterals() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(IL_5, FL_2_0))))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64.F64.nl, i64 5, double 2.0)"))
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
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr @_.str.0)",
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr @_.str.1)",
            "%2 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr @_.str.0)",
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
    fun arithmeticStringExpressions() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            AddExpression(SL_FOO, SL_BAR),
        ))))
        assertContains(result, listOf(
            "%0 = call ptr @add_Str_Str(ptr @_.str.0, ptr @_.str.1)",
            "%2 = call i64 @free(ptr %0)", // Automatically free memory allocated when adding strings
        ))
    }

    @Test
    fun relationalStringExpressions() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            EqualExpression(SL_FOO, SL_BAR),
        ))))
        assertContains(result, listOf(
            "declare i32 @strcmp(ptr, ptr)",
            "%0 = call i32 @strcmp(ptr @_.str.0, ptr @_.str.1)",
            "%1 = icmp eq i32 %0, 0",
        ))
    }

    @Test
    fun callFunctionCint() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            FunctionCallExpression(BF_CINT_F64.identifier, listOf(FL_0_5), BF_CINT_F64),
        ))))
        assertContains(result, listOf(
            // CINT rounds half-to-even (QuickBASIC 4.5): llvm.roundeven, not llvm.round (issue #52)
            "%0 = call double @llvm.roundeven.f64(double 0.5)",
            "%1 = fptosi double %0 to i64",
        ))
    }

    @Test
    fun callFunctionAsc() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            FunctionCallExpression(BF_ASC_STR.identifier, listOf(SL_BAR), BF_ASC_STR),
        ))))
        assertContains(result, listOf(
            "%0 = getelementptr i8, ptr @_.str.0, i64 0",
        ))
    }

    @Test
    fun callFunctionLen() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(
            FunctionCallExpression(BF_LEN_STR.identifier, listOf(AddExpression(SL_FOO, SL_BAR)), BF_LEN_STR),
        ))))
        assertContains(result, listOf(
            "%1 = call i64 @strlen(ptr %0)",
            "%2 = call i64 @free(ptr %0)", // Automatically free memory allocated when adding strings
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
            "@_s.do = private global ptr @_.str.empty",
            "%0 = load i64, ptr @_a.pe",
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64.nl, i64 %0)",
            "%2 = load double, ptr @_f.ha",
            "%3 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.F64.nl, double %2)",
            "%4 = load ptr, ptr @_s.do",
            "%5 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr %4)",
        ))
    }

    @Test
    fun assignUndefinedVariables() {
        val result = assembleProgram(cg, listOf(
            AssignStatement(INE_I64_A, IL_3),
            AssignStatement(INE_F64_F, FL_1_2),
            AssignStatement(INE_STR_S, SL_FOO),
        ))
        assertContains(result, listOf(
            "@_a.pe = private global i64 0",
            "@_f.ha = private global double 0.0",
            "@_s.do = private global ptr @_.str.empty",
            "store i64 3, ptr @_a.pe",
            "store double 1.2, ptr @_f.ha",
            "store ptr @_.str.0, ptr @_s.do",
        ))
    }

    @Test
    fun defineStringVariable() {
        val ds = Declaration(IDENT_STR_S.name(), IDENT_STR_S.type())
        val result = assembleProgram(cg, listOf(
            VariableDeclarationStatement(listOf(ds), GLOBAL)
        ))
        assertContains(result, listOf(
            "@_s.do = private global ptr @_.str.empty",
        ))
    }

    @Test
    fun defineVariables() {
        val da = Declaration(IDENT_I64_A.name(), IDENT_I64_A.type())
        val df = Declaration(IDENT_F64_F.name(), IDENT_F64_F.type())
        val ds = Declaration(IDENT_STR_S.name(), IDENT_STR_S.type())
        val result = assembleProgram(cg, listOf(
            VariableDeclarationStatement(listOf(da, df), GLOBAL),
            VariableDeclarationStatement(listOf(ds), GLOBAL)
        ))
        assertContains(result, listOf(
            "@_a.pe = private global i64 0",
            "@_f.ha = private global double 0.0",
            "@_s.do = private global ptr @_.str.empty",
        ))
    }

    @Test
    fun defineConstants() {
        val da = DeclarationAssignment(IDENT_I64_A.name(), IDENT_I64_A.type(), IL_3)
        val df = DeclarationAssignment(IDENT_F64_F.name(), IDENT_F64_F.type(), FL_2_0)
        val ds = DeclarationAssignment(IDENT_STR_S.name(), IDENT_STR_S.type(), SL_FOO)
        val result = assembleProgram(cg, listOf(
            ConstDeclarationStatement(listOf(da, df, ds)),
        ))
        assertContains(result, listOf(
            "@_a.pe = private constant i64 3",
            "@_f.ha = private constant double 2.0",
            "@_s.do = private constant [4 x i8] c\"foo\\00\"",
        ))
    }

    @Test
    fun swapVariables() {
        val da = Declaration(IDENT_I64_A.name(), IDENT_I64_A.type())
        val db = Declaration(IDENT_I64_B.name(), IDENT_I64_B.type())
        val df = Declaration(IDENT_F64_F.name(), IDENT_F64_F.type())
        val dg = Declaration(IDENT_F64_G.name(), IDENT_F64_G.type())
        val ds = Declaration(IDENT_STR_S.name(), IDENT_STR_S.type())
        val dx = Declaration(IDENT_STR_X.name(), IDENT_STR_X.type())
        val result = assembleProgram(cg, listOf(
            VariableDeclarationStatement(listOf(da, db), GLOBAL),
            VariableDeclarationStatement(listOf(df, dg), GLOBAL),
            VariableDeclarationStatement(listOf(ds, dx), GLOBAL),
            SwapStatement(INE_I64_A, INE_I64_B),
            SwapStatement(INE_F64_F, INE_F64_G),
            SwapStatement(INE_STR_S, INE_STR_X),
            SwapStatement(INE_I64_A, INE_F64_F),
            SwapStatement(INE_F64_G, INE_I64_B)
        ))
        // NOTE (issue #52): for the mixed SWAP a% <-> f#, the LLVM backend *truncates* float->int
        // (fptosi), whereas the FASM backend *rounds* (RoundFloatRegToIntReg, the QuickBASIC-correct
        // behaviour) — see BasicCodeGeneratorTests.shouldSwapIntegerAndFloat. This divergence is
        // captured deliberately; Phase 4 of issue #52 will reconcile the two backends.
        assertContains(result, listOf(
            "%6 = load i64, ptr @_a.pe",
            "%7 = sitofp i64 %6 to double",
            "%8 = load double, ptr @_f.ha",
            "%9 = fptosi double %8 to i64",
            "store i64 %9, ptr @_a.pe",
            "store double %7, ptr @_f.ha",
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
    fun inputWithoutPrompt() {
        val result = assembleProgram(cg, listOf(LineInputStatement.builder(IDENT_STR_S).build()))
        assertContains(result, listOf(
            "%0 = call ptr @read_line()",
            "store ptr %0, ptr @_s.do"
        ))
        // No newline is printed after input (matching the FASM backend)
        assertNotContains(result, listOf("@_.printf.fmt..nl"))
    }

    @Test
    fun inputWithPrompt() {
        val result = assembleProgram(cg, listOf(LineInputStatement.builder(IDENT_STR_S).prompt("PROMPT").build()))
        assertContains(result, listOf(
            "@_.str.0 = private constant [7 x i8] c\"PROMPT\\00\"",
            // Prompt printed before reading the line
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str, ptr @_.str.0)",
            "%1 = call ptr @read_line()",
            "store ptr %1, ptr @_s.do"
        ))
    }

    @Test
    fun inputWithInhibitedNewline() {
        // The inhibitNewline flag has no effect: no newline is printed in either case
        val result = assembleProgram(cg, listOf(LineInputStatement.builder(IDENT_STR_S).inhibitNewline(true).build()))
        assertContains(result, listOf(
            "%0 = call ptr @read_line()",
            "store ptr %0, ptr @_s.do"
        ))
        assertNotContains(result, listOf("@_.printf.fmt..nl"))
    }

    @Test
    fun randomizeWithExpression() {
        val result = assembleProgram(cg, listOf(RandomizeStatement(0, 0, FL_2_0)))
        assertContains(result, listOf("call void @randomize(double 2.0)"))
    }

    @Test
    fun randomizeWithoutExpression() {
        val result = assembleProgram(cg, listOf(RandomizeStatement(0, 0)))
        assertContains(result, listOf(
            "c\"Random Number Seed (-32768 to 32767)? \\00\"",
            // Prompt printed before reading the seed
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str, ptr @_.str.0)",
            "%1 = call ptr @read_line()",
            "%2 = call double @atof(ptr %1)",
            // The line read is freed directly after the conversion
            "%3 = call i64 @free(ptr %1)",
            "call void @randomize(double %2)",
        ))
    }
}
