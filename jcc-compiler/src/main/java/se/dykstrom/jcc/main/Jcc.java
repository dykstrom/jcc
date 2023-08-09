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

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import se.dykstrom.jcc.common.error.*;
import se.dykstrom.jcc.common.utils.GcOptions;
import se.dykstrom.jcc.common.utils.OptimizationOptions;
import se.dykstrom.jcc.common.utils.VerboseLogger;
import se.dykstrom.jcc.common.utils.Version;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * The main class of the Johan Compiler Collection (JCC). It parses command line arguments,
 * creates a compiler instance using the compiler factory, and starts the compilation.
 * <p>
 * For help, type "jcc --help".
 *
 * @author Johan Dykstrom
 */
@SuppressWarnings("java:S106")
public class Jcc {

    private static final String PROGRAM = "jcc";

    private final String[] args;

    @SuppressWarnings({"FieldCanBeLocal", "CanBeFinal"})
    @Parameter(names = "-assembler", description = "Use <assembler> to assemble intermediate files")
    private String assemblerExecutable = "fasm";

    @Parameter(names = "-assembler-include", description = "Set the assembler's include directory to <directory>")
    private String assemblerInclude;

    @Parameter(names = "--help", description = "Show this help text", help = true)
    private boolean showHelp;

    @SuppressWarnings({"FieldCanBeLocal", "CanBeFinal"})
    @Parameter(names = "-initial-gc-threshold", description = "Set the number of allocations before first garbage collection")
    private int initialGcThreshold = 100;

    @Parameter(names = {"-O", "-O1"}, description = "Optimize output")
    private boolean o1;

    @Parameter(names = "-o", description = "Place output in <file>", converter = ToPathConverter.class)
    private Path outputPath;

    @Parameter(names = "-print-gc", description = "Print messages at garbage collection")
    private boolean printGc;

    @Parameter(names = "-S", description = "Compile only; do not assemble")
    private boolean compileOnly;

    @Parameter(names = "-save-temps", description = "Save temporary intermediate files permanently")
    private boolean saveTemps;

    @Parameter(names = "--version", description = "Show compiler version", help = true)
    private boolean showVersion;

    @Parameter(description = "<source file>", converter = ToPathConverter.class)
    private Path sourcePath;

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
            } else if (showHelp || sourcePath == null) {
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

        final CompilationErrorListener errorListener = new CompilationErrorListener();

        final CompilerFactory factory = CompilerFactory.builder()
                .compileOnly(compileOnly)
                .saveTemps(saveTemps)
                .assemblerExecutable(assemblerExecutable)
                .assemblerInclude(assemblerInclude)
                .errorListener(errorListener)
                .build();

        final Compiler compiler;
        try {
            compiler = factory.create(sourcePath, outputPath);
        } catch (IllegalArgumentException e) {
            System.err.println(PROGRAM + ": error: " + e.getMessage());
            return 1;
        } catch (FileNotFoundException e) {
            System.err.println(PROGRAM + ": error: " + sourcePath + ": No such file or directory");
            return 1;
        }

        try {
            compiler.compile();
        } catch (SyntaxException | SemanticsException e) {
            showErrors(sourcePath, errorListener.getErrors());
            return 1;
        } catch (JccException e) {
            System.err.println(PROGRAM + ": error: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    private void showErrors(final Path sourcePath, final List<CompilationError> errors) {
        Collections.sort(errors);
        for (CompilationError error : errors) {
            // Convert column from 0 based to 1 based
            System.err.println(sourcePath + ":" + error.line() + ":" + (error.column() + 1) + " " + error.msg());
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

    /**
     * A class that converts a String to a Path for some JCommander parameters.
     */
    private static class ToPathConverter implements IStringConverter<Path> {
        @Override
        public Path convert(final String filename) {
            return Path.of(filename);
        }
    }
}
