# 0003. LLVM garbage collection via a frontend-managed shadow stack

*2026-07-09*

## Context

The LLVM backend has no garbage collector. Strings are `malloc`'d `char *`
passed on a "caller frees" contract; the backend frees only transient temporaries
inline, and every string stored into a variable or returned from a user-defined
function is deliberately leaked (freeing a result that aliased an argument or a
string literal aborted at runtime). Dynamic memory therefore grows without bound.
GitHub issue #63 requires a real collector for the LLVM backend, functionally on
par with the retiring FASM collector but of a new design, with an API that emits
idiomatic LLVM IR and is extensible beyond strings and reusable by other
languages.

LLVM offers three levels of GC support, and JCC's pipeline — it emits **textual
IR compiled by an external, stock `clang`** — decides which are reachable:

- `gc.statepoint` (precise stack maps at safepoints; the modern path for
  relocating collectors) and `llvm.gcroot` (LLVM-lowered shadow stack) both
  require a `GCStrategy` plugin loaded into the code generator (`-load MyGC.so`)
  and, for statepoints, an `llc`-driven pass pipeline. Neither is reachable from
  textual IR handed to a stock `clang`, and both are overkill for non-moving
  string collection.
- Emitting the GC intrinsics as text without such a plugin does nothing: the
  intrinsics only acquire behavior when a matching strategy lowers them.

Two non-LLVM approaches were also weighed and rejected: reference counting (far
more IR at every string operation, fragile pairing in codegen, and future
closures/records can form cycles that would need a backup tracer anyway), and
conservative Boehm-style collection (needs platform-specific stack scanning for
three OS/arch targets, can retain garbage on false positives, and offers no
explicit root API — a poor fit for the idiomatic-API and extensibility
requirements). The design analysis and phased plan live in issue #63.

## Decision

We will implement a precise mark-and-sweep collector with a **frontend-managed
shadow stack**: JCC emits ordinary LLVM IR that calls a plain C runtime
(`jcc_gc_*`, in `libjccbas`) — no LLVM GC intrinsics, no `GCStrategy` plugin, no
`llc` pipeline. The collector never inspects the native stack.

- **Roots are slot addresses.** A value is kept live by being stored into a
  registered slot (an `alloca` or a global); the collector reads each slot's
  current value at mark time and ignores values that are `NULL`, string literals,
  or otherwise not registered.
- **Global roots** are registered once in `main` via a single null-terminated
  data table of `{slots, count}` ranges (a scalar is a count-1 range; a string
  array's element region is a count-N range).
- **Local roots** live in per-function shadow-stack **frames**: each function
  pushes a frame in its prologue, roots its string parameters, string locals, and
  synthetic temporary slots, and pops the frame before `ret`.
- **Registration transfers ownership.** `jcc_gc_register(p)` takes a `malloc`'d
  pointer *value*, may run a collection *before* inserting `p` (so a registration
  never sweeps the block it registers), and returns `p`; the caller stores the
  result into a rooted slot before the next registration.
- **Existing `libjccbas` string functions are unchanged** — they keep allocating
  with `malloc`; the GC takes ownership at the register call.
- The collector is **language-agnostic** (new code in `jcc-llvm`) so COL can
  reuse it, and **extensible** via per-allocation type descriptors carrying
  `trace`/`finalize` callbacks (strings are leaf objects: `NULL` type → plain
  `free`).

The full design and the client-facing API are documented in
`docs/GarbageCollection.md`.

## Consequences

The GC works at every optimization level (`-O0` included) with an unmodified
`clang`, needs no LLVM cooperation, and keeps the whole mechanism in code and IR
that JCC already controls. A single ownership model — all dynamic memory is
GC-owned, with no manual frees — removes the two structural defects the FASM
collector carried: eager freeing of function arguments after a call
(use-after-free when the callee stashed or returned the pointer) and the split
between GC-managed variables and manually-freed temporaries. Because roots are
slot addresses read at mark time, a function may freely return its own argument
or a string literal.

The costs are borne in the emitted IR: every function pushes and pops a shadow
frame, string parameters and locals are rooted with explicit calls, and each
string-producing expression emits a register call plus a store into a synthetic
slot. The collector's own implementation lives in `libjccbas` (delivered in a
later phase) rather than in this repository, so its internals are an external
dependency. This decision governs the LLVM backend across all languages; the FASM
collector is untouched and will be removed with the FASM backend.
