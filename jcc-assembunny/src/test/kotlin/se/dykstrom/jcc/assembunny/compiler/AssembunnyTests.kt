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

package se.dykstrom.jcc.assembunny.compiler

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import java.nio.file.Path

class AssembunnyTests {

    companion object {

        val IL_1 = IntegerLiteral(0, 0, "1")

        val INE_A = IdentifierNameExpression(0, 0, Identifier("A", I64.INSTANCE))
        val INE_B = IdentifierNameExpression(0, 0, Identifier("B", I64.INSTANCE))
        val INE_C = IdentifierNameExpression(0, 0, Identifier("C", I64.INSTANCE))
        val INE_D = IdentifierNameExpression(0, 0, Identifier("D", I64.INSTANCE))

        val IDE_A: IdentifierDerefExpression = IdentifierDerefExpression.from(INE_A)
        val IDE_B: IdentifierDerefExpression = IdentifierDerefExpression.from(INE_B)

        val ERROR_LISTENER = object : BaseErrorListener() {
            override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
                throw IllegalStateException("Syntax error at $line:$charPositionInLine: $msg", e)
            }
        }

        val SOURCE_PATH: Path = Path.of("file.asmb")
    }
}
