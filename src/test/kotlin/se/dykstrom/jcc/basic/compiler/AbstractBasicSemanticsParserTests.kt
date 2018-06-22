/*
 * Copyright (C) 2017 Johan Dykstrom
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

import org.antlr.v4.runtime.*
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FloatLiteral
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.Program
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.*
import se.dykstrom.jcc.common.utils.ParseUtils
import java.util.Collections.emptyList

abstract class AbstractBasicSemanticsParserTests {

    private val semanticsParser = BasicSemanticsParser()

    /**
     * Defines a function in the current scope.
     */
    fun defineFunction(function: Function) {
        semanticsParser.symbols.addFunction(function)
    }

    fun parseAndExpectException(text: String, message: String) {
        try {
            parse(text)
            fail("\nExpected: '$message'\nActual:   ''")
        } catch (e: Exception) {
            assertTrue("\nExpected: '" + message + "'\nActual:   '" + e.message + "'", e.message?.contains(message) ?: false)
        }
    }

    fun parse(text: String): Program {
        val lexer = BasicLexer(CharStreams.fromString(text))
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER)

        val syntaxParser = BasicParser(CommonTokenStream(lexer))
        syntaxParser.addErrorListener(SYNTAX_ERROR_LISTENER)

        val ctx = syntaxParser.program()
        ParseUtils.checkParsingComplete(syntaxParser)

        val visitor = BasicSyntaxVisitor()
        val program = visitor.visitProgram(ctx) as Program

        semanticsParser.addErrorListener(SEMANTICS_ERROR_LISTENER)
        return semanticsParser.program(program)
    }

    companion object {
        val FL_3_14: Expression = FloatLiteral(0, 0, "3.14")
        val FL_2_0: Expression = FloatLiteral(0, 0, "2.0")
        val IL_1: Expression = IntegerLiteral(0, 0, "1")

        val IDENT_BOOL_B = Identifier("b", Bool.INSTANCE)
        val IDENT_I64_A = Identifier("a", I64.INSTANCE)
        val IDENT_F64_F = Identifier("f", F64.INSTANCE)

        val FUN_COMMAND: Function = LibraryFunction("command$", emptyList(), Str.INSTANCE, "", "")
        val FUN_SUM1: Function = LibraryFunction("sum", listOf(I64.INSTANCE), I64.INSTANCE, "", "")
        val FUN_SUM2: Function = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", "")
        val FUN_SUM3: Function = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", "")

        private val SEMANTICS_ERROR_LISTENER =
                { line: Int, column: Int, msg: String, exception: SemanticsException -> throw IllegalStateException("Semantics error at $line:$column: $msg", exception) }

        private val SYNTAX_ERROR_LISTENER = object : BaseErrorListener() {
            override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException) {
                throw IllegalStateException("Syntax error at $line:$charPositionInLine: $msg", e)
            }
        }
    }
}
