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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_B
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_B
import se.dykstrom.jcc.common.ast.ArrayAccessExpression
import se.dykstrom.jcc.common.ast.ArrayDeclaration
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement
import se.dykstrom.jcc.common.symbols.Scope.GLOBAL
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str

/**
 * Tests LLVM code generation for arrays: global emission, element access (read/write), and the
 * multiply-accumulate index computation. LBOUND/UBOUND and SWAP are covered end-to-end by
 * BasicLlvmCompileAndRunArrayIT.
 *
 * These tests bypass semantic analysis, so they supply the inclusive-adjusted subscripts and the
 * I64 access indices directly, as the semantics parser would.
 *
 * @author Johan Dykstrom
 */
internal class BasicLlvmCodeGeneratorArrayTests : AbstractBasicCodeGeneratorTests() {

    private val cg = BasicLlvmCodeGenerator(typeManager, symbols, optimizer)

    private val identVec = Identifier("vec", Arr.from(1, I64.INSTANCE))
    private val identMat = Identifier("mat", Arr.from(2, I64.INSTANCE))
    private val identFvec = Identifier("fv", Arr.from(1, F64.INSTANCE))
    private val identSvec = Identifier("sv", Arr.from(1, Str.INSTANCE))

    private fun il(value: Long) = IntegerLiteral(0, 0, value)

    private fun dim(identifier: Identifier, vararg sizes: Long) =
        VariableDeclarationStatement(
            0, 0,
            listOf(ArrayDeclaration(0, 0, identifier.name(), identifier.type() as Arr, sizes.map { il(it) })),
            GLOBAL
        )

    @Test
    fun shouldEmitIntegerArrayGlobalAndDimsMetadata() {
        val result = assembleProgram(cg, listOf(dim(identVec, 4)))
        assertContains(result, listOf(
            "@_vec_arr = private global [4 x i64] zeroinitializer",
            "@_vec_arr_dims = private constant [1 x i64] [i64 4]",
        ))
    }

    @Test
    fun shouldEmitFloatArrayGlobal() {
        val result = assembleProgram(cg, listOf(dim(identFvec, 3)))
        assertContains(result, listOf(
            "@_fv_arr = private global [3 x double] zeroinitializer",
        ))
    }

    @Test
    fun shouldEmitStringArrayGlobalWithEmptyStringDefault() {
        // String elements default to the empty-string constant, not a null pointer.
        val result = assembleProgram(cg, listOf(dim(identSvec, 2)))
        assertContains(result, listOf(
            "@_sv_arr = private global [2 x ptr] [ptr @_.str.empty, ptr @_.str.empty]",
        ))
    }

    @Test
    fun shouldEmitMultiDimensionalArrayGlobalAndDims() {
        // dim mat(3, 5) -> 15 elements, dimension sizes [3, 5]
        val result = assembleProgram(cg, listOf(dim(identMat, 3, 5)))
        assertContains(result, listOf(
            "@_mat_arr = private global [15 x i64] zeroinitializer",
            "@_mat_arr_dims = private constant [2 x i64] [i64 3, i64 5]",
        ))
    }

    @Test
    fun shouldGenerateElementReadWithGetElementPtr() {
        // b% = vec(2)
        val access = ArrayAccessExpression(0, 0, identVec, listOf(il(2)))
        val assign = AssignStatement(INE_I64_B, access)
        val result = assembleProgram(cg, listOf(dim(identVec, 4), assign))
        assertContains(result, listOf(
            "getelementptr i64, ptr @_vec_arr, i64 2",
        ))
    }

    @Test
    fun shouldGenerateElementStore() {
        // vec(2) = 42
        val access = ArrayAccessExpression(0, 0, identVec, listOf(il(2)))
        val assign = AssignStatement(access, il(42))
        val result = assembleProgram(cg, listOf(dim(identVec, 4), assign))
        // Compute the element address, then store the value into it.
        assertContains(result, listOf(
            "getelementptr i64, ptr @_vec_arr, i64 2",
            "store i64 42, ptr",
        ))
    }

    @Test
    fun shouldComputeMultiDimensionalIndexWithMultiplyAccumulate() {
        // mat(a%, b%) with dimension sizes [3, 5]: index = a% * 5 + b%
        val access = ArrayAccessExpression(0, 0, identMat, listOf(IDE_I64_A, IDE_I64_B))
        val assign = AssignStatement(INE_I64_A, access)
        val result = assembleProgram(cg, listOf(dim(identMat, 3, 5), assign))
        assertContains(result, listOf(
            "= mul i64 ", // index * size(dim 1)
            ", 5",        // size of the second dimension
            "= add i64 ", // + subscript
            "getelementptr i64, ptr @_mat_arr, i64 ",
        ))
    }
}
