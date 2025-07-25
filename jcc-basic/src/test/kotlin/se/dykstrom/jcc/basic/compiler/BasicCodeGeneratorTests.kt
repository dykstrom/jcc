/*
 * Copyright (C) 2016 Johan Dykstrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_17_E4
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_3_14
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_F64_G
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_H
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_STR_S
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_H
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_2
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_4
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_M1
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_F64_G
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_H
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_STR_B
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_STR_S
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_BAR
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_FOO
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_ONE
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_TWO
import se.dykstrom.jcc.basic.ast.*
import se.dykstrom.jcc.basic.functions.LibJccBasBuiltIns.FUN_RANDOMIZE
import se.dykstrom.jcc.basic.functions.LibJccBasBuiltIns.FUN_VAL
import se.dykstrom.jcc.common.assembly.directive.DataDefinition
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.instruction.floating.*
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_GETLINE
import se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_EXIT
import se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_PRINTF_STR_VAR
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str
import java.util.Collections.emptyList

/**
 * Tests class `BasicCodeGenerator`. This class tests mostly general features. Other
 * classes test code generation involving if and while statements, function calls,
 * floating point operations, and garbage collection.
 *
 * @author Johan Dykstrom
 * @see BasicCodeGenerator
 */
class BasicCodeGeneratorTests : AbstractBasicCodeGeneratorTests() {

    @BeforeEach
    fun setUp() {
        symbols.addFunction(FUN_GETLINE)
        symbols.addFunction(FUN_RANDOMIZE)
        symbols.addFunction(FUN_VAL)
    }

