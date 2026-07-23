# Language semantics

Operational rules in MUST voice; cite the source for each.

- The BASIC compiler MUST behave as similarly as possible to QuickBASIC 4.5, on both backends. QB 4.5 has no 64-bit integers, but its behavior between smaller float and int types is the reference, scaled up to JCC's types. In particular, float→int conversions — implicit assignment and `CINT` alike — MUST round to nearest, ties to even (banker's rounding). _Source: design guideline, 2026-06-06; see issue [#52](https://github.com/dykstrom/jcc/issues/52)._
- The COL compiler MUST behave as much as possible like a modern C/Go/Rust compiler. In particular, float→int casts (`i64()`, `i32()`) MUST truncate toward zero. _Source: design guideline, 2026-06-06._
- When a design choice trades FASM-backend output stability against LLVM-backend correctness or efficiency, the LLVM backend MUST win. Changing FASM-generated code is acceptable; the FASM backend is slated for deprecation. _Source: design guideline, 2026-06-06; deprecation noted in AGENTS.md._
