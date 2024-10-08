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

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Assertions.*
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.optimization.BasicAstExpressionOptimizer
import se.dykstrom.jcc.common.ast.ArrayDeclaration
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.error.Warning
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str
import java.util.Collections.emptyList

abstract class AbstractBasicSemanticsParserTests {

    val errorListener = CompilationErrorListener()
    val baseErrorListener = Antlr4Utils.asBaseErrorListener(errorListener)!!
    val symbolTable = SymbolTable()
    val typeManager = BasicTypeManager()
    val optimizer = BasicAstExpressionOptimizer(typeManager)

    val semanticsParser = BasicSemanticsParser(errorListener, symbolTable, typeManager, optimizer)

    /**
     * Defines a function in the current scope.
     */
    fun defineFunction(function: Function) {
        symbolTable.addFunction(function)
    }

    fun parseAndExpectException(text: String, expectedMessage: String) {
        try {
            parse(text)
            fail("\nExpected: '$expectedMessage'\nActual:   ''")
        } catch (e: SemanticsException) {
            assertTrue(errorListener.hasErrors())
            val foundMessage = errorListener.errors
                .map { it.exception.message!! }
                .any { it.contains(expectedMessage) }
            assertTrue(foundMessage, "\nExpected: '" + expectedMessage + "'\nActual:   '" + errorListener.errors + "'")
        }
    }

    fun parseAndExpectWarning(text: String, expectedMessage: String, expectedWarning: Warning) {
        parse(text)
        assertFalse(errorListener.warnings.isEmpty())
        val foundMessage = errorListener.warnings
            .filter { it.warning == expectedWarning }
            .map { it.msg }
            .any { it.contains(expectedMessage) }
        assertTrue(foundMessage, "\nExpected: '" + expectedMessage + "'\nActual:   '" + errorListener.warnings + "'")
    }

    fun parse(text: String): AstProgram {
        val lexer = BasicLexer(CharStreams.fromString(text))
        lexer.addErrorListener(baseErrorListener)

        val syntaxParser = BasicParser(CommonTokenStream(lexer))
        syntaxParser.addErrorListener(baseErrorListener)
        val ctx = syntaxParser.program()
        Antlr4Utils.checkParsingComplete(syntaxParser)

        val visitor = BasicSyntaxVisitor(typeManager)
        val program = visitor.visitProgram(ctx) as AstProgram

        return semanticsParser.parse(program)
    }

    companion object {
        // Array types
        private val TYPE_ARR_I64: Arr = Arr.from(1, I64.INSTANCE)

        val IDENT_ARR_I64_X = Identifier("x", TYPE_ARR_I64)

        val INE_ARR_I64_X = IdentifierNameExpression(0, 0, IDENT_ARR_I64_X)

        val DECL_ARR_I64_X = ArrayDeclaration(0, 0, "x", TYPE_ARR_I64, listOf(IL_1))
        val DECL_STR_X = Declaration(0, 0, "x", Str.INSTANCE)

        val FUN_COMMAND = LibraryFunction("command$", emptyList(), Str.INSTANCE, "", ExternalFunction(""))
        val FUN_SUM1 = LibraryFunction("sum", listOf(I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        val FUN_SUM2 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        val FUN_SUM3 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
    }
}
