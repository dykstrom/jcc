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

package se.dykstrom.jcc.llvm

import org.junit.jupiter.api.Assertions.assertTrue
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.FloatLiteral
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.llvm.code.FixedLabel
import se.dykstrom.jcc.llvm.code.TestLlvmCodeGenerator
import java.nio.file.Path

class LlvmTests {

    companion object {

        /**
         * Asserts that the given text contains lines that contain the expected strings, in the given order.
         */
        fun assertContainsInOrder(text: String, expected: List<String>) {
            val lines = text.lines()
            var index = 0
            expected.forEach { line ->
                val found = (index until lines.size).firstOrNull { lines[it].contains(line) }
                assertTrue(found != null, "missing line after index $index: $line\n$text")
                index = found!! + 1
            }
        }

        /**
         * Returns the number of times the given substring occurs in the given text.
         */
        fun countOccurrences(text: String, substring: String) = text.windowed(substring.length).count { it == substring }

        /**
         * Generates code for a program made of the given statements, and returns it as text.
         */
        fun generate(codeGenerator: TestLlvmCodeGenerator, vararg statements: Statement): String =
            codeGenerator.generate(AstProgram(0, 0, statements.toList()).withSourcePath(Path.of("test.jcc"))).toText()

        // Literals
        val FL_0_0 = FloatLiteral(0, 0, "0.0")
        val FL_1_0 = FloatLiteral(0, 0, "1.0")

        val IL_M1 = IntegerLiteral(0, 0, "-1")
        val IL_1 = IntegerLiteral(0, 0, "1")
        val IL_2 = IntegerLiteral(0, 0, "2")
        val IL_5 = IntegerLiteral(0, 0, "5")

        val IL_I32_0 = IntegerLiteral(0, 0, "0", I32.INSTANCE)
        val IL_I32_3 = IntegerLiteral(0, 0, "3", I32.INSTANCE)

        // Labels
        val LABEL_L0 = FixedLabel("L0")
        val LABEL_L1 = FixedLabel("L1")
        val LABEL_L2 = FixedLabel("L2")
        val LABEL_L3 = FixedLabel("L3")

        // Identifiers
        val IDENT_F64_X = Identifier("x", F64.INSTANCE)
        val IDENT_F64_Y = Identifier("y", F64.INSTANCE)
        val IDENT_I32_K = Identifier("k", I32.INSTANCE)
        val IDENT_I64_I = Identifier("i", I64.INSTANCE)
        val IDENT_I64_J = Identifier("j", I64.INSTANCE)
        val IDENT_I64_M = Identifier("m", I64.INSTANCE)
        val IDENT_I64_N = Identifier("n", I64.INSTANCE)
        val IDENT_I64_S = Identifier("s", I64.INSTANCE)

        // Identifier references
        val INE_F64_X = IdentifierNameExpression(0, 0, IDENT_F64_X)
        val INE_I32_K = IdentifierNameExpression(0, 0, IDENT_I32_K)
        val INE_I64_I = IdentifierNameExpression(0, 0, IDENT_I64_I)
        val INE_I64_J = IdentifierNameExpression(0, 0, IDENT_I64_J)

        val IDE_F64_Y = IdentifierDerefExpression(0, 0, IDENT_F64_Y)
        val IDE_I64_M = IdentifierDerefExpression(0, 0, IDENT_I64_M)
        val IDE_I64_N = IdentifierDerefExpression(0, 0, IDENT_I64_N)
        val IDE_I64_S = IdentifierDerefExpression(0, 0, IDENT_I64_S)
    }
}
