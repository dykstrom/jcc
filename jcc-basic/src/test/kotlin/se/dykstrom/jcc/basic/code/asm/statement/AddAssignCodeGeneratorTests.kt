package se.dykstrom.jcc.basic.code.asm.statement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_0_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_F64_X
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_FOO
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_4
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_53
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.assembly.instruction.Instruction
import se.dykstrom.jcc.common.assembly.instruction.floating.AddMemToFloatReg
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveFloatRegToMem
import se.dykstrom.jcc.common.ast.AddAssignStatement
import se.dykstrom.jcc.common.ast.ArrayAccessExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.code.statement.AddAssignCodeGenerator

/**
 * This class tests the common class [AddAssignCodeGenerator] but it uses BASIC classes,
 * for example the [BasicTypeManager], so it needs to be part of the BASIC tests.
 */
class AddAssignCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = AddAssignCodeGenerator(codeGenerator)

    @Test
    fun generateAddAssignToIntegerIdentifier() {
        // Given
        val identifierExpression = IdentifierNameExpression(0, 0, IDENT_I64_FOO)
        val statement = AddAssignStatement(0, 0, identifierExpression, IL_53)

        // When
        val lines = generator.generate(statement).filterIsInstance<Instruction>().map { it.toText() }

        // Then
        assertEquals(1, lines.size)
        assertEquals("add [${IDENT_I64_FOO.mappedName}], ${IL_53.value}", lines[0])
    }

    @Test
    fun generateAddAssignToFloatIdentifier() {
        // Given
        val identifierExpression = IdentifierNameExpression(0, 0, IDENT_F64_X)
        val statement = AddAssignStatement(0, 0, identifierExpression, FL_0_3)

        // When
        val lines = generator.generate(statement)

        // Then
        assertEquals(1, lines.filterIsInstance<AddMemToFloatReg>().count { it.source == "[${IDENT_F64_X.mappedName}]" })
        assertEquals(1, lines.filterIsInstance<MoveFloatRegToMem>().count { it.destination == "[${IDENT_F64_X.mappedName}]" })
    }

    @Test
    fun generateAddAssignToArrayIdentifier() {
        // Given
        val identifierExpression = ArrayAccessExpression(0, 0, IDENT_ARR_I64_ONE, listOf(IL_4))
        val statement = AddAssignStatement(0, 0, identifierExpression, IL_53)

        // When
        val lines = generator.generate(statement).filterIsInstance<Instruction>().map { it.toText() }

        // Then
        assertEquals(2, lines.size)
        val move = """mov (r[a-z0-9]+), ${IL_4.value}""".toRegex()
        val offset = assertRegexMatches(move, lines[0])
        val add = """add \[${IDENT_ARR_I64_ONE.mappedName}\+8\*${offset}], ${IL_53.value}""".toRegex()
        assertRegexMatches(add, lines[1])
    }
}
