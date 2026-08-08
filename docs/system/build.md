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

A unit test that must not need Clang therefore has to avoid the assembler step
itself; the tag will not do it. `JccTests` is untagged and drives the full
`Jcc.run()` pipeline, and because the default backend is LLVM, `-S` shells out to
`clang -S -O0` — so every one of its tests passes `-fsyntax-only`, which stops
after semantic analysis and invokes no external tool. Its assertions are JCC
diagnostics (from the semantics parser) and JCC's own CLI output, none of which
need a toolchain. Keep it that way: no test in `JccTests` may run `clang` or
`fasm`. A test that genuinely needs one belongs in `JccIT`, tagged `@Tag("LLVM")`
(see `JccIT.compileButNotAssembleLlvm`, which covers `-S` end to end).

## Failsafe reports outlive the run that wrote them

`target/failsafe-reports/<class>.txt` is written only when that class is selected and runs, and no
run deletes an earlier file. A class the OS gate skips writes nothing at all, and the `llvm-tests`
profile sets `failsafe.groups=LLVM`, which deselects the FASM `*CompileAndRunIT` classes rather
than skipping them. So a green `mvn -P llvm-tests install` without `clean` can leave the folder
holding failing FASM reports from an earlier `-Djunit.jupiter.conditions.deactivate='*'` run.
Take the result from Maven's summary, not from aggregating the report files.

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

A timeout in that harness must not be silent. `startAndWait` bounds the wait at
`PROCESS_TIMEOUT_MILLIS` (30 s); on expiry it destroys the process *and its
descendants* — a hung tool may itself be blocked on a child, and
`destroyForcibly` alone does not reach one — drops the output capture, and throws
`TimeoutException`. Cleanup happens there because no `Process` is returned, so the
caller's `finally { tearDownProcess }` never runs. `LlvmAssembler` and
`FasmAssembler` translate it into a `JccException` naming the configured executable
and the bound (`clang timed out after 30 seconds`), reported as `jcc: error: …` with
exit code 1. `readOutput` keeps a separate, shorter `DRAIN_TIMEOUT_MILLIS` (10 s)
join bound, which is safe because the process has provably exited by then, so EOF
arrives at once.

A `Process` returned by `setUpProcess` is thus guaranteed to have exited, and callers
rely on that: they call `exitValue()` directly, with no liveness check. Do not
discard the `waitFor` result (issue #90).

## Kotlin incremental compilation is disabled

The parent POM pins `kotlin.compiler.incremental` to `false`. When it was enabled,
scoped `mvn -pl ...` runs repeatedly left stale, partial test-classes behind
(tests failing with `NoClassDefFoundError` for a test base class, or test runs
silently discovering no tests at all). Do not re-enable it.
