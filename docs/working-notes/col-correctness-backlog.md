# COL correctness backlog

> **Working note — not authoritative.** Binding rules live in `architecture/` and `adr/`. Nothing here is a rule until it's promoted — however settled it reads.

Status: Research note

Companion to [col-correctness.md](col-correctness.md), which covers the four highest impact-per-effort items (overflow/div-by-zero semantics, guaranteed tail calls, opaque newtypes, evaluation order — the last already implemented). This note collects the remaining correctness-oriented ideas from the same discussion, roughly ordered by how soon they apply to COL. Same goal throughout: make it easier for humans and agents to write *correct* programs, on the theory that deterministic semantics and compiler-checkable intent beat cleverness.

## Applicable now

### Assertions and contracts

Even a bare `assert(cond)` builtin that traps with file:line is high value. A step further fits COL unusually well: since function bodies are single expressions, pre/postconditions have a natural home in the signature —

```
fun gcd(a as i64, b as i64) -> i64
    require a >= 0 and b >= 0
    := if b == 0 then a else gcd(b, a mod b)
```

Postconditions over the result (`ensure result >= 0`) turn the signature into an executable spec. Agents are good at both writing and respecting contracts, and a contract failure points at the *caller's* bug, not the callee's. Shares the trap mechanism with the overflow work in the companion note — design them together.

### Diagnostics as a feature

Agents converge by looping on compiler errors; humans do too, just slower. Worth treating error quality as a feature with its own backlog:

- Precise source spans on every error (some COL errors may already have this — audit).
- Actionable suggestions: `expected i64, found f64 — wrap the expression in i64(...)`; `unknown function 'pritnln' — did you mean 'println'?`.
- A machine-readable diagnostics mode (`--diagnostics json`) so tooling and agents can parse errors without regex.
- No warnings, only errors. A toy language can afford it, and agents notoriously ignore warnings. Anything worth flagging is worth rejecting.

### Canonical formatter

gofmt-style: one layout, no options. Agents produce consistent code, diffs stay semantic, style debates end. Cheap for a language this small — the ANTLR parse tree is already there to pretty-print from.

### Conformance suite as spec

Already largely practiced: the LLVM ITs pin observable behavior, and the evaluation-order work showed the pattern (spec sentence in `col-language.md` + pinning test, named so doc and test reference each other). Keep the discipline: every sentence in the language doc that promises runtime behavior should have a test that fails if the promise breaks.

## To bake in when planned features land

### Immutability by default (when `var`/`val` arrive)

- Make the immutable binding the short/default form; mutation should be the marked case.
- Require an initializer at declaration — if uninitialized declarations don't exist, no definite-assignment analysis is ever needed.
- Consider banning shadowing outright. In a small language the convenience is minor and the misread-the-wrong-`x` bug class disappears.

### Sum types before (or with) structs

The general form of "avoid null". If structs ship without sum types, absence will get encoded as sentinel values (`-1`, `0.0`) and implicit nullability sneaks in the back door. Tagged unions plus pattern matching with **exhaustiveness checking** is the payoff feature: add a variant, and the compiler lists every site that needs updating — the single best change-amplification guard for agents. `Option`/`Result` then fall out as library types rather than language magic.

### Effect / purity tracking

COL is unusually well positioned: function bodies are single expressions and `println`/`millis` are essentially the only side effects. Pure by default, with an annotation for effectful functions (working syntax: `fun log(x as i64) -> i32 does io := ...`), checked transitively. Nearly free to retrofit *now*; nearly impossible later. Pure functions are testable, cacheable, reorderable — and agents can reason about them locally. Interaction to resolve: the `println`-returns-`i32` sequencing idiom only exists *because* effects can hide in expressions; effect tracking would make that idiom visible in signatures, which is arguably the point.

### Named arguments

When functions grow more parameters, allow `f(width = 3, height = 4)` call syntax. Complements opaque newtypes (companion note) against the argument-transposition bug class — newtypes catch swaps of different-unit values, named arguments catch swaps the type system can't see.

## Resolved

- Nothing yet — all items here await a decision to pursue or drop.

## Open questions

- Contracts: compile-time-checkable subset, or purely runtime traps? Runtime-only is the pragmatic start.
- Should contract checks be elidable (release mode), or always on? Always-on matches the "correctness playground" stance.
- Effect tracking granularity: a single `io` effect, or distinguish read/write/time? Start with one bit.
- Formatter: separate tool or a compiler flag (`--format`)?
- Do named arguments allow reordering, or only labeling in declaration order? Labeling-only is simpler and catches the same bugs.
