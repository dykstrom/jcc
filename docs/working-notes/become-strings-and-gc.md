# `become` tail calls and GC frames with string values

> **Working note — not authoritative.** Binding rules live in `architecture/` and `adr/`. Nothing here is a rule until it's promoted — however settled it reads.

Status: Stabilizing

Spike for ticket 004 of the col-strings epic: the questions below were worked through with a
runnable harness, so the answers are evidence-backed — but they are answers within this note,
not rules, until tickets 006 and 007 implement them.

## The question

COL's `become` is lowered as an LLVM `musttail` call under `tailcc` (ADR 0001). The
collector's shadow-stack contract says `jcc_gc_pop_frame` is called "before `ret`, and
before a `musttail` call" (ADR 0003, `docs/GarbageCollection.md`) — but nothing has ever
exercised the `musttail` half, because BASIC has no `become` and COL has no strings. Ticket
006 is about to make COL's strings GC-managed. So: **when a COL function whose parameters or
return type are `string` performs a `become`, what must the generated IR look like for every
string value to stay reachable?**

## Verdict

**String-typed `become` is safe, and ticket 007 should implement it rather than reject it in
semantics.** The ordering jcc's shared code generators already produce is correct, and the
`musttail` structural constraint leaves exactly enough room for the GC calls it needs. Two
implementation gaps stand between that and a working COL — both in
`ColFunDefCodeGenerator`, both listed below as R4 and R5 — and one dependency on the
collector's C implementation has to be written down before someone optimises it away (R3).

## The hazard, and why it is not a bug

The argument to a `become` is produced in the frame that the `become` pops. Between the pop
and the callee rooting its parameter, the pointer exists **only in an SSA register** — and
the collector never scans registers or the native stack (it is precise by design). Nothing in
the IR keeps it alive there:

```
  %7  = call ptr @jcc_gc_register(ptr %6)     ; the new accumulator
  store ptr %7, ptr %_.gc.slot.0              ; rooted — in THIS frame
  call void @jcc_gc_pop_frame()               ; ← root dropped; %7 now reachable from nowhere
  %13 = musttail call tailcc ptr @build(ptr %7, i64 %12)
  ret ptr %13
                                              ; callee: push_frame, alloca, store, add_root
```

The window is safe for one reason: **`jcc_gc_push_frame` and `jcc_gc_add_root` never
collect.** The only collection points are `jcc_gc_register` and an explicit
`jcc_gc_collect` (`libjcccol/src/jcc_gc.c:286-318` and `:410-435` — push/add only `realloc`
their arrays). The callee's prologue does nothing but push, `alloca`, store and root, so no
collection can run until the parameter is rooted.

That is a **dependency on the collector's implementation, not on its published contract**.
`jcc_gc.h` does not promise that rooting cannot collect. If a future collector ever collects
from `add_root` — say, to amortise work — string-typed `become` breaks silently across both
BASIC and COL. Requirement R3 states it so the next person has to decide deliberately.

## Requirements for the implementation

Tagged with the ticket that owns each.

**R1 (006) — The callee roots every string parameter in its own frame, before the body.**
This is what makes the argument reachable again after the caller's pop.
`RuntimeGcCodeGenerator.rootVariables` already does it. Removing just this one call turns E1
into E2: a textbook `heap-use-after-free`, and, unsanitized, output that is silently wrong
(`11` instead of `123`) — the exact failure mode ticket 004 was written to pre-empt.

**R2 (006) — Arguments are evaluated before the pop, and every intermediate is stored into a
rooted slot of the frame about to be popped.** A `become` argument built from two string
sub-expressions has a registration — a collection point — between them;
`register`-then-store keeps the first one alive across the second.
`FunctionCallCodeGenerator.toLlvmTailCall:130-145` already emits exactly this order:
evaluate args → `gc.exitFunction()` → `musttail`.

**R3 (006) — No allocation may occur between the pop and the callee's `add_root`.** Enforced
structurally at the caller (LLVM permits nothing between `musttail` and `ret`) and by
convention at the callee (a prologue that only pushes, allocates, stores and roots) — but
ultimately resting on `push_frame`/`add_root` never collecting. If that changes in
`libjcccol`, this design has to change with it.

