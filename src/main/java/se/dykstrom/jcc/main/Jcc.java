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
 *
 * For help, type "jcc -help".
 *
 * @author Johan Dykstrom
 */
public class Jcc {

    private static final String PROGRAM = "jcc";

    private static final String ASSEMBUNNY = "asmb";
    private static final String BASIC = "bas";
    private static final String TINY = "tiny";

    private static final String FASM_INCLUDE_DIR = "INCLUDE";

    private final String[] args;

    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(names = "-assembler", description = "Use <assembler> to assemble intermediate files")
    private String assembler = "fasm";

    @Parameter(names = "-assembler-include", description = "Set the assembler's include directory to <directory>")
    private String assemblerInclude;

    @Parameter(names = "-help", description = "Show help", help = true)
    private boolean help;

    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(names = "-initial-gc-threshold", description = "Number of allocations done before first garbage collection")
    private int initialGcThreshold = 100;

    @Parameter(names = "-o", description = "Place output in file <file>")
    private String outputFilename;

    @Parameter(names = "-print-gc", description = "Print messages at garbage collection")
    private boolean printGc;

    @Parameter(names = "-save-temps", description = "Save temporary intermediate files permanently")
    private boolean saveTemps;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Parameter(description = "<source file>")
    private List<String> sourceFilenames = new ArrayList<>();

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

            if (help || sourceFilenames.size() != 1) {
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

        // Turn on verbose mode if required
        VerboseLogger.setVerbose(verbose);

        String sourceFilename = sourceFilenames.get(0);

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

        // If no output filename has been specified, derive it from the input filename
        if (outputFilename == null) {
            outputFilename = getBasename(sourceFilename) + ".exe";
        }

        String asmFilename = getBasename(outputFilename) + ".asm";

        // If user has not requested to save temporary files, delete them on exit
        if (!saveTemps) {
            Paths.get(asmFilename).toFile().deleteOnExit();
        }

        // Create intermediate assembly file
        log("Writing assembly file '" + asmFilename + "'");
        List<String> asmText = Collections.singletonList(asmProgram.toAsm());
        try {
            Files.write(Paths.get(asmFilename), asmText, UTF_8);
        } catch (IOException e) {
            System.err.println(PROGRAM + ": failed to write assembly file: " + e.getMessage());
            return 1;
        }

        List<String> fasmCommandLine = buildCommandLine(asmFilename);
        Map<String, String> fasmEnvironment = buildEnvironment();

        log("Assembling executable '" + outputFilename + "'");
        log("Assembler environment '" + fasmEnvironment + "'");
        log("Assembler command line '" + String.join(" ", fasmCommandLine) + "'");
        Process process;
        try {
            process = ProcessUtils.setUpProcess(fasmCommandLine, fasmEnvironment);
        } catch (IOException | InterruptedException e) {
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
        Map<String, String> env = new HashMap<>();
        if (assemblerInclude != null) {
            env.put(FASM_INCLUDE_DIR, assemblerInclude);
        }
        return env;
    }

    private List<String> buildCommandLine(String asmFilename) {
        List<String> args = new ArrayList<>();
        args.add(assembler);
        args.add(asmFilename);
        args.add(outputFilename);
        return args;
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
            System.err.println(sourceFilename + ":" + error.getLine() + ":" + (error.getColumn() + 1) + " " + error.getMsg());
        }
    }

    private void showUsage(JCommander jCommander) {
        jCommander.setProgramName(PROGRAM);
        jCommander.usage();
    }

    public static void main(String[] args) {
        Jcc jcc = new Jcc(args);
        int status = jcc.run();
        System.exit(status);
    }
}
