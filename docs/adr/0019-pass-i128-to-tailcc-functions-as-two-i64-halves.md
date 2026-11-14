# 0019. Pass i128 to tailcc functions as two i64 halves

*2026-11-14*

## Context

COL compiles a user-defined function with the `tailcc` calling convention, so that `become` can be a `musttail` call (ADR 0001). Clang miscompiles an `i128` argument of a `tailcc` call in two ways:

- On arm64 Linux and Windows at `-O0`, a `tailcc` tail call puts the two halves of each `i128` in the wrong registers. The callee reads other registers, so it computes with garbage. The fix, llvm-project pull request 213811, is not merged. The pull request says that arm64 macOS is affected for some signatures.
- On x86-64, a `tailcc` call with an `i128` argument on the stack leaves the stack pointer 16 bytes off after the call (llvm-project issue 105223). The fix is in LLVM 24 only.

Every Clang version that jcc supports has at least one of the two bugs. The `i128` type of COL made the first one visible: `become` with `i128` arguments printed wrong results on arm64 Linux and Windows.

We considered four alternatives:

- Wait for fixed Clang releases, and print a warning as jcc does for Clang 22. No supported Clang is correct, so `become` with an `i128` would stay broken on two platforms.
- Split the argument only on the affected platforms. The IR would differ between platforms, and a unit test on one platform would not cover the others.
- Give up `tailcc` for functions with an `i128` parameter. A `musttail` call between functions with different parameter lists needs `tailcc`, so this would break `become` across overloads.
- Pass an `i128` by pointer. The value would live in the caller's stack frame, which a tail call removes.

## Decision

We will pass every `i128` parameter of a `tailcc` function as two `i64` parameters, the low half first, on all platforms and at all optimization levels. A function still returns an `i128` as an `i128`.

## Consequences

`become` with `i128` arguments works on every supported platform and Clang version. The IR is the same on every platform, so one unit test covers them all. The IR for a function with an `i128` parameter is longer. The function rebuilds each value on entry, and each call splits each value. At `-O1` and above, Clang removes most of that code.

`I128Halves` in `jcc-llvm` holds the rule. `FunDefCodeGenerator` and `FunctionCallCodeGenerator` apply it, and it covers an opaque type over `i128`. Code that emits a definition or a call of a `tailcc` function in another way must apply the same rule, or the definition and the call no longer agree. A new integer type wider than 64 bits needs the same treatment.

A call to a C function is not affected, because it does not use `tailcc`. `col_string_i128` in libjcccol keeps its `i128` parameter. A return value is not affected by either bug. If a later Clang miscompiles an `i128` return value under `tailcc`, that needs a new decision.

When the oldest supported Clang has both fixes, a new ADR can remove the split.
