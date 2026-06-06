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

## Stale test-classes after scoped builds

A failsafe/surefire run can fail with `NoClassDefFoundError` for a test base class
(e.g. `se/dykstrom/jcc/main/AbstractIntegrationTests`) in the forked JVM while the
individual test reports under `target/failsafe-reports/` all show passing. The cause
is a stale, partial Kotlin test-compile in that module's `target/test-classes`
(observed in `jcc-compiler` after module-scoped `mvn -pl ...` runs). Fix:
`mvn -pl <module> clean`, then rebuild.

The same stale cache can also fail silently: surefire/failsafe discover no test
classes at all and the build reports SUCCESS without a single `Tests run:` line,
while the compiled tests sit only under `target/kotlin-ic/` and the Kotlin plugin
logs "Nothing to compile - all classes are up to date". If a test run prints no
`Tests run:` summary, treat it as not having tested anything — clean the module
and rebuild.
