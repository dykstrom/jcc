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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.antlr.v4.runtime.CharStreams;
import se.dykstrom.jcc.assembunny.compiler.AssembunnyCompiler;
import se.dykstrom.jcc.basic.compiler.BasicCompiler;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.compiler.Compiler;
import se.dykstrom.jcc.common.error.CompilationError;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.utils.*;
import se.dykstrom.jcc.tiny.compiler.TinyCompiler;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static se.dykstrom.jcc.common.utils.FileUtils.getBasename;
import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * The main class of the Johan Compiler Collection (JCC). It parses command line arguments,
 * creates a suitable compiler depending on file type, and starts the compilation.
 * <p>
 * For help, type "jcc --help".
 *
 * @author Johan Dykstrom
 */
@SuppressWarnings("java:S106")
public class Jcc {

    private static final String PROGRAM = "jcc";

    private static final String ASSEMBUNNY = "asmb";
    private static final String BASIC = "bas";
    private static final String TINY = "tiny";

    private static final String FASM_INCLUDE_DIR = "INCLUDE";

    private final String[] args;

    @SuppressWarnings({"FieldCanBeLocal", "CanBeFinal"})
    @Parameter(names = "-assembler", description = "Use <assembler> to assemble intermediate files")
    private String assembler = "fasm";

    @Parameter(names = "-assembler-include", description = "Set the assembler's include directory to <directory>")
    private String assemblerInclude;

    @Parameter(names = "--help", description = "Show this help text", help = true)
    private boolean showHelp;

    @SuppressWarnings({"FieldCanBeLocal", "CanBeFinal"})
    @Parameter(names = "-initial-gc-threshold", description = "Set the number of allocations before first garbage collection")
    private int initialGcThreshold = 100;

    @Parameter(names = {"-O", "-O1"}, description = "Optimize output")
    private boolean o1;

    @Parameter(names = "-o", description = "Place output in <file>")
    private String outputFilename;

    @Parameter(names = "-print-gc", description = "Print messages at garbage collection")
    private boolean printGc;

    @Parameter(names = "-S", description = "Compile only; do not assemble")
    private boolean compileOnly;

    @Parameter(names = "-save-temps", description = "Save temporary intermediate files permanently")
    private boolean saveTemps;

    @Parameter(names = "--version", description = "Show compiler version", help = true)
    private boolean showVersion;

    @Parameter(description = "<source file>")
    private String sourceFilename;

    @Parameter(names = "-v", description = "Verbose mode")
    private boolean verbose;

    public Jcc(String[] args) {
        this.args = args;
    }

