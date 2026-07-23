/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.col.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.common.ast.DeclarationAssignment
import se.dykstrom.jcc.common.types.I64

internal class ColCodeGeneratorValTests : AbstractColCodeGeneratorTests() {

    @Test
    fun shouldNotSupportValOnFasmBackend() {
        // The val statement is an LLVM-only feature
        val statement = ValDeclarationStatement(DeclarationAssignment("a", I64.INSTANCE, IL_17))
        val exception = assertThrows<IllegalArgumentException> { assembleProgram(listOf(statement)) }
        assertTrue(exception.message!!.contains("unsupported statement: ValDeclarationStatement"))
    }
}
