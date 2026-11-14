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

package se.dykstrom.jcc.llvm.code

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.functions.ReferenceFunction
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.I128
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Opaque
import se.dykstrom.jcc.common.types.Str
import se.dykstrom.jcc.llvm.operand.LiteralOperand
import se.dykstrom.jcc.llvm.operand.LlvmOperand
import se.dykstrom.jcc.llvm.operand.TempOperand

class I128HalvesTests {

    private val symbolTable = SymbolTable()
    private val lines = mutableListOf<Line>()

    @Test
    fun shouldSplitI128OfTailccFunction() {
        assertTrue(I128Halves.isSplit(UDF, I128.INSTANCE))
        assertTrue(I128Halves.isSplit(UDF, WIDE))
        assertTrue(I128Halves.isSplit(REFERENCE, I128.INSTANCE))
    }

    @Test
    fun shouldNotSplitOtherTypes() {
        assertFalse(I128Halves.isSplit(UDF, I64.INSTANCE))
        assertFalse(I128Halves.isSplit(UDF, Opaque("Narrow", I64.INSTANCE)))
        assertFalse(I128Halves.isSplit(UDF, Str.INSTANCE))
    }

    @Test
    fun shouldNotSplitI128OfCFunction() {
        assertFalse(I128Halves.isSplit(LIBRARY, I128.INSTANCE))
        assertFalse(I128Halves.isSplit(MAIN, I128.INSTANCE))
    }

    @Test
    fun shouldSplitI128Args() {
        // Given
        val opA = TempOperand(symbolTable.nextTempName(), I128.INSTANCE)
        val opB = LiteralOperand(7, I64.INSTANCE)
        val opC = TempOperand(symbolTable.nextTempName(), WIDE)

        // When
        val result = I128Halves.split(UDF, listOf(opA, opB, opC), lines, symbolTable)

        // Then
        assertEquals(listOf("i64 %2", "i64 %4", "i64 7", "i64 %5", "i64 %7"), result.map { it.toTypedText() })
        assertEquals(
            listOf(
                "%2 = trunc i128 %0 to i64",
                "%3 = lshr i128 %0, 64",
                "%4 = trunc i128 %3 to i64",
                "%5 = trunc i128 %1 to i64",
                "%6 = lshr i128 %1, 64",
                "%7 = trunc i128 %6 to i64",
            ),
            lines.map { it.toText() }
        )
    }

    @Test
    fun shouldSplitLiteralI128Arg() {
        // When
        val result = I128Halves.split(UDF, listOf(LiteralOperand(5, I128.INSTANCE)), lines, symbolTable)

        // Then
        assertEquals(listOf("i64 %0", "i64 %2"), result.map { it.toTypedText() })
        assertEquals(
            listOf("%0 = trunc i128 5 to i64", "%1 = lshr i128 5, 64", "%2 = trunc i128 %1 to i64"),
            lines.map { it.toText() }
        )
    }

    @Test
    fun shouldNotSplitArgsWithoutI128() {
        // Given
        val args = listOf<LlvmOperand>(LiteralOperand(7, I64.INSTANCE), TempOperand("%s", Str.INSTANCE))

        // When
        val result = I128Halves.split(UDF, args, lines, symbolTable)

        // Then
        assertEquals(args, result)
        assertTrue(lines.isEmpty())
    }

    @Test
    fun shouldNotSplitArgsOfCFunction() {
        // Given
        val args = listOf<LlvmOperand>(TempOperand("%x", I128.INSTANCE))

        // When
        val result = I128Halves.split(LIBRARY, args, lines, symbolTable)

        // Then
        assertEquals(args, result)
        assertTrue(lines.isEmpty())
    }

    @Test
    fun shouldJoinHalves() {
        // Given
        val opLow = TempOperand(symbolTable.nextTempName(), I64.INSTANCE)
        val opHigh = TempOperand(symbolTable.nextTempName(), I64.INSTANCE)

        // When
        val result = I128Halves.join(opLow, opHigh, lines, symbolTable)

        // Then
        assertEquals("i128 %5", result.toTypedText())
        assertEquals(
            listOf(
                "%2 = zext i64 %0 to i128",
                "%3 = zext i64 %1 to i128",
                "%4 = shl i128 %3, 64",
                "%5 = or i128 %4, %2",
            ),
            lines.map { it.toText() }
        )
    }

    private fun LlvmOperand.toTypedText() = type().llvmName() + " " + toText()

    companion object {
        private val WIDE = Opaque("Wide", I128.INSTANCE)
        private val UDF = UserDefinedFunction("foo", listOf("a"), listOf(I128.INSTANCE), I64.INSTANCE)
        private val MAIN = UserDefinedFunction("main", listOf(), listOf(), I64.INSTANCE)
        private val REFERENCE = ReferenceFunction("f", listOf(I128.INSTANCE), I64.INSTANCE)
        private val LIBRARY = LibraryFunction("bar", listOf(I128.INSTANCE), Str.INSTANCE, ExternalFunction("bar"))
    }
}
