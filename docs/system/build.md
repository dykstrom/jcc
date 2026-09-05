# Build and code quality

Maven multi-module build. Shared plugin declarations live in the parent POM's
`<build><plugins>` so every module — and any future one — inherits them
automatically. Child POMs declare only module-specific plugins:
`antlr4-maven-plugin` in the grammar modules (assembunny, basic, col, tiny), and
`maven-dependency-plugin` / `maven-antrun-plugin` / `maven-assembly-plugin` plus a
`maven-jar-plugin` `Main-Class` override in `jcc-compiler`. When adding a module,
do not re-declare the shared plugins.

## Enforcer

`maven-enforcer-plugin` runs on every module with seven rules:
`requireMavenVersion [3.9.0,)`, `requireJavaVersion [21,)`,
`banDuplicatePomDependencyVersions`, `reactorModuleConvergence`,
`requireProfileIdsExist`, `requireNoRepositories`, `dependencyConvergence`.

`reactorModuleConvergence` makes a phase-based build of a single module fail:
`mvn -pl jcc-compiler test` stops at "Rule 3 ... failed with message: Module
parents have been found which could not be found in the reactor", before
compiling anything. The message names neither the enforcer's constraint nor the
fix. Run phase builds from the root reactor (`mvn -Dtest=JccTests test`), or add
`-am` so the parent POM is in the reactor. Invoking a plugin goal directly is
unaffected — `mvn -pl jcc-compiler dependency:copy-dependencies` and
`mvn -pl jcc-compiler failsafe:integration-test ...` both work, because no
lifecycle phase runs, so the enforcer's `enforce` execution never fires.

## Use `install`, not `verify`

`install` runs everything `verify` does — failsafe's `integration-test` and `verify`
goals, `checkstyle:check` and `spotbugs:check` all bind at or before the `verify`
phase — and then writes each module's jar to `~/.m2`. That last step matters:
`jcc-compiler`'s integration tests resolve the other modules from `~/.m2`, and so does
the `dependency:copy-dependencies` refresh a hand-run `java -jar` needs. Stopping at
`verify` leaves both resolving the previous `install`'s jars, so the full test suite can
pass on a change the jar does not contain — seen as the compiler printing a diagnostic
that had already been reworded, immediately after `mvn clean verify` reported success.

The CI workflows run `mvn -B verify`, which does not hit this: a fresh checkout has no
earlier `install` to go stale against, and CI never runs the jar by hand.

## Checkstyle

