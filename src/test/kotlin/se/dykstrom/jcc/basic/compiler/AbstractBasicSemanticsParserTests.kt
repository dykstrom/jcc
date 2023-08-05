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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.*
import se.dykstrom.jcc.common.utils.ParseUtils
import java.util.Collections.emptyList

abstract class AbstractBasicSemanticsParserTests {

    val typeManager = BasicTypeManager()

    val symbolTable: SymbolTable = SymbolTable()

    val errorListener = CompilationErrorListener()

    val semanticsParser = BasicSemanticsParser(typeManager, symbolTable, errorListener)

    /**
     * Defines a function in the current scope.
     */
    fun defineFunction(function: Function) {
        symbolTable.addFunction(function)
    }

    fun parseAndExpectException(text: String, message: String) {
        try {
            parse(text)
            fail("\nExpected: '$message'\nActual:   ''")
        } catch (e: SemanticsException) {
            assertTrue(errorListener.hasErrors())
            val foundMessage = errorListener.errors
                .map { it.exception.message!! }
                .any { it.contains(message) }
            assertTrue("\nExpected: '" + message + "'\nActual:   '" + e.message + "'", foundMessage)
        }
    }

    fun parse(text: String): Program {
        val lexer = BasicLexer(CharStreams.fromString(text))
        lexer.addErrorListener(errorListener)

        val syntaxParser = BasicParser(CommonTokenStream(lexer))
        syntaxParser.addErrorListener(errorListener)

        val ctx = syntaxParser.program()
        ParseUtils.checkParsingComplete(syntaxParser)

        val visitor = BasicSyntaxVisitor(typeManager)
        val program = visitor.visitProgram(ctx) as Program

        return semanticsParser.parse(program)
    }

    companion object {
        val FL_2_0 = FloatLiteral(0, 0, "2.0")
        val FL_3_14 = FloatLiteral(0, 0, "3.14")
        val IL_0 = IntegerLiteral(0, 0, "0")
        val IL_1 = IntegerLiteral(0, 0, "1")
        val IL_2 = IntegerLiteral(0, 0, "2")

        private val TYPE_ARR_I64: Arr = Arr.from(1, I64.INSTANCE)

        private val IDENT_I64_A = Identifier("a%", I64.INSTANCE)
        private val IDENT_F64_F = Identifier("f", F64.INSTANCE)
        private val IDENT_F64_X = Identifier("x", F64.INSTANCE)
        private val IDENT_STR_X = Identifier("x", Str.INSTANCE)
        val IDENT_ARR_I64_X = Identifier("x", TYPE_ARR_I64)

        val DECL_ARR_I64_X = ArrayDeclaration(0, 0, IDENT_ARR_I64_X.name, TYPE_ARR_I64, listOf(IL_1))
        val DECL_STR_X = Declaration(0, 0, IDENT_STR_X.name, Str.INSTANCE)

        val INE_I64_A = IdentifierNameExpression(0, 0, IDENT_I64_A)
        val INE_F64_F = IdentifierNameExpression(0, 0, IDENT_F64_F)
        val INE_ARR_I64_X = IdentifierNameExpression(0, 0, IDENT_ARR_I64_X)
        val IDE_F64_X = IdentifierDerefExpression(0, 0, IDENT_F64_X)

        val FUN_COMMAND = LibraryFunction("command$", emptyList(), Str.INSTANCE, "", ExternalFunction(""))
        val FUN_SUM1 = LibraryFunction("sum", listOf(I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        val FUN_SUM2 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        val FUN_SUM3 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
    }
}
