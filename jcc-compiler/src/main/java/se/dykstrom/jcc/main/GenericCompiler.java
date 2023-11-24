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

import java.io.InputStream;
import java.nio.file.Path;

import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.compiler.CodeGenerator;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.SyntaxParser;
import se.dykstrom.jcc.common.intermediate.IntermediateProgram;
import se.dykstrom.jcc.common.optimization.AstOptimizer;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * A generic compiler that contains very little logic. It calls the different steps
 * in the compilation pipeline in order, but knows very little about what they do.
 */
public class GenericCompiler implements Compiler {

    private final InputStream inputStream;
    private final Path sourcePath;
    private final Path outputPath;
    private final SyntaxParser syntaxParser;
    private final SemanticsParser<?> semanticsParser;
    private final CodeGenerator codeGenerator;
    private final AstOptimizer astOptimizer;
    private final Assembler assembler;

    private GenericCompiler(final Builder builder) {
        this.inputStream = requireNonNull(builder.inputStream);
        this.sourcePath = requireNonNull(builder.sourcePath);
        this.outputPath = requireNonNull(builder.outputPath);
        this.syntaxParser = requireNonNull(builder.syntaxParser);
        this.semanticsParser = requireNonNull(builder.semanticsParser);
        this.astOptimizer = requireNonNull(builder.astOptimizer);
        this.codeGenerator = requireNonNull(builder.codeGenerator);
        this.assembler = requireNonNull(builder.assembler);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Path getSourcePath() {
        return sourcePath;
    }

    @Override
    public IntermediateProgram compile() {
        log("  Parsing syntax");
        final Program parsedProgram = syntaxParser.parse(inputStream).withSourcePath(sourcePath);

        log("  Checking semantics");
        final Program checkedProgram = semanticsParser.parse(parsedProgram);

        log("  Optimizing");
        final Program optimizedProgram = astOptimizer.program(checkedProgram);

        log("  Generating intermediate code");
        final IntermediateProgram generatedProgram = codeGenerator.generate(optimizedProgram);
        //generatedProgram.lines().forEach(line -> System.out.println(line.toText()));

        log("Assembling output");
        assembler.assemble(generatedProgram, sourcePath, outputPath);

        return generatedProgram;
    }

    public static class Builder {

        private InputStream inputStream;
        private Path sourcePath;
        private Path outputPath;
        private SyntaxParser syntaxParser;
        private SemanticsParser<?> semanticsParser;
        private CodeGenerator codeGenerator;
        private Assembler assembler;
        private AstOptimizer astOptimizer;

        public Builder inputStream(final InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder sourcePath(final Path sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder outputPath(final Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder syntaxParser(final SyntaxParser syntaxParser) {
            this.syntaxParser = syntaxParser;
            return this;
        }

        public Builder semanticsParser(final SemanticsParser<?> semanticsParser) {
            this.semanticsParser = semanticsParser;
            return this;
        }

        public Builder astOptimizer(final AstOptimizer astOptimizer) {
            this.astOptimizer = astOptimizer;
            return this;
        }

        public Builder codeGenerator(final CodeGenerator codeGenerator) {
            this.codeGenerator = codeGenerator;
            return this;
        }

        public Builder assembler(final Assembler assembler) {
            this.assembler = assembler;
            return this;
        }

        public GenericCompiler build() {
            return new GenericCompiler(this);
        }
    }
}
