# COL correctness features

> **Working note — not authoritative.** Binding rules live in `architecture/` and `adr/`. Nothing here is a rule until it's promoted — however settled it reads.

Status: Research note

COL already has a correctness-oriented core: static typing, explicit casts (only lossless widening is implicit), `/` vs `div`/`mod`, no integer truthiness, mandatory `else` in if-expressions, and `call` as the explicit way to discard a return value. This note explores four additions in the same spirit, picked for impact-per-effort; the remaining ideas from the same discussion live in [col-correctness-backlog.md](col-correctness-backlog.md). The first two close actual holes in COL-as-it-exists; the third and fourth are cheap wins.

## 1. Defined overflow and division-by-zero semantics

**Today.** Integer `+`, `-`, `*` emit plain `add`/`sub`/`mul` without `nsw` flags (`AbstractLlvmCodeGenerator.java`), so they wrap in two's complement — defined behavior at the IR level, but by accident, not by spec. `div`/`mod` emit `sdiv`/`srem`, which are **undefined behavior** in LLVM IR for division by zero and for `INT_MIN div -1`. The language doc says overflow and division by zero are "whatever the backend does."

**Goal.** Every COL program either fails to compile or has fully defined behavior. Deterministic semantics matter more than any single feature — for agents especially, which converge by looping on observable behavior.

**Proposal.** Trap on signed overflow and on the two `sdiv`/`srem` UB cases, with a runtime message naming the operation (ideally with source position). Implementation sketch:

- `+`, `-`, `*`: emit `llvm.sadd.with.overflow.*` / `ssub` / `smul` intrinsics, branch to a trap block on the overflow bit.
- `div`/`mod`: guard `rhs != 0` and `!(lhs == INT_MIN and rhs == -1)`, branch to trap.
- Trap = call a `libjcccol` runtime function that prints the message to stderr and exits nonzero (better diagnostics than `llvm.trap`).
- For intentional wrapping, add builtins later if a need appears (`wrapping_add(a, b)` etc.) — don't add them speculatively.

**Alternative considered.** Specify wrapping for `+`, `-`, `*` (document current behavior) and only fix the `sdiv`/`srem` UB. Cheapest path to "no UB", but wrapping silently produces wrong values; trapping turns wrong values into visible failures, which is the point of the exercise.

**Cost.** Trap checks at every arithmetic op are not free, but COL is a correctness playground, not a benchmark contender. Clang at `-O1+` optimizes the intrinsic+branch pattern reasonably well.

## 2. Guaranteed tail calls

**Today.** Recursion is COL's only loop. `CallOperation` emits plain `call` with no `tail`/`musttail` marker, and the default optimization level is **`-O0`** (`OptimizationOptions`), so tail calls are not optimized by default at all. `fac.col` and `fib.col` only survive deep recursion when the user passes `-O1`/`-O2`. A stack overflow on a correct program is a correctness bug by another name.

**Proposal.** Make tail calls a language guarantee rather than an optimizer accident. Options:

1. **Compiler-detected:** mark every tail-position call `musttail`. `musttail` requires matching signatures and calling convention — always satisfied for self-recursion, mostly satisfied for mutual recursion between COL functions.
2. **Explicit keyword:** `become f(...)` in tail position, verified by the compiler (error if not actually a tail call), emitted as `musttail`. Plain calls stay plain.
3. **Loop transform:** rewrite self-tail-recursion to a branch back to the entry block in the IR. Backend-independent, but only covers self-recursion.

**Current lean: explicit `become` (2).** The explicit form makes intent checkable — the compiler can reject a `become` that is not in tail position, which catches the "thought it was tail-recursive but it isn't" bug (e.g. `n * fac(n - 1)`). That diagnostic is valuable to humans and agents alike; silent TCO gives no such signal. It also matches COL's existing explicitness (explicit casts, explicit `call`) and words-over-symbols style. In use:

