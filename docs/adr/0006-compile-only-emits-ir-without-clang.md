# 0006. `-S` emits LLVM IR without invoking Clang

*2026-08-23*

## Context

`-S` is documented as "compile only; do not assemble". Under the FASM backend
`FasmAssembler` honoured that literally: it wrote the `.asm` file and returned without
launching the assembler. The LLVM `Assembler` did not — it wrote the `.ll` and then ran
`clang -S`, which left a native `.s` file in the working directory that nothing
consumed and `.gitignore` did not cover. Deleting the FASM backend made the difference
matter: the four `*CompilerTests` build a `compileOnly` factory, they run under
surefire, and `docs/architecture/build.md` requires that no surefire test invoke clang.
The alternatives were to keep running clang and move those tests into `JccIT` as
integration tests, or to let them shell out and accept that `mvn test` needs a
toolchain.

## Decision

We will return from `Assembler.assemble` immediately after writing the `.ll` file when
`compileOnly` is set, so `-S` invokes no external tool.

## Consequences

`jcc -S program.bas` now works without Clang installed and leaves only `program.ll`,
which removes the stray `.s` from the working directory. `mvn test` stays free of the
toolchain, and the four `*CompilerTests` keep driving the whole pipeline through
`CompilerFactory` rather than being demoted to integration tests. The trade-off is that
`-S` no longer reports what only Clang would catch: invalid IR now fails at the next
full compile instead of at `-S` time. Anything that wants the native assembly must run
clang against the `.ll` itself.