**R4 (006) — Every tail leaf pops exactly once.** `ColFunDefCodeGenerator.generateTail`
(`jcc-col/.../code/llvm/statement/ColFunDefCodeGenerator.java:75-97`) adds a
`ReturnOperation` **directly** for a non-`become` tail leaf (`:92-95`) and for tail
if-expressions (`:81-91`), bypassing `ReturnCodeGenerator` — which is the only place
`gc.exitFunction()` is emitted on the return path. So the value leaf of
`if n == 0 then acc else become loop(...)` — COL's accumulator idiom, the single most likely
shape of a string `become` — emits **no** `jcc_gc_pop_frame`. Measured cost below.

**R5 (006) — `ColFunDefCodeGenerator` must be handed the language's `GcCodeGenerator`.** It
constructs its own `FunctionCallCodeGenerator` with `NoOpGcCodeGenerator.INSTANCE` hardcoded
(`:55`) and inherits the no-op default via `super(codeGenerator)` (`:54`). Wiring
`RuntimeGcCodeGenerator` into `ColLlvmCodeGenerator` alone will **not** reach the `become`
path: it would keep emitting no GC calls at all, which is not a crash but a leak of every
string a `become` chain touches.

**R6 (007) — Lambdas and mutual/cross-overload recursion need no special rule.** Lifted
lambdas are ordinary top-level `tailcc` functions (`@lambda.0_Str_I64`), and mutual `become`
between mismatched prototypes (`ptr, i64` ↔ `i64, ptr`) is precisely what `tailcc` was
adopted for. Both verified.

**R7 (007) — A `become`'s string result is rooted by the ultimate caller, never in the
chain.** `musttail` admits no post-call plumbing, so there is nowhere to root it anyway; the
value is registered in whichever function finally produces it and travels back past every
popped frame to the original caller, which stores it into a rooted slot before its next
registration. Also: never pass the *address* of a rooted slot as a `become` argument — the
frame is destroyed by the tail call. jcc passes loaded values, so this is a note, not a risk.

## Evidence

Eight hand-written IR cases under `become-gc-spike/`, each linked against the real
`libjcccol.a` (0.2.0) twice — plain and under AddressSanitizer — and run with
`JCC_GC_DEBUG=1` and an initial threshold of 1 or 8, so a collection runs at nearly every
registration. `./become-gc-spike/run.sh` reproduces all of it; `--scale` adds the two
scaling comparisons. Every case passes LLVM's verifier, which is what proves the GC calls
*fit* the `musttail`-immediately-before-`ret` constraint.

| Case | What it shows | Result |
|------|---------------|--------|
| E1 `e1-arg-across-pop` | Accumulator `become` whose argument is built from two string sub-expressions, so a collection runs between `acc`'s load and its use | `123`, no ASan report |
| E2 `e2-negative-control` | E1 minus the callee's `jcc_gc_add_root(%_acc)` | **Caught**: `heap-use-after-free` in `col_concat_str_str`, freed by `jcc_gc_collect` ← `jcc_gc_register`; unsanitized it prints `11` |
| E3a/E3b `tail-if-{no-pop,with-pop}` | The mixed tail-if, as emitted today vs with R4's pop | Both print `yyyy`; RSS diverges (below) |
| E4 `e4-deep-chain` | 10⁵ and 10⁶ deep `become` chain | `live` 5 → 8, RSS 1504 KB → 1488 KB — flat |
| E5 `e5-string-return` | String produced at the bottom of a `become` chain, returned up through plain recursion that keeps allocating | `0yyyyy`, no ASan report |
| E6 `e6-lambda-callee` | `become` to `@lambda.0_Str_I64` | `123`, no ASan report |
| E7 `e7-mutual-mismatched-sigs` | Mutual `become`, `ptr,i64` ↔ `i64,ptr` | `pqpqpqpq`, no ASan report |

E2's report, trimmed:

```
==ERROR: AddressSanitizer: heap-use-after-free ... READ of size 2
    #0 strlen  #1 col_concat_str_str+0x28  #2 build_Str_I64+0x7c
freed by thread T0 here:
    #0 free  #1 jcc_gc_collect+0x290  #2 jcc_gc_register_object+0x54  #3 build_Str_I64+0x6c
```

### The missing pop (R4), measured

The leaked roots do **not** show up in the `live` count — E3a and E3b report an identical
`registered=800 collections=402 freed=797 live=3`. The reason is worth knowing: `main` calls
`build` repeatedly from the same stack depth, so the stale slot addresses left behind by call
*k* are the *same addresses* as call *k+1*'s live slots. The leak marks through the current
call's own slots and retains nothing extra. Change the shape so the stack depth varies
between calls and that coincidence disappears.

