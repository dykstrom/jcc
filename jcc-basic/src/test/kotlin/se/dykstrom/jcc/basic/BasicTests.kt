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

package se.dykstrom.jcc.basic

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.basic.compiler.BasicErrorStrategy
import se.dykstrom.jcc.basic.compiler.BasicLexer
import se.dykstrom.jcc.basic.compiler.BasicParser
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.error.CompilationError
import se.dykstrom.jcc.common.types.*
import se.dykstrom.jcc.llvm.code.LlvmBuiltIns.LF_ROUNDEVEN_F64

class BasicTests {

    companion object {

        /**
         * Parses the given program text the way BasicSyntaxParser does, reporting every syntax
         * error to the given error listener, and returns the parse tree. Pass [ERROR_LISTENER]
         * to have the first error throw, or a listener of your own to collect them all.
         */
        fun parseProgram(text: String, errorListener: BaseErrorListener): BasicParser.ProgramContext {
            val lexer = BasicLexer(CharStreams.fromString(text))
            lexer.removeErrorListeners()
            lexer.addErrorListener(errorListener)

            val parser = BasicParser(CommonTokenStream(lexer))
            parser.removeErrorListeners()
            parser.addErrorListener(errorListener)
            parser.errorHandler = BasicErrorStrategy()

            val ctx = parser.program()
            Antlr4Utils.checkParsingComplete(parser)
            return ctx
        }

        /**
         * Asserts that the given errors were reported on exactly the given lines, in that order.
         */
        fun assertLines(errors: List<CompilationError>, vararg lines: Int) {
            assertEquals(
                lines.toList(), errors.map { it.line() },
                "\nExpected errors on lines ${lines.toList()}\nActual: ${errors.map { "${it.line()}:${it.column()} ${it.msg()}" }}"
            )
        }

        fun assertMessageContains(errors: List<CompilationError>, expected: String) {
            assertTrue(
                errors.any { it.msg().contains(expected) },
                "\nExpected some message to contain: '$expected'\nActual: ${errors.map { it.msg() }}"
            )
        }

        fun assertNoMessageContains(errors: List<CompilationError>, unexpected: String) {
            assertTrue(
                errors.none { it.msg().contains(unexpected) },
                "\nExpected no message to contain: '$unexpected'\nActual: ${errors.map { it.msg() }}"
            )
        }

        /**
         * Wraps an integer-typed expression in the int→float cast that semantic analysis inserts at an
         * implicit-conversion site. Code generation no longer re-derives conversions (issue #52), so
         * codegen tests must supply the explicit cast nodes, just as the semantics parser does.
         */
        fun castToFloat(expression: Expression): Expression =
            CastToFloatExpression(0, 0, expression, F64.INSTANCE)

        /**
         * Wraps a float-typed expression in the float→integer cast that semantic analysis inserts at an
         * implicit-conversion site: a truncating [CastToIntExpression] over a [RoundExpression] that
         * rounds half-to-even (QuickBASIC 4.5), mirroring BasicSemanticsParser.makeCastExplicit.
         */
        fun castToInt(expression: Expression): Expression =
            CastToIntExpression(0, 0, RoundExpression(expression, LF_ROUNDEVEN_F64), I64.INSTANCE)

        // Literals
        val FL_0_3 = FloatLiteral(0, 0, "0.3")
        val FL_1_0 = FloatLiteral(0, 0, "1.0")
        val FL_0_5 = FloatLiteral(0, 0, "0.5")
        val FL_1_2 = FloatLiteral(0, 0, "1.2")
        val FL_2_0 = FloatLiteral(0, 0, "2.0")
        val FL_3_14 = FloatLiteral(0, 0, "3.14")
        val FL_7_5_E10 = FloatLiteral(0, 0, "7.5e+10")
        val FL_M_3_14 = FloatLiteral(0, 0, "-3.14")

        val IL_0 = IntegerLiteral(0, 0, "0")
        val IL_1 = IntegerLiteral(0, 0, "1")
        val IL_2 = IntegerLiteral(0, 0, "2")
        val IL_3 = IntegerLiteral(0, 0, "3")
        val IL_4 = IntegerLiteral(0, 0, "4")
        val IL_5 = IntegerLiteral(0, 0, "5")
        val IL_10 = IntegerLiteral(0, 0, "10")
        val IL_11 = IntegerLiteral(0, 0, "11")
        val IL_255 = IntegerLiteral(0, 0, "255")
        val IL_M1 = IntegerLiteral(0, 0, "-1")
        val IL_M3 = IntegerLiteral(0, 0, "-3")

        val SL_A = StringLiteral(0, 0, "A")
        val SL_B = StringLiteral(0, 0, "B")
        val SL_C = StringLiteral(0, 0, "C")
        val SL_FOO = StringLiteral(0, 0, "foo")
        val SL_BAR = StringLiteral(0, 0, "bar")

        // Named types, as an AS clause spells them before semantic analysis resolves them
        val NT_DOUBLE = NamedType("DOUBLE")
        val NT_INTEGER = NamedType("INTEGER")
        val NT_STRING = NamedType("STRING")

        // Identifiers
        val IDENT_F64_F = Identifier("f#", F64.INSTANCE)
        val IDENT_F64_G = Identifier("g#", F64.INSTANCE)
        val IDENT_F64_X = Identifier("x", F64.INSTANCE)
        val IDENT_I64_A = Identifier("a%", I64.INSTANCE)
        val IDENT_I64_B = Identifier("b%", I64.INSTANCE)
        val IDENT_I64_H = Identifier("h%", I64.INSTANCE)
        val IDENT_STR_S = Identifier("s$", Str.INSTANCE)
        val IDENT_STR_X = Identifier("x", Str.INSTANCE)

        // Identifier references
        val INE_F64_F = IdentifierNameExpression(0, 0, IDENT_F64_F)
        val INE_F64_G = IdentifierNameExpression(0, 0, IDENT_F64_G)
        val INE_I64_A = IdentifierNameExpression(0, 0, IDENT_I64_A)
        val INE_I64_B = IdentifierNameExpression(0, 0, IDENT_I64_B)
        val INE_STR_S = IdentifierNameExpression(0, 0, IDENT_STR_S)
        val INE_STR_X = IdentifierNameExpression(0, 0, IDENT_STR_X)

        val IDE_I64_A = IdentifierDerefExpression(0, 0, IDENT_I64_A)
        val IDE_I64_B = IdentifierDerefExpression(0, 0, IDENT_I64_B)
        val IDE_I64_H = IdentifierDerefExpression(0, 0, IDENT_I64_H)
        val IDE_F64_F = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        val IDE_F64_X = IdentifierDerefExpression(0, 0, IDENT_F64_X)
        val IDE_STR_S = IdentifierDerefExpression(0, 0, IDENT_STR_S)
        val IDE_STR_X = IdentifierDerefExpression(0, 0, IDENT_STR_X)

        // Function types
        val FUN_F64_TO_F64: Fun = Fun.from(listOf(F64.INSTANCE), F64.INSTANCE)
        val FUN_I64_F64_TO_F64: Fun = Fun.from(listOf(I64.INSTANCE, F64.INSTANCE), F64.INSTANCE)
        val FUN_STR_TO_STR: Fun = Fun.from(listOf(Str.INSTANCE), Str.INSTANCE)
        val FUN_TO_F64: Fun = Fun.from(listOf(), F64.INSTANCE)
        val FUN_TO_I64: Fun = Fun.from(listOf(), I64.INSTANCE)
        val FUN_TO_STR: Fun = Fun.from(listOf(), Str.INSTANCE)

        // Function identifiers
        val IDENT_FUN_BAR_I64 = Identifier("bar%", FUN_TO_I64) // bar%(...) : I64
        val IDENT_FUN_FOO_F64 = Identifier("foo", FUN_TO_F64) // foo(...) : F64
        val IDENT_FUN_FNFOO_F64 = Identifier("FNfoo", FUN_F64_TO_F64) // FNfoo(F64) : F64
        val IDENT_FUN_COMMAND_STR = Identifier("command$", FUN_TO_STR) // command$(...) : Str

        val ERROR_LISTENER = object : BaseErrorListener() {
            override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
                throw IllegalStateException("Syntax error at $line:$charPositionInLine: $msg", e)
            }
        }
    }
}
