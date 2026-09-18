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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.assertLines
import se.dykstrom.jcc.basic.BasicTests.Companion.assertMessageContains
import se.dykstrom.jcc.basic.BasicTests.Companion.assertNoMessageContains
import se.dykstrom.jcc.basic.ast.statement.DefIntStatement
import se.dykstrom.jcc.basic.ast.statement.PrintStatement
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.AndExpression
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.EqualExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.NotEqualExpression
import se.dykstrom.jcc.common.ast.OrExpression
import se.dykstrom.jcc.common.error.CompilationError
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier

/**
 * Tests the mistakes `Basic.g4` accepts only so that [BasicSyntaxVisitor] can name them. The
 * grammar has to parse them: rejecting a mistake on a block header line costs the whole block,
 * because the parser gives up on the rule and orphans every terminator inside it.
 *
 * @author Johan Dykstrom
 */
class BasicSyntaxVisitorErrorTests : AbstractBasicSyntaxVisitorTests() {

    // ELSE IF written as two words:

    @Test
    fun shouldReportElseIfWrittenAsTwoWords() {
        val errors = parseCollectingErrors(
            """
                IF a THEN
                    PRINT 1
                ELSE IF b THEN
                    PRINT 2
                ELSEIF c THEN
                    PRINT 3
                ELSE
                    PRINT 4
                END IF
            """.trimIndent()
        )
        assertLines(errors, 3)
        assertMessageContains(errors, "'ELSE IF' is not 'ELSEIF'")
    }

    @Test
    fun shouldPointElseIfErrorAtTheElse() {
        // The ELSE is what has to change, so that is where the caret belongs
        val errors = parseCollectingErrors("IF a THEN\n    PRINT 1\n    ELSE IF b THEN\n    PRINT 2\nEND IF\n")
        assertLines(errors, 3)
        assertEquals(4, errors[0].column())
    }

    @Test
    fun shouldPointElseIfErrorAtTheElseAfterALineNumber() {
        val errors = parseCollectingErrors("10 IF a THEN\n20 PRINT 1\n30 ELSE IF b THEN\n40 PRINT 2\n50 END IF\n")
        assertLines(errors, 3)
        assertEquals(3, errors[0].column())
    }

    @Test
    fun shouldReportEveryElseIfWrittenAsTwoWords() {
        // visitIfThenBlock walks the ELSEIF blocks in reverse, so these reach the listener
        // bottom-up. CompilationMessage.compareTo is what puts them back in reading order, which
        // is the order they are printed in
        val errors = parseCollectingErrors("IF a THEN\nPRINT 1\nELSE IF b THEN\nPRINT 2\nELSE IF c THEN\nPRINT 3\nEND IF\n")
        assertLines(errors.sorted(), 3, 5)
    }

    @Test
    fun shouldNotReportElseIfWrittenAsOneWord() {
        // parse itself asserts that the visitor reported nothing
        parse("IF a THEN\nPRINT 1\nELSEIF b THEN\nPRINT 2\nEND IF\n")
    }

    @Test
    fun shouldNotReportSingleLineIfWithElseIf() {
        // Valid QuickBASIC: an ELSE holding a single-line IF, not the two-word ELSEIF mistake
        parse("IF a THEN PRINT 1 ELSE IF b THEN PRINT 2\n")
    }

    @Test
    fun shouldNotReportNestedIfOnItsOwnLineInsideElse() {
        parse("IF a THEN\nPRINT 1\nELSE\nIF b THEN\nPRINT 2\nEND IF\nEND IF\n")
    }

    // Unsupported QuickBASIC statements:

    @Test
    fun shouldReportForNextLoop() {
        val errors = parseCollectingErrors("FOR i = 1 TO 10\nPRINT i\nNEXT i\n")
        // NEXT closes the loop the message already named, so it adds nothing of its own
        assertLines(errors, 1)
        assertMessageContains(errors, "'FOR ... NEXT' is not supported by JCC; use 'WHILE ... WEND'")
    }

