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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.type.ColTypeManager
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.error.Warning
import se.dykstrom.jcc.common.functions.Function
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractColSemanticsParserTests {

    val errorListener = CompilationErrorListener()
    val symbolTable = ColSymbols()
    val typeManager = ColTypeManager()
    val syntaxParser = ColSyntaxParser(errorListener)
    val semanticsParser = ColSemanticsParser(errorListener, symbolTable, typeManager)

    fun funCall(function: Function, vararg expressions: Expression) =
        FunCallStatement(FunctionCallExpression(function.identifier, expressions.toList()))

    fun parse(text: String): AstProgram {
        val parsedProgram = syntaxParser.parse(ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8)))
        assertFalse { errorListener.hasErrors() }
        val checkedProgram = semanticsParser.parse(parsedProgram)
        assertFalse { errorListener.hasErrors() }
        return checkedProgram
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

    fun parseAndExpectError(text: String, errorText: String) {
        parseAndExpectErrors(text, errorText)
    }

    /**
     * Parses [text], asserting that semantic analysis fails and that every string in [errorTexts]
     * appears in some reported error — used to pin multi-error reporting in a single compile.
     */
    fun parseAndExpectErrors(text: String, vararg errorTexts: String) {
        assertThrows<SemanticsException> {
            semanticsParser.parse(syntaxParser.parse(ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))))
        }
        assertTrue { errorListener.hasErrors() }
        for (errorText in errorTexts) {
            assertTrue(
                errorListener.errors.any { it.msg.contains(errorText) },
                "\nMissing: '$errorText'.\nFound:\n" + errorListener.errors.joinToString(
                    prefix = "'",
                    postfix = "'"
                ) { it.msg } + "\n"
            )
        }
    }
}