```
fun fac_iter(n as i64, result as i64) -> i64 :=
    if n <= 1 then result else become fac_iter(n - 1, n * result)
```

**Marker placement — alternatives considered.** The marker could live elsewhere:

| Model | Precedent | Shape in COL |
|---|---|---|
| Call site | Rust `become` RFC, Zig `@call(.always_tail, …)`, Clojure `recur` | `else become fac_iter(n - 1, n * result)` |
| Function header | Kotlin `tailrec` | `tailrec fun fac_iter(...) -> i64 := …` |
| No marker — automatic | Scheme, Erlang | nothing; guarantee is in the language spec |
| Automatic + header assertion | Scala `@tailrec` | guarantee always on; the marker only verifies |

A header marker only earns its keep with *verify* semantics ("every recursive call in this function is in tail position; error otherwise") — *enable* semantics wouldn't catch the `n * fac(n - 1)` bug at all. Even then, COL's own examples expose three frictions: `fac_iter(n)` tail-calls the *other overload* `fac_iter(n, 1)`, so "recursive" must be defined across overload sets; mutual recursion needs both headers marked, each implicitly referencing the other; and a mixed function (`if done then helper(x) + 1 else become next(x)`) can't be expressed at function granularity without reinventing per-call marking with worse error locations.

If call-site syntax were rejected, the stronger headerless model is Scala's, not Kotlin's: make the guarantee automatic for every tail-position call and offer a header annotation that is purely a checked assertion. Its real argument against `become`: with opt-in marking, *forgetting* `become` leaves correct-looking code unprotected — the `-O0` stack overflow survives wherever the keyword is missing — whereas automatic eliminates the failure mode by default. The counter-weights: automatic forces the `tailcc` switch (below) from day one, acquires fine print around tail-position calls to external functions (which silently get no guarantee), and gives no signal when intent and reality diverge. On balance we lean explicit `become`; the trade is "explicit, locally readable intent" over "no silent unprotected recursion", consistent with how the language already prefers explicit over implicit.

**Syntax.** Because function bodies are single expressions, tail position is a small tree: the body expression itself, the `then`/`else` branches of an if-expression in tail position, and parenthesized expressions in tail position. Nothing else — not operands of any operator (`a and become f()` is never a tail call), not call arguments, not the if-condition. Parse `become functionCall` liberally as an `expr` alternative and verify tail position in the semantics pass, rather than restricting it in the grammar: ANTLR's "mismatched input" errors can't say *what consumed the result*, a semantics pass can.

**Semantic rules.** A `BecomeExpression` node wrapping a `FunctionCallExpression`, checked in the function-definition semantics parser:

1. Must be in tail position (definition above).
2. Only inside `fun` bodies — top-level `call become f()` is an error.
3. The callee's return type must *exactly equal* the enclosing function's declared return type — implicit widening is disallowed, because the widening would be a `sext`/`fpext` *after* the call, which destroys tail position at the IR level. `become` is stricter than a plain call here, and the error message must explain why.
4. Direct calls to named functions only, in v1. `become` on a function-typed parameter would work (`musttail` allows indirect calls; all COL functions share the C calling convention) but is deferred to keep overload resolution and diagnostics simple.

**Code generation.** The `call` instruction is the easy part: `CallOperation` grows a marker to emit `musttail call` instead of `call`; `musttail` is honored at every optimization level, which is exactly what fixes the `-O0` default. The hard part is the if-expression structure: LLVM requires a `musttail` call to immediately precede a `ret` returning its value, but today's codegen evaluates if-expressions by branching to a merge block with a `phi` (the shape pinned in `ColLlvmCodeGeneratorTests`). A branch containing `become` cannot flow into a phi — it must terminate itself with `musttail call` + `ret`, and the merge block's phi loses that incoming edge (disappearing entirely if both branches `become`). So the body code generator needs a notion of *tail context*; a contained but real refactor of the if-expression/function-definition codegen. The LLVM verifier rejects malformed `musttail`, so codegen bugs fail loudly instead of miscompiling.

