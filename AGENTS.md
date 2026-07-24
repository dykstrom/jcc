# AGENTS.md

## What is this

JCC (Johan Compiler Collection) is a multi-module compiler infrastructure for compiling four toy languages — BASIC, Tiny, Assembunny, and COL — to native executables. It has two fully supported backends: FASM (Flat Assembler) for x86-64 assembly and an LLVM IR backend (via external Clang). Built with ANTLR4 for parsing; implemented in Java 21 and Kotlin.

Before creating a branch or opening a PR, read [CONTRIBUTING.md](CONTRIBUTING.md) for the branch conventions.

## Stack

| Piece | Choice |
|-------|--------|
| Language | Java 21 + Kotlin |
| Build | Maven (multi-module) |
| Parsing | ANTLR4 |
| CLI parsing | JCommander |
| Backends | FASM (x86-64 assembly, Windows-only); LLVM IR (via external Clang) |
| Runtime libs | `msvcrt.dll`, `libjccbas.dll` (FASM backend); `libjccbas.a` (BASIC), `libjcccol.a` (COL), plus `-lm` on Linux (LLVM backend) |
| Testing | JUnit 5 |

## Directory index

| Path | What's there |
|------|-------------|
| `jcc-base/` | Shared foundation: AST nodes, type system, symbol table, code-gen framework, compiler interfaces |
| `jcc-antlr4/` | ANTLR4 integration utilities |
| `jcc-llvm/` | LLVM IR backend infrastructure (operations, operands, code generators) |
| `jcc-basic/` | BASIC language module (grammar, parsers, type manager, code generators) |
| `jcc-tiny/` | Tiny language module |
| `jcc-assembunny/` | Assembunny language module |
| `jcc-col/` | COL language module |
| `jcc-compiler/` | CLI entry point (`Jcc`), `CompilerFactory`, `GenericCompiler`, assemblers |
| `fasm/` | Bundled Flat Assembler binary (Windows) and its license |
| `config/` | Checkstyle config (`config/checkstyle/checkstyle.xml`); SpotBugs runs blocking at `verify` with an exclude filter at `config/spotbugs/exclude.xml`; see `docs/system/build.md` |
| `docs/` | Durable project context. Sub-folder layout below shows where each kind of doc goes. |

```
docs/
├── system/         ← what the code does today (updated as code changes)
├── architecture/   ← what the system must do (updated when rules change)
├── adr/            ← architecture decisions (immutable once shipped)
├── reference/      ← long-form rationale (append-only)
└── working-notes/  ← research; NOT authoritative — rules live in architecture/ + adr/
```

`docs/` also holds existing hand-written docs (`Architecture.md`, `Arrays.md`, `GarbageCollector.md`, `LLVM.md`, `diagrams/`) and `docs/languages/` — user-facing language guides linked from the README. Note the two parallel language-doc sets: `docs/languages/<lang>.md` is the user-facing guide; `docs/system/<lang>-language.md` records agent-facing gotchas. Keep them distinct.

## Commands

| What | Command |
|------|---------|
| Build all modules | `mvn clean install` |
| All unit tests | `mvn test` (excludes LLVM tests) |
| Single unit test class | `mvn -Dtest=BasicTypeManagerTest test` |
| Single unit test method | `mvn -Dtest=BasicTypeManagerTest#testGetType test` |
| All integration tests | `mvn verify` (excludes LLVM tests) |
| Single integration test | `mvn -Dit.test=BasicCompilerIT verify` |
| All tests incl. LLVM | `mvn -P llvm-tests verify` (requires Clang 20+) |
| Run compiler | `java -jar jcc-compiler/target/jcc-compiler-*.jar program.bas` — needs its dependency jars as siblings; on `NoClassDefFoundError`, run `mvn -q -pl jcc-compiler dependency:copy-dependencies -DoutputDirectory='${project.build.directory}' -DincludeScope=runtime` |
| LLVM backend | add `--backend LLVM --library-path jcc-compiler/target` (where `libjccbas.a`/`libjcccol.a` are built) |
| Regression test | `./regression_test` — compiles BASIC examples with `-S`, diffs assembly against references in `jcc-compiler/src/test/resources/`. Windows-only; on other platforms use the Maven tests |

This repo has many unit and integration tests; a full `mvn verify` is slow. Work outward from the smallest scope: first run the single unit test for the code you changed (`-Dtest=...`), then the rest of that module's unit tests, then any affected integration tests (`-Dit.test=...`), and only then a full build. Widen the scope only after the narrower one passes.

For Java symbol navigation (go-to-definition, find-references, hover) and type/import diagnostics, prefer the LSP tool over `grep`/file reads — it resolves symbols through the type system and across Maven modules, and catches signature/import errors without a `mvn compile` cycle. (LSP covers Java only, not the Kotlin sources.)

## Gotchas

- Only `fasm.exe` (assembling and running the output) is Windows-only; FASM *code generation* runs on any platform. Reproduce FASM codegen bugs off Windows with `java -jar jcc-compiler/target/jcc-compiler-*.jar -S program.bas`, which writes the `.asm` and stops before assembling. The FASM backend is slated for future deprecation in favor of the LLVM backend.
- FASM compile-and-run ITs (`*CompileAndRunIT`) are annotated `@EnabledOnOs(OS.WINDOWS)` — they need `fasm.exe` to assemble and run, so they are silently skipped everywhere else. A change that breaks FASM compilation (e.g. a shared-semantics tightening) therefore passes a local `mvn verify` and the Linux/macOS CI legs; only the Windows CI leg catches it. The LLVM ITs (`*LlvmCompileAndRunIT`) are not OS-gated.
- The LLVM backend is fully supported across all four languages (BASIC, Tiny, Assembunny, COL). A few narrow BASIC gaps remain (e.g. `SLEEP` has no LLVM IT; `LINE INPUT;` `inhibitNewline` has no effect) — see `docs/system/standard-libraries.md`.
- Integration tests in `jcc-compiler` compile via the test classpath, resolved from the local Maven repo — after changing another module, refresh the repo before running ITs or they exercise stale code. The only refresh observed to be reliable is `mvn clean install -DskipTests`: both `-pl jcc-compiler -am` and an incremental `mvn -pl <module> -am install` have produced jars missing changes that compiled minutes earlier. Manual `java -jar` runs additionally need the `dependency:copy-dependencies` refresh from Commands (and again after every `clean`). Stale jars show up as phantom failures: fixes that don't take effect, or ITs failing on features that exist in source.
