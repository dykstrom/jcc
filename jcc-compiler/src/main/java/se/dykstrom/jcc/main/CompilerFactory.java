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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

import se.dykstrom.jcc.assembunny.compiler.AssembunnyCodeGenerator;
import se.dykstrom.jcc.assembunny.compiler.AssembunnySemanticsParser;
import se.dykstrom.jcc.assembunny.compiler.AssembunnySyntaxParser;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicSemanticsParser;
import se.dykstrom.jcc.basic.compiler.BasicSymbols;
import se.dykstrom.jcc.basic.compiler.BasicSyntaxParser;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.basic.optimization.BasicAstOptimizer;
import se.dykstrom.jcc.col.compiler.ColCodeGenerator;
import se.dykstrom.jcc.col.compiler.ColSemanticsParser;
import se.dykstrom.jcc.col.compiler.ColSyntaxParser;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.compiler.CodeGenerator;
import se.dykstrom.jcc.common.compiler.DefaultTypeManager;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.SyntaxParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.optimization.AstExpressionOptimizer;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.tiny.compiler.TinyCodeGenerator;
import se.dykstrom.jcc.tiny.compiler.TinySemanticsParser;
import se.dykstrom.jcc.tiny.compiler.TinySyntaxParser;

import static java.nio.charset.StandardCharsets.UTF_8;
import static se.dykstrom.jcc.common.utils.FileUtils.withExtension;
import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * The CompilerFactory class creates a compiler by creating and assembling several components,
 * like a symbol table, a syntax parser, and a code generator. This class depends heavily on the
 * different language implementations so that the compiler itself does not have to.
 */