**Calling-convention constraint.** Under the default C calling convention, `musttail` requires the caller and callee *prototypes to match*. Self-recursion always qualifies, but `fac_iter(i64)` tail-calling `fac_iter(i64, i64)` does not — mismatched prototypes are invalid IR. The fix is to compile all COL-internal functions with `tailcc`, the calling convention built for guaranteed tail calls, which lifts the restriction. External functions (`printf`, `libjcccol`) stay `ccc`, which conveniently makes `become println(...)` rejectable on calling-convention grounds. One-time codegen change; needed for cross-overload and mutual tail recursion regardless of marker syntax.

**Diagnostics.** Most of the feature's value — `become` exists so the compiler can reject false beliefs about tail recursion:

| Situation | Message sketch |
|---|---|
| `n * become fac(n - 1)` | "`become` is not in tail position: its result is used by `*`. The tail call must be the function's final action — consider an accumulator parameter." |
| `become g()` where `g -> i32`, function `-> i64` | "tail call returns `i32` but the function returns `i64`; the implicit widening would run after the call. Declare matching return types." |
| top-level `call become f()` | "`become` is only allowed inside a function body" |

The first is the payoff: writing `become` in plain `fac` gets a compile error that effectively teaches the accumulator rewrite.

**Testing.** A codegen test asserting `musttail call` + adjacent `ret` (same register-pinning style as the evaluation-order tests), plus an IT recursing ~10⁸ deep at default `-O0` — which today blows the stack and with `become` must terminate.

**Interaction with #1:** narrower than first feared. Argument expressions (`n * result`) evaluate *before* the call, so overflow traps in arguments don't affect tail position at all. Only operations *after* the call break it — and the exact-return-type rule (semantic rule 3) eliminates the one such case, implicit widening. The two features compose cleanly.

## 3. Opaque type aliases (newtypes)

**Superseded by issue #94**, which carries the full design: `type X as T`, nominal identity, the v1
inherited-operations table (`==`/`!=`, ordering, same-type `+`/`-`), opaque function types that stay
directly callable, the language-wide impacts, diagnostics, and an implementation plan. The
`alias`-or-not question is left open there and is the decision to take before implementation starts.
The rest of this section is the original sketch.

**Today.** `alias` is fully transparent: `AliasPass1SemanticsParser` registers the name directly to the resolved type via `defineTypeName`, so an alias and its underlying type unify everywhere.

**Proposal.** Add a distinct declaration — working syntax `type Meters as f64` — creating a type that does *not* unify with `f64` or with other newtypes over `f64`. Conversion is explicit and follows the existing cast-function convention: declaring the type introduces `Meters(x as f64) -> Meters` and makes the existing `f64(m)` cast accept `Meters`. Zero runtime cost; the LLVM type is just `double`.

This directly attacks the argument-transposition bug class (two adjacent same-typed parameters, swapped at the call site) that humans and agents commit constantly — and it is cheap precisely because COL's type checker is already strict.

`alias` stays as-is for transparent shorthand, which remains genuinely useful for function types (`alias F2 as (i64, i64) -> i64`).

**Open design point:** which operations does a newtype inherit? Options range from none (everything via conversion — safe but unusable) to same-type closure (`Meters + Meters -> Meters`, `Meters * f64 -> Meters`?). Probably start with comparisons + same-type `+`/`-` only, and grow on demand.

## 4. Defined evaluation order

**Today.** `FunctionCallCodeGenerator` evaluates arguments left-to-right (stream over the argument list in encounter order), and binary operators evaluate left operand first. But nothing specifies this, and the `println`-returns-`i32` sequencing idiom (see `fib.col`) silently depends on it.

