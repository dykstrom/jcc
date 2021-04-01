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

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.basic.ast.*
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_RANDOMIZE
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_VAL
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.instruction.floating.*
import se.dykstrom.jcc.common.assembly.other.DataDefinition
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.functions.BuiltInFunctions.*
import java.util.Collections.emptyList
import kotlin.test.assertTrue

/**
 * Tests class `BasicCodeGenerator`. This class tests mostly general features. Other
 * classes test code generation involving if and while statements, function calls,
 * floating point operations, and garbage collection.
 *
 * @author Johan Dykstrom
 * @see BasicCodeGenerator
 */
class BasicCodeGeneratorTests : AbstractBasicCodeGeneratorTest() {

    @Before
    fun setUp() {
        defineFunction(FUN_GETLINE)
        defineFunction(FUN_RANDOMIZE)
        defineFunction(FUN_VAL)
    }

    @Test
    fun testEmptyProgram() {
        val result = assembleProgram(emptyList())

        assertDependencies(result.dependencies, FUN_EXIT.name)
        assertCodes(result.codes(), 1, 1, 1, 1)
    }

    @Test
    fun testEnd() {
        val es = EndStatement(0, 0, "10")

        val result = assembleProgram(listOf(es))

        assertDependencies(result.dependencies, FUN_EXIT.name)
        assertCodes(result.codes(), 1, 1, 2, 1)
    }

    @Test
    fun testRem() {
        val rs = CommentStatement(0, 0, "10")

        val result = assembleProgram(listOf(rs))

        assertDependencies(result.dependencies, FUN_EXIT.name)
        assertCodes(result.codes(), 1, 1, 2, 1)
    }

    @Test
    fun testGosub() {
        val gs = GosubStatement(0, 0, "10", "10")

        val result = assembleProgram(listOf(gs))

        assertCodes(result.codes(), 1, 1, 3, 3)
    }

    @Test
    fun testGosubLabel() {
        val gs = GosubStatement(0, 0, "loop", "10")

        val result = assembleProgram(listOf(gs))

        assertCodes(result.codes(), 1, 1, 3, 3)
    }

    @Test
    fun testGoto() {
        val gs = GotoStatement(0, 0, "10", "10")

        val result = assembleProgram(listOf(gs))

        assertDependencies(result.dependencies, FUN_EXIT.name)

        val codes = result.codes()
        assertCodes(codes, 1, 1, 2, 1)
        assertEquals(1, countInstances(Jmp::class.java, codes))
    }

    @Test
    fun testTwoGotos() {
        val gs10 = GotoStatement(0, 0, "20", "10")
        val gs20 = GotoStatement(0, 0, "10", "20")

        val result = assembleProgram(listOf(gs10, gs20))

        assertDependencies(result.dependencies, FUN_EXIT.name)

        val codes = result.codes()
        assertCodes(codes, 1, 1, 3, 1)
        assertEquals(2, countInstances(Jmp::class.java, codes))
    }

    @Test
    fun testOnGoto() {
        val os = OnGotoStatement(0, 0, IL_3, listOf("10", "20"), "10")
        val cs = CommentStatement(0, 0, "comment", "20")

        val result = assembleProgram(listOf(os, cs))
        val codes = result.codes()

        assertCodes(codes, 1, 1, 3, 1)
        // Two compares for the two goto labels
        assertEquals(2, countInstances(CmpRegWithImm::class.java, codes))
        // Two jumps for the two goto labels
        assertEquals(2, countInstances(Je::class.java, codes))
    }

    @Test
    fun testOnGotoLabels() {
        val os = OnGotoStatement(0, 0, IL_3, listOf("foo", "bar"), "foo")
        val cs = CommentStatement(0, 0, "comment", "bar")

        val result = assembleProgram(listOf(os, cs))
        val codes = result.codes()

        assertCodes(codes, 1, 1, 3, 1)
        // Two compares for the two goto labels
        assertEquals(2, countInstances(CmpRegWithImm::class.java, codes))
        // Two jumps for the two goto labels
        assertEquals(2, countInstances(Je::class.java, codes))
    }

