# AGENTS.md

## What is this

JCC (Johan Compiler Collection) is a multi-module compiler infrastructure for compiling four toy languages — BASIC, Tiny, Assembunny, and COL — to native executables. It has two backends: an LLVM IR backend (via external Clang), which is the default, and FASM (Flat Assembler) for x86-64 assembly, which is deprecated and will be removed in a future release (select it with `--backend FASM`). Built with ANTLR4 for parsing; implemented in Java 21 and Kotlin.

Before creating a branch or opening a PR, read [CONTRIBUTING.md](CONTRIBUTING.md) for the branch conventions.

## Stack

| Piece | Choice |
|-------|--------|
| Language | Java 21 + Kotlin |
| Build | Maven (multi-module) |
| Parsing | ANTLR4 |
| CLI parsing | JCommander |
| Backends | LLVM IR (via external Clang, default); FASM (x86-64 assembly, Windows-only, deprecated) |
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
| Regression test | `./regression_test` — compiles BASIC examples with `-S`, diffs FASM assembly against references in `jcc-compiler/src/test/resources/`. Windows-only; on other platforms use the Maven tests. The references are stale: they date from 0.8.2, and all 18 differ from current output even with the 3-line header stripped as the script does, while `cursor.bas` and `reverse.bas` have no reference at all. Until they are regenerated the script cannot detect a regression — treat its diffs as expected |

This repo has many unit and integration tests; a full `mvn verify` is slow. Work outward from the smallest scope: first run the single unit test for the code you changed (`-Dtest=...`), then the rest of that module's unit tests, then any affected integration tests (`-Dit.test=...`), and only then a full build. Widen the scope only after the narrower one passes.

For Java symbol navigation (go-to-definition, find-references, hover) and type/import diagnostics, prefer the LSP tool over `grep`/file reads — it resolves symbols through the type system and across Maven modules, and catches signature/import errors without a `mvn compile` cycle. (LSP covers Java only, not the Kotlin sources.)

## Gotchas

- LLVM is the default backend; the FASM backend is deprecated and requires an explicit `--backend FASM` (which also prints a deprecation warning to stdout). Note `-S program.bas` now emits LLVM IR (`.ll`) by default — add `--backend FASM` to write FASM `.asm` instead.
- Only `fasm.exe` (assembling and running the output) is Windows-only; FASM *code generation* runs on any platform. Reproduce FASM codegen bugs off Windows with `java -jar jcc-compiler/target/jcc-compiler-*.jar --backend FASM -S program.bas`, which writes the `.asm` and stops before assembling.
- FASM compile-and-run ITs (`*CompileAndRunIT`) are annotated `@EnabledOnOs(OS.WINDOWS)` — they need `fasm.exe` to assemble and run, so they are silently skipped everywhere else. A change that breaks FASM compilation (e.g. a shared-semantics tightening) therefore passes a local `mvn verify` and the Linux/macOS CI legs; only the Windows CI leg catches it. The LLVM ITs (`*LlvmCompileAndRunIT`) are not OS-gated. To exercise their sources off Windows, deactivate the OS gate: `mvn -pl jcc-compiler failsafe:integration-test -Dit.test='Basic*CompileAndRun*IT' -Djunit.jupiter.conditions.deactivate='*'`. Every source is then parsed and semantically checked before the run fails at `Cannot run program "../fasm/FASM.EXE"`, so grepping the output for `extraneous input`, `no viable alternative`, `mismatched input` or `unexpected '` catches a front-end regression locally. It proves nothing about FASM code generation or program output — only that the sources still compile as far as the assembler.
- "Excludes LLVM tests" in Commands means the `LLVM`-tagged integration tests only. The tag is wired on failsafe, not surefire, so tagging a unit test `@Tag("LLVM")` does not exclude it from `mvn test` — a unit test must avoid the assembler step itself instead. `mvn test` needs no Clang: `JccTests` drives the full `Jcc.run()` pipeline but passes `-fsyntax-only`, which stops after semantic analysis. Keep it that way; a CLI test that genuinely needs Clang belongs in `JccIT`, tagged `@Tag("LLVM")`. See `docs/system/build.md`.
- The LLVM backend is fully supported across all four languages (BASIC, Tiny, Assembunny, COL). A few narrow BASIC gaps remain (e.g. `SLEEP` has no LLVM IT; `LINE INPUT;` `inhibitNewline` has no effect) — see `docs/system/standard-libraries.md`.
- All four grammars are ANTLR *combined* grammars. Adding a `@lexer::members` block collides with an unqualified `@members` block: ANTLR reports `error(94): redefinition of members action`, generates the parser but *not* the lexer, and the build then fails with dozens of "cannot find symbol" errors inside generated code plus "cannot find symbol: class `<Lang>Lexer`" — none of which name the real cause. Qualify the existing block as `@parser::members`.
- Integration tests in `jcc-compiler` compile via the test classpath, resolved from the local Maven repo — after changing another module, refresh the repo before running ITs or they exercise stale code. The only refresh observed to be reliable is `mvn clean install -DskipTests`: both `-pl jcc-compiler -am` and an incremental `mvn -pl <module> -am install` have produced jars missing changes that compiled minutes earlier. Manual `java -jar` runs additionally need the `dependency:copy-dependencies` refresh from Commands (and again after every `clean`). Stale jars show up as phantom failures: fixes that don't take effect, or ITs failing on features that exist in source. Every worktree shares one `~/.m2`, so the last `install` wins across checkouts: a `jcc-compiler` jar built in one worktree loads whichever module jars another worktree installed most recently, with no error. Comparing two branches' output means installing each in turn, not building both and running them side by side.
