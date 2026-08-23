/*
 * Copyright (C) 2024 Johan Dykstrom
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

import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.error.JccException;
import se.dykstrom.jcc.common.utils.OptimizationOptions;
import se.dykstrom.jcc.common.utils.OsUtils;
import se.dykstrom.jcc.common.utils.ProcessUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static se.dykstrom.jcc.common.utils.FileUtils.withExtension;
import static se.dykstrom.jcc.common.utils.FormatUtils.indentText;
import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * Compiles a target program, which is LLVM IR, to a native executable by
 * invoking Clang. The term assemble is used broadly here: the "assembling"
 * is really a compilation, because the target language is LLVM IR.
 */
public class Assembler {

    private final String clangExecutable;
    private final boolean compileOnly;
    private final boolean saveTemps;
    private final String stdlib;

    public Assembler(final String clangExecutable,
                     final boolean compileOnly,
                     final boolean saveTemps,
                     final String stdlib) {
        this.clangExecutable = clangExecutable;
        this.compileOnly = compileOnly;
        this.saveTemps = saveTemps;
        this.stdlib = stdlib;
    }

    /**
     * Compiles the given target language program and writes the result
     * to the outputPath.
     */
    public void assemble(final TargetProgram program, final Path sourcePath, final Path outputPath, final Path libraryPath) {
        final Path llvmPath = withExtension(sourcePath, "ll");

        // If user has not requested to save temporary files, delete them on exit
        if (!saveTemps && !compileOnly) {
            llvmPath.toFile().deleteOnExit();
        }

        // Create LLVM IR file
        log("  Writing LLVM IR file '" + llvmPath + "'");
        final List<String> llvmText = List.of(program.toText());
        try {
            Files.write(llvmPath, llvmText, UTF_8);
        } catch (IOException e) {
            throw new JccException("Failed to write LLVM IR file: " + e.getMessage());
        }

        // If user requested compilation only, we are done now
        if (compileOnly) {
            return;
        }

        final List<String> clangCommandLine = buildCommandLine(llvmPath, outputPath, libraryPath);

        if (outputPath == null) {
            log("  Creating default executable: a.exe or a.out");
        } else {
            log("  Creating executable '" + outputPath + "'");
        }
        log("  Clang command line '" + String.join(" ", clangCommandLine) + "'");
        Process process;
        try {
            process = ProcessUtils.setUpProcess(clangCommandLine, Map.of());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JccException("Failed to run clang: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new JccException(clangExecutable + " timed out after " + ProcessUtils.processTimeoutSeconds() + " seconds");
        } catch (IOException e) {
            throw new JccException("Failed to run clang: " + e.getMessage());
        }

        try {
            final var output = ProcessUtils.readOutput(process);
            log(indentText(output, 2));
            if (process.exitValue() != 0) {
                throw new JccException("Compilation failed, see clang output: " + output);
            }
        } finally {
            ProcessUtils.tearDownProcess(process);
        }
    }

    private List<String> buildCommandLine(final Path llvmPath, final Path outputPath, final Path libraryPath) {
        final var args = new ArrayList<String>();
        args.add(clangExecutable);
        if (saveTemps) {
            args.add("-save-temps");
        }
        args.add("-O" + OptimizationOptions.INSTANCE.getLevel());
        if (outputPath != null) {
            args.add("-o");
            args.add(outputPath.toString());
        }
        args.add(llvmPath.toString());
        // Libraries must come after the input file for proper linking
        if (libraryPath != null && stdlib != null) {
            args.add("-L" + libraryPath);
            args.add("-l" + stdlib); // Standard library
        }
        if (OsUtils.isLinux()) {
            args.add("-lm"); // Math library - required on Linux
        }
        return args;
    }
}
