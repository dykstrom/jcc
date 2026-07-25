# COL

COL is a small language that exists only in this project — a playground for
experimenting with language and compiler ideas. It is an imperative, statically
typed language with functional elements, inspired by BASIC, C, Go, and Rust.

Its guiding style is *words over symbols*: `and` instead of `&&`, `div` and `mod`
for integer division, and `if c then a else b` instead of `c ? a : b`. The
familiar arithmetic, relational, and bitwise operators stay symbolic. The LLVM
backend is the primary target for COL.

## Example

Recursion is one of COL's two loop mechanisms (the other is the `while` loop).
Because deep recursion must not grow the stack, COL provides `become`, which turns
a tail call into a guaranteed constant-stack call at every optimization level. The
program below computes factorials with both plain and tail recursion:

```
// Plain recursion
fun fac(n as i64) -> i64 :=
    if n <= 1 then 1 else n * fac(n - 1)

// Tail-recursion, made a language guarantee with become
fun fac_iter(n as i64) -> i64 :=
    become fac_iter(n, 1)

fun fac_iter(n as i64, result as i64) -> i64 :=
    if n <= 1 then result else become fac_iter(n - 1, n * result)

// Main
call println(fac(5))
call println(fac_iter(5))
```

## Language summary

- **Types** — `i32`, `i64`, `f32`, `f64`, `bool`, and function types written
  `(i64, i64) -> i64`. Conversions are explicit except for lossless widening.
- **Functions** — `fun name(p as type, ...) -> rettype := expr` defines an
  expression function (the body is a single expression). Functions are
  first-class and may be overloaded by arity and parameter types.
- **Values** — `val name [as type] := expr` declares an immutable value. COL has
  no mutable variables.
- **Control flow** — `while cond do ... end` loops, and `if cond then a else b`
  as an expression.
- **Tail calls** — prefix a tail call with `become` to guarantee constant-stack
  recursion.
- **Built-ins** — casts (`i32`, `i64`, `f32`, `f64`), math (`abs`, `sqrt`, `pow`,
  `sin`, `log`, ...), `millis()`, and `println(x)`.

## File extension and runtime

COL source files use the `.col` extension. COL executables require the COL
standard library, `libjcccol.a`, which is distributed together with JCC. COL uses
the default LLVM backend; the FASM backend still compiles COL but is deprecated and
will be removed in a future release.
