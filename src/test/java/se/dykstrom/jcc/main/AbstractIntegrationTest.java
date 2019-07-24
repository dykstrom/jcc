/*
 * Copyright (C) 2016 Johan Dykstrom
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

package se.dykstrom.jcc.main;

import se.dykstrom.jcc.common.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

/**
 * Abstract base class for integration tests.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractIntegrationTest {

    static final String ASM = "asm";
    static final String ASSEMBUNNY = "asmb";
    @SuppressWarnings("WeakerAccess")
    static final String EXE = "exe";
    static final String BASIC = "bas";
    static final String TINY = "tiny";

    private static final String ASM_OPTION = "-assembler";
    private static final String ASM_VALUE = "fasm/FASM.EXE";
    private static final String ASM_INC_OPTION = "-assembler-include";
    private static final String ASM_INC_VALUE = "fasm/INCLUDE";

    // -----------------------------------------------------------------------

    /**
     * Creates a temporary file, whose contents will be {@code source} and extension
     * will be {@code extension}. The file will be encoded in UTF-8.
     */
    static Path createSourceFile(List<String> source, String extension) throws IOException {
        Path path = Files.createTempFile(null, "." + extension);
        path.toFile().deleteOnExit();
        Files.write(path, source, StandardCharsets.UTF_8);
        return path;
    }

    /**
     * Converts the source file name by changing the extension to {@code newExtension}.
     */
    static String convertFilename(String sourceFilename, String newExtension) {
        int index = sourceFilename.lastIndexOf(".");
        return sourceFilename.substring(0, index + 1) + newExtension;
    }

    /**
     * Builds a command line for running the flat assembler.
     */
    static String[] buildCommandLine(String sourceFilename, String... otherArgs) {
        List<String> args = new ArrayList<>();
        args.add(ASM_OPTION);
        args.add(ASM_VALUE);
        args.add(ASM_INC_OPTION);
        args.add(ASM_INC_VALUE);
        args.addAll(asList(otherArgs));
        args.add(sourceFilename);
        return args.toArray(new String[0]);
    }

    /**
     * Asserts that the compilation finished successfully, and that the asm and exe files exist.
     */
    static void assertSuccessfulCompilation(Jcc jcc, String asmFilename, String exeFilename) {
        assertEquals("Compiler exit value non-zero,", 0, jcc.run());
        assertTrue("asm file not found: " + asmFilename, Files.exists(Paths.get(asmFilename)));
        assertTrue("exe file not found: " + exeFilename, Files.exists(Paths.get(exeFilename)));
    }

    /**
     * Compiles the given source file, and asserts that the compilation failed.
     */
    static void compileAndAssertFail(Path path) {
        Jcc jcc = new Jcc(buildCommandLine(path.toString()));
        assertEquals(1, jcc.run());
    }

    /**
     * Compiles the given source file, and asserts that the compilation is successful.
     */
    static void compileAndAssertSuccess(Path sourceFile) {
        compileAndAssertSuccess(sourceFile, false, 100, null);
    }

    /**
     * Compiles the given source file, and asserts that the compilation is successful.
     */
    static void compileAndAssertSuccess(Path sourceFile, String optimization) {
        compileAndAssertSuccess(sourceFile, false, 100, optimization);
    }

    /**
     * Compiles the given source file, and asserts that the compilation is successful.
     */
    static void compileAndAssertSuccess(Path sourceFile, boolean printGc, int initialGcThreshold) {
        compileAndAssertSuccess(sourceFile, printGc, initialGcThreshold, null);
    }

    /**
     * Compiles the given source file, and asserts that the compilation is successful.
     *
     * @param sourceFile The source file to compile.
     * @param printGc Enable GC debug information if true.
     * @param initialGcThreshold Number of allocation before first GC.
     * @param optimization Optimization flag, or {@code null} if no optimization.
     */
    static void compileAndAssertSuccess(Path sourceFile, boolean printGc, int initialGcThreshold, String optimization) {
        String sourceFilename = sourceFile.toString();
        String asmFilename = convertFilename(sourceFilename, ASM);
        String exeFilename = convertFilename(sourceFilename, EXE);

        Paths.get(exeFilename).toFile().deleteOnExit();

        List<String> args = new ArrayList<>();
        if (printGc) {
            args.add("-print-gc");
            args.add("-initial-gc-threshold");
            args.add(Integer.toString(initialGcThreshold));
        }
        if (optimization != null) {
            args.add(optimization);
        }

        Jcc jcc = new Jcc(buildCommandLine(sourceFilename, args.toArray(new String[0])));
        assertSuccessfulCompilation(jcc, asmFilename, exeFilename);
    }

    /**
     * Runs the program that results from compiling the given source file,
     * and compares the output of the program with the expected output.
     *
     * @param sourceFile A source file that has previously been compiled to an executable program.
     * @param expectedOutput The expected output of the program.
     * @throws Exception If running the compiled programs fails with an exception.
     */
    static void runAndAssertSuccess(Path sourceFile, String expectedOutput) throws Exception {
        runAndAssertSuccess(sourceFile, expectedOutput, null);
    }

    /**
     * Runs the program that results from compiling the given source file,
     * and compares the output and exit value of the program with the expected 
     * output and exit value.
     *
     * @param sourceFile A source file that has previously been compiled to an executable program.
     * @param expectedOutput The expected output of the program.
     * @param expectedExitValue The expected exit value, or {@code null} if exit value does not matter.
     * @throws Exception If running the compiled programs fails with an exception.
     */
    static void runAndAssertSuccess(Path sourceFile, String expectedOutput, Integer expectedExitValue) throws Exception {
        String exeFilename = convertFilename(sourceFile.toString(), EXE);

        Process process = null;
        try {
            process = ProcessUtils.setUpProcess(singletonList(exeFilename), emptyMap());
            assertFalse("Process is still alive", process.isAlive());
            if (expectedExitValue != null) {
                assertEquals("Exit value differs:", expectedExitValue.intValue(), process.exitValue());
            }
            String actualOutput = ProcessUtils.readOutput(process);
            assertEquals("Program output differs:", expectedOutput, actualOutput);
        } finally {
            if (process != null) {
                ProcessUtils.tearDownProcess(process);
            }
        }
    }

    /**
     * Runs the program that results from compiling the given source file,
     * and compares the output and exit value of the program with the expected
     * output and exit value.
     *
     * @param sourceFile A source file that has previously been compiled to an executable program.
     * @param input Text to provide as input to the program.
     * @param expectedOutput The expected output of the program.
     * @param expectedExitValue The expected exit value, or {@code null} if exit value does not matter.
     * @throws Exception If running the compiled programs fails with an exception.
     */
    static void runAndAssertSuccess(Path sourceFile, List<String> input, List<String> expectedOutput, Integer expectedExitValue) throws Exception {
        String exeFilename = convertFilename(sourceFile.toString(), EXE);

        // Write input to a temporary file
        Path inputPath = Files.createTempFile(null, null);
        Files.write(inputPath, input, UTF_8);
        File inputFile = inputPath.toFile();
        inputFile.deleteOnExit();

        Process process = null;
        try {
            process = ProcessUtils.setUpProcess(singletonList(exeFilename), inputFile, emptyMap());
            assertFalse("Process is still alive", process.isAlive());
            if (expectedExitValue != null) {
                assertEquals("Exit value differs:", expectedExitValue.intValue(), process.exitValue());
            }
            String actualOutput = ProcessUtils.readOutput(process);
            assertOutput(expectedOutput, actualOutput);
        } finally {
            if (process != null) {
                ProcessUtils.tearDownProcess(process);
            }
        }
    }

    /**
     * Runs the program that results from compiling the given source file,
     * and compares the output and exit value of the program with the expected
     * output and exit value. This method splits the actual output into lines,
     * and then compares with the expected output line by line. However, only
     * the beginning of the lines are compared. This can be used when you know
     * what will be printed at the beginning of each line, but not the complete
     * line, which may contain some dynamic data.
     *
     * @param sourceFile A source file that has previously been compiled to an executable program.
     * @param expectedOutput The expected output of the program.
     * @param expectedExitValue The expected exit value, or {@code null} if exit value does not matter.
     * @throws Exception If running the compiled programs fails with an exception.
     */
    static void runAndAssertSuccess(Path sourceFile, List<String> expectedOutput, Integer expectedExitValue) throws Exception {
        String exeFilename = convertFilename(sourceFile.toString(), EXE);

        Process process = null;
        try {
            process = ProcessUtils.setUpProcess(singletonList(exeFilename), emptyMap());
            assertFalse("Process is still alive", process.isAlive());
            if (expectedExitValue != null) {
                assertEquals("Exit value differs:", expectedExitValue.intValue(), process.exitValue());
            }
            String actualOutput = ProcessUtils.readOutput(process);
            assertOutput(expectedOutput, actualOutput);
        } finally {
            if (process != null) {
                ProcessUtils.tearDownProcess(process);
            }
        }
    }

    /**
     * Asserts that the actual output equals the expected output, after first splitting
     * the actual output into several lines.
     */
    private static void assertOutput(List<String> expectedOutput, String actualOutput) {
        String[] actualLines = actualOutput.split("\n");
        assertEquals("Actual output:\n\n" + actualOutput + "\nNumber of lines differ:", expectedOutput.size(), actualLines.length);

        for (int i = 0; i < expectedOutput.size(); i++) {
            assertTrue("Output differs on line " + i + ": "
                            + "expected:<" + expectedOutput.get(i) + "> but was:<" + actualLines[i] + ">",
                    actualLines[i].startsWith(expectedOutput.get(i)));
        }
    }
}
