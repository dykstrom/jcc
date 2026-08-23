# Build and test

Operational rules in MUST voice; cite the source (ADR, contract, incident) for each.

## Unit tests and the external toolchain

- Unit tests (surefire) MUST NOT invoke `clang` or `fasm`. A unit test that drives
  `Jcc.run()` MUST pass `-fsyntax-only`, which stops after semantic analysis.
  _Source: issue #90 — a `clang` call from `JccTests` flaked on the Windows CI runner._
- A test that genuinely needs the toolchain MUST be a failsafe integration test tagged
  `@Tag("LLVM")`, so the `llvm-tests` profile gates it. Tagging a surefire test has no
  effect. _Source: issue #90._

## Process harness

- `ProcessUtils.setUpProcess` MUST NOT return a process that may still be running. On
  timeout it MUST destroy the process and its descendants, and throw `TimeoutException`.
  _Source: issue #90 — the discarded `waitFor` boolean surfaced as
  `IllegalThreadStateException` from `LlvmAssembler`._
- Callers in `jcc-compiler` MUST translate that `TimeoutException` into a `JccException`
  naming the tool and the timeout. _Source: issue #90._
