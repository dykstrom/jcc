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

package se.dykstrom.jcc.main

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.utils.FileUtils
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class JccTests {

    @Test
    fun shouldPrintVersion() {
        // Given
        val args = arrayOf("--version")

        // When
        val output = tapSystemOut {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertTrue(output.startsWith("jcc"))
    }

    @Test
    fun shouldPrintHelp() {
        // Given
        val args = arrayOf("--help")

        // When
        val output = tapSystemOut {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.startsWith("Usage: jcc"))
    }

    @Test
    fun shouldPrintHelpIfNoArgs() {
        // Given
        val args: Array<String> = arrayOf()

        // When
        val output = tapSystemOut {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.startsWith("Usage: jcc"))
    }

    @Test
    fun shouldReportUnknownOption() {
        // Given
        val args = arrayOf("--backend", "LLVM")

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("--backend"), output)
    }

    @Test
    fun shouldReportNoFileType() {
        // Given
        val path = Files.createTempFile("ut_", "")
        val args = arrayOf(path.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("Cannot determine file type"))
    }

    @Test
    fun shouldReportInvalidFileType() {
        // Given
        val path = Files.createTempFile("ut_", ".invalid")
        val args = arrayOf(path.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("Invalid file type"))
    }

    @Test
    fun shouldReportFileNotFound() {
        // Given
        val args = arrayOf("does_not_exist.tiny")

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.startsWith("jcc: error: does_not_exist.tiny: No such file or directory"))
    }

    @Test
    fun shouldReportUndefinedFunctionError() {
        // Given
        // With only numeric arguments this would be an implicitly defined array instead
        val sourcePath = createSourceFile("PRINT foo(\"17\")")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("error: undefined function: foo"))
    }

    @Test
    fun shouldReportUndefinedArrayWarning() {
        // Given
        val sourcePath = createSourceFile("a(3) = 7")
        val args = arrayOf("-fsyntax-only", "-Wundefined-variable", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("warning: undefined array: a"))
    }

    @Test
    fun shouldReportUndefinedVariableWarning() {
        // Given
        val sourcePath = createSourceFile("PRINT foo")
        val args = arrayOf("-fsyntax-only", "-Wundefined-variable", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("warning: undefined variable: foo"))
    }

    @Test
    fun shouldNotReportUndefinedVariableWarning() {
        // Given
        val sourcePath = createSourceFile("PRINT foo")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertFalse(output.contains("warning"))
    }

    @Test
    fun shouldReportFloatConversionWarning() {
        // Given
        val sourcePath = createSourceFile("PRINT hex$(27.5)")
        val args = arrayOf("-fsyntax-only", "-Wfloat-conversion", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("warning: implicit conversion turns floating-point number into integer"))
    }

    @Test
    fun shouldReportUnusedVariableWarning() {
        // Given
        val sourcePath = createSourceFile("DIM foo AS INTEGER")
        val args = arrayOf("-fsyntax-only", "-Wunused-variable", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("warning: unused variable: foo"))
    }

    @Test
    fun shouldNotReportUnusedVariableWarning() {
        // Given
        val sourcePath = createSourceFile("DIM foo AS INTEGER")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertFalse(output.contains("warning"))
    }

    @Test
    fun shouldPrintEachSyntaxErrorOnce() {
        // Given: two mistakes, on line 1 and line 3
        val sourcePath = createSourceFile("FORi = 1 TO10\nPRINT i\nNEXTi")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then: nothing in ANTLR's own console format, and one error line per mistake
        val lines = output.lines().filter { it.isNotBlank() }
        assertTrue(lines.none { it.matches(Regex("^line \\d+:\\d+ .*")) }, "ANTLR console output: $output")
        assertEquals(2, lines.count { it.contains(" error: ") }, "Expected exactly two errors in: $output")
    }

    @Test
    fun shouldReportUnsupportedStatements() {
        // Given: three QuickBASIC statements JCC does not have, the first of them a whole block
        val sourcePath = createSourceFile("FOR i = 1 TO 10\nPRINT i\nNEXT i\nINPUT n%\nLOCATE 1, 1")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then: the parse succeeds, so all three are reported in one compile, one message each
        assertTrue(output.contains("error: 'FOR ... NEXT' is not supported by JCC; use 'WHILE ... WEND'"), output)
        assertTrue(output.contains("error: 'INPUT' is not supported by JCC; use 'LINE INPUT'"), output)
        assertTrue(output.contains("error: 'LOCATE' and 'COLOR' are not supported by JCC"), output)
        assertEquals(3, output.lines().count { it.contains(" error: ") }, output)
    }

    @Test
    fun shouldReportUnsupportedTypeSuffixes() {
        // Given: QuickBASIC's suffixes for the two types JCC does not have. '!' did not lex at
        // all before, which stopped the compile before anything else was reported.
        val sourcePath = createSourceFile("a! = 1.5\nPRINT a!\nb& = 5\nPRINT b&")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then: every occurrence is named, with the caret on the suffix
        assertTrue(output.contains("error: type suffix '!' (single precision) is not supported by JCC; use '#' for double precision"), output)
        assertTrue(output.contains("error: type suffix '&' (long) is not supported by JCC; use '%' for integer"), output)
        assertFalse(output.contains("token recognition error"), output)
        assertTrue(output.contains("    1 | a! = 1.5"), output)
        assertTrue(output.contains("      |  ^"), output)
        assertEquals(4, output.lines().count { it.contains(" error: ") }, output)
    }

    @Test
    fun shouldReportOneMessageForOneTypeError() {
        // Given: the repro from issue #86, item 9. It used to give three messages: "illegal
        // expression" twice, and an assignment error about the double the type check fell back
        // to. Item 10 replaced the wording of the first.
        val sourcePath = createSourceFile("DIM b AS STRING\nb = 1 - \"x\"")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("error: cannot subtract integer and string"), output)
        assertFalse(output.contains("you cannot assign"), output)
        assertEquals(1, output.lines().count { it.contains(" error: ") }, output)
    }

    @Test
    fun shouldNameTheOperatorAndTheOperandTypes() {
        // Given: two operands their operator does not accept. The message used to be "illegal
        // expression: "a" % 2", naming neither the operator as written nor the types.
        val sourcePath = createSourceFile("PRINT \"a\" MOD 2")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("error: cannot mod string and integer"), output)
        assertFalse(output.contains("illegal expression"), output)
        assertEquals(1, output.lines().count { it.contains(" error: ") }, output)
    }

    @Test
    fun shouldReportMissingThenOnceForABlockIf() {
        // Given: a block IF whose THEN is missing. The mistake is on the block's header line, so
        // the parser used to give up on the block and report the orphaned END IF as well.
        val sourcePath = createSourceFile("IF a% = 1\n    PRINT 1\nEND IF")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("error: 'THEN' is missing after the IF condition"), output)
        assertFalse(output.contains("without matching"), output)
        assertFalse(output.contains("no viable alternative"), output)
        assertEquals(1, output.lines().count { it.contains(" error: ") }, output)
    }

    @Test
    fun shouldReportMalformedLiterals() {
        // Given: a radix literal without digits and a string without its closing quote. The
        // string failed in the lexer before, which reported the raw text of the line and left
        // the parser to report the line after it as well.
        val sourcePath = createSourceFile("PRINT \"hello\nPRINT &H\nPRINT 1")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then: one message for each mistake, and none for the correct line between them
        assertTrue(output.contains("error: unterminated string literal; add the closing '\"'"), output)
        assertTrue(
            output.contains("error: malformed hexadecimal literal '&H'; expected at least one hexadecimal digit (0-9, A-F)"),
            output
        )
        assertFalse(output.contains("token recognition error"), output)
        assertTrue(output.contains("    2 | PRINT &H"), output)
        assertTrue(output.contains("      |       ^"), output)
        assertEquals(2, output.lines().count { it.contains(" error: ") }, output)
    }

    @Test
    fun shouldReportCStyleOperators() {
        // Given: the ==, !=, && and || any programmer arriving from another language writes first
        val sourcePath = createSourceFile(
            "IF a% == 1 THEN PRINT \"one\"\nIF a% != 2 THEN PRINT \"two\"\nIF a% > 3 && b% THEN PRINT \"three\"\n" +
                "IF a% > 4 || b% THEN PRINT \"four\""
        )
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then: the parse succeeds, so all four are reported in one compile, with the caret on the operator
        assertTrue(output.contains("error: BASIC uses '=' for equality, not '==': write 'a% = 1'"), output)
        assertTrue(output.contains("error: BASIC uses '<>' for inequality, not '!=': write 'a% <> 2'"), output)
        assertTrue(output.contains("error: BASIC uses 'AND', not '&&': write 'a% > 3 AND b%'"), output)
        assertTrue(output.contains("error: BASIC uses 'OR', not '||': write 'a% > 4 OR b%'"), output)
        assertTrue(output.contains("    1 | IF a% == 1 THEN PRINT \"one\""), output)
        assertTrue(output.contains("      |       ^"), output)
        assertEquals(4, output.lines().count { it.contains(" error: ") }, output)
    }

    @Test
    fun shouldReportGrammarRulesWithoutPredicateText() {
        // Given: the two rules Basic.g4 used to state as semantic predicates, whose failure
        // printed the predicate's own source code at the user
        val sourcePath = createSourceFile("DEFINT ab\nDEF foo(x) = x + 1\nDEFSTR n-a")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then: each rule is stated in one sentence, and all three are reported in one compile
        assertTrue(output.contains("error: 'ab' is not a single letter; DEFINT takes single letters and letter ranges: write 'DEFINT a-n'"), output)
        assertTrue(output.contains("error: user-defined function names must start with 'FN': write 'DEF FNfoo'"), output)
        assertTrue(output.contains("error: 'n-a' is a reversed letter range; DEFSTR takes ranges in alphabetical order: write 'DEFSTR a-n'"), output)
        assertFalse(output.contains("failed predicate"), output)
        assertEquals(3, output.lines().count { it.contains(" error: ") }, output)
    }

    @Test
    fun shouldNameUnexpectedToken() {
        // Given: a Tiny program with a stray token after END, which stops the parser before EOF.
        // Tiny, COL and Assembunny reach the catch-all this way; the BASIC grammar matches EOF
        // itself, so ANTLR reports those errors before the catch-all is reached.
        val sourcePath = createSourceFile("BEGIN\n  WRITE 1\nEND\nJUNK", "tiny")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("error: unexpected 'JUNK'"), output)
        assertFalse(output.contains("EOF"), output)
    }

    @Test
    fun shouldQuoteSourceLineForError() {
        // Given
        val sourcePath = createSourceFile("DIM a AS DOBLE")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(1, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("    1 | DIM a AS DOBLE"), output)
        assertTrue(output.contains("      |          ^"), output)
    }

    @Test
    fun shouldQuoteSourceLineForWarning() {
        // Given
        val sourcePath = createSourceFile("DIM foo AS INTEGER")
        val args = arrayOf("-fsyntax-only", "-Wunused-variable", sourcePath.toString())

        // When
        val output = tapSystemErr {
            assertEquals(0, Jcc(args).run())
        }

        // Then
        assertTrue(output.contains("warning: unused variable: foo"), output)
        assertTrue(output.contains("    1 | DIM foo AS INTEGER"), output)
        assertTrue(output.contains("      |     ^"), output)
    }

    @Test
    fun shouldCheckSyntaxOnlyAndGenerateNoCode() {
        // Given
        val sourcePath = createSourceFile("PRINT")
        val args = arrayOf("-fsyntax-only", sourcePath.toString())

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(0, returnCode)
        listOf("ll", "s", "exe").forEach {
            val outputPath = FileUtils.withExtension(sourcePath, it)
            assertFalse(Files.exists(outputPath), "Unexpected output file: $outputPath")
        }
    }


    /**
     * Creates a temporary source file. All tests in this class use -fsyntax-only,
     * so no output files are created, and none have to be cleaned up.
     */
    private fun createSourceFile(text: String, sourceExt: String = "bas"): Path {
        val sourcePath = Files.createTempFile("ut_", ".$sourceExt")
        sourcePath.toFile().deleteOnExit()
        Files.write(sourcePath, listOf(text), UTF_8)
        return sourcePath
    }
}
