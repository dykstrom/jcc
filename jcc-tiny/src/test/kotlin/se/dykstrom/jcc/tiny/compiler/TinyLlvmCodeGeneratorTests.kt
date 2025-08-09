package se.dykstrom.jcc.tiny.compiler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.code.TargetProgram
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import se.dykstrom.jcc.tiny.ast.ReadStatement
import se.dykstrom.jcc.tiny.ast.WriteStatement
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.IDENT_A
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.IDE_A
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.IDE_B
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.IL_0
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.IL_1
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.IL_5
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.INE_A
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.INE_B
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.SOURCE_PATH

internal class TinyLlvmCodeGeneratorTests {

    private val typeManager = DefaultTypeManager()
    private val symbolTable = TinySymbols()
    private val optimizer = DefaultAstOptimizer(typeManager, symbolTable)
    private val codeGenerator = TinyLlvmCodeGenerator(typeManager, symbolTable, optimizer)

    @Test
    fun emptyProgram() {
        val result = assembleProgram(listOf())
        assertContains(result, listOf("ret i32 0"))
    }

    @Test
    fun writeOneLiteral() {
        val result = assembleProgram(listOf(WriteStatement(0, 0, listOf(IL_5))))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64, i64 5)"))
    }

    @Test
    fun writeTwoLiterals() {
        val result = assembleProgram(listOf(WriteStatement(0, 0, listOf(IL_5, IL_0))))
        assertContains(result, listOf(
            "%0 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64, i64 5)",
            "%1 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64, i64 0)",
        ))
    }

    @Test
    fun writeAddLiterals() {
        val addExpression = AddExpression(0, 0, IL_5, IL_0)
        val writeStatement = WriteStatement(0, 0, listOf(addExpression))
        val result = assembleProgram(listOf(writeStatement))
        assertFalse(result.lines().isEmpty())
    }

    @Test
    fun writeAddSubLiterals() {
        val subExpression = SubExpression(0, 0, IL_0, IL_1)
        val addExpression = AddExpression(0, 0, IL_5, subExpression)
        val writeStatement = WriteStatement(0, 0, listOf(addExpression))
        val result = assembleProgram(listOf(writeStatement))
        assertFalse(result.lines().isEmpty())
    }

    @Test
    fun writeOneVariable() {
        val result = assembleProgram(listOf(WriteStatement(0, 0, listOf(IDE_A))))
        assertContains(result, listOf(
            "%0 = load i64, ptr %a",
            "%1 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64, i64 %0)"
        ))
    }

    @Test
    fun writeAddVariables() {
        val addExpression = AddExpression(0, 0, IDE_A, IDE_B)
        val writeStatement = WriteStatement(0, 0, listOf(addExpression))
        val result = assembleProgram(listOf(writeStatement))
        assertContains(result, listOf(
            "%0 = load i64, ptr %a",
            "%1 = load i64, ptr %b",
            "%2 = add i64 %0, %1",
            "%3 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64, i64 %2)"
        ))
    }

    @Test
    fun assignOneLiteral() {
        val result = assembleProgram(listOf(AssignStatement(0, 0, INE_A, IL_5)))
        assertContains(result, listOf("store i64 5, ptr %a"))
    }

    @Test
    fun assignAddExpression() {
        val addExpression = AddExpression(0, 0, IDE_B, IL_1)
        val result = assembleProgram(listOf(AssignStatement(0, 0, INE_A, addExpression)))
        assertContains(result, listOf(
            "%0 = load i64, ptr %b",
            "%1 = add i64 %0, 1",
            "store i64 %1, ptr %a"
        ))
    }

    @Test
    fun readOneVariable() {
        val result = assembleProgram(listOf(ReadStatement(0, 0, listOf(IDENT_A))))
        assertContains(result, listOf("%0 = call i32 (ptr, ...) @scanf(ptr @.scanf.fmt.I64, ptr %a)"))
    }

    @Test
    fun readAssignWrite() {
        val readStatement = ReadStatement(0, 0, listOf(IDENT_A))
        val assignStatement = AssignStatement(0, 0, INE_B, AddExpression(0, 0, IDE_A, IL_1))
        val writeStatement = WriteStatement(0, 0, listOf(IDE_B))
        val result = assembleProgram(listOf(readStatement, assignStatement, writeStatement))
        assertContains(result, listOf(
            "%0 = call i32 (ptr, ...) @scanf(ptr @.scanf.fmt.I64, ptr %a)",
            "%1 = load i64, ptr %a",
            "%2 = add i64 %1, 1",
            "store i64 %2, ptr %b",
            "%3 = load i64, ptr %b",
            "%4 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64, i64 %3)"
        ))
        assertTrue(symbolTable.contains("@.printf.fmt.I64"))
        assertTrue(symbolTable.contains("@.scanf.fmt.I64"))
    }

    private fun assertContains(program: TargetProgram, lines: List<String>) {
        val text = program.toText()
        lines.forEach { assertTrue(text.contains(it), "missing line: $it") }
    }

    private fun assembleProgram(statements: List<Statement>): TargetProgram {
        val program = AstProgram(0, 0, statements).withSourcePath(SOURCE_PATH)
        return codeGenerator.generate(program)
    }
}
