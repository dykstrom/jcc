/*
 * Copyright (C) 2024 Johan Dykstrom
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

package se.dykstrom.jcc.basic.code.asm.statement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_FOO
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_53
import se.dykstrom.jcc.basic.code.AbstractBasicCodeGeneratorComponentTests
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.assembly.instruction.IMulImmWithReg
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.MulAssignStatement
import se.dykstrom.jcc.common.code.statement.MulAssignCodeGenerator

/**
 * This class tests the common class [MulAssignCodeGenerator] but it uses BASIC classes,
 * for example the [BasicTypeManager], so it needs to be part of the BASIC tests.
 */
class MulAssignCodeGeneratorTests : AbstractBasicCodeGeneratorComponentTests() {

    private val generator = MulAssignCodeGenerator(codeGenerator)

    @Test
    fun generateMulAssignToIntegerIdentifier() {
        // Given
        val identifierExpression = IdentifierNameExpression(0, 0, IDENT_I64_FOO)
        val statement = MulAssignStatement(0, 0, identifierExpression, IL_53)

        // When
        val lines = generator.generate(statement)

        // Then
        assertEquals(1, lines.filterIsInstance<IMulImmWithReg>().count { it.source == IL_53.value })
    }
}