    @Test
    fun testOnGosub() {
        val os = OnGosubStatement(0, 0, IL_3, listOf("10", "20"), "10")
        val cs = CommentStatement(0, 0, "comment", "20")

        val result = assembleProgram(listOf(os, cs))
        val codes = result.codes()

        // Six labels - main, two lines, and five for on-gosub (including bridge calls)
        // Three calls - exit, and four for on-gosub (including bridge calls)
        assertCodes(codes, 1, 1, 8, 5)
        // Two compares for the two on-gosub labels
        assertEquals(2, countInstances(CmpRegWithImm::class.java, codes))
        // Two jumps for the two on-gosub labels
        assertEquals(2, countInstances(Je::class.java, codes))
    }

    @Test
    fun shouldGenerateCodeForReturn() {
        val rs = ReturnStatement(0, 0, "100")

        val result = assembleProgram(listOf(rs))
        val codes = result.codes()

        assertCodes(result.codes(), 1, 2, 4, 5)
        assertEquals(2, countInstances(Ret::class.java, codes))
    }

    @Test
    fun testPrint() {
        val ps = PrintStatement(0, 0, listOf(SL_FOO), "100")

        val result = assembleProgram(listOf(ps))

        assertDependencies(result.dependencies, FUN_EXIT.name, FUN_PRINTF.name)
        assertCodes(result.codes(), 1, 2, 2, 2)
    }

    @Test
    fun testPrintTwoStrings() {
        val s1 = StringLiteral(0, 0, "Hello, ")
        val s2 = StringLiteral(0, 0, "world!")
        val ps = PrintStatement(0, 0, listOf(s1, s2), "100")

        val result = assembleProgram(listOf(ps))

        assertDependencies(result.dependencies, FUN_EXIT.name, FUN_PRINTF.name)
        assertCodes(result.codes(), 1, 2, 2, 2)
    }

    @Test
    fun testTwoPrintsOneLine() {
        val ps100a = PrintStatement(0, 0, listOf(SL_ONE), "100")
        val ps100b = PrintStatement(0, 0, listOf(SL_TWO))

        val result = assembleProgram(listOf(ps100a, ps100b))

        assertDependencies(result.dependencies, FUN_EXIT.name, FUN_PRINTF.name)
        assertCodes(result.codes(), 1, 2, 2, 3)
    }

    @Test
    fun testTwoPrintsTwoLines() {
        val ps100 = PrintStatement(1, 0, listOf(SL_ONE), "100")
        val ps110 = PrintStatement(2, 0, listOf(SL_TWO), "110")

        val result = assembleProgram(listOf(ps100, ps110))

        assertDependencies(result.dependencies, FUN_EXIT.name, FUN_PRINTF.name)
        assertCodes(result.codes(), 1, 2, 3, 3)
    }

    @Test
    fun testOnePrintFiveStrings() {
        val s1 = StringLiteral(1, 10, "<1>")
        val s2 = StringLiteral(1, 20, "<2>")
        val s3 = StringLiteral(1, 30, "<3>")
        val s4 = StringLiteral(1, 40, "<4>")
        val s5 = StringLiteral(1, 50, "<5>")

        val ps = PrintStatement(1, 0, listOf(s1, s2, s3, s4, s5), "100")
        val result = assembleProgram(listOf(ps))

        assertDependencies(result.dependencies, FUN_EXIT.name, FUN_PRINTF.name)

        val codes = result.codes()
        assertCodes(codes, 1, 2, 2, 2)
        assertEquals(7, countInstances(PushReg::class.java, codes))
    }

    @Test
    fun shouldPrintDefDblVariable() {
        val defdblStatement = DefDblStatement(0, 0, setOf('f'))
        val printStatement = PrintStatement(0, 0, listOf(IDE_F64_F))
        val result = assembleProgram(listOf(defdblStatement, printStatement))
        val codes = result.codes()

        // A format string for a float proves that identifier 'u' with type unknown has been interpreted as a float
        assertTrue(codes
                .filterIsInstance<DataDefinition>()
                .any { it.identifier.mappedName == "__fmt_F64" })
    }

