package se.dykstrom.jcc.assembunny.compiler

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.assembunny.ast.CpyStatement
import se.dykstrom.jcc.assembunny.ast.JnzStatement
import se.dykstrom.jcc.assembunny.ast.OutnStatement
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.IDE_A
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.IDE_B
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.IL_1
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.INE_A
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.INE_B
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.SOURCE_PATH
import se.dykstrom.jcc.assembunny.compiler.AssembunnyUtils.END_JUMP_TARGET
import se.dykstrom.jcc.assembunny.types.AssembunnyTypeManager
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.code.TargetProgram
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer

internal class AssembunnyLlvmCodeGeneratorTests {

    private val typeManager = AssembunnyTypeManager()
    private val symbolTable = AssembunnySymbols()
    private val optimizer = DefaultAstOptimizer(typeManager, symbolTable)
    private val codeGenerator = AssembunnyLlvmCodeGenerator(typeManager, symbolTable, optimizer)

    @Test
    fun emptyProgram() {
        val result = assembleProgram(listOf())
        assertContains(result, listOf("ret i32 %1"))
    }

    @Test
    fun incRegister() {
        val result = assembleProgram(listOf(IncStatement(0, 0, INE_A)))
        assertContains(result, listOf(
            "@A = private global i64 0",
            "%0 = load i64, ptr @A",
            "%1 = add i64 %0, 1",
            "store i64 %1, ptr @A"
        ))
    }

    @Test
    fun decRegister() {
        val result = assembleProgram(listOf(DecStatement(0, 0, INE_A)))
        assertContains(result, listOf(
            "@A = private global i64 0",
            "%0 = load i64, ptr @A",
            "%1 = sub i64 %0, 1",
            "store i64 %1, ptr @A"
        ))
    }

    @Test
    fun printRegister() {
        val result = assembleProgram(listOf(OutnStatement(0, 0, IDE_A)))
        assertContains(result, listOf(
            "%0 = load i64, ptr @A",
            "%1 = call i32 (ptr, ...) @printf(ptr @.printf.fmt.I64, i64 %0)"
        ))
    }

    @Test
    fun copyFromRegister() {
        val result = assembleProgram(listOf(CpyStatement(0, 0, IDE_A, INE_B)))
        assertContains(result, listOf(
            "%0 = load i64, ptr @A",
            "store i64 %0, ptr @B"
        ))
    }

    @Test
    fun copyFromLiteral() {
        val result = assembleProgram(listOf(CpyStatement(0, 0, IL_1, INE_B)))
        assertContains(result, listOf(
            "store i64 1, ptr @B"
        ))
    }

    @Test
    fun jnzOnInt() {
        val js = JnzStatement(0, 0, IL_1, END_JUMP_TARGET)
        val result = assembleProgram(listOf(LabelledStatement("line0", js)))
        assertContains(result, listOf(
            "line0:",
            "%0 = icmp eq i64 1, 0",
            "br i1 %0, label %L0, label %end",
            "L0:",
            "br label %end",
            "end:",
        ))
    }

    @Test
    fun jnzOnReg() {
        val dec0 = DecStatement(0, 0, INE_B)
        val jnz1 = JnzStatement(0, 0, IDE_B, "line0")
        val dec2 = DecStatement(0, 0, INE_A)
        val jnz3 = JnzStatement(0, 0, IDE_A, "line2")
        val result = assembleProgram(listOf(
            LabelledStatement("line0", dec0),
            LabelledStatement("line1", jnz1),
            LabelledStatement("line2", dec2),
            LabelledStatement("line3", jnz3),
        ))
        assertContains(result, listOf(
            "line0:",
            "line1:",
            "line2:",
            "line3:",
            "L0:",
            "L1:",
            "end:",
        ))
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
