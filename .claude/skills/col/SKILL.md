---
name: col
description: Writing COL programs, integration tests, or example files. Use whenever a task involves COL source code (.col files), COL integration tests (Col*IT), or the jcc-col module's language behavior. COL is a toy language that exists only in this repo — never guess its syntax from general knowledge.
---

# COL programming

COL has no internet presence; training data contains nothing about it. Before writing any COL code, read `docs/system/col-language.md` (semantics, types, built-ins, gotchas). The grammar `jcc-col/src/main/antlr4/se/dykstrom/jcc/col/compiler/Col.g4` is authoritative for syntax; `ColSymbols.java` for built-in function overloads.

## Hard rules

- Target the LLVM backend. New COL features get LLVM ITs only — do not add FASM ITs (`ColCompileAndRun*IT`, Windows-only).
- Never use `import` statements in tests or examples — unsupported on the LLVM backend.
- Every example in `jcc-compiler/src/examples/col` compiles with the LLVM backend and is a valid style reference.

## Writing an integration test

LLVM ITs live in `jcc-compiler/src/test/kotlin/se/dykstrom/jcc/main/ColLlvm*IT.kt`, tagged `@Tag("LLVM")`, extending `AbstractIntegrationTests`:

```kotlin
@Test
fun shouldDoSomething() {
    val source = listOf(
        "call println(7)",
    )
    val sourcePath = createSourceFile(source, COL)
    compileLlvmAndAssertSuccess(sourcePath, language = COL)
    runLlvmAndAssertSuccess(listOf(), listOf("7"))  // stdin lines, expected stdout lines
}
```

Run with the `llvm-tests` profile (requires Clang 20+):

```
mvn -P llvm-tests -Dit.test=ColLlvmCompileAndRunIT verify
```

Expected-output gotchas: floats print with six decimals (`5.3` → `"5.300000"`), booleans print `1`/`0`, and `println` returns the number of characters printed (not its argument) — relevant when using it to sequence side effects.

## Writing an example program

New examples go in `jcc-compiler/src/examples/col/` and must compile with the LLVM backend. From the repo root:

```
java -jar jcc-compiler/target/jcc-compiler-*.jar --backend LLVM --library-path jcc-compiler/target -o /tmp/out example.col
```

`--library-path` points at the directory holding `libjcccol.a` (built into `jcc-compiler/target`). If the jar fails with `NoClassDefFoundError`, its sibling dependency jars are missing — restore them with `mvn -q -pl jcc-compiler dependency:copy-dependencies -DoutputDirectory='${project.build.directory}' -DincludeScope=runtime`.

Common mistakes to avoid: the only loop is `while cond do ... end` (compiles on both backends; body allows `call`/`while`/`val`, though a `val` body is LLVM-only like all vals; loop-scoped vals; with no mutable state, a terminating loop needs a side-effecting condition like `millis()` — otherwise use recursion, prefixing a tail call with `become` to guarantee constant stack since deep recursion otherwise overflows at the default `-O0` rather than relying on Clang's optimizer), no mutable variables (only immutable `val`s, top-level or loop-local, invisible inside `fun` bodies), no strings; if- and while-conditions must be `bool` (no integer truthiness); mixed int/float arithmetic needs an explicit cast (`1 + 2.0` is an error — write `f64(1) + 2.0`).

`become f(args)` is only valid in tail position (the body, or a tail if-expression's branches), the callee must be a user-defined function whose return type exactly matches the enclosing function's, and it is LLVM-backend only. See the `become` section of `docs/system/col-language.md`.
