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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.IDENT_I64_A
import se.dykstrom.jcc.col.ColTests.Companion.IDENT_I64_B
import se.dykstrom.jcc.col.ColTests.Companion.IDE_I64_A
import se.dykstrom.jcc.col.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_I64
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.DeclarationAssignment

internal class ColCodeGeneratorValTests : AbstractColCodeGeneratorTests() {

    @Test
    fun valDeclaration() {
        val statement = ValDeclarationStatement(DeclarationAssignment(IDENT_I64_A.name(), IDENT_I64_A.type(), IL_17))
        val result = assembleProgram(cg, listOf(statement))
        assertContains(result, listOf(
            "%_a = alloca i64",
            "store i64 17, ptr %_a",
        ))
    }

    @Test
    fun valDeclarationAndReference() {
        val statements = listOf(
            ValDeclarationStatement(DeclarationAssignment(IDENT_I64_A.name(), IDENT_I64_A.type(), IL_17)),
            funCall(BF_PRINTLN_I64, IDE_I64_A),
        )
        val result = assembleProgram(cg, statements)
        assertContains(result, listOf(
            "%_a = alloca i64",
            "store i64 17, ptr %_a",
            "%0 = load i64, ptr %_a",
            "%1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64.nl, i64 %0)",
        ))
    }

    @Test
    fun valReferencingEarlierVal() {
        val statements = listOf(
            ValDeclarationStatement(DeclarationAssignment(IDENT_I64_A.name(), IDENT_I64_A.type(), IL_17)),
            ValDeclarationStatement(DeclarationAssignment(IDENT_I64_B.name(), IDENT_I64_B.type(), AddExpression(IDE_I64_A, IL_17))),
        )
        val result = assembleProgram(cg, statements)
        assertContains(result, listOf(
            "%_a = alloca i64",
            "%_b = alloca i64",
            "store i64 17, ptr %_a",
            "%0 = load i64, ptr %_a",
            "%1 = add i64 %0, 17",
            "store i64 %1, ptr %_b",
        ))
    }
}
