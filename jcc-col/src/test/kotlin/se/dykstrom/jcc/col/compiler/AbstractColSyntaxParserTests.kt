/*
 * Copyright (C) 2023 Johan Dykstrom
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

import org.junit.jupiter.api.Assertions.assertFalse
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.Identifier
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

abstract class AbstractColSyntaxParserTests {

    private val errorListener = CompilationErrorListener()
    private val syntaxParser = ColSyntaxParser(errorListener)

    protected fun parse(text: String): AstProgram {
        val program = syntaxParser.parse(ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8)))
        assertFalse { errorListener.hasErrors() }
        return program
    }

    fun printlnCall(): Statement = FunCallStatement(FunctionCallExpression(IDENT_FUN_PRINTLN_ZERO, listOf()))
    fun printlnCall(one : Expression): Statement = FunCallStatement(FunctionCallExpression(IDENT_FUN_PRINTLN_ONE, listOf(one)))

    companion object {
        private val IDENT_FUN_PRINTLN_ZERO = Identifier("println", Fun.from(listOf(), null))
        private val IDENT_FUN_PRINTLN_ONE = Identifier("println", Fun.from(listOf(null), null))
    }
}