    @Test
    fun shouldReportNextWithoutFor() {
        val errors = parseCollectingErrors("NEXT i\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'FOR ... NEXT' is not supported by JCC")
    }

    @Test
    fun shouldReportDoLoop() {
        val errors = parseCollectingErrors("i% = 0\nDO\nPRINT i%\nLOOP WHILE i% < 3\n")
        assertLines(errors, 2)
        assertMessageContains(errors, "'DO ... LOOP' is not supported by JCC; use 'WHILE ... WEND'")
    }

    @Test
    fun shouldReportSelectCase() {
        // Every CASE belongs to the block SELECT opened, and so does END SELECT
        val errors = parseCollectingErrors("SELECT CASE a%\nCASE 1\nPRINT 1\nCASE ELSE\nPRINT 2\nEND SELECT\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'SELECT CASE' is not supported by JCC; use 'IF ... ELSEIF ... END IF'")
    }

    @Test
    fun shouldReportSub() {
        val errors = parseCollectingErrors("SUB greet\nPRINT \"hi\"\nEND SUB\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'SUB' is not supported by JCC; use 'GOSUB ... RETURN'")
    }

    @Test
    fun shouldReportFunction() {
        val errors = parseCollectingErrors("FUNCTION twice(x)\ntwice = 2 * x\nEND FUNCTION\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'FUNCTION' is not supported by JCC; use 'DEF FN' to define a function")
    }

    @Test
    fun shouldReportType() {
        // The member declarations a TYPE block holds are not statements, and are not parsed;
        // the block header is what carries the message
        val errors = parseCollectingErrors("TYPE person\nEND TYPE\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'TYPE' is not supported by JCC")
    }

    @Test
    fun shouldReportExit() {
        val errors = parseCollectingErrors("EXIT FOR\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'EXIT' is not supported by JCC; use 'GOTO' to leave a loop")
    }

    @Test
    fun shouldReportPlainInput() {
        val errors = parseCollectingErrors("INPUT n%\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'INPUT' is not supported by JCC; use 'LINE INPUT'")
    }

    @Test
    fun shouldReportPrintUsing() {
        val errors = parseCollectingErrors("PRINT USING \"###\"; 42\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'PRINT USING' is not supported by JCC")
    }

    @Test
    fun shouldReportFileStatements() {
        val errors = parseCollectingErrors("OPEN \"f.txt\" FOR INPUT AS #1\nCLOSE #1\n")
        assertLines(errors, 1, 2)
        assertMessageContains(errors, "'OPEN' and 'CLOSE' are not supported by JCC; file I/O is not available")
    }

    @Test
    fun shouldReportScreenStatements() {
        val errors = parseCollectingErrors("LOCATE 1, 1\nCOLOR 7\n")
        assertLines(errors, 1, 2)
        assertMessageContains(errors, "'LOCATE' and 'COLOR' are not supported by JCC")
    }

    @Test
    fun shouldReportRedim() {
        val errors = parseCollectingErrors("REDIM a(20) AS INTEGER\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'REDIM' and 'ERASE' are not supported by JCC; arrays are static, use 'DIM'")
    }

    @Test
    fun shouldReportData() {
        val errors = parseCollectingErrors("DATA 1, 2, 3\nREAD a%\n")
        assertLines(errors, 1, 2)
        assertMessageContains(errors, "'DATA' and 'READ' are not supported by JCC; assign the values in code")
    }

    @Test
    fun shouldReportUnsupportedStatementAfterColon() {
        // The tail of an unsupported statement stops at COLON, so what follows it is still parsed
        val errors = parseCollectingErrors("PRINT 1 : LOCATE 1, 1 : PRINT 2\n")
        assertLines(errors, 1)
        assertEquals(10, errors[0].column())
    }

