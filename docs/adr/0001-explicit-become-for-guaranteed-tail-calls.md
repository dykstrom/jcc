# 0001. Explicit `become` for guaranteed tail calls in COL

*2026-06-26*

## Context

COL has no loop primitives, so recursion is the only way to iterate and a tail
call must run in constant stack. A plain tail call carries no guarantee: Clang
eliminates it only when optimizing, so deep recursion overflows the stack at the
default `-O0`. Three models were weighed: header-only verification (Kotlin
`tailrec` style), which trips over COL's overload-hopping and mutual recursion;
Scala-style automatic elimination plus an assertion; and an explicit call-site
marker. LLVM's `musttail` gives a per-call-site guarantee, but under the C
calling convention it requires caller and callee prototypes to match — which
they do not for cross-overload or mutual recursion.

## Decision

We will guarantee tail calls in COL with an explicit `become` keyword that
prefixes a call in tail position, lowered to an LLVM `musttail` call. To make
`musttail` valid across mismatched prototypes, COL-internal (user-defined)
functions are compiled with the `tailcc` calling convention; external, library,
and built-in functions, and the synthesized `main`, stay the C convention.

## Consequences

Deep recursion runs in constant stack at every optimization level, `-O0`
included, and the explicit marker lets the compiler reject false beliefs about
tail recursion instead of failing silently at runtime — consistent with COL's
explicit-over-implicit stance. The cost is a set of constraints `become` must
respect: the call must be in tail position; the callee's return type must equal
the enclosing function's (implicit widening would emit a `sext` after the call
and destroy tail position); and the callee must be user-defined — `become` to an
external or built-in is rejected because those keep the C convention. The FASM
backend rejects `become` outright (it is being phased out). `tailcc` now governs
every user-defined function definition and call site in the LLVM backend across
all languages, not only COL. Deferred: `become` on function-typed parameters,
and detecting the "forgot `become`" case where unmarked deep recursion still
overflows. The resulting rules live in `docs/system/col-language.md`; this
record supersedes the #2 deliberation in `docs/working-notes/col-correctness.md`.