    @Test
    fun shouldAddStrings() {
        val expression = AddExpression(0, 0, SL_ONE, SL_TWO)
        val statement = PrintStatement(0, 0, listOf(expression), "10")
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(4, countInstances(MoveImmToReg::class.java, codes))
        assertEquals(1, countInstances(AddRegToReg::class.java, codes))
        // strlen*2, malloc, strcpy, strcat, printf, free, exit
        assertEquals(8, countInstances(CallIndirect::class.java, codes))
        assertEquals(2, codes
                .filterIsInstance<CallIndirect>()
                .filter { it.target.contains("strlen") }
                .count()
        )
    }

    @Test
    fun shouldAddIntegers() {
        val expression = AddExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression), "10")
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(4, countInstances(MoveImmToReg::class.java, codes))
        assertEquals(1, countInstances(AddRegToReg::class.java, codes))
    }

    @Test
    fun testOnePrintSub() {
        val expression = SubExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression), "10")
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(4, countInstances(MoveImmToReg::class.java, codes))
        assertEquals(1, countInstances(SubRegFromReg::class.java, codes))
    }

    @Test
    fun testOnePrintMul() {
        val expression = MulExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression), "10")
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(4, countInstances(MoveImmToReg::class.java, codes))
        assertEquals(1, countInstances(IMulRegWithReg::class.java, codes))
    }

    @Test
    fun testOnePrintDiv() {
        val expression = DivExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression), "10")
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(4, countInstances(MoveImmToReg::class.java, codes))
        // Floating point division even though the arguments are integers
        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, codes))
        assertEquals(2, countInstances(ConvertIntRegToFloatReg::class.java, codes))
    }

    @Test
    fun testOnePrintIDiv() {
        val expression = IDivExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression), "10")
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(4, countInstances(MoveImmToReg::class.java, codes))
        assertEquals(1, countInstances(IDivWithReg::class.java, codes))
        assertEquals(1, countInstances(Cqo::class.java, codes))
    }

    @Test
    fun testOnePrintMod() {
        val expression = ModExpression(0, 0, IL_1, IL_2)
        val statement = PrintStatement(0, 0, listOf(expression), "10")
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(4, countInstances(MoveImmToReg::class.java, codes))
        assertEquals(1, countInstances(IDivWithReg::class.java, codes))
        assertEquals(1, countInstances(Cqo::class.java, codes))
    }

    @Test
    fun testOnePrintMulAddMul() {
        // 4 * 2 + 3 * 1
        val ms1 = MulExpression(0, 0, IL_4, IL_2)
        val ms2 = MulExpression(0, 0, IL_3, IL_1)
        val assignStatement = AddExpression(0, 0, ms1, ms2)
        val printStatement = PrintStatement(0, 0, listOf(assignStatement), "10")

        val result = assembleProgram(listOf(printStatement))
        val codes = result.codes()

        assertEquals(6, countInstances(MoveImmToReg::class.java, codes))
        assertEquals(3, countInstances(MoveRegToReg::class.java, codes))
        assertEquals(2, countInstances(IMulRegWithReg::class.java, codes))
        assertEquals(1, countInstances(AddRegToReg::class.java, codes))
    }

    @Test
    fun testGotoPrintAndEnd() {
        val gs100 = GotoStatement(1, 0, "110", "100")
        val ps110 = PrintStatement(2, 0, listOf(SL_FOO), "110")
        val es120 = EndStatement(3, 0, "120")

        val result = assembleProgram(listOf(gs100, ps110, es120))

        assertDependencies(result.dependencies, FUN_EXIT.name, FUN_PRINTF.name)

        val codes = result.codes()
        assertCodes(codes, 1, 2, 4, 2)
        assertEquals(2, countInstances(PushReg::class.java, codes))
        assertEquals(1, countInstances(Jmp::class.java, codes))
    }

    @Test
    fun shouldAssignIntegerLiteral() {
        val statement = AssignStatement(0, 0, NAME_A, IL_4)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Exit code, and evaluating the integer literal
        assertEquals(2, countInstances(MoveImmToReg::class.java, codes))
        // Storing the evaluated integer literal
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun shouldAssignFloatLiteral() {
        val statement = AssignStatement(0, 0, NAME_F, FL_3_14)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Evaluating the literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, codes))
        // Storing the evaluated literal
        assertEquals(1, countInstances(MoveFloatRegToMem::class.java, codes))
    }

    @Test
    fun shouldAssignStringLiteral() {
        val statement = AssignStatement(0, 0, NAME_B, SL_FOO)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Exit code, evaluating the string literal, and resetting the type pointer for b$
        assertEquals(3, countInstances(MoveImmToReg::class.java, codes))
        // Storing the evaluated string literal and the type pointer
        assertEquals(2, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun shouldAssignBooleanLiteral() {
        val assignStatement = AssignStatement(0, 0, NAME_C, BL_TRUE)
        val result = assembleProgram(listOf(assignStatement))
        val codes = result.codes()

        // Exit code, and evaluating the boolean literal
        assertEquals(2, countInstances(MoveImmToReg::class.java, codes))
        // Find move that stores the literal value in register while evaluating
        assertEquals(1, codes.stream()
                .filter { code -> code is MoveImmToReg }
                .map { code -> (code as MoveImmToReg).immediate }
                .filter { immediate -> immediate == BL_TRUE.value }
                .count())
        // Storing the evaluated literal in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun shouldRandomizeWithoutExpression() {
        val statement = RandomizeStatement(0, 0)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // The randomize statement calls randomize(val(getline()))
        assertEquals(1, codes
                .filterIsInstance<CallDirect>()
                .filter { it.target.contains("getline") }
                .count())
        assertEquals(1, codes
                .filterIsInstance<CallIndirect>()
                .filter { it.target.contains("atoi64") }
                .count())
        assertEquals(1, codes
                .filterIsInstance<CallIndirect>()
                .filter { it.target.contains("randomize") }
                .count())
    }

    @Test
    fun shouldRandomizeWithInteger() {
        val statement = RandomizeStatement(0, 0, IL_3)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // The randomize statement calls randomize function in the standard library
        assertEquals(1, codes
                .filterIsInstance<CallIndirect>()
                .filter { it.target.contains("randomize") }
                .count())
    }

    @Test
    fun shouldRandomizeWithFloatExpression() {
        val statement = RandomizeStatement(0, 0, AddExpression(0, 0, FL_3_14, FL_17_E4))
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // The randomize statement calls randomize function in the standard library
        assertEquals(1, codes
                .filterIsInstance<CallIndirect>()
                .filter { it.target.contains("randomize") }
                .count())
    }

    @Test
    fun shouldSwapIntegers() {
        val statement = SwapStatement(0, 0, NAME_A, NAME_H)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Moving the variable contents to registers
        assertEquals(2, countInstances(MoveMemToReg::class.java, codes))
        // Moving the register contents to variables
        assertEquals(2, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun shouldSwapFloats() {
        val statement = SwapStatement(0, 0, NAME_F, NAME_G)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Moving the variable contents to registers
        assertEquals(2, countInstances(MoveMemToReg::class.java, codes))
        // Moving the register contents to variables
        assertEquals(2, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun shouldSwapIntegerAndFloat() {
        val statement = SwapStatement(0, 0, NAME_A, NAME_G)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Moving the variable contents to registers
        assertEquals(1, countInstances(MoveMemToReg::class.java, codes))
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, codes))
        // Moving the register contents to variables
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
        assertEquals(1, countInstances(MoveFloatRegToMem::class.java, codes))
        // Converting from integer to float and vice versa
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        assertEquals(1, countInstances(RoundFloatRegToIntReg::class.java, codes))
    }

    @Test
    fun shouldSwapStrings() {
        val statement = SwapStatement(0, 0, NAME_B, NAME_S)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Moving the variable contents (and variable type pointers) to registers
        assertEquals(4, countInstances(MoveMemToReg::class.java, codes))
        // Moving the register contents to variables (and variable type pointers)
        assertEquals(4, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun testOneAssignmentAddExpression() {
        val ae = AddExpression(0, 0, IL_1, IL_2)

        val statement = AssignStatement(0, 0, NAME_A, ae)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(1, countInstances(AddRegToReg::class.java, codes))
        assertEquals(1, codes
                .stream()
                .filter { code -> code is MoveRegToMem }
                .map { code -> (code as MoveRegToMem).destination }
                .filter { name -> name == "[" + IDENT_I64_A.mappedName + "]" }
                .count())
    }

    @Test
    fun testOneAssignmentIdentifierExpression() {
        val statement = AssignStatement(0, 0, NAME_A, IDE_I64_H)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(1, countInstances(MoveImmToReg::class.java, codes))
        assertEquals(1, codes
                .stream()
                .filter { code -> code is MoveMemToReg }
                .map { code -> (code as MoveMemToReg).source }
                .filter { name -> name == "[" + IDENT_I64_H.mappedName + "]" }
                .count())
        assertEquals(1, codes
                .stream()
                .filter { code -> code is MoveRegToMem }
                .map { code -> (code as MoveRegToMem).destination }
                .filter { name -> name == "[" + IDENT_I64_A.mappedName + "]" }
                .count())
    }

    @Test
    fun testPrintTwoIdentifierExpressions() {
        val statement = PrintStatement(0, 0, listOf(IDE_I64_A, IDE_I64_H))
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        assertEquals(2, countInstances(MoveImmToReg::class.java, codes))
        assertEquals(1, codes
                .stream()
                .filter { code -> code is MoveMemToReg }
                .map { code -> (code as MoveMemToReg).source }
                .filter { name -> name == "[" + IDENT_I64_H.mappedName + "]" }
                .count())
        assertEquals(1, codes
                .stream()
                .filter { code -> code is MoveMemToReg }
                .map { code -> (code as MoveMemToReg).source }
                .filter { name -> name == "[" + IDENT_I64_A.mappedName + "]" }
                .count())
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
        val result = assembleProgram(listOf(AssignStatement(0, 0, NAME_C, expression)))
        val codes = result.codes()

        // One for the exit code, two for the integer subexpressions, and two for the boolean results
        assertEquals(5, countInstances(MoveImmToReg::class.java, codes))
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(Cmp::class.java, codes))
        // One for the conditional jump
        assertEquals(1, countInstances(conditionalJump, codes))
        // One for the unconditional jump
        assertEquals(1, countInstances(Jmp::class.java, codes))
        // Storing the boolean result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
    }

    private fun assertRelationalExpressionStrings(expression: Expression, conditionalJump: Class<out Jump>) {
        val result = assembleProgram(listOf(AssignStatement(0, 0, NAME_C, expression)))
        val codes = result.codes()

        // Libraries: msvcrt
        // Imports: strcmp, exit
        // Labels: main, @@, after_cmp
        // Calls: strcmp, exit
        assertCodes(codes, 1, 2, 3, 2)

        // One for the exit code, two for the integer subexpressions, and two for the boolean results
        assertEquals(5, countInstances(MoveImmToReg::class.java, codes))
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(Cmp::class.java, codes))
        // One for the conditional jump
        assertEquals(1, countInstances(conditionalJump, codes))
        // One for the unconditional jump
        assertEquals(1, countInstances(Jmp::class.java, codes))
        // Storing the boolean result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun testOneAssignmentWithOneComplexEqualExpression() {
        val ae = AddExpression(0, 0, IDE_I64_A, IL_1)
        val se = SubExpression(0, 0, IL_2, IDE_I64_H)
        val expression = EqualExpression(0, 0, ae, se)

        val statement = AssignStatement(0, 0, NAME_C, expression)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // One for the exit code, two for the integer subexpressions, and two for the boolean results
        assertEquals(5, countInstances(MoveImmToReg::class.java, codes))
        // Two for the ident subexpressions
        assertEquals(2, countInstances(MoveMemToReg::class.java, codes))
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(Cmp::class.java, codes))
        // One for the conditional jump
        assertEquals(1, countInstances(Je::class.java, codes))
        // One for the unconditional jump
        assertEquals(1, countInstances(Jmp::class.java, codes))
        // Storing the boolean result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
        // Find assignment to memory location
        assertEquals(1, codes
                .stream()
                .filter { code -> code is MoveRegToMem }
                .map { code -> (code as MoveRegToMem).destination }
                .filter { name -> name == "[" + IDENT_BOOL_C.mappedName + "]" }
                .count())
    }

    @Test
    fun testOneAssignmentWithOneAnd() {
        val ee = EqualExpression(0, 0, IL_3, IL_4)
        val expression = AndExpression(0, 0, BL_FALSE, ee)

        val statement = AssignStatement(0, 0, NAME_C, expression)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // One for the exit code, one for the boolean subexpression,
        // two for the integer subexpressions, and two for the boolean results
        assertEquals(6, countInstances(MoveImmToReg::class.java, codes))
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(Cmp::class.java, codes))
        // One for the conditional jump
        assertEquals(1, countInstances(Je::class.java, codes))
        // One for the unconditional jump
        assertEquals(1, countInstances(Jmp::class.java, codes))
        // One for the and:ing of booleans
        assertEquals(1, countInstances(AndRegWithReg::class.java, codes))
        // Storing the boolean result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun testOneAssignmentWithOneOr() {
        val expression = OrExpression(0, 0, BL_FALSE, BL_TRUE)

        val statement = AssignStatement(0, 0, NAME_C, expression)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // One for the exit code, two for the boolean subexpressions
        assertEquals(3, countInstances(MoveImmToReg::class.java, codes))
        // One for the or:ing of booleans
        assertEquals(1, countInstances(OrRegWithReg::class.java, codes))
        // Storing the boolean result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun testOneAssignmentWithOneXor() {
        val expression = XorExpression(0, 0, BL_FALSE, BL_TRUE)

        val statement = AssignStatement(0, 0, NAME_C, expression)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // One for the exit code, two for the boolean subexpressions
        assertEquals(3, countInstances(MoveImmToReg::class.java, codes))
        // One for the xor:ing of booleans
        assertEquals(1, countInstances(XorRegWithReg::class.java, codes))
        // Storing the boolean result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun testOneAssignmentWithOneNot() {
        val expression = NotExpression(0, 0, BL_FALSE)

        val statement = AssignStatement(0, 0, NAME_C, expression)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // One for the exit code, one for the boolean subexpression
        assertEquals(2, countInstances(MoveImmToReg::class.java, codes))
        // One for the not:ing
        assertEquals(1, countInstances(NotReg::class.java, codes))
        // Storing the boolean result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
    }

    @Test
    fun testComplexBooleanExpression() {
        val ee = EqualExpression(0, 0, IL_3, IL_4)
        val ge = GreaterExpression(0, 0, IL_2, IDE_I64_A)
        val ae1 = AndExpression(0, 0, BL_FALSE, ee)
        val ae2 = AndExpression(0, 0, ge, BL_TRUE)
        val oe = OrExpression(0, 0, ae1, ae2)
        val ne = NotExpression(0, 0, oe)

        val statement = AssignStatement(0, 0, IdentifierNameExpression(0, 0, IDENT_BOOL_C), ne)
        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // One for the exit code, two for the boolean literals,
        // three for the integer literals, and four for the boolean results
        assertEquals(10, countInstances(MoveImmToReg::class.java, codes))
        // Two for comparing two integer subexpressions
        assertEquals(2, countInstances(Cmp::class.java, codes))
        // One for the conditional jump
        assertEquals(1, countInstances(Je::class.java, codes))
        // One for the other conditional jump
        assertEquals(1, countInstances(Jg::class.java, codes))
        // Two for the unconditional jumps
        assertEquals(2, countInstances(Jmp::class.java, codes))
        // Two for the and:ing of booleans
        assertEquals(2, countInstances(AndRegWithReg::class.java, codes))
        // One for the or:ing of booleans
        assertEquals(1, countInstances(OrRegWithReg::class.java, codes))
        // One for the not:ing
        assertEquals(1, countInstances(NotReg::class.java, codes))
        // Storing the boolean result in memory
        assertEquals(1, countInstances(MoveRegToMem::class.java, codes))
    }

    private fun assertDependencies(dependencies: Map<String, Set<String>>, vararg expectedFunctions: String) {
        val library = dependencies.keys.iterator().next()
        assertEquals(expectedFunctions.toSet(), dependencies[library])
    }
}
