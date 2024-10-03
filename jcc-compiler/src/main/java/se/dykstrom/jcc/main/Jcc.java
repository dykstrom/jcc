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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static se.dykstrom.jcc.common.utils.VerboseLogger.log;
import static se.dykstrom.jcc.main.Backend.FASM;

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

    @Parameter(names = "--backend", description = "Generate code for <backend>")
    private Backend backend = FASM;

    @Parameter(names = "-assembler", description = "Use <assembler> as the backend assembler. Default: 'fasm' for the FASM backend, and 'clang' for the LLVM backend")
    private String assemblerExecutable;

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

    @Parameter(names = "-Wall", description = "Enable all warnings")
    private boolean wAll;

    @Parameter(names = "-Wundefined-variable", description = "Warn about undefined variables")
    private boolean wUndefinedVariable;

    @Parameter(names = {"-v", "--verbose"}, description = "Verbose mode")
    private boolean verbose;

    @Parameter(description = "<source file>", converter = ToPathConverter.class)
    private Path sourcePath;

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

        // Set up warning options
        if (wAll) {
            wUndefinedVariable = true;
        }

        // Set up assembler executable
        if (assemblerExecutable == null) {
            assemblerExecutable = backend.executable();
        }

        // Turn on verbose mode if required
        VerboseLogger.setVerbose(verbose);

        log("Running " + PROGRAM + " " + Version.instance());
        log("Creating compiler");

        final CompilationErrorListener errorListener = new CompilationErrorListener();

        final CompilerFactory factory = CompilerFactory.builder()
                .backend(backend)
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
            showMessages(sourcePath, errorListener.getWarnings(), errorListener.getErrors());
            return 1;
        } catch (JccException e) {
            System.err.println(PROGRAM + ": error: " + e.getMessage());
            return 1;
        }

        // Here there will be no errors, but maybe some warnings
        showMessages(sourcePath, errorListener.getWarnings(), errorListener.getErrors());
        return 0;
    }

    private void showMessages(final Path sourcePath,
                              final List<CompilationWarning> warnings,
                              final List<CompilationError> errors) {
        final List<CompilationMessage> messages = new ArrayList<>(warnings);
        messages.addAll(errors);
        Collections.sort(messages);

        for (CompilationMessage message : messages) {
            final var text = new StringBuilder();
            text.append(sourcePath).append(":");
            text.append(message.line()).append(":");
            // Convert column from 0 based to 1 based
            text.append(message.column() + 1).append(" ");

            if (message instanceof CompilationWarning warning) {
                if (!shouldShowWarning(warning.warning())) {
                    continue;
                }
                text.append("warning: ");
            } else {
                text.append("error: ");
            }

            text.append(message.msg());
            System.err.println(text);
        }
    }

    private boolean shouldShowWarning(Warning warning) {
        return switch (warning) {
            case UNDEFINED_VARIABLE -> wUndefinedVariable;
        };
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