Google-based config at `config/checkstyle/checkstyle.xml` (120-column lines,
4-space indentation). Bound to the `verify` phase and **blocking**
(`failOnViolation=true`): error-severity violations fail the build. Warning-severity
checks are only reported (the plugin's `violationSeverity` defaults to `error`).

Most checks are warning severity. A few are error severity: `NeedBraces`,
`OneStatementPerLine`, `CyclomaticComplexity` (max 10), `EmptyCatchBlock`,
`RedundantImport`, `UnusedImports`. `NeedBraces` is suppressed inside `equals()` methods
(IntelliJ generates brace-less guard clauses there).

The two import checks are error severity while `AvoidStarImport` is only a warning: an
unused or duplicated import is unambiguous and mechanical to remove, whereas the star
imports are deliberate (`CompilerFactory` resolves `CodeGenerator` through one). Note
what they do *not* cover: `includeTestSourceDirectory=false`, and Checkstyle reads Java
only, so unused imports in the Kotlin test sources are caught by neither. `UnusedImports`
leaves `processJavadoc` at its default of `true`, so an import referenced only from a
`{@link}` counts as used.

`CyclomaticComplexity` errors in dispatch-heavy methods are fixed by replacing
`instanceof` chains with map dispatch keyed by exact class
(`BasicSemanticsParser.statementParsers`, `ColFunctions`,
`DefaultAstExpressionOptimizer.binaryExpressionOptimizers`) or by extracting
helper methods. A pattern-matching `switch` does not reduce the score by
itself: checkstyle counts each `case` label as a decision point
(`switchBlockAsSingleDecisionPoint` is not set), so a 15-case switch scores 16.
Switches only fixed methods with few branches (`BasicCodeGenerator.containsReturn`,
`BasicCodeGenerator.updateStatement`). Class-keyed maps match exact
classes only: register every concrete subclass (e.g. all three
`Def*Statement` classes), since an unregistered type falls through to the
default behavior without an error.

The plugin resolves the config via
`${maven.multiModuleProjectDirectory}/config/checkstyle/checkstyle.xml`. The empty
`.mvn/` directory at the repo root exists only to anchor that property so the path
resolves from any module — do not delete it.

## SpotBugs

`spotbugs-maven-plugin` runs the `check` goal at the `verify` phase on every module,
with `effort=Max`, `threshold=Medium`, core detectors only. It is **blocking**
(`failOnError=true`): any finding not suppressed by the exclude filter fails the build.
`includeTests=false`, so only main classes (all Java; Kotlin is test-only) are analyzed.

Because the build is blocking, the exclude filter at `config/spotbugs/exclude.xml`
(wired via `excludeFilterFile` in the parent POM) carries the project's accepted
exceptions. Adding hand-written code that trips a new pattern will fail the build until
the bug is fixed or the pattern is deliberately added to the filter. Current exclusions:

- All findings in the ANTLR-generated lexers/parsers/visitors
  (`<Grammar>Lexer/Parser/Visitor/BaseVisitor` under `target/generated-sources`) —
  not hand-written source.
- `OBL_UNSATISFIED_OBLIGATION` on `CompilerFactory` — the source stream's ownership is
  transferred to the returned compiler; `create` only guards the construction-failure
  path, and SpotBugs cannot follow the transfer.
- `EQ_DOESNT_OVERRIDE_EQUALS` on `ArrayDeclaration` — declarations are intentionally
  identified by name+type; subscripts are rewritten across compiler phases, so they are
  not part of identity.
- `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` (project-wide) — AST nodes, symbol tables, code
  containers, and function metadata pass collection/array references around rather than
  defensively copying them. Intentional in a single-threaded compiler; copying at every
  getter/constructor would add cost and churn for no real safety benefit.
- `ME_ENUM_FIELD_SETTER` (project-wide) — the global-options singletons (`GcOptions`,
  `OptimizationOptions`) expose setters that mutate their fields, by design.
- `EQ_COMPARETO_USE_OBJECT_EQUALS` on `se.dykstrom.jcc.common.types.*` — the numeric
  type classes (`F32`, `F64`, `I8`, `I32`, `I64`) are field-less singletons whose
  inherited `compareTo` (from `FloatType`/`IntegerType`) orders by `bits()`, family-scoped
  with unique widths, so identity `equals` is already consistent with it.

These exclusions took the analysis from a ~155-finding baseline to zero, after which the
build was switched from report-only to blocking.

## Releases

Releases are cut by pushing a `v<version>` tag (e.g. `v0.11.0`) on `master`.
`.github/workflows/release.yml` then runs a `verify-branch` guard (the tag must be
an ancestor of `origin/master`), builds the distribution on all six platforms with
`mvn -B package`, and publishes a GitHub Release named with the version only
(leading `v` stripped), attaching one archive per platform (`.zip` for Windows,
`.tar.gz` for Linux/macOS).

The release build uses `mvn package`, not `verify`. Failsafe binds to
`integration-test`, which runs after `package`, so the release runners never compile
or run a test program and need no Clang.

Two couplings fail silently if broken:
- The maven-release-plugin `<tagNameFormat>v@{project.version}</tagNameFormat>` in
  the parent POM must match the workflow's `v*` trigger. Change one without the
  other and `release:perform` produces a tag that never triggers a release.
- Tag-triggered workflows run the workflow file as it exists at the tagged commit.
  `release.yml` must be present on `master` for a release to fire.

## A same-repo pull request's checks are skipped by design

`linux.yml`, `macos.yml` and `windows.yml` trigger on `push` to every branch and on
`pull_request` to `master` and `dev`. Each build job then guards itself with

    if: github.event_name != 'pull_request' || github.event.pull_request.head.repo.full_name != github.repository

so a pull request from a branch in this repository builds once, on the push, not twice.
The `pull_request` run still appears, and completes with conclusion `skipped`.

So `gh run list --branch <branch>` shows six rows per commit: three `push` runs carrying
the result, and three `pull_request` runs that are always `skipped`. Read the `push` rows.
Filter on `event == "push"` when waiting for a branch to go green, or the three skipped
runs answer first and look like a finished build. A pull request from a fork is the
opposite case: it has no push run in this repository, and its `pull_request` runs are the
result.

## Clang is required from the integration-test phase on

Failsafe runs every integration test, in every build. There is no tag and no profile
gating them — the `LLVM` JUnit tag and the `llvm-tests` profile existed to separate
the LLVM tests from the Windows-only FASM ones, and went with the FASM backend. So
`mvn verify` and `mvn install` need Clang 20+ on the path, while `mvn test` and
`mvn package` do not.

Surefire has no `groups`/`excludedGroups` configuration, so a unit test that must not
need Clang has to avoid the assembler step itself. `JccTests` drives the full
`Jcc.run()` pipeline, so every one of its tests passes `-fsyntax-only`, which stops
after semantic analysis and invokes no external tool. Its assertions are JCC
diagnostics (from the semantics parser) and JCC's own CLI output, none of which
need a toolchain. Keep it that way: no test in `JccTests` may run `clang`.

`-S` also invokes no external tool: `Assembler.assemble` writes the `.ll` file and
returns when `compileOnly` is set. A CLI test that genuinely needs the toolchain
belongs in `JccIT` (see `JccIT.optionOutputFilename`, which covers `-o` end to end).

## Windows needs a UCRT mingw-w64 Clang

An MSVC-targeting Clang — what Chocolatey's `llvm` package and
`egor-tensin/setup-clang` install — cannot link jcc's output, and says so
unhelpfully: `lld-link: error: could not open 'jccbas.lib'` (under MSVC
`-ljccbas` means `jccbas.lib`, but the published archive is `libjccbas.a`) and
`lld-link: error: undefined symbol: scanf` (the UCRT provides `scanf` as a
header inline, not an exported symbol, and jcc emits a bare `declare`).

The mingw environment must also be UCRT-based, not msvcrt. libjccbas's README
states that its Windows `libjccbas.a` is built with UCRT for LLVM/Clang, and
that only the msvcrt-linked `.dll` was for FASM. On MSYS2 `MINGW64`, `printf`
writes `1.#INF00` where C99 says `inf`.

CI installs it through `msys2/setup-msys2` — `UCRT64` with
`mingw-w64-ucrt-x86_64-clang` on x86-64, `CLANGARM64` with
`mingw-w64-clang-aarch64-clang` on arm64, the same environments libjcccol's own
CI builds those platforms with. `docs/LLVM.md` points users at llvm-mingw's
ucrt releases.

## Global options leak between tests in a shared JVM

`OptimizationOptions` and `GcOptions` are enum singletons (`INSTANCE`) with mutable fields.
`OptimizationOptions.INSTANCE.level` defaults to 0 and gates the whole AST optimizer:
`DefaultAstOptimizer.program` returns the program unchanged unless the level is at least 1.

Two kinds of test set it. A few set the field directly in `@BeforeEach`
(`DefaultAstOptimizerTests`, `BasicAstOptimizerTests`, `BasicCodeGeneratorOptimizationTests`,
`BasicCodeGeneratorOptimizationTests`). The ones that matter more set it *indirectly*: `Jcc`
itself assigns the level from the `-O` flag while parsing arguments, so every integration test that
compiles with `-O1` — `BasicCompileAndRunOptimizationIT`, `TinyCompileAndRunIT` — leaves the
optimizer enabled behind it. Each of those classes now resets the level in an `@AfterEach`; for the
integration tests the reset lives once in `AbstractIntegrationTests`, so it also covers any future
IT that passes an `-O` flag.

Maven hides the leak either way. Surefire forks a JVM per module, and its default includes
(`**/*Tests.java`) leave the `*IT` classes to failsafe, which forks again — so a level set by a
`jcc-compiler` IT can never reach a `jcc-basic` unit test. IntelliJ IDEA runs a whole-project
selection in one JVM and includes the `*IT` classes. A test that depends on the optimizer being off
therefore passed under `mvn clean verify` and failed in IDEA on identical code: that is how
`AssembunnyCompilerTests.shouldCompileOk` failed, because the optimizer rewrites `cpy` (see
`code-generation.md`). `mvn` cannot reproduce it; one JVM over all modules' test classpaths can.

So a green `mvn test` does not mean the suite is order-independent, and a test that reaches a global
option through `Jcc` rather than by assignment leaks it just the same.

## Failsafe reports outlive the run that wrote them

`target/failsafe-reports/<class>.txt` is written only when that class is selected and runs, and no
run deletes an earlier file. A scoped run — `-Dit.test=...` — therefore leaves the reports of every
class it did not select in place, so a green build without `clean` can sit next to failing reports
from an earlier run. Take the result from Maven's summary, not from aggregating the report files.

## Integration-test process harness

`ProcessUtils.setUpProcess` (in `jcc-base`) starts each compiled test program,
then drains its combined stdout/stderr on a background daemon thread while
`waitFor` blocks; `readOutput` returns the captured buffer afterward. The
draining must stay concurrent. If a change reads output only after `waitFor`
returns, a program that writes more than the OS pipe buffer (~4 KB on Windows
anonymous pipes) blocks on `write` and never exits, so `waitFor` times out and
the test fails with "Process is still alive". This surfaces only on the
IT paths, not on CI, and depends on output volume — e.g. a `-print-gc` GC log
crossing 4 KB. The same harness backs `Assembler`.

A timeout in that harness must not be silent. `startAndWait` bounds the wait at
`PROCESS_TIMEOUT_MILLIS` (30 s); on expiry it destroys the process *and its
descendants* — a hung tool may itself be blocked on a child, and
`destroyForcibly` alone does not reach one — drops the output capture, and throws
`TimeoutException`. Cleanup happens there because no `Process` is returned, so the
caller's `finally { tearDownProcess }` never runs. `Assembler`
translates it into a `JccException` naming the configured executable
and the bound (`clang timed out after 30 seconds`), reported as `jcc: error: …` with
exit code 1. `readOutput` keeps a separate, shorter `DRAIN_TIMEOUT_MILLIS` (10 s)
join bound, which is safe because the process has provably exited by then, so EOF
arrives at once.

A `Process` returned by `setUpProcess` is thus guaranteed to have exited, and callers
rely on that: they call `exitValue()` directly, with no liveness check. Do not
discard the `waitFor` result (issue #90).

`AbstractIntegrationTests.assertOutput` compares line by line after
`dropLastWhile { it.isEmpty() }`, so trailing empty lines are invisible to it: a test whose
program's last output is a blank line fails with "Number of lines differ" no matter what it
expects. Order the program's output so a blank line is never last. Each comparison is
`startsWith`, not equality, so an expected line matches any longer actual line with that prefix.

`runAndAssertSuccess(input, …)` writes its stdin with `Files.write(path, List<String>)`, which
newline-terminates every element, so it cannot express input whose final line has no trailing
newline. A read-loop test written with it never exercises the unterminated-final-line case, and
passes regardless. Use `runAndAssertSuccessWithRawInput`, which takes the whole stdin as one
string written with `Files.writeString`.

## Examples are packaged, never compiled

`jcc-compiler/pom.xml` copies `src/examples` into the distribution as a resource
(`<directory>src/examples</directory>`, target path `../examples`). Nothing compiles them: no
surefire or failsafe test reads that folder, and `./regression_test` is broken until it is
rewritten to diff `.ll` files. So `docs/system/col-language.md`'s claim that every example
"must compile" is a convention, not something enforced — an example can rot
without any build failing. Verify a changed or added example by hand with the `Run compiler` command
in `AGENTS.md`. The COL examples `strings.col` and `echo.col` are the most exposed, being the only
examples that depend on libjcccol's string functions.

## Kotlin incremental compilation is disabled

The parent POM pins `kotlin.compiler.incremental` to `false`. When it was enabled,
scoped `mvn -pl ...` runs repeatedly left stale, partial test-classes behind
(tests failing with `NoClassDefFoundError` for a test base class, or test runs
silently discovering no tests at all). Do not re-enable it.