@SuppressWarnings("SwitchStatementWithTooFewBranches")
public record CompilerFactory(boolean compileOnly,
                              boolean saveTemps,
                              String assemblerExecutable,
                              String assemblerInclude,
                              CompilationErrorListener errorListener) {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a compiler instance that reads its input from sourcePath,
     * and writes its output to outputPath. If outputPath is null, it will
     * be derived from sourcePath.
     *
     * @param sourcePath The path to the source file to compile, not null.
     * @param outputPath The path to the output file to create, may be null.
     * @return The compiler.
     * @throws FileNotFoundException If the file denoted by sourcePath does not exist.
     */
    public Compiler create(final Path sourcePath, final Path outputPath) throws FileNotFoundException {
        return create(new FileInputStream(sourcePath.toFile()), sourcePath, outputPath);
    }

    /**
     * Creates a compiler instance that reads its input from the string sourceText,
     * and writes its output to outputPath. If outputPath is null, it will be derived
     * from sourcePath. The sourcePath parameter must be set even though the input is
     * read from sourceText, because the language to compile is determined from
     * sourcePath.
     *
     * @param sourceText The source text to compile.
     * @param sourcePath The path to the (imaginary) source file, not null.
     * @param outputPath The path to the output file to create, may be null.
     * @return The compiler.
     */
    public Compiler create(final String sourceText,
                           final Path sourcePath,
                           final Path outputPath) {
        return create(new ByteArrayInputStream(sourceText.getBytes(UTF_8)), sourcePath, outputPath);
    }


    /**
     * Creates a compiler instance that reads its input from the given inputStream,
     * and writes its output to outputPath. If outputPath is null, it will be derived
     * from sourcePath. The sourcePath parameter must be set even though the input is
     * read from inputStream, because the language to compile is determined from
     * sourcePath.
     *
     * @param inputStream An input stream from which to read the text to compile.
     * @param sourcePath The path to the (imaginary) source file, not null.
     * @param outputPath The path to the output file to create, may be null.
     * @return The compiler.
     */
    public Compiler create(final InputStream inputStream,
                           final Path sourcePath,
                           final Path outputPath) {
        final var actualOutputPath = outputPath != null ? outputPath : withExtension(sourcePath, "exe");

        log("Reading source file '" + sourcePath + "'");
        final var language = Language.fromSource(sourcePath);
        log("  Source file of type " + language);

        final TypeManager typeManager = createTypeManager(language);
        final SymbolTable symbolTable = createSymbolTable(language);
        final SyntaxParser syntaxParser = createSyntaxParser(language, typeManager);
        // Make a copy of the symbol table, because it may change during parsing
        final AstOptimizer astOptimizer = createAstOptimizer(language, typeManager, new SymbolTable(symbolTable));
        final SemanticsParser semanticsParser = createSemanticsParser(language, typeManager, new SymbolTable(symbolTable), astOptimizer.expressionOptimizer());
        final CodeGenerator codeGenerator = createCodeGenerator(language, typeManager, astOptimizer, new SymbolTable(symbolTable));
        final Assembler assembler = createAssembler();

        return GenericCompiler.builder()
                .inputStream(inputStream)
                .sourcePath(sourcePath)
                .outputPath(actualOutputPath)
                .syntaxParser(syntaxParser)
                .semanticsParser(semanticsParser)
                .astOptimizer(astOptimizer)
                .codeGenerator(codeGenerator)
                .assembler(assembler)
                .build();
    }

    private TypeManager createTypeManager(final Language language) {
        return switch (language) {
            case BASIC -> new BasicTypeManager();
            case COL -> new ColTypeManager();
            default -> new DefaultTypeManager();
        };
    }

    private SymbolTable createSymbolTable(final Language language) {
        return switch (language) {
            case BASIC -> new BasicSymbols();
            default -> new SymbolTable();
        };
    }

    private SyntaxParser createSyntaxParser(final Language language, final TypeManager typeManager) {
        return switch (language) {
            case ASSEMBUNNY -> new AssembunnySyntaxParser(errorListener);
            case BASIC -> new BasicSyntaxParser((BasicTypeManager) typeManager, errorListener);
            case COL -> new ColSyntaxParser(errorListener);
            case TINY -> new TinySyntaxParser(errorListener);
        };
    }

    private AstOptimizer createAstOptimizer(final Language language,
                                            final TypeManager typeManager,
                                            final SymbolTable symbolTable) {
        return switch (language) {
            case BASIC -> new BasicAstOptimizer((BasicTypeManager) typeManager, symbolTable);
            default -> new DefaultAstOptimizer(typeManager, symbolTable);
        };
    }

    private SemanticsParser createSemanticsParser(final Language language,
                                                  final TypeManager typeManager,
                                                  final SymbolTable symbolTable,
                                                  final AstExpressionOptimizer optimizer) {
        return switch (language) {
            case ASSEMBUNNY -> new AssembunnySemanticsParser(errorListener);
            case BASIC -> new BasicSemanticsParser(errorListener, symbolTable, (BasicTypeManager) typeManager, optimizer);
            case COL -> new ColSemanticsParser(errorListener, symbolTable, (ColTypeManager) typeManager);
            case TINY -> new TinySemanticsParser(errorListener, symbolTable);
        };
    }

    private CodeGenerator createCodeGenerator(final Language language,
                                              final TypeManager typeManager,
                                              final AstOptimizer astOptimizer,
                                              final SymbolTable symbolTable) {
        return switch (language) {
            case ASSEMBUNNY -> new AssembunnyCodeGenerator(typeManager, symbolTable, astOptimizer);
            case BASIC -> new BasicCodeGenerator(typeManager, symbolTable, astOptimizer);
            case COL -> new ColCodeGenerator(typeManager, symbolTable, astOptimizer);
            case TINY -> new TinyCodeGenerator(typeManager, symbolTable, astOptimizer);
        };
    }

    private Assembler createAssembler() {
        return new FasmAssembler(assemblerExecutable, assemblerInclude, compileOnly, saveTemps);
    }

    public static class Builder {

        private boolean compileOnly;
        private boolean saveTemps;
        private String assemblerExecutable;
        private String assemblerInclude;
        private CompilationErrorListener errorListener;

        public Builder compileOnly(final boolean compileOnly) {
            this.compileOnly = compileOnly;
            return this;
        }

        public Builder saveTemps(final boolean saveTemps) {
            this.saveTemps = saveTemps;
            return this;
        }

        public Builder assemblerExecutable(final String assemblerExecutable) {
            this.assemblerExecutable = assemblerExecutable;
            return this;
        }

        public Builder assemblerInclude(final String assemblerInclude) {
            this.assemblerInclude = assemblerInclude;
            return this;
        }

        public Builder errorListener(CompilationErrorListener errorListener) {
            this.errorListener = errorListener;
            return this;
        }

        public CompilerFactory build() {
            return new CompilerFactory(
                    compileOnly,
                    saveTemps,
                    assemblerExecutable,
                    assemblerInclude,
                    errorListener
            );
        }
    }
}