    @Test
    fun shouldReportEveryUnsupportedStatementInOneCompile() {
        // The parse succeeds, so one unsupported statement does not hide the next
        val errors = parseCollectingErrors("INPUT n%\nSELECT CASE n%\nEND SELECT\nLOCATE 1, 1\n")
        assertLines(errors, 1, 2, 4)
    }

    @Test
    fun shouldReportUnsupportedStatementInsideBlock() {
        val errors = parseCollectingErrors("IF a THEN\nFOR i = 1 TO 10\nNEXT i\nEND IF\n")
        assertLines(errors, 2)
    }

    // Reserved words used as variable names:

    @Test
    fun shouldReportReservedWordAsAssignmentTarget() {
        val errors = parseCollectingErrors("print = 5\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'print' is a reserved word and cannot be used as a variable name")
    }

    @Test
    fun shouldReportReservedWordInDim() {
        val errors = parseCollectingErrors("DIM goto AS INTEGER\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'goto' is a reserved word and cannot be used as a variable name")
    }

    @Test
    fun shouldPointReservedWordErrorAtTheWord() {
        val errors = parseCollectingErrors("DIM goto AS INTEGER\n")
        assertEquals(4, errors[0].column())
    }

    @Test
    fun shouldReportReservedWordAfterLet() {
        val errors = parseCollectingErrors("LET while = 5\n")
        assertMessageContains(errors, "'while' is a reserved word and cannot be used as a variable name")
    }

    @Test
    fun shouldReportEveryReservedWordInOneCompile() {
        // The parse succeeds, so one reserved word does not hide the next
        val errors = parseCollectingErrors("print = 1\nif = 2\nand = 3\n")
        assertLines(errors, 1, 2, 3)
    }

    @Test
    fun shouldReportOperatorKeywordAsVariableName() {
        val errors = parseCollectingErrors("mod = 5\n")
        assertMessageContains(errors, "'mod' is a reserved word and cannot be used as a variable name")
    }

    // The keywords above are soft keywords, so they are still identifiers everywhere else:

    @Test
    fun shouldParseSoftKeywordAsVariableName() {
        val assignStatement = AssignStatement(0, 0, IdentifierNameExpression(0, 0, Identifier("data", F64.INSTANCE)), IL_3)
        parseAndAssert("data = 3", assignStatement)
    }

    @Test
    fun shouldParseSoftKeywordAsLabel() {
        val errors = parseCollectingErrors("next: GOTO next\n")
        assertEquals(emptyList<Any>(), errors)
    }

    @Test
    fun shouldParseSoftKeywordInExpression() {
        val assignStatement = AssignStatement(0, 0, IdentifierNameExpression(0, 0, Identifier("step", F64.INSTANCE)),
            AddExpression(0, 0, IdentifierDerefExpression(0, 0, Identifier("loop", F64.INSTANCE)), IL_1))
        parseAndAssert("step = loop + 1", assignStatement)
    }

    // AS, BASE, INPUT and LINE are reserved, as in QuickBASIC, but only mean something in one place:

    @Test
    fun shouldReportContextualKeywordAsAssignmentTarget() {
        listOf("as", "base", "input", "line").forEach { word ->
            val errors = parseCollectingErrors("$word = 3\n")
            assertMessageContains(errors, "'$word' is a reserved word and cannot be used as a variable name")
        }
    }

    @Test
    fun shouldStillParseTheStatementsThoseKeywordsBelongTo() {
        assertEquals(emptyList<Any>(), parseCollectingErrors("LINE INPUT \"Name: \"; n$\n"))
        assertEquals(emptyList<Any>(), parseCollectingErrors("OPTION BASE 1\n"))
        assertEquals(emptyList<Any>(), parseCollectingErrors("DIM a AS INTEGER\n"))
    }

    // The C-style operators ==, !=, && and ||:

