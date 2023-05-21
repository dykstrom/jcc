package se.dykstrom.jcc.tiny.compiler

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier

abstract class AbstractTinyTests {
    companion object {

        const val NAME_A = "a"
        const val NAME_B = "b"
        const val NAME_C = "c"
        const val NAME_N = "n"
        const val NAME_UNDEFINED = "undefined"

        val IDENT_A = Identifier(NAME_A, I64.INSTANCE)
        val IDENT_B = Identifier(NAME_B, I64.INSTANCE)
        val IDENT_C = Identifier(NAME_C, I64.INSTANCE)
        val IDENT_N = Identifier(NAME_N, I64.INSTANCE)

        val NE_A = IdentifierNameExpression(0, 0, IDENT_A)
        val NE_B = IdentifierNameExpression(0, 0, IDENT_B)
        val NE_C = IdentifierNameExpression(0, 0, IDENT_C)

        val IDE_A = IdentifierDerefExpression(0, 0, IDENT_A)
        val IDE_B = IdentifierDerefExpression(0, 0, IDENT_B)
        val IDE_C = IdentifierDerefExpression(0, 0, IDENT_C)
        val IDE_N = IdentifierDerefExpression(0, 0, IDENT_N)

        val IL_M3 = IntegerLiteral(0, 0, "-3")
        val IL_0 = IntegerLiteral(0, 0, "0")
        val IL_1 = IntegerLiteral(0, 0, "1")
        val IL_2 = IntegerLiteral(0, 0, "2")
        val IL_5 = IntegerLiteral(0, 0, "5")
        val IL_23 = IntegerLiteral(0, 0, "23")
        val IL_17 = IntegerLiteral(0, 0, "17")

        val SEMANTICS_ERROR_LISTENER =
            { line: Int, column: Int, msg: String, exception: SemanticsException -> throw IllegalStateException("Semantics error at $line:$column: $msg", exception) }

        val SYNTAX_ERROR_LISTENER = object : BaseErrorListener() {
            override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
                throw IllegalStateException("Syntax error at $line:$charPositionInLine: $msg", e)
            }
        }
    }
}
