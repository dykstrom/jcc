package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_2_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_5
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_BAR
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_FOO
import se.dykstrom.jcc.basic.ast.statement.PrintStatement
import se.dykstrom.jcc.common.ast.*

internal class BasicLlvmCodeGeneratorTests : AbstractBasicCodeGeneratorTests() {

    private val cg = BasicLlvmCodeGenerator(typeManager, symbols, optimizer)

    @Test
    fun emptyProgram() {
        val result = assembleProgram(cg, listOf())
        assertContains(result, listOf("ret i32 0"))
    }

    @Test
    fun printLiteral() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(IL_5))))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64, i64 5)"))
    }

    @Test
    fun printTwoLiterals() {
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(IL_5, FL_2_0))))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64.F64, i64 5, double 2.0)"))
    }

    @Test
    fun printStringLiterals() {
        val result = assembleProgram(cg, listOf(
            PrintStatement(listOf(SL_FOO)),
            PrintStatement(listOf(SL_BAR)),
            PrintStatement(listOf(SL_FOO)),
        ))
        assertContains(result, listOf(
            "@.str.0 = private constant [4 x i8] c\"foo\\00\"",
            "@.str.1 = private constant [4 x i8] c\"bar\\00\"",
            "%0 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.Str, ptr @.str.0)",
            "%1 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.Str, ptr @.str.1)",
            "%2 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.Str, ptr @.str.0)",
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
}
