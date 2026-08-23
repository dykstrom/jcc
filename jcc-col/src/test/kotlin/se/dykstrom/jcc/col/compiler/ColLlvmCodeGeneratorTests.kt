package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.ColTests.Companion.FL_1_5_F32
import se.dykstrom.jcc.col.ColTests.Companion.FL_2_0
import se.dykstrom.jcc.col.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.ColTests.Companion.IL_17_I32
import se.dykstrom.jcc.col.ColTests.Companion.IL_18
import se.dykstrom.jcc.col.ColTests.Companion.IL_1_000
import se.dykstrom.jcc.col.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.ColTests.Companion.IL_M_1
import se.dykstrom.jcc.col.compiler.ColSymbols.*
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.BooleanLiteral.FALSE
import se.dykstrom.jcc.common.ast.BooleanLiteral.TRUE
import se.dykstrom.jcc.common.ast.FloatLiteral.FL_F32_0_0
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE_I32
import se.dykstrom.jcc.common.ast.IntegerLiteral.TWO_I32
import se.dykstrom.jcc.common.types.F32

internal class ColLlvmCodeGeneratorTests : AbstractColCodeGeneratorTests() {

    @Test
    fun emptyProgram() {
        val result = assembleProgram(cg, listOf())
        assertContains(result, listOf("ret i32 0"))
    }

