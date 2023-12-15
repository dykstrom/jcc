package se.dykstrom.jcc.basic.code.statement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_FOO
import se.dykstrom.jcc.basic.ast.OnGosubStatement
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.common.assembly.base.Instruction
import se.dykstrom.jcc.common.assembly.base.Label
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression

class OnGosubCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = OnGosubCodeGenerator(codeGenerator)

    @Test
    fun generateAddAssignToScalarIdentifier() {
        // Given
        val identifierExpression = IdentifierDerefExpression(0, 0, IDENT_I64_FOO)
        val statement = OnGosubStatement(0, 0, identifierExpression, listOf("10", "20"))

        // When
        val lines = generator.generate(statement).filter { it is Instruction || it is Label }.map { it.toText() }

        // Then
        assertEquals(13, lines.size)
        val moveExpr = """mov (r[a-z0-9]+), \[${IDENT_I64_FOO.mappedName}]""".toRegex()
        val expr = assertRegexMatches(moveExpr, lines[0])
        assertEquals("cmp $expr, 1", lines[1])
        val jumpToLabel0 = """je ([a-z0-9_]+)""".toRegex()
        val label0 = assertRegexMatches(jumpToLabel0, lines[2])
        val jumpToEnd = """jmp (__on_gosub_end_[0-9]+)""".toRegex()
        val labelEnd = assertRegexMatches(jumpToEnd, lines[5])
        assertEquals("$label0:", lines[6])
        assertEquals("$labelEnd:", lines[12])
    }
}
