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

package se.dykstrom.jcc.main;

import se.dykstrom.jcc.common.intermediate.IntermediateProgram;
import se.dykstrom.jcc.common.error.AssemblyException;
import se.dykstrom.jcc.common.utils.ProcessUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static se.dykstrom.jcc.common.utils.FileUtils.withExtension;
import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * An Assembler implementation that uses the flat assembler to assemble
 * the intermediate language program.
 */
public class FasmAssembler implements Assembler {

    private static final String FASM_INCLUDE_DIR = "INCLUDE";

    private final String assemblerExecutable;
    private final String assemblerInclude;
    private final boolean compileOnly;
    private final boolean saveTemps;

    public FasmAssembler(final String assemblerExecutable,
                         final String assemblerInclude,
                         final boolean compileOnly,
                         final boolean saveTemps) {
        this.assemblerExecutable = assemblerExecutable;
        this.assemblerInclude = assemblerInclude;
        this.compileOnly = compileOnly;
        this.saveTemps = saveTemps;
    }

    @Override
    public void assemble(final IntermediateProgram asmProgram,
                         final Path sourcePath,
                         final Path outputPath) throws AssemblyException {
        final Path asmPath = withExtension(sourcePath, "asm");

        // If user has not requested to save temporary files, delete them on exit
        if (!saveTemps && !compileOnly) {
            asmPath.toFile().deleteOnExit();
        }

        // Create intermediate assembly file
        log("  Writing assembly file '" + asmPath + "'");
        final List<String> asmText = Collections.singletonList(asmProgram.toText());
        try {
            Files.write(asmPath, asmText, UTF_8);
        } catch (IOException e) {
            throw new AssemblyException("Failed to write assembly file: " + e.getMessage());
        }

        // If user requested compilation only, we are done now
        if (compileOnly) {
            return;
        }

        final List<String> fasmCommandLine = buildCommandLine(asmPath, outputPath);
        final Map<String, String> fasmEnvironment = buildEnvironment();

        log("  Assembling executable '" + outputPath + "'");
        log("  Assembler environment '" + fasmEnvironment + "'");
        log("  Assembler command line '" + String.join(" ", fasmCommandLine) + "'");
        Process process;
        try {
            process = ProcessUtils.setUpProcess(fasmCommandLine, fasmEnvironment);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssemblyException("Failed to run assembler: " + e.getMessage());
        } catch (IOException e) {
            throw new AssemblyException("Failed to run assembler: " + e.getMessage());
        }

        if (process.exitValue() != 0) {
            final var output = ProcessUtils.readOutput(process);
            ProcessUtils.tearDownProcess(process);
            throw new AssemblyException("Compilation failed, see assembler output: " + output);
        }

        ProcessUtils.tearDownProcess(process);
    }


    private Map<String, String> buildEnvironment() {
        final Map<String, String> environment = new HashMap<>();
        if (assemblerInclude != null) {
            environment.put(FASM_INCLUDE_DIR, assemblerInclude);
        }
        return environment;
    }

    private List<String> buildCommandLine(final Path asmPath, final Path outputPath) {
        return List.of(assemblerExecutable, asmPath.toString(), outputPath.toString());
    }
}
