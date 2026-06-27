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

These exclusions took the analysis from a ~155-finding baseline to zero, after which the
build was switched from report-only to blocking.

## Kotlin incremental compilation is disabled

The parent POM pins `kotlin.compiler.incremental` to `false`. When it was enabled,
scoped `mvn -pl ...` runs repeatedly left stale, partial test-classes behind
(tests failing with `NoClassDefFoundError` for a test base class, or test runs
silently discovering no tests at all). Do not re-enable it.