    int run() {
        // Parse and validate command line arguments
        try {
            JCommander jCommander = new JCommander(this);
            jCommander.parse(args);

            if (showVersion) {
                showVersion();
                return 0;
            } else if (showHelp || sourceFilename == null) {
                showUsage(jCommander);
                return 1;
            }
        } catch (ParameterException pe) {
            System.err.println(PROGRAM + ": " + pe.getMessage());
            return 1;
        }

        // Set up GC options
        GcOptions.INSTANCE.setPrintGc(printGc);
        GcOptions.INSTANCE.setInitialGcThreshold(initialGcThreshold);

        // Set up optimization options
        if (o1) {
            OptimizationOptions.INSTANCE.setLevel(1);
        } else {
            OptimizationOptions.INSTANCE.setLevel(0);
        }

        // Turn on verbose mode if required
        VerboseLogger.setVerbose(verbose);

        log("Running " + PROGRAM + " " + Version.instance());
        log("Creating compiler");
        Compiler compiler = createCompiler(sourceFilename);
        if (compiler == null) {
            System.err.println(PROGRAM + ": invalid file type: " + sourceFilename);
            return 1;
        }
        compiler.setSourceFilename(sourceFilename);

        log("Reading source file '" + sourceFilename + "'");
        try {
            compiler.setInputStream(CharStreams.fromStream(new FileInputStream(sourceFilename), UTF_8));
        } catch (IOException e) {
            System.err.println(PROGRAM + ": file not found: " + sourceFilename);
            return 1;
        }

        compiler.setErrorListener(new CompilationErrorListener());

        log("Compiling source file to assembly code");
        AsmProgram asmProgram = compiler.compile();

        List<CompilationError> errors = compiler.getErrorListener().getErrors();
        if (!errors.isEmpty()) {
            showErrors(sourceFilename, errors);
            return 1;
        }

        //asmProgram.lines().forEach(line -> System.out.println(line.toAsm()));

        // If no output filename has been specified, derive it from the input filename
        if (outputFilename == null) {
            outputFilename = getBasename(sourceFilename) + ".exe";
        }

        final var asmFilename = getBasename(outputFilename) + ".asm";
        final var asmPath = Paths.get(asmFilename);

        // If user has not requested to save temporary files, delete them on exit
        if (!saveTemps && !compileOnly) {
            asmPath.toFile().deleteOnExit();
        }

        // Create intermediate assembly file
        log("Writing assembly file '" + asmFilename + "'");
        List<String> asmText = Collections.singletonList(asmProgram.toAsm());
        try {
            Files.write(asmPath, asmText, UTF_8);
        } catch (IOException e) {
            System.err.println(PROGRAM + ": failed to write assembly file: " + e.getMessage());
            return 1;
        }

        // If user requested compilation only, we are done now
        if (compileOnly) {
            return 0;
        }

        List<String> fasmCommandLine = buildCommandLine(asmFilename);
        Map<String, String> fasmEnvironment = buildEnvironment();

        log("Assembling executable '" + outputFilename + "'");
        log("Assembler environment '" + fasmEnvironment + "'");
        log("Assembler command line '" + String.join(" ", fasmCommandLine) + "'");
        Process process;
        try {
            process = ProcessUtils.setUpProcess(fasmCommandLine, fasmEnvironment);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(PROGRAM + ": failed to run assembler: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            System.err.println(PROGRAM + ": failed to run assembler: " + e.getMessage());
            return 1;
        }

        if (process.exitValue() != 0) {
            System.err.println(PROGRAM + ": compilation failed, see assembler output:");
            System.err.println(ProcessUtils.readOutput(process));
            ProcessUtils.tearDownProcess(process);
            return 1;
        }

        ProcessUtils.tearDownProcess(process);

        return 0;
    }

    private Map<String, String> buildEnvironment() {
        Map<String, String> environment = new HashMap<>();
        if (assemblerInclude != null) {
            environment.put(FASM_INCLUDE_DIR, assemblerInclude);
        }
        return environment;
    }

    private List<String> buildCommandLine(String asmFilename) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(assembler);
        commandLine.add(asmFilename);
        commandLine.add(outputFilename);
        return commandLine;
    }

    private Compiler createCompiler(String sourceFilename) {
        String extension = FileUtils.getExtension(sourceFilename);
        log("  Source file of type '" + extension + "'");
        if (ASSEMBUNNY.equals(extension)) {
            return new AssembunnyCompiler();
        } else if (BASIC.equals(extension)) {
            return new BasicCompiler();
        } else if (TINY.equals(extension)) {
            return new TinyCompiler();
        }
        return null;
    }

    private void showErrors(String sourceFilename, List<CompilationError> errors) {
        Collections.sort(errors);
        for (CompilationError error : errors) {
            // Convert column from 0 based to 1 based
            System.err.println(sourceFilename + ":" + error.line() + ":" + (error.column() + 1) + " " + error.msg());
        }
    }

    private void showUsage(JCommander jCommander) {
        jCommander.setProgramName(PROGRAM);
        jCommander.usage();
    }

    private void showVersion() {
        System.out.println(PROGRAM + " " + Version.instance());
    }

    public static void main(String[] args) {
        Jcc jcc = new Jcc(args);
        int status = jcc.run();
        System.exit(status);
    }
}