    @Test
    fun shouldReportEqEqAsEquality() {
        val errors = parseCollectingErrors("IF a% == 1 THEN PRINT \"yes\"\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "BASIC uses '=' for equality, not '==': write 'a% = 1'")
    }

    @Test
    fun shouldReportBangEqAsInequality() {
        val errors = parseCollectingErrors("IF a% != 1 THEN PRINT \"yes\"\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "BASIC uses '<>' for inequality, not '!=': write 'a% <> 1'")
    }

    @Test
    fun shouldPointCStyleOperatorErrorAtTheOperator() {
        // The operator is what has to change, so that is where the caret belongs
        val errors = parseCollectingErrors("IF a% == 1 THEN PRINT \"yes\"\n")
        assertEquals(6, errors[0].column())
    }

    @Test
    fun shouldSuggestRewriteInTheUsersOwnText() {
        val errors = parseCollectingErrors("IF foo(x) + 1 == bar THEN PRINT 1\n")
        assertMessageContains(errors, "write 'foo(x) + 1 = bar'")
    }

    @Test
    fun shouldNameBothReadingsOfGluedBangEq() {
        // QuickBASIC reads a!=b as the single-precision suffix followed by '=', a newcomer means
        // inequality, and JCC supports neither, so both readings are named
        val errors = parseCollectingErrors("IF a!=1 THEN PRINT \"yes\"\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'!=' is either inequality or the type suffix '!' followed by '='")
        assertMessageContains(errors, "write 'a <> 1' for inequality, or 'a! = 1' for the suffix")
    }

    @Test
    fun shouldNotNameTheSuffixWhenBangEqIsSpaced() {
        // A space rules the suffix out, since a suffix binds to its name
        val errors = parseCollectingErrors("IF a !=1 THEN PRINT \"yes\"\n")
        assertNoMessageContains(errors, "type suffix")
    }

    @Test
    fun shouldOmitRewriteWhenExpressionSpansLines() {
        val errors = parseCollectingErrors("IF a% _\n== 1 THEN PRINT \"yes\"\n")
        assertLines(errors, 2)
        assertEquals("BASIC uses '=' for equality, not '=='", errors[0].msg())
    }

    @Test
    fun shouldReportAmpAmpAsAnd() {
        val errors = parseCollectingErrors("IF a && b THEN PRINT 1\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "BASIC uses 'AND', not '&&': write 'a AND b'")
    }

    @Test
    fun shouldReportPipePipeAsOr() {
        val errors = parseCollectingErrors("IF a || b THEN PRINT 1\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "BASIC uses 'OR', not '||': write 'a OR b'")
    }

    @Test
    fun shouldGiveAmpAmpTheSamePrecedenceAsAnd() {
        // && binds tighter than ||, as AND does than OR, so the AST is the one C would build too
        val expression = OrExpression(0, 0,
            IdentifierDerefExpression(0, 0, Identifier("a", F64.INSTANCE)),
            AndExpression(0, 0,
                IdentifierDerefExpression(0, 0, Identifier("b", F64.INSTANCE)),
                IdentifierDerefExpression(0, 0, Identifier("c", F64.INSTANCE))))
        val program = parseIgnoringErrors("PRINT a || b && c")
        assertEquals(listOf(PrintStatement(0, 0, listOf(expression))), program.statements)
    }

    @Test
    fun shouldReportEveryCStyleOperatorInOneCompile() {
        // The parse succeeds, so one wrong operator does not hide the next
        val errors = parseCollectingErrors(
            "IF a == 1 THEN PRINT 1\nIF b != 2 THEN PRINT 2\nIF c && d THEN PRINT 3\nIF e || f THEN PRINT 4\n"
        )
        assertLines(errors, 1, 2, 3, 4)
    }

    @Test
    fun shouldReportBothOperatorsOfOneExpression() {
        val errors = parseCollectingErrors("IF a == 1 && b == 2 THEN PRINT 1\n")
        assertLines(errors, 1, 1, 1)
    }

    @Test
    fun shouldParseIntendedExpressionAfterReporting() {
        // The expression the programmer meant is returned, so analysis carries on
        val expression = EqualExpression(0, 0, IdentifierDerefExpression(0, 0, Identifier("a", F64.INSTANCE)), IL_1)
        val program = parseIgnoringErrors("PRINT a == 1")
        assertEquals(listOf(PrintStatement(0, 0, listOf(expression))), program.statements)
    }

    @Test
    fun shouldParseIntendedNotEqualExpressionAfterReporting() {
        val expression = NotEqualExpression(0, 0, IdentifierDerefExpression(0, 0, Identifier("a", F64.INSTANCE)), IL_1)
        val program = parseIgnoringErrors("PRINT a != 1")
        assertEquals(listOf(PrintStatement(0, 0, listOf(expression))), program.statements)
    }

    // DEFDBL, DEFINT and DEFSTR letter intervals:

    @Test
    fun shouldReportLetterIntervalOfMoreThanOneLetter() {
        val errors = parseCollectingErrors("defdbl abc\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'abc' is not a single letter; defdbl takes single letters and letter ranges: write 'defdbl a-n'")
    }

    @Test
    fun shouldReportEachEndOfALetterInterval() {
        val errors = parseCollectingErrors("DEFINT abc-de\n")
        assertLines(errors, 1, 1)
        assertMessageContains(errors, "'abc' is not a single letter")
        assertMessageContains(errors, "'de' is not a single letter")
    }

    @Test
    fun shouldReportTypeSuffixAsLetter() {
        val errors = parseCollectingErrors("DEFINT a%\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'a%' is not a single letter")
    }

    @Test
    fun shouldPointLetterErrorAtTheLetter() {
        val errors = parseCollectingErrors("DEFSTR a, bc\n")
        assertEquals(10, errors[0].column())
    }

    @Test
    fun shouldReportEveryBadLetterInOneCompile() {
        // A reported interval contributes no letters, so the statement carries on to the next
        val errors = parseCollectingErrors("DEFINT ab, cd\n")
        assertLines(errors, 1, 1)
    }

    @Test
    fun shouldDefineTheGoodLettersAfterReporting() {
        val program = parseIgnoringErrors("DEFINT ab, c\n")
        assertEquals(listOf(DefIntStatement(0, 0, setOf('c'))), program.statements)
    }

    @Test
    fun shouldReportReversedLetterRange() {
        // QuickBASIC requires the range to run in alphabetical order
        val errors = parseCollectingErrors("DEFINT n-a\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'n-a' is a reversed letter range; DEFINT takes ranges in alphabetical order: write 'DEFINT a-n'")
    }

    @Test
    fun shouldAcceptRangeOfOneLetter() {
        val program = parseIgnoringErrors("DEFINT a-a\n")
        assertEquals(listOf(DefIntStatement(0, 0, setOf('a'))), program.statements)
    }

    // Function names without the FN prefix:

    @Test
    fun shouldReportFunctionNameWithoutFnPrefix() {
        val errors = parseCollectingErrors("DEF foo(x) = x + 1\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "user-defined function names must start with 'FN': write 'DEF FNfoo'")
    }

    @Test
    fun shouldPointFunctionNameErrorAtTheName() {
        val errors = parseCollectingErrors("DEF foo(x) = x + 1\n")
        assertEquals(4, errors[0].column())
    }

    @Test
    fun shouldDefineFunctionUnderFnNameAfterReporting() {
        // The function is defined under the name the message asks for, so the body is analysed
        val ident = Identifier("FNfoo", Fun.from(listOf(), F64.INSTANCE))
        val program = parseIgnoringErrors("DEF foo() = 1\n")
        assertEquals(listOf(FunctionDefinitionStatement(0, 0, ident, listOf(), IL_1)), program.statements)
    }

    @Test
    fun shouldAcceptFnPrefixInAnyCase() {
        assertEquals(emptyList<CompilationError>(), parseCollectingErrors("DEF fnfoo() = 1\n"))
        assertEquals(emptyList<CompilationError>(), parseCollectingErrors("DEF Fnfoo() = 1\n"))
    }

    @Test
    fun shouldReportEveryFunctionNameInOneCompile() {
        val errors = parseCollectingErrors("DEF foo() = 1\nDEF bar() = 2\n")
        assertLines(errors, 1, 2)
    }

    // QuickBASIC type suffixes JCC does not have:

    @Test
    fun shouldReportBangSuffix() {
        val errors = parseCollectingErrors("a! = 1\n")
        assertLines(errors, 1)
        assertEquals("type suffix '!' (single precision) is not supported by JCC; use '#' for double precision", errors[0].msg())
    }

    @Test
    fun shouldReportAmpersandSuffix() {
        val errors = parseCollectingErrors("b& = 1\n")
        assertLines(errors, 1)
        assertEquals("type suffix '&' (long) is not supported by JCC; use '%' for integer", errors[0].msg())
    }

    @Test
    fun shouldPointSuffixErrorAtTheSuffix() {
        // The suffix is what has to change, so that is where the caret belongs
        val errors = parseCollectingErrors("PRINT a!\n")
        assertEquals(7, errors[0].column())
    }

    @Test
    fun shouldCarryOnWithDoubleAfterBangSuffix() {
        // The type the message asks for, so the rest of the program is analysed
        val ine = IdentifierNameExpression(0, 0, Identifier("a!", F64.INSTANCE))
        val program = parseIgnoringErrors("a! = 1")
        assertEquals(listOf(AssignStatement(0, 0, ine, IL_1)), program.statements)
    }

    @Test
    fun shouldCarryOnWithIntegerAfterAmpersandSuffix() {
        val ine = IdentifierNameExpression(0, 0, Identifier("b&", I64.INSTANCE))
        val program = parseIgnoringErrors("b& = 1")
        assertEquals(listOf(AssignStatement(0, 0, ine, IL_1)), program.statements)
    }

    @Test
    fun shouldReportEverySuffixInOneCompile() {
        // Every occurrence is a place the user has to edit, so every occurrence is named
        val errors = parseCollectingErrors("a! = 1\nPRINT a!\nb& = 2\nPRINT b&\n")
        assertLines(errors, 1, 2, 3, 4)
    }

    @Test
    fun shouldReportSuffixOnConstName() {
        val errors = parseCollectingErrors("CONST c! = 1\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "type suffix '!'")
    }

    @Test
    fun shouldReportSuffixOnLabel() {
        // Both the definition and the jump to it, since neither is written through visitIdent
        val errors = parseCollectingErrors("GOTO done!\ndone!:\nPRINT 1\n")
        assertLines(errors, 1, 2)
        assertMessageContains(errors, "type suffix '!'")
    }

    @Test
    fun shouldPreferInequalityOverSuffixWhenGlued() {
        // '!' is a token of its own, so '!=' still wins over it at the same position, and the
        // glued form keeps naming both readings rather than becoming a suffix
        val errors = parseCollectingErrors("IF a!=1 THEN PRINT 1\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'!=' is either inequality or the type suffix")
    }

    @Test
    fun shouldReportSuffixWhenBangEqIsWrittenAsTheMessageAsks() {
        // The rewrite the glued message suggests, which is a suffix and gets the suffix message
        val errors = parseCollectingErrors("IF a! = 1 THEN PRINT 1\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "type suffix '!'")
    }

    @Test
    fun shouldNotTakeAmpAmpOrRadixLiteralAsASuffix() {
        assertNoMessageContains(parseCollectingErrors("IF a && b THEN PRINT 1\n"), "type suffix")
        assertEquals(emptyList<CompilationError>(), parseCollectingErrors("PRINT a, &HFF\n"))
    }
}
