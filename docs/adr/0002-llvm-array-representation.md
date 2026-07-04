# 0002. LLVM array representation with inline bounds

*2026-07-01*

## Context

Arrays were implemented only on the FASM backend, which stores each array as one
contiguous block with dimension metadata (dimension count and per-dimension sizes)
packed in the words immediately before the element data, and reads that metadata at
runtime through the prebuilt `libjccbas` functions `.lbound`, `.ubound`, and
`.option_base`. Bringing arrays to the LLVM backend forced a choice between two
representations of that metadata: replicate the FASM in-memory layout (a single
heterogeneous global, with the array pointer aimed at the element data) so the same
`libjccbas` functions work unchanged, or emit an idiomatic LLVM representation and
compute the bounds without the runtime. The `libjccbas` source is not in this repo
(it ships as a prebuilt static library), so its private metadata layout is an
external contract the FASM approach depends on. The dimension sizes and the OPTION
BASE value are all compile-time constants for static arrays.

## Decision

We will represent each LLVM array as its own private `[N x T]` element-storage
global plus a separate `[D x i64]` dimension-size metadata global, and lower
`LBOUND`/`UBOUND` inline — `LBOUND` to the compile-time OPTION BASE constant, `UBOUND`
to `size(d) - 1` read from the metadata global — so the `libjccbas`
`.lbound`/`.ubound`/`.option_base` functions are not used on this backend. Element
addresses are computed with the same multiply-accumulate flat index as FASM, via
`getelementptr`.

## Consequences

The IR is self-contained and idiomatic: no dependency on `libjccbas`'s private,
unversioned metadata layout, each array is its own global so an out-of-bounds write
cannot corrupt an adjacent variable, and OPTION BASE needs no runtime call. The cost
is a deliberate divergence from the FASM representation — the two backends now lay
arrays out differently, so array behavior must be kept in parity through tests
(`BasicLlvmCompileAndRunArrayIT` mirrors the FASM `BasicCompileAndRunArrayIT`) rather
than by sharing a runtime contract. The dimension-metadata global is emitted for
every array even though only `UBOUND` with a runtime dimension argument strictly needs
it at run time. Garbage collection of string array elements remains out of scope and
is deferred to a dedicated GC issue.