    @Test
    fun testEmptyProgram() {
        val result = assembleProgram(emptyList())

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT)
        assertCodeLines(result.lines(), 1, 1, 1, 1)
    }

    @Test
    fun testEnd() {
        val es = LabelledStatement("10", EndStatement(0, 0))

        val result = assembleProgram(listOf(es))

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT)
        assertCodeLines(result.lines(), 1, 1, 2, 1)
    }

    @Test
    fun shouldGenerateCodeForCls() {
        val cs = ClsStatement(0, 0)

        val result = assembleProgram(listOf(cs))

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT, LF_PRINTF_STR_VAR)
        assertCodeLines(result.lines(), 1, 2, 1, 2)
    }

    @Test
    fun testRem() {
        val rs = CommentStatement(0, 0)

        val result = assembleProgram(listOf(rs))

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT)
        assertCodeLines(result.lines(), 1, 1, 1, 1)
    }

    @Test
    fun testGosub() {
        val gs = LabelledStatement("10", GosubStatement(0, 0, "10"))

        val result = assembleProgram(listOf(gs))

        assertCodeLines(result.lines(), 1, 1, 3, 3)
    }

    @Test
    fun testGosubLabel() {
        val gs = GosubStatement(0, 0, "loop")

        val result = assembleProgram(listOf(gs))

        assertCodeLines(result.lines(), 1, 1, 2, 3)
    }

    @Test
    fun testGoto() {
        val gs = LabelledStatement("10", GotoStatement(0, 0, "10"))

        val result = assembleProgram(listOf(gs))
        val lines = result.lines()

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT)
        assertCodeLines(lines, 1, 1, 2, 1)
        assertEquals(1, countInstances(Jmp::class.java, lines))
    }

    @Test
    fun testTwoGotos() {
        val gs10 = LabelledStatement("10", GotoStatement(0, 0, "20"))
        val gs20 = LabelledStatement("20", GotoStatement(0, 0, "10"))

        val result = assembleProgram(listOf(gs10, gs20))
        val lines = result.lines()

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT)
        assertCodeLines(lines, 1, 1, 3, 1)
        assertEquals(2, countInstances(Jmp::class.java, lines))
    }

    @Test
    fun testOnGoto() {
        val os = LabelledStatement("10", OnGotoStatement(0, 0, IL_3, listOf("10", "20")))
        val cs = LabelledStatement("20", CommentStatement(0, 0, "comment"))

        val result = assembleProgram(listOf(os, cs))
        val lines = result.lines()

        assertCodeLines(lines, 1, 1, 3, 1)
        // Two compares for the two goto labels
        assertEquals(2, countInstances(CmpRegWithImm::class.java, lines))
        // Two jumps for the two goto labels
        assertEquals(2, countInstances(Je::class.java, lines))
    }

    @Test
    fun testOnGotoLabels() {
        val os = LabelledStatement("foo", OnGotoStatement(0, 0, IL_3, listOf("foo", "bar")))
        val cs = LabelledStatement("bar", CommentStatement(0, 0, "comment"))

        val result = assembleProgram(listOf(os, cs))
        val lines = result.lines()

        assertCodeLines(lines, 1, 1, 3, 1)
        // Two compares for the two goto labels
        assertEquals(2, countInstances(CmpRegWithImm::class.java, lines))
        // Two jumps for the two goto labels
        assertEquals(2, countInstances(Je::class.java, lines))
    }

    @Test
    fun testOnGosub() {
        val os = LabelledStatement("10", OnGosubStatement(0, 0, IL_3, listOf("10", "20")))
        val cs = LabelledStatement("20", CommentStatement(0, 0, "comment"))

        val result = assembleProgram(listOf(os, cs))
        val lines = result.lines()

        // Six labels - main, two lines, and five for on-gosub (including bridge calls)
        // Three calls - exit, and four for on-gosub (including bridge calls)
        assertCodeLines(lines, 1, 1, 8, 5)
        // Two compares for the two on-gosub labels
        assertEquals(2, countInstances(CmpRegWithImm::class.java, lines))
        // Two jumps for the two on-gosub labels
        assertEquals(2, countInstances(Je::class.java, lines))
    }

    @Test
    fun shouldGenerateCodeForReturn() {
        val rs = LabelledStatement("100", ReturnStatement(0, 0))

        val result = assembleProgram(listOf(rs))
        val lines = result.lines()

        assertCodeLines(lines, 1, 2, 4, 5)
        assertEquals(2, countInstances(Ret::class.java, lines))
    }

    @Test
    fun shouldGenerateCodeForReturnInWhile() {
        val rs = ReturnStatement(0, 0)
        val ws = WhileStatement(0, 0, IL_0, listOf(rs))

        val result = assembleProgram(listOf(ws))
        val lines = result.lines()

        // 5 labels: main, return-without-gosub * 2, while * 2
        assertCodeLines(lines, 1, 2, 5, 5)
        assertEquals(2, countInstances(Ret::class.java, lines))
    }

    @Test
    fun shouldGenerateCodeForReturnInIf() {
        val rs = ReturnStatement(0, 0)
        val ifs = IfStatement.builder(IL_0, rs).build()

        val result = assembleProgram(listOf(ifs))
        val lines = result.lines()

        // 5 labels: main, return-without-gosub * 2, if * 2
        assertCodeLines(lines, 1, 2, 5, 5)
        assertEquals(2, countInstances(Ret::class.java, lines))
        assertTrue(lines
            .filterIsInstance<DataDefinition>()
            .any { it.value.contains("RETURN without GOSUB") })
    }

    @Test
    fun testPrint() {
        val ps = PrintStatement(0, 0, listOf(SL_FOO))

        val result = assembleProgram(listOf(ps))

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT, LF_PRINTF_STR_VAR)
        assertCodeLines(result.lines(), 1, 2, 1, 2)
    }

    @Test
    fun testPrintTwoStrings() {
        val s1 = StringLiteral(0, 0, "Hello, ")
        val s2 = StringLiteral(0, 0, "world!")
        val ps = PrintStatement(0, 0, listOf(s1, s2))

        val result = assembleProgram(listOf(ps))

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT, LF_PRINTF_STR_VAR)
        assertCodeLines(result.lines(), 1, 2, 1, 2)
    }

    @Test
    fun printingTwoEqualStringsShouldOnlyGenerateOneConstant() {
        val s1 = StringLiteral(0, 0, "foo")
        val s2 = StringLiteral(0, 0, "foo")
        val ps = PrintStatement(0, 0, listOf(s1, s2))

        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        assertCodeLines(lines, 1, 2, 1, 2)
        assertEquals(1, lines.filterIsInstance<DataDefinition>().count { it.value == "\"foo\",0" })
    }

    @Test
    fun testTwoPrintsOneLine() {
        val ps100a = PrintStatement(0, 0, listOf(SL_ONE))
        val ps100b = PrintStatement(0, 0, listOf(SL_TWO))

        val result = assembleProgram(listOf(ps100a, ps100b))

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT, LF_PRINTF_STR_VAR)
        assertCodeLines(result.lines(), 1, 2, 1, 3)
    }

    @Test
    fun testTwoPrintsTwoLines() {
        val ps100 = PrintStatement(1, 0, listOf(SL_ONE))
        val ps110 = PrintStatement(2, 0, listOf(SL_TWO))

        val result = assembleProgram(listOf(ps100, ps110))

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT, LF_PRINTF_STR_VAR)
        assertCodeLines(result.lines(), 1, 2, 1, 3)
    }

    @Test
    fun testOnePrintFiveStrings() {
        val s1 = StringLiteral(1, 10, "<1>")
        val s2 = StringLiteral(1, 20, "<2>")
        val s3 = StringLiteral(1, 30, "<3>")
        val s4 = StringLiteral(1, 40, "<4>")
        val s5 = StringLiteral(1, 50, "<5>")

        val ps = LabelledStatement("100", PrintStatement(1, 0, listOf(s1, s2, s3, s4, s5)))
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT, LF_PRINTF_STR_VAR)
        assertCodeLines(lines, 1, 2, 2, 2)
        // Save base pointer, one non-volatile register, store two arguments on stack
        assertEquals(4, countInstances(PushReg::class.java, lines))
    }

    @Test
    fun shouldPrintFloatVariable() {
        val printStatement = PrintStatement(0, 0, listOf(IDE_F64_F))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        assertTrue(lines.filterIsInstance<DataDefinition>().any { it.identifier() == IDENT_F64_F })
        assertTrue(lines.filterIsInstance<DataDefinition>().any { it.identifier().mappedName == "__fmt_F64" })
    }

    @Test
    fun shouldDefineIntegerConstant() {
        val declarations = listOf(DeclarationAssignment(0, 0, "a%", I64.INSTANCE, IL_3))
        val statement = ConstDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        val identifier = lines.filterIsInstance<DataDefinition>().find { it.identifier() == IDENT_I64_A }!!
        assertTrue(identifier.constant)
        assertEquals(identifier.value, IL_3.value)
    }

    @Test
    fun shouldDefineMultipleConstants() {
        val declarations = listOf(
            DeclarationAssignment(0, 0, "a%", I64.INSTANCE, IL_3),
            DeclarationAssignment(0, 0, "g#", F64.INSTANCE, FL_17_E4),
            DeclarationAssignment(0, 0, "s$", Str.INSTANCE, SL_BAR)
        )
        val statement = ConstDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        val a = lines.filterIsInstance<DataDefinition>().find { it.identifier() == IDENT_I64_A }!!
        assertTrue(a.constant)
        assertEquals(a.value, IL_3.value)

        val g = lines.filterIsInstance<DataDefinition>().find { it.identifier() == IDENT_F64_G }!!
        assertTrue(g.constant)
        assertEquals(g.value, FL_17_E4.value)

        val s = lines.filterIsInstance<DataDefinition>().find { it.identifier() == IDENT_STR_S }!!
        assertTrue(s.constant)
        assertEquals(s.value, "\"${SL_BAR.value}\",0")
    }

    @Test
    fun shouldReuseAlreadyDefinedStringConstant() {
        val declarations = listOf(DeclarationAssignment(0, 0, "s$", Str.INSTANCE, SL_BAR))
        val constDeclarationStatement = ConstDeclarationStatement(0, 0, declarations)
        val printStatement = PrintStatement(0, 0, listOf(SL_BAR))

        val result = assembleProgram(listOf(constDeclarationStatement, printStatement))
        val lines = result.lines()

        val s = lines.filterIsInstance<DataDefinition>().find { it.identifier() == IDENT_STR_S }!!
        assertTrue(s.constant)
        assertEquals(s.value, "\"${SL_BAR.value}\",0")

        // There should only be one definition that contains "bar"
        assertEquals(1, lines
            .filterIsInstance<DataDefinition>()
            .count { it.value.contains(SL_BAR.value) })
    }

    @Test
    fun shouldAddStrings() {
        val expression = AddExpression(0, 0, SL_ONE, SL_TWO)
        val statement = PrintStatement(0, 0, listOf(expression))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(4, countInstances(MoveImmToReg::class.java, lines))
        assertEquals(1, countInstances(AddRegToReg::class.java, lines))
        // strlen*2, malloc, strcpy, strcat, printf, free, exit
        assertEquals(8, countInstances(CallIndirect::class.java, lines))
        assertEquals(2, lines
            .filterIsInstance<CallIndirect>()
            .count { it.target.contains("strlen") }
        )
    }

    @Test
    fun shouldAddIntegers() {
        val expression = AddExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(4, countInstances(MoveImmToReg::class.java, lines))
        assertEquals(1, countInstances(AddRegToReg::class.java, lines))
    }

    @Test
    fun testOnePrintSub() {
        val expression = SubExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(4, countInstances(MoveImmToReg::class.java, lines))
        assertEquals(1, countInstances(SubRegFromReg::class.java, lines))
    }

    @Test
    fun testOnePrintMul() {
        val expression = MulExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(4, countInstances(MoveImmToReg::class.java, lines))
        assertEquals(1, countInstances(IMulRegWithReg::class.java, lines))
    }

    @Test
    fun testOnePrintDiv() {
        val expression = DivExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(4, countInstances(MoveImmToReg::class.java, lines))
        // Floating point division even though the arguments are integers
        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, lines))
        assertEquals(2, countInstances(ConvertIntRegToFloatReg::class.java, lines))
    }

    @Test
    fun testOnePrintIDiv() {
        val expression = IDivExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(4, countInstances(MoveImmToReg::class.java, lines))
        assertEquals(1, countInstances(IDivWithReg::class.java, lines))
        assertEquals(1, countInstances(Cqo::class.java, lines))
    }

    @Test
    fun testOnePrintMod() {
        val expression = ModExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(4, countInstances(MoveImmToReg::class.java, lines))
        assertEquals(1, countInstances(IDivWithReg::class.java, lines))
        assertEquals(1, countInstances(Cqo::class.java, lines))
    }

    @Test
    fun testOnePrintMulAddMul() {
        // 4 * 2 + 3 * 1
        val me1 = MulExpression(0, 0, IL_4, IL_2)
        val me2 = MulExpression(0, 0, IL_3, IL_1)
        val addExpression = AddExpression(0, 0, me1, me2)
        val printStatement = PrintStatement(0, 0, listOf(addExpression))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        assertEquals(6, countInstances(MoveImmToReg::class.java, lines))
        assertEquals(2, countInstances(MoveRegToReg::class.java, lines))
        assertEquals(2, countInstances(IMulRegWithReg::class.java, lines))
        assertEquals(1, countInstances(AddRegToReg::class.java, lines))
    }

    @Test
    fun testGotoPrintAndEnd() {
        val gs100 = LabelledStatement("100", GotoStatement(1, 0, "110"))
        val ps110 = LabelledStatement("110", PrintStatement(2, 0, listOf(SL_FOO)))
        val es120 = LabelledStatement("120", EndStatement(3, 0))

        val result = assembleProgram(listOf(gs100, ps110, es120))
        val lines = result.lines()

        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT, LF_PRINTF_STR_VAR)
        assertCodeLines(lines, 1, 2, 4, 2)
        assertEquals(1, countInstances(Jmp::class.java, lines))
    }

    @Test
    fun shouldAssignIntegerLiteral() {
        val statement = AssignStatement(0, 0, INE_I64_A, IL_4)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Exit code, and evaluating the integer literal
        assertEquals(2, countInstances(MoveImmToReg::class.java, lines))
        // Storing the evaluated integer literal
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun shouldAssignFloatLiteral() {
        val statement = AssignStatement(0, 0, INE_F64_F, FL_3_14)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Evaluating the literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
        // Storing the evaluated literal
        assertEquals(1, countInstances(MoveFloatRegToMem::class.java, lines))
    }

    @Test
    fun shouldAssignStringLiteral() {
        val statement = AssignStatement(0, 0, INE_STR_B, SL_FOO)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Exit code, evaluating the string literal, and resetting the type pointer for b$
        assertEquals(3, countInstances(MoveImmToReg::class.java, lines))
        // Storing the evaluated string literal and the type pointer
        assertEquals(2, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun shouldDifferBetweenVariablesWithTypeSpecifiers() {
        val identFloat = IdentifierNameExpression(0, 0, Identifier("i#", F64.INSTANCE))
        val identInteger = IdentifierNameExpression(0, 0, Identifier("i%", I64.INSTANCE))
        val identString = IdentifierNameExpression(0, 0, Identifier("i$", Str.INSTANCE))

        val assignFloat = AssignStatement(0, 0, identFloat, FL_3_14)
        val assignInteger = AssignStatement(0, 0, identInteger, IL_0)
        val assignString = AssignStatement(0, 0, identString, SL_BAR)

        val result = assembleProgram(listOf(assignFloat, assignInteger, assignString))
        val lines = result.lines()

        assertEquals(1, lines
            .filterIsInstance<DataDefinition>()
            .count { it.identifier == identFloat.identifier })
        assertEquals(1, lines
            .filterIsInstance<DataDefinition>()
            .count { it.identifier == identInteger.identifier })
        assertEquals(1, lines
            .filterIsInstance<DataDefinition>()
            .count { it.identifier == identString.identifier })
    }

    @Test
    fun shouldRandomizeWithoutExpression() {
        val statement = RandomizeStatement(0, 0)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // The randomize statement calls randomize(val(getline()))
        assertEquals(1, lines.filterIsInstance<CallDirect>().count { it.target.contains("getline") })
        assertEquals(1, lines.filterIsInstance<CallIndirect>().count { it.target.contains("atof") })
        assertEquals(1, lines.filterIsInstance<CallIndirect>().count { it.target.contains("randomize") })
    }

    @Test
    fun shouldRandomizeWithInteger() {
        val statement = RandomizeStatement(0, 0, IL_3)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // The randomize statement calls randomize function in the standard library
        assertEquals(1, lines.filterIsInstance<CallIndirect>().count { it.target.contains("randomize") })
    }

    @Test
    fun shouldRandomizeWithFloatExpression() {
        val statement = RandomizeStatement(0, 0, AddExpression(0, 0, FL_3_14, FL_17_E4))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // The randomize statement calls randomize function in the standard library
        assertEquals(1, lines.filterIsInstance<CallIndirect>().count { it.target.contains("randomize") })
    }

    @Test
    fun shouldSleepWithFloatExpression() {
        val statement = SleepStatement(0, 0, AddExpression(0, 0, FL_3_14, FL_17_E4))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // The sleep statement calls sleep function in the standard library
        assertEquals(1, lines.filterIsInstance<CallIndirect>().count { it.target.contains("sleep") })
    }

    @Test
    fun shouldSwapIntegers() {
        val statement = SwapStatement(0, 0, INE_I64_A, INE_I64_H)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Moving the variable contents to registers
        assertEquals(2, countInstances(MoveMemToReg::class.java, lines))
        // Moving the register contents to variables
        assertEquals(2, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun shouldSwapFloats() {
        val statement = SwapStatement(0, 0, INE_F64_F, INE_F64_G)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Moving the variable contents to registers
        assertEquals(2, countInstances(MoveMemToReg::class.java, lines))
        // Moving the register contents to variables
        assertEquals(2, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun shouldSwapIntegerAndFloat() {
        val statement = SwapStatement(0, 0, INE_I64_A, INE_F64_G)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Moving the variable contents to registers
        assertEquals(1, countInstances(MoveMemToReg::class.java, lines))
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
        // Moving the register contents to variables
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
        assertEquals(1, countInstances(MoveFloatRegToMem::class.java, lines))
        // Converting from integer to float and vice versa
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertEquals(1, countInstances(RoundFloatRegToIntReg::class.java, lines))
    }

    @Test
    fun shouldSwapStrings() {
        val statement = SwapStatement(0, 0, INE_STR_B, INE_STR_S)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Moving the variable contents (and variable type pointers) to registers
        assertEquals(4, countInstances(MoveMemToReg::class.java, lines))
        // Moving the register contents to variables (and variable type pointers)
        assertEquals(4, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun testOneAssignmentAddExpression() {
        val ae = AddExpression(0, 0, IL_1, IL_2)
        val statement = AssignStatement(0, 0, INE_I64_A, ae)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(1, countInstances(AddRegToReg::class.java, lines))
        assertEquals(1, lines
            .filterIsInstance<MoveRegToMem>()
            .map { code -> code.destination }
            .count { name -> name == "[" + IDENT_I64_A.mappedName + "]" })
    }

    @Test
    fun testOneAssignmentNegatedExpression() {
        val expression = NegateExpression(0, 0, IL_3)
        val statement = AssignStatement(0, 0, INE_I64_A, expression)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(1, countInstances(NegReg::class.java, lines))
        assertEquals(1, lines
            .filterIsInstance<MoveRegToMem>()
            .map { code -> code.destination }
            .count { name -> name == "[" + IDENT_I64_A.mappedName + "]" })
    }

    @Test
    fun testOneAssignmentIdentifierExpression() {
        val statement = AssignStatement(0, 0, INE_I64_A, IDE_I64_H)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        assertEquals(1, countInstances(MoveImmToReg::class.java, lines))
        assertEquals(1, lines
            .filterIsInstance<MoveMemToReg>()
            .map { code -> code.source }
            .count { name -> name == "[" + IDENT_I64_H.mappedName + "]" })
        assertEquals(1, lines
            .filterIsInstance<MoveRegToMem>()
            .map { code -> code.destination }
            .count { name -> name == "[" + IDENT_I64_A.mappedName + "]" })
    }

    @Test
    fun testPrintTwoIdentifierExpressions() {
        val statement = PrintStatement(0, 0, listOf(IDE_I64_A, IDE_I64_H))

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Both a% and h% should be defined as symbols
        assertEquals(IDENT_I64_A, symbols.getIdentifier(IDENT_I64_A.name()))
        assertEquals(IDENT_I64_H, symbols.getIdentifier(IDENT_I64_H.name()))
        assertEquals(2, countInstances(MoveImmToReg::class.java, lines))
        assertEquals(1, lines
            .filterIsInstance<MoveMemToReg>()
            .map { code -> code.source }
            .count { name -> name == "[" + IDENT_I64_H.mappedName + "]" })
        assertEquals(1, lines
            .filterIsInstance<MoveMemToReg>()
            .map { code -> code.source }
            .count { name -> name == "[" + IDENT_I64_A.mappedName + "]" })
    }

    @Test
    fun shouldGenerateEqualExpressionIntegers() {
        assertRelationalExpressionIntegers(EqualExpression(0, 0, IL_3, IL_4), Je::class.java)
    }

    @Test
    fun shouldGenerateNotEqualExpressionIntegers() {
        assertRelationalExpressionIntegers(NotEqualExpression(0, 0, IL_3, IL_4), Jne::class.java)
    }

    @Test
    fun shouldGenerateGreaterExpressionIntegers() {
        assertRelationalExpressionIntegers(GreaterExpression(0, 0, IL_3, IL_4), Jg::class.java)
    }

    @Test
    fun shouldGenerateGreaterOrEqualExpressionIntegers() {
        assertRelationalExpressionIntegers(GreaterOrEqualExpression(0, 0, IL_3, IL_4), Jge::class.java)
    }

    @Test
    fun shouldGenerateLessExpressionIntegers() {
        assertRelationalExpressionIntegers(LessExpression(0, 0, IL_3, IL_4), Jl::class.java)
    }

    @Test
    fun shouldGenerateLessOrEqualExpressionIntegers() {
        assertRelationalExpressionIntegers(LessOrEqualExpression(0, 0, IL_3, IL_4), Jle::class.java)
    }

    @Test
    fun shouldGenerateEqualExpressionStrings() {
        assertRelationalExpressionStrings(EqualExpression(0, 0, SL_ONE, SL_TWO), Je::class.java)
    }

    @Test
    fun shouldGenerateNotEqualExpressionStrings() {
        assertRelationalExpressionStrings(NotEqualExpression(0, 0, SL_ONE, SL_TWO), Jne::class.java)
    }

    @Test
    fun shouldGenerateGreaterExpressionStrings() {
        assertRelationalExpressionStrings(GreaterExpression(0, 0, SL_ONE, SL_TWO), Jg::class.java)
    }

    @Test
    fun shouldGenerateGreaterOrEqualExpressionStrings() {
        assertRelationalExpressionStrings(GreaterOrEqualExpression(0, 0, SL_ONE, SL_TWO), Jge::class.java)
    }

    @Test
    fun shouldGenerateLessExpressionStrings() {
        assertRelationalExpressionStrings(LessExpression(0, 0, SL_ONE, SL_TWO), Jl::class.java)
    }

    @Test
    fun shouldGenerateLessOrEqualExpressionStrings() {
        assertRelationalExpressionStrings(LessOrEqualExpression(0, 0, SL_ONE, SL_TWO), Jle::class.java)
    }

    private fun assertRelationalExpressionIntegers(expression: Expression, conditionalJump: Class<out Jump>) {
        val result = assembleProgram(listOf(AssignStatement(0, 0, INE_I64_H, expression)))
        val lines = result.lines()

        // One for the exit code, two for the integer subexpressions, and two for the results
        assertEquals(5, countInstances(MoveImmToReg::class.java, lines))
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(Cmp::class.java, lines))
        // One for the conditional jump
        assertEquals(1, countInstances(conditionalJump, lines))
        // One for the unconditional jump
        assertEquals(1, countInstances(Jmp::class.java, lines))
        // Storing the result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
    }

    private fun assertRelationalExpressionStrings(expression: Expression, conditionalJump: Class<out Jump>) {
        val result = assembleProgram(listOf(AssignStatement(0, 0, INE_I64_H, expression)))
        val lines = result.lines()

        // Libraries: msvcrt
        // Imports: strcmp, exit
        // Labels: main, @@, after_cmp
        // Calls: strcmp, exit
        assertCodeLines(lines, 1, 2, 3, 2)

        // One for the exit code, two for the integer subexpressions, and two for the results
        assertEquals(5, countInstances(MoveImmToReg::class.java, lines))
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(Cmp::class.java, lines))
        // One for the conditional jump
        assertEquals(1, countInstances(conditionalJump, lines))
        // One for the unconditional jump
        assertEquals(1, countInstances(Jmp::class.java, lines))
        // Storing the result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun testOneAssignmentWithOneComplexEqualExpression() {
        val ae = AddExpression(0, 0, IDE_I64_A, IL_1)
        val se = SubExpression(0, 0, IL_2, IDE_I64_H)
        val expression = EqualExpression(0, 0, ae, se)
        val statement = AssignStatement(0, 0, INE_I64_H, expression)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // One for the exit code, two for the integer subexpressions, and two for the results
        assertEquals(5, countInstances(MoveImmToReg::class.java, lines))
        // Two for the ident subexpressions
        assertEquals(2, countInstances(MoveMemToReg::class.java, lines))
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(Cmp::class.java, lines))
        // One for the conditional jump
        assertEquals(1, countInstances(Je::class.java, lines))
        // One for the unconditional jump
        assertEquals(1, countInstances(Jmp::class.java, lines))
        // Storing the result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
        // Find assignment to memory location
        assertEquals(1, lines
            .filterIsInstance<MoveRegToMem>()
            .map { code -> code.destination }
            .count { name -> name == "[" + IDENT_I64_H.mappedName + "]" })
    }

    @Test
    fun testOneAssignmentWithOneAnd() {
        val ee = EqualExpression(0, 0, IL_3, IL_4)
        val expression = AndExpression(0, 0, IL_0, ee)
        val statement = AssignStatement(0, 0, INE_I64_H, expression)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // One for the exit code, three for the integer subexpressions, and two for the integer results
        assertEquals(6, countInstances(MoveImmToReg::class.java, lines))
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(Cmp::class.java, lines))
        // One for the conditional jump
        assertEquals(1, countInstances(Je::class.java, lines))
        // One for the unconditional jump
        assertEquals(1, countInstances(Jmp::class.java, lines))
        // One for the and:ing
        assertEquals(1, countInstances(AndRegWithReg::class.java, lines))
        // Storing the integer result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun testOneAssignmentWithOneOr() {
        val expression = OrExpression(0, 0, IL_0, IL_M1)
        val statement = AssignStatement(0, 0, INE_I64_H, expression)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // One for the exit code, two for the integer subexpressions
        assertEquals(3, countInstances(MoveImmToReg::class.java, lines))
        // One for the or:ing
        assertEquals(1, countInstances(OrRegWithReg::class.java, lines))
        // Storing the integer result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun testOneAssignmentWithOneXor() {
        val expression = XorExpression(0, 0, IL_0, IL_M1)
        val statement = AssignStatement(0, 0, INE_I64_H, expression)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // One for the exit code, two for the integer subexpressions
        assertEquals(3, countInstances(MoveImmToReg::class.java, lines))
        // One for the xor:ing
        assertEquals(1, countInstances(XorRegWithReg::class.java, lines))
        // Storing the integer result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun testOneAssignmentWithOneEqv() {
        val expression = EqvExpression(0, 0, IL_0, IL_M1)
        val statement = AssignStatement(0, 0, INE_I64_H, expression)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // NOT(0 XOR -1)
        assertEquals(1, countInstances(NotReg::class.java, lines))
        assertEquals(1, countInstances(XorRegWithReg::class.java, lines))
    }

    @Test
    fun testOneAssignmentWithOneImp() {
        val expression = ImpExpression(0, 0, IL_0, IL_M1)
        val statement = AssignStatement(0, 0, INE_I64_H, expression)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // NOT(0) OR -1
        assertEquals(1, countInstances(NotReg::class.java, lines))
        assertEquals(1, countInstances(OrRegWithReg::class.java, lines))
    }

    @Test
    fun testOneAssignmentWithOneNot() {
        val expression = NotExpression(0, 0, IL_0)
        val statement = AssignStatement(0, 0, INE_I64_H, expression)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // One for the exit code, one for the integer subexpression
        assertEquals(2, countInstances(MoveImmToReg::class.java, lines))
        // One for the not:ing
        assertEquals(1, countInstances(NotReg::class.java, lines))
        // Storing the integer result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun testComplexRelationalExpression() {
        val ee = EqualExpression(0, 0, IL_3, IL_4)
        val ge = GreaterExpression(0, 0, IL_2, IDE_I64_A)
        val ae1 = AndExpression(0, 0, IL_0, ee)
        val ae2 = AndExpression(0, 0, ge, IL_M1)
        val oe = OrExpression(0, 0, ae1, ae2)
        val ne = NotExpression(0, 0, oe)
        val statement = AssignStatement(0, 0, IdentifierNameExpression(0, 0, IDENT_I64_H), ne)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // One for the exit code, five for the integer literals, and four for the integer results
        assertEquals(10, countInstances(MoveImmToReg::class.java, lines))
        // Two for comparing two integer subexpressions
        assertEquals(2, countInstances(Cmp::class.java, lines))
        // One for the conditional jump
        assertEquals(1, countInstances(Je::class.java, lines))
        // One for the other conditional jump
        assertEquals(1, countInstances(Jg::class.java, lines))
        // Two for the unconditional jumps
        assertEquals(2, countInstances(Jmp::class.java, lines))
        // Two for the and:ing
        assertEquals(2, countInstances(AndRegWithReg::class.java, lines))
        // One for the or:ing
        assertEquals(1, countInstances(OrRegWithReg::class.java, lines))
        // One for the not:ing
        assertEquals(1, countInstances(NotReg::class.java, lines))
        // Storing the integer result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
    }
}