**Proposal.** Specify left-to-right evaluation for function arguments and binary operands in `docs/system/col-language.md`, and pin it with a codegen test that asserts emission order for a call whose arguments have observable side effects. No code change — the cheapest item in this note; it just turns current behavior into a commitment.

## Resolved

- Current `+`/`-`/`*` behavior is wrapping (no `nsw` flags emitted), so today's UB exposure is limited to `sdiv`/`srem` (division by zero, `INT_MIN div -1`).
- Tail calls genuinely break at the default `-O0` — this is not theoretical.
- Argument evaluation is already left-to-right in the code generator; #4 needs documentation and a test only.
- #4 is implemented (2026-06-05): specified in `docs/system/col-language.md`, pinned by codegen tests (`ColLlvmCodeGeneratorTests`) and a runtime IT (`ColLlvmCompileAndRunIT#shouldEvaluateLeftToRight`). No code change was needed.
- #2 design worked through (2026-06-05): explicit `become`, parsed liberally and verified semantically; exact return-type match required (no widening after the call); direct calls only in v1; codegen needs a tail-context mode because the phi-merging if-expression shape can't host a `musttail` call.
- #2 implemented (2026-06-23, issue #55): explicit `become` keyword, emitted as LLVM `musttail`; tail position verified in semantics (`FunDefPass2SemanticsParser`) with the documented diagnostics; COL-internal functions compiled with `tailcc` (externals/`main` stay `ccc`), derived uniformly from the function in the shared `CallingConvention`; tail-context codegen in `ColFunDefCodeGenerator` (no phi for `become` branches). The settled rules now live in `docs/system/col-language.md` (Tail calls section), which is authoritative, and the decision and its alternatives are recorded in `docs/adr/0001-explicit-become-for-guaranteed-tail-calls.md`; this note is superseded for #2. Future work from this note still open: `become` on function-typed parameters, and the "forgot `become`" failure mode.
- The #1/#2 interaction is benign: argument expressions evaluate before the call, so overflow traps in arguments never break tail position; the exact-return-type rule removes the only after-the-call operation (implicit widening).
- Marker placement compared (2026-06-05): header-only (Kotlin `tailrec`) verification trips over COL's overload-hopping recursion, mutual recursion, and mixed functions; the strongest headerless model is Scala-style automatic + assertion. We lean explicit call-site `become` — consistent with COL's explicit-over-implicit stance.
- `musttail` under `ccc` requires matching caller/callee prototypes, so cross-overload and mutual tail recursion need COL-internal functions compiled with `tailcc`; externals stay `ccc` (making `become` to externals rejectable).
- The `f32`/`f64` question is resolved and implemented (2026-06-05): IEEE 754 is the defined behavior, documented in `col-language.md` and pinned by `ColLlvmCompileAndRunIT#shouldFollowIeee754Semantics`. Documenting it surfaced a real bug — `!=` lowered to `fcmp one`, making `NaN != NaN` false (IEEE requires true); fixed to `une`. The shared AST optimizer also folded `0.0 / x` to `+0.0` (wrong for negative or NaN `x`) — fold removed — and would have folded overflowing literal divisions into inf literals — now guarded. Division by a literal zero stays a compile-time error, Go-style.

## Open questions

- Trap mechanism: runtime function in `libjcccol` (good messages) vs `llvm.trap` (no message) — and can the trap carry source positions without bloating the IR?
- Should overflow trapping be a compiler flag during a transition period, or on from day one?
- The "forgot `become`" failure mode remains: unmarked deep recursion still overflows at `-O0`. Live with it, or mitigate later (Scala-style automatic guarantee on top, once `become` has settled)?
- When to lift the direct-calls-only restriction on `become` (indirect calls via function-typed parameters are `musttail`-compatible)?
- Newtype operation inheritance — answered for v1 in issue #94 (`==`/`!=`, ordering, same-type
  `+`/`-`; scaling and unary `-` still open there), along with the fate of `alias`.
