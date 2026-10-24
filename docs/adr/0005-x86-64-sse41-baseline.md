# 0005. SSE4.1 baseline for x86-64 output

*2026-08-23*

## Context

BASIC rounds float-to-int half-to-even, matching QuickBASIC 4.5 (issue #52), and jcc
emits that as `llvm.roundeven.f64`. LLVM lowers the intrinsic to the SSE4.1 `roundsd`
instruction when SSE4.1 is enabled, and to a libm call to `roundeven` otherwise. glibc
and the macOS libm export `roundeven`; mingw-w64's libm does not. The Windows CI leg
found this when it first ran the integration tests: eight BASIC tests failed to link
with `undefined reference to 'roundeven'`. AArch64 was never affected, because LLVM
emits a native `frintn` there. The alternative was to emit `llvm.rint.f64`, which every
libm provides and which rounds to nearest-even under the default floating-point
environment — but that ties the rounding guarantee to the FP mode rather than to the
instruction.

## Decision

We will pass `-msse4.1` to clang on x86-64 hosts, and keep emitting
`llvm.roundeven.f64`.

## Consequences

Executables jcc produces for x86-64 now require an SSE4.1-capable CPU — Intel Penryn,
2008 — which the README's system requirements state. Rounding stays pinned to the
instruction rather than depending on the floating-point mode, so nothing can change it
at runtime. `Assembler` gates the flag on `OsUtils.isX86_64()`, so AArch64 gets no flag
and needs none. A new intrinsic jcc emits must now be checked against mingw-w64's libm,
not only glibc's: the platform with the thinnest libm sets the floor, and the failure
is a link error on one CI leg rather than anything the compiler itself reports.