    @Test
    fun printlnLiteral() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, IL_5)))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64.nl, i64 5)"))
    }

    @Test
    fun printlnTypedIntegerLiteral() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, IL_17_I32)))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I32.nl, i32 17)"))
    }

    @Test
    fun printlnTypedFloatLiteral() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FL_1_5_F32)))
        assertContains(result, listOf(
            "%0 = fpext float 1.5 to double",
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.F32.nl, double %0)",
        ))
    }

    @Test
    fun f32LiteralIsRoundedToSinglePrecision() {
        // LLVM rejects decimal float constants that are not exactly representable in single precision
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_F32, FloatLiteral(0, 0, "5.3", F32.INSTANCE))))
        assertContains(result, listOf("%0 = fpext float 5.300000190734863 to double"))
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
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, NegateExpression(ONE_I32))))
        assertContains(result, listOf("%0 = sub i32 0, 1"))
    }

    @Test
    fun callArgsEvaluateLeftToRight() {
        // min(println(1), println(2)) - register numbers pin the evaluation order
        val left = FunctionCallExpression(BF_PRINTLN_I32.identifier, listOf(ONE_I32), BF_PRINTLN_I32)
        val right = FunctionCallExpression(BF_PRINTLN_I32.identifier, listOf(TWO_I32), BF_PRINTLN_I32)
        val result = assembleProgram(cg, listOf(funCall(BF_MIN_I32_I32, left, right)))
        assertContains(result, listOf(
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I32.nl, i32 1)",
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I32.nl, i32 2)",
            "%2 = call i32 @llvm.smin.i32(i32 %0, i32 %1)",
        ))
    }

    @Test
    fun binaryOperandsEvaluateLeftToRight() {
        // println(1) + println(2) - register numbers pin the evaluation order
        val left = FunctionCallExpression(BF_PRINTLN_I32.identifier, listOf(ONE_I32), BF_PRINTLN_I32)
        val right = FunctionCallExpression(BF_PRINTLN_I32.identifier, listOf(TWO_I32), BF_PRINTLN_I32)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I32, AddExpression(left, right))))
        assertContains(result, listOf(
            "%0 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I32.nl, i32 1)",
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I32.nl, i32 2)",
            "%2 = add i32 %0, %1",
        ))
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
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.F64.nl, double %0)"
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
    fun neFloatLiterals() {
        // Unordered une, so that NaN != NaN is true per IEEE 754
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, NotEqualExpression(FL_1_0, FL_2_0))))
        assertContains(result, listOf("%0 = fcmp une double 1.0, 2.0"))
    }

    @Test
    fun eqFloatLiteralsOfTypeF32() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, EqualExpression(FL_F32_0_0, FL_F32_0_0))))
        assertContains(result, listOf("%0 = fcmp oeq float 0.0, 0.0"))
    }

    @Test
    fun geFloatLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, GreaterOrEqualExpression(FL_1_0, FL_1_0))))
        assertContains(result, listOf("%0 = fcmp oge double 1.0, 1.0"))
    }

    @Test
    fun eqBoolLiterals() {
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, EqualExpression(TRUE, FALSE))))
        assertContains(result, listOf("%0 = icmp eq i1 1, 0"))
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

    @Test
    fun logicAndWithNestedIfInLeft() {
        // COL: (if true then true else false) and true
        val ifExpr = IfExpression(TRUE, TRUE, FALSE)
        val andExpr = LogicalAndExpression(ifExpr, TRUE)

        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, andExpr)))

        // Label allocations:
        // AND expression allocates L0 (Left), L1 (Right), L2 (Result) first.
        // Then IF expression (nested in Left) allocates L3 (Then), L4 (Else), L5 (Result).
        assertContains(result, listOf(
            // --- If Expression (Left side of AND) ---
            // Condition uses IF labels (L3, L4)
            "br i1 1, label %L3, label %L4",
            // IF result comes from L3/L4 and merges into L5
            "%0 = phi i1 [ 1, %L3 ], [ 0, %L4 ]",

            // --- Logical AND ---
            // Checks result of IF (%0).
            // If true -> go to AND-Right (L1).
            // If false -> go to AND-Result (L2) (short-circuit).
            "br i1 %0, label %L1, label %L2",

            // Right side (TRUE)
            // L1 is the label for the right operand of the AND expression
            "br label %L2", // Jump to result

            // Result of AND (L2)
            // The first incoming value (0) comes from the IF-result block (L5).
            // The second value (1) comes from the AND-Right block (L1).
            "%1 = phi i1 [ 0, %L5 ], [ 1, %L1 ]"
        ))
    }

    @Test
    fun logicOrWithNestedIfInRight() {
        // COL: false or (if true then true else false)
        val ifExpr = IfExpression(TRUE, TRUE, FALSE)
        val orExpr = LogicalOrExpression(FALSE, ifExpr)

        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, orExpr)))

        assertContains(result, listOf(
            // --- Left side of OR (FALSE) ---
            // Starts at L0.
            "br label %L0",
            // Checks false. Since it's OR, we must check right side (L1).
            // If it were true, we would jump to Result (L2).
            "br i1 0, label %L2, label %L1",

            // --- Right side (If Expression) ---
            // Starts at L1.
            // IF expression allocates L3, L4, L5.
            "br i1 1, label %L3, label %L4",
            // IF result merges into L5
            "%0 = phi i1 [ 1, %L3 ], [ 0, %L4 ]",

            // --- Result of OR ---
            // Merges at L2.
            // Value 1 comes from Left (L0) (short-circuit path).
            // Value %0 comes from the IF-result block (L5).
            "%1 = phi i1 [ 1, %L0 ], [ %0, %L5 ]"
        ))
    }

    @Test
    fun ifExpression() {
        val le = LessExpression(IL_5, IL_17)
        val ae = AddExpression(FL_1_0, FL_2_0)
        val se = SubExpression(FL_1_0, FL_2_0)
        val ie = IfExpression(le, ae, se)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_BOOL, ie)))
        assertContains(result, listOf(
            "br i1 %0, label %L0, label %L1",
            "br label %L2",
            "%3 = phi double [ %1, %L0 ], [ %2, %L1 ]",
        ))
    }

    @Test
    fun ifExpressionNestedInThen() {
        val tie = IfExpression(TRUE, IL_5, IL_17)
        val ie = IfExpression(FALSE, tie, IL_18)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, ie)))
        assertContains(result, listOf(
            "br i1 0, label %L0, label %L1",
            "br i1 1, label %L3, label %L4",
            "%0 = phi i64 [ 5, %L3 ], [ 17, %L4 ]",
            "%1 = phi i64 [ %0, %L5 ], [ 18, %L1 ]",
        ))
    }

    @Test
    fun ifExpressionNestedInElse() {
        val eie = IfExpression(TRUE, IL_5, IL_17)
        val ie = IfExpression(FALSE, IL_18, eie)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, ie)))
        assertContains(result, listOf(
            "br i1 0, label %L0, label %L1",
            "br i1 1, label %L3, label %L4",
            "%0 = phi i64 [ 5, %L3 ], [ 17, %L4 ]",
            "%1 = phi i64 [ 18, %L0 ], [ %0, %L5 ]",
        ))
    }

    @Test
    fun ifExpressionNestedInThenAndElse() {
        val tie = IfExpression(TRUE, IL_5, IL_17)
        val eie = IfExpression(TRUE, IL_18, IL_1_000)
        val ie = IfExpression(FALSE, tie, eie)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, ie)))
        assertContains(
            result, listOf(
                "br i1 0, label %L0, label %L1",
                "br i1 1, label %L3, label %L4",
                "%0 = phi i64 [ 5, %L3 ], [ 17, %L4 ]",
                "br i1 1, label %L6, label %L7",
                "%1 = phi i64 [ 18, %L6 ], [ 1000, %L7 ]",
                "%2 = phi i64 [ %0, %L5 ], [ %1, %L8 ]",
            )
        )
    }

    /**
     * if false then
     *   (if true 5 else (if false then 17 else -1))
     * else
     *   (if true then 18 else 1000)
     */
    @Test
    fun ifExpressionNestedInThenAndThenElseAndElse() {
        val tie = IfExpression(TRUE, IL_5, IfExpression(FALSE, IL_17, IL_M_1))
        val eie = IfExpression(TRUE, IL_18, IL_1_000)
        val ie = IfExpression(FALSE, tie, eie)
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_I64, ie)))
        assertContains(result, listOf(
            "br i1 0, label %L0, label %L1",
            "br i1 1, label %L3, label %L4",
            "br i1 0, label %L6, label %L7",
            "%0 = phi i64 [ 17, %L6 ], [ -1, %L7 ]",
            "%1 = phi i64 [ 5, %L3 ], [ %0, %L8 ]",
            "br i1 1, label %L9, label %L10",
            "%2 = phi i64 [ 18, %L9 ], [ 1000, %L10 ]",
            "%3 = phi i64 [ %1, %L5 ], [ %2, %L11 ]",
        ))
    }

    @Test
    fun whileStatement() {
        // while 5 > 17 do call println(5) end
        val condition = GreaterExpression(IL_5, IL_17)
        val body = listOf(funCall(BF_PRINTLN_I64, IL_5))
        val result = assembleProgram(cg, listOf(WhileStatement(condition, body)))
        assertContains(result, listOf(
            // Branch into the loop (and the back-edge) target the before-label
            "br label %L0",
            // Condition is evaluated at the top of the loop
            "%0 = icmp sgt i64 5, 17",
            // Conditional branch: inside-label if true, after-label if false
            "br i1 %0, label %L1, label %L2",
            // Loop body
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64.nl, i64 5)",
        ))
    }
}