What the leak does do is grow the collector's frame and root arrays without bound, one frame
and two roots per call:

```
  e3a (no pop)  n=10000    rss=2160 KB     e3b (pop)  n=10000    rss=1488 KB
  e3a (no pop)  n=100000   rss=5440 KB     e3b (pop)  n=100000   rss=1488 KB
```

It also makes collection **quadratic** — every collection walks every stale root — which is
why the comparison stops at 10⁵: E3a at 10⁶ calls does not terminate in any reasonable time,
while E3b is unchanged at 1488 KB and linear.

And it is latent undefined behaviour beyond the leak: those stale roots are addresses of
`alloca` slots in native frames that have already returned, so every later collection *reads
dead stack memory*. Whether that retains garbage or reads something harmless is down to stack
reuse. This spike could not observe it (see limitations) — the leak and the quadratic
slowdown are the observable harm.

## Resolved

The ticket's four specific questions, worked through within this note.

- **Ordering around the pop** — Safe as designed. The argument is unreachable between the
  pop and the callee's `add_root`, but no collection can run in that window. No deferred pop
  or extra rooting is needed. (R1, R2, R3; E1, E2.)
- **`musttail`'s structural constraints** — LLVM permits nothing between the `musttail` call
  and its `ret`, but places no constraint on what precedes the call, so `jcc_gc_pop_frame`
  fits. The consequence is that *no* post-call GC plumbing is possible, which is why the
  result is rooted by the ultimate caller (R7). This holds for a `become` inside the branches
  of a tail if-expression: the branch's `musttail`/`ret` pair terminates its own block, which
  is what `ColFunDefCodeGenerator` was written to produce. Verified by LLVM's verifier on all
  eight cases, and at `-O0` and `-O2`.
- **Frame accounting across a chain** — Balanced, provided R4. E4 runs 10⁶ iterations with
  `live` at 8 and RSS flat at ~1.5 MB, identical to the 10⁵ run. Tail calls survive ASan
  instrumentation too: the same 10⁶ chain completes under `-fsanitize=address` without a
  stack overflow.
- **Anonymous functions** — Nothing special. E6.

## Limitations

- macOS arm64 only, clang 21, `libjcccol` 0.2.0. Nothing was run on Linux or Windows.
- These are hand-written IR cases modelling what the compiler *will* emit, not compiler
  output — COL has no string type yet. They were built from real emitted IR (COL's `become`
  skeleton from `fac.col`, the GC plumbing from a BASIC program), but 006 should re-run
  `run.sh` against real output and, better, promote E1/E3/E4 to COL integration tests.
- ASan cannot see the dead-stack reads described above: `libjcccol` is not built with
  instrumentation, so the loads inside `jcc_gc_collect` are unchecked. ASan here catches heap
  misuse only, via its `malloc`/`free` interceptors.
- Nothing was tested with a collector that collects during `add_root` — the design depends on
  it not doing so (R3).

## Open questions

- Should R3 become a *documented* guarantee in `jcc_gc.h` ("rooting never collects") rather
  than an emergent property of the current implementation? That is a change to `libjcccol`,
  which ticket 004 is not allowed to make — it belongs to whoever next touches the collector.
- Is the dead-stack read behind R4 reachable in practice before R4 is fixed, or does stack
  reuse always make it benign? Answering it needs an instrumented build of `libjcccol`, which
  nothing in either repo produces today.
- Should E1/E3/E4 be promoted to COL integration tests once 006 lands, or does a compiled COL
  program plus the existing `-print-gc` exit-stats assertions cover the same ground more
  cheaply? Leaning towards promoting E3 and E4 only: E3 is the regression this note exists to
  prevent, E4 is the constant-memory guarantee COL advertises.
  **Answered by ticket 007**, which promoted E4 as
  `ColLlvmGarbageCollectionIT#shouldKeepShadowStackFlatAcrossStringBecomeChain` (10⁵ string
  `become`s, `live` bounded by the threshold) and E1 as
  `#shouldKeepEveryCallersStringAliveAcrossDeepRecursion` (100 nested frames, asserting
  `freed=0` — a frame that failed to root its parameter shows up as a reclaimed live string).
  E3's missing pop was fixed in 006 and is pinned by a codegen test instead, where counting
  pops is exact. The compiled-COL equivalents of E1, E5, E6 and E7 were re-run by hand under
  AddressSanitizer at a threshold of 1, per the procedure in `docs/GarbageCollection.md`, with
  no reports.
