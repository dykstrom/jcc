# Build and code quality

Maven multi-module build. Shared plugin declarations live in the parent POM's
`<build><plugins>` so every module — and any future one — inherits them
automatically. Child POMs declare only module-specific plugins:
`antlr4-maven-plugin` in the grammar modules (assembunny, basic, col, tiny), and
`maven-dependency-plugin` / `maven-antrun-plugin` / `maven-assembly-plugin` plus a
`maven-jar-plugin` `Main-Class` override in `jcc-compiler`. When adding a module,
do not re-declare the shared plugins.

## Enforcer

`maven-enforcer-plugin` runs on every module with: `requireMavenVersion [3.9.0,)`,
`requireJavaVersion [21,)`, `banDuplicatePomDependencyVersions`.

## Checkstyle

Google-based config at `config/checkstyle/checkstyle.xml` (120-column lines,
4-space indentation). Bound to the `verify` phase and **blocking**
(`failOnViolation=true`): error-severity violations fail the build. Warning-severity
checks are only reported (the plugin's `violationSeverity` defaults to `error`).

Most checks are warning severity. A few are error severity: `NeedBraces`,
`OneStatementPerLine`, `CyclomaticComplexity` (max 10), `EmptyCatchBlock`.
`AvoidStarImport` is warning. `NeedBraces` is suppressed inside `equals()` methods
(IntelliJ generates brace-less guard clauses there).

`CyclomaticComplexity` errors in dispatch-heavy methods are fixed by replacing
`instanceof` chains with map dispatch keyed by exact class
(`BasicSemanticsParser.statementParsers`, `ColAsmFunctions`,
`DefaultAstExpressionOptimizer.binaryExpressionOptimizers`) or by extracting
helper methods. A pattern-matching `switch` does not reduce the score by
itself: checkstyle counts each `case` label as a decision point
(`switchBlockAsSingleDecisionPoint` is not set), so a 15-case switch scores 16.
Switches only fixed methods with few branches (`BasicCodeGenerator.containsReturn`,
`BasicLlvmCodeGenerator.updateStatement`). Class-keyed maps match exact
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
an ancestor of `origin/master`), builds the distribution on all five platforms with
`mvn -B package`, and publishes a GitHub Release named with the version only
(leading `v` stripped), attaching one archive per platform (`.zip` for Windows,
`.tar.gz` for Linux/macOS).

The release build uses `mvn package`, not `verify` — it skips the LLVM integration
tests, so no Clang setup is needed on the release runners.

Two couplings fail silently if broken:
- The maven-release-plugin `<tagNameFormat>v@{project.version}</tagNameFormat>` in
  the parent POM must match the workflow's `v*` trigger. Change one without the
  other and `release:perform` produces a tag that never triggers a release.
- Tag-triggered workflows run the workflow file as it exists at the tagged commit.
  `release.yml` must be present on `master` for a release to fire.

## LLVM test gating covers integration tests only

The `LLVM` JUnit tag is applied through the parent POM's `maven-failsafe-plugin`
configuration (`<groups>${failsafe.groups}</groups>`,
`<excludedGroups>${failsafe.excludedGroups}</excludedGroups>`), defaulting to
`failsafe.excludedGroups=LLVM` and flipped by the `llvm-tests` profile. The
`maven-surefire-plugin` block configures only `failIfNoSpecifiedTests` — no
`groups`, no `excludedGroups`. Surefire therefore runs every unit test in every
profile, and tagging a unit test `@Tag("LLVM")` does not exclude it from `mvn test`.

`JccTests` is untagged and drives the full `Jcc.run()` pipeline. Because the
default backend is LLVM, `-S` still shells out to `clang -S -O0`, so `mvn test`
invokes `clang` 8 times and 8 of the class's 16 tests fail if `clang` is missing
or exits non-zero. A unit test that must not need Clang has to avoid the assembler
step itself (for example `--backend FASM`, whose code generation runs on any
platform); the tag will not do it.

## Integration-test process harness

`ProcessUtils.setUpProcess` (in `jcc-base`) starts each compiled test program,
then drains its combined stdout/stderr on a background daemon thread while
`waitFor` blocks; `readOutput` returns the captured buffer afterward. The
draining must stay concurrent. If a change reads output only after `waitFor`
returns, a program that writes more than the OS pipe buffer (~4 KB on Windows
anonymous pipes) blocks on `write` and never exits, so `waitFor` times out and
the test fails with "Process is still alive". This surfaces only on the
Windows-only FASM run and the LLVM IT paths, neither on CI, and depends on
output volume — e.g. a `-print-gc` GC log crossing 4 KB. The same harness backs
`LlvmAssembler` and `FasmAssembler`.

A timeout in that harness is silent. `startAndWait` discards the boolean returned
by `process.waitFor(TIMEOUT_MILLIS, ...)` and returns the process under a comment
asserting it has ended; `readOutput` then joins the drain thread with a second,
independent `TIMEOUT_MILLIS` bound, which also returns normally on timeout because
the drain thread only ends at EOF. On the assembler path nothing checks either
result, so `LlvmAssembler.assemble` calls `exitValue()` on a process that is still
running and fails with `IllegalThreadStateException: process has not exited` after
roughly 20 seconds — the two 10-second bounds in series. The exception names
neither Clang nor a timeout. Tracked as issue #90.

This only bites slow machines: locally each `clang -S -O0` in `JccTests` returns in
tens of milliseconds, far under the bound. It has been observed as a flaky
`JccTests` failure on the Windows CI runner, on unrelated branches.

## Kotlin incremental compilation is disabled

The parent POM pins `kotlin.compiler.incremental` to `false`. When it was enabled,
scoped `mvn -pl ...` runs repeatedly left stale, partial test-classes behind
(tests failing with `NoClassDefFoundError` for a test base class, or test runs
silently discovering no tests at all). Do not re-enable it.
