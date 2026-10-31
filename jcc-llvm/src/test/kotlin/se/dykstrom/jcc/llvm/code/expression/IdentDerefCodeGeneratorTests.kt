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

package se.dykstrom.jcc.llvm.code.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.symbols.Scope
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Opaque
import se.dykstrom.jcc.common.types.Str
import se.dykstrom.jcc.llvm.code.Line
import se.dykstrom.jcc.llvm.operand.LiteralOperand
import se.dykstrom.jcc.llvm.operation.LoadOperation

class IdentDerefCodeGeneratorTests {

    private val symbols = SymbolTable()
    private val codeGenerator = IdentDerefCodeGenerator(Scope.NONE)
    private val lines = mutableListOf<Line>()

    @Test
    fun shouldLoadNumericConstant() {
        val identifier = symbols.addConstant(Identifier("n", I64.INSTANCE), "17")
        codeGenerator.toLlvm(IdentifierDerefExpression(0, 0, identifier), lines, symbols)
        assertEquals(1, lines.size)
        assertTrue(lines[0] is LoadOperation)
    }

    @Test
    fun shouldUseAddressOfStringConstant() {
        val identifier = symbols.addConstant(Identifier("s", Str.INSTANCE), "hi")
        val operand = codeGenerator.toLlvm(IdentifierDerefExpression(0, 0, identifier), lines, symbols)
        assertEquals(LiteralOperand("@_s", Str.INSTANCE), operand)
        assertTrue(lines.isEmpty())
    }

    @Test
    fun shouldUseAddressOfOpaqueStringConstant() {
        val name = Opaque("Name", Str.INSTANCE)
        val identifier = symbols.addConstant(Identifier("s", name), "Bob")
        val operand = codeGenerator.toLlvm(IdentifierDerefExpression(0, 0, identifier), lines, symbols)
        assertEquals(LiteralOperand("@_s", name), operand)
        assertTrue(lines.isEmpty())
    }

    @Test
    fun shouldLoadStringVariable() {
        val identifier = Identifier("s", Str.INSTANCE)
        symbols.addVariable(identifier, null)
        codeGenerator.toLlvm(IdentifierDerefExpression(0, 0, identifier), lines, symbols)
        assertEquals(1, lines.size)
        assertTrue(lines[0] is LoadOperation)
    }
}
