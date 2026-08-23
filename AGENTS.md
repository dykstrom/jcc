# AGENTS.md

## What is this

JCC (Johan Compiler Collection) is a multi-module compiler infrastructure for compiling four toy languages — BASIC, Tiny, Assembunny, and COL — to native executables. It emits LLVM IR and compiles it with an external Clang. Built with ANTLR4 for parsing; implemented in Java 21 and Kotlin.

Before creating a branch or opening a PR, read [CONTRIBUTING.md](CONTRIBUTING.md) for the branch conventions.

## Stack

| Piece | Choice |
|-------|--------|
| Language | Java 21 + Kotlin |
| Build | Maven (multi-module) |
| Parsing | ANTLR4 |
| CLI parsing | JCommander |
| Backend | LLVM IR, compiled by an external Clang 20+ |
| Runtime libs | `libjccbas.a` (BASIC), `libjcccol.a` (COL), plus `-lm` on Linux |
| Testing | JUnit 5 |

## Directory index

| Path | What's there |
|------|-------------|
| `jcc-base/` | Shared foundation: AST nodes, type system, symbol table, code-gen framework, compiler interfaces |
| `jcc-antlr4/` | ANTLR4 integration utilities |
| `jcc-llvm/` | LLVM IR infrastructure (operations, operands, code generators) |
| `jcc-basic/` | BASIC language module (grammar, parsers, type manager, code generators) |
| `jcc-tiny/` | Tiny language module |
| `jcc-assembunny/` | Assembunny language module |
| `jcc-col/` | COL language module |
| `jcc-compiler/` | CLI entry point (`Jcc`), `CompilerFactory`, `GenericCompiler`, `Assembler` |
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

`docs/` also holds existing hand-written docs (`Architecture.md`, `Arrays.md`, `GarbageCollection.md`, `LLVM.md`, `diagrams/`) and `docs/languages/` — user-facing language guides linked from the README. Note the two parallel language-doc sets: `docs/languages/<lang>.md` is the user-facing guide; `docs/system/<lang>-language.md` records agent-facing gotchas. Keep them distinct.

## Commands

| What | Command |
|------|---------|
| Build all modules | `mvn clean install` |
| All unit tests | `mvn test` (no Clang needed) |
| Single unit test class | `mvn -Dtest=BasicTypeManagerTest test` |
| Single unit test method | `mvn -Dtest=BasicTypeManagerTest#testGetType test` |
| All integration tests | `mvn install` (requires Clang 20+) |
| Single integration test | `mvn -Dit.test=BasicCompileAndRunIT install` |
| Run compiler | `java -jar jcc-compiler/target/jcc-compiler-*.jar --library-path jcc-compiler/target program.bas` — `--library-path` points at the directory holding `libjccbas.a`/`libjcccol.a`. Needs its dependency jars as siblings; on `NoClassDefFoundError`, run `mvn -q -pl jcc-compiler dependency:copy-dependencies -DoutputDirectory='${project.build.directory}' -DincludeScope=runtime` |
| Regression test | `./regression_test` is broken. It still passes `--backend FASM` and diffs `.asm` files whose references were deleted with the FASM backend. It is kept, unchanged, until someone rewrites it to diff `.ll` files |

Use `install`, not `verify`. Both run the same checks — failsafe, Checkstyle and SpotBugs all bind at or before the `verify` phase — but only `install` refreshes `~/.m2`, which the `jcc-compiler` integration tests and any hand-run `java -jar` resolve the other modules from. Stopping at `verify` leaves them on the previous `install`'s jars, so a green build can sit on top of a jar that does not contain the change. See `docs/system/build.md`.

This repo has many unit and integration tests; a full `mvn install` is slow. Work outward from the smallest scope: first run the single unit test for the code you changed (`-Dtest=...`), then the rest of that module's unit tests, then any affected integration tests (`-Dit.test=...`), and only then a full build. Widen the scope only after the narrower one passes.

For Java symbol navigation (go-to-definition, find-references, hover) and type/import diagnostics, prefer the LSP tool over `grep`/file reads — it resolves symbols through the type system and across Maven modules, and catches signature/import errors without a `mvn compile` cycle. (LSP covers Java only, not the Kotlin sources.)

## Gotchas

- Clang is needed from the `integration-test` phase on: `mvn verify` and `mvn install` require it, `mvn test` and `mvn package` do not. Surefire has no tag filtering, so a unit test must avoid the assembler step itself. `JccTests` drives the full `Jcc.run()` pipeline but passes `-fsyntax-only`, which stops after semantic analysis. Keep it that way; a CLI test that genuinely needs Clang belongs in `JccIT`. See `docs/system/build.md`.
- All four languages (BASIC, Tiny, Assembunny, COL) are fully supported. A few narrow BASIC gaps remain (e.g. `SLEEP` has no IT; `LINE INPUT;` `inhibitNewline` has no effect) — see `docs/system/standard-libraries.md`.
- All four grammars are ANTLR *combined* grammars. Adding a `@lexer::members` block collides with an unqualified `@members` block: ANTLR reports `error(94): redefinition of members action`, generates the parser but *not* the lexer, and the build then fails with dozens of "cannot find symbol" errors inside generated code plus "cannot find symbol: class `<Lang>Lexer`" — none of which name the real cause. Qualify the existing block as `@parser::members`.
- Hand-running the compiler litters the working tree. With no `-o` the executable goes to the **current directory** as `a.out` (gitignored), not next to the source. `-S` writes `<source>.ll` *next to the source*, which is not gitignored. Do not add a blanket `*.ll` ignore — spike `.ll` files under `docs/working-notes/` are tracked, so a blanket pattern would silently hide new ones. Compiling an example in place therefore leaves an untracked `.ll` beside it.
- Integration tests in `jcc-compiler` compile via the test classpath, resolved from the local Maven repo — after changing another module, refresh the repo before running ITs or they exercise stale code. The only refresh observed to be reliable is `mvn clean install -DskipTests`: both `-pl jcc-compiler -am` and an incremental `mvn -pl <module> -am install` have produced jars missing changes that compiled minutes earlier. Manual `java -jar` runs additionally need the `dependency:copy-dependencies` refresh from Commands (and again after every `clean`). Stale jars show up as phantom failures: fixes that don't take effect, or ITs failing on features that exist in source. Every worktree shares one `~/.m2`, so the last `install` wins across checkouts: a `jcc-compiler` jar built in one worktree loads whichever module jars another worktree installed most recently, with no error. Comparing two branches' output means installing each in turn, not building both and running them side by side.
