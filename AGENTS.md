# AGENTS.md

## What is this

JCC (Johan Compiler Collection) is a multi-module compiler infrastructure for compiling four toy languages — BASIC, Tiny, Assembunny, and COL — to native executables. It supports two backends: FASM (Flat Assembler) for x86-64 assembly and an experimental LLVM IR backend. Built with ANTLR4 for parsing; implemented in Java 21 and Kotlin.

## Stack

| Piece | Choice |
|-------|--------|
| Language | Java 21 + Kotlin |
| Build | Maven (multi-module) |
| Parsing | ANTLR4 |
| CLI parsing | JCommander |
| Backends | FASM (x86-64 assembly, Windows-only); LLVM IR (via external Clang) |
| Runtime libs | `msvcrt.dll`, `libjccbas.dll` (FASM backend); `libjccbas.a` (BASIC), `libjcccol.a` (COL) (LLVM backend) |
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
| `config/` | Checkstyle config (`config/checkstyle/checkstyle.xml`); see `docs/system/build.md` |
| `docs/` | Durable project context. Sub-folder layout below shows where each kind of doc goes. |

```
docs/
├── system/         ← what the code does today (updated as code changes)
├── architecture/   ← what the system must do (updated when rules change)
├── adr/            ← architecture decisions (immutable once shipped)
├── reference/      ← long-form rationale (append-only)
└── working-notes/  ← research; NOT authoritative — rules live in architecture/ + adr/
```

`docs/` also holds existing hand-written docs (`Architecture.md`, `Arrays.md`, `GarbageCollector.md`, `LLVM.md`, `diagrams/`).

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

- The FASM backend only runs on Windows (`fasm.exe` is Windows-only) and is slated for future deprecation in favor of the LLVM backend.
- LLVM backend support is experimental; BASIC's LLVM support is still a work in progress.
- Integration tests in `jcc-compiler` compile via the test classpath, resolved from the local Maven repo — after changing another module, run `mvn -pl <module> install -DskipTests` before running ITs, or they exercise stale code (`-pl jcc-compiler -am` has been observed to be insufficient). Manual `java -jar` runs additionally need the `dependency:copy-dependencies` refresh from Commands. Stale jars show up as phantom failures: fixes that don't take effect, or ITs failing on features that exist in source.
