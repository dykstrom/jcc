# Language semantics

Operational rules in MUST voice; cite the source for each.

- The BASIC compiler MUST behave as similarly as possible to QuickBASIC 4.5. QB 4.5 has no 64-bit integers, but its behavior between smaller float and int types is the reference, scaled up to JCC's types. In particular, float→int conversions — implicit assignment and `CINT` alike — MUST round to nearest, ties to even (banker's rounding). _Source: design guideline, 2026-06-06; see issue [#52](https://github.com/dykstrom/jcc/issues/52)._
- The COL compiler MUST behave as much as possible like a modern C/Go/Rust compiler. In particular, float→int casts (`i64()`, `i32()`) MUST truncate toward zero. _Source: design guideline, 2026-06-06._
- COL strings MUST be byte-oriented UTF-8. `len` MUST count bytes, `substr` and `indexof` MUST take and return byte offsets, and no string operation may interpret the encoding — non-ASCII text MUST pass through the compiler and the runtime byte-exact. A codepoint-aware API MUST be additive and explicitly named; `len` MUST NOT be redefined to count characters. COL strings MUST also remain immutable: every operation returns a new string, and no operation may mutate one in place. _Source: [ADR 0004](../adr/0004-col-string-representation.md)._
