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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        return args.toArray(new String[args.size()]);
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
        String sourceFilename = sourceFile.toString();
        String asmFilename = convertFilename(sourceFilename, ASM);
        String exeFilename = convertFilename(sourceFilename, EXE);

        Paths.get(exeFilename).toFile().deleteOnExit();

        Jcc jcc = new Jcc(buildCommandLine(sourceFilename));
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
            process = ProcessUtils.setUpProcess(singletonList(exeFilename), Collections.emptyMap());
            String actualOutput = ProcessUtils.readOutput(process);
            if (expectedExitValue != null) {
                assertEquals("Exit value differs:", expectedExitValue.intValue(), process.exitValue());
            }
            assertEquals("Program output differs:", expectedOutput, actualOutput);
        } finally {
            if (process != null) {
                ProcessUtils.tearDownProcess(process);
            }
        }
    }
}
