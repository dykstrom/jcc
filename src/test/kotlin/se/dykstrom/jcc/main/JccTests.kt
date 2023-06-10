package se.dykstrom.jcc.main

import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JccTests {

    @JvmField
    @Rule
    val systemOutRule: SystemOutRule = SystemOutRule().enableLog()

    @Test
    fun shouldPrintVersion() {
        // Given
        val args = arrayOf("--version")

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(0, returnCode)
        assertTrue(systemOutRule.log.startsWith("jcc"))
    }

    @Test
    fun shouldPrintHelp() {
        // Given
        val args = arrayOf("--help")

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(1, returnCode)
        assertTrue(systemOutRule.log.startsWith("Usage: jcc"))
    }

    @Test
    fun shouldPrintHelpIfNoArgs() {
        // Given
        val args: Array<String> = arrayOf()

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(1, returnCode)
        assertTrue(systemOutRule.log.startsWith("Usage: jcc"))
    }

    @Test
    fun shouldCompileButNotAssemble() {
        // Given
        val sourcePath = Files.createTempFile(Paths.get("target"), "ut_", ".bas")
        sourcePath.toFile().deleteOnExit()
        Files.write(sourcePath, listOf("10 PRINT"), UTF_8)
        val sourceFilename = sourcePath.toString()

        val asmFilename = sourceFilename.replace(".bas", ".asm")
        val asmPath = Paths.get(asmFilename)
        asmPath.toFile().deleteOnExit()

        val args = arrayOf("-S", sourceFilename)

        // When
        val returnCode = Jcc(args).run()

        // Then
        assertEquals(0, returnCode)
        assertTrue(Files.exists(asmPath), "asm file not found: $asmFilename")
    }
}
