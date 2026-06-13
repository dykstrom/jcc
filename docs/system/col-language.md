# COL language

COL is a toy language that exists only in this repo — it has no internet presence, so nothing about it can be assumed from outside knowledge. It is mostly a playground for humans. The source of truth for syntax is the grammar `jcc-col/src/main/antlr4/se/dykstrom/jcc/col/compiler/Col.g4`; for built-in functions it is `ColSymbols.java`. This file records the semantics neither of those can express.

COL is an imperative, statically typed language with functional elements, inspired by BASIC, C, Go, and others. Its guiding style is *words over symbols*: `and` instead of `&&`, `div`/`mod` for integer division, `if c then a else b` instead of `c ? a : b`. Exceptions are the familiar arithmetic, relational, and bitwise operators, which stay symbolic.

The LLVM backend is the primary development target for COL. The FASM backend still compiles COL but is being phased out; behavior differences are resolved in the LLVM backend's favor.

## Program structure

A program is a sequence of top-level statements. There are four; nothing else is implemented (no variables, no loops, no strings, no structs):

- `call f(args)` — call a function as a statement, discarding its return value. Top-level `call` statements run in order; they are the program's "main".
- `fun name(p as type, ...) -> rettype := expr` — define an expression function. The body is a single expression; there are no statement bodies. Functions may be defined before or after their uses. Overloading by arity and parameter types is allowed.
- `alias Name as type` — define a type alias, for scalar types or function types.
- `import lib.fn(types) -> type [as name]` — import a function from an external library. **FASM-only; not supported by the LLVM backend and may be removed entirely. Do not use imports in examples or tests.**

Comments are `//` to end of line. Identifiers start with a letter, followed by letters, digits, or underscores.

Since there are no loop primitives, the only way to loop is recursion; deep loops rely on Clang optimizing tail calls (see `fac.col`, `fib.col`).

## Types

`i32`, `i64`, `f32`, `f64`, `bool`, and function types written `(i64, i64) -> i64`. Integer literals default to `i64`, float literals to `f64`. `void` is not a usable type name (it appears only in FASM import signatures). There is no string type yet.

Literals: decimal with optional `_` separators (`10_000`), binary `0b0010`, hex `0xfe` (lowercase digits), floats `0.99`, `1.5`, `1E9`, booleans `true`/`false`. A decimal point must have digits on both sides: `.99` and `17.` are syntax errors — write `0.99` and `17.0`.

Decimal literals take an optional Rust-style type suffix naming one of the scalar numeric types: `17i32`, `17i64`, `1.5f32`, `1E9f64`, `10_000i32`. A float suffix on an integer-shaped literal is allowed (`17f32` ≡ `17.0f32`); the reverse is a syntax error (`1.5i32`). Hex and binary literals take no suffix (`0x17i32` and `0b101i64` are syntax errors; note `f` is a hex digit, so `0x17f32` is just a hex number). A value outside the suffixed type's range is a compile-time error (`5000000000i32`, `3.5E39f32`); boundary values like `-2147483648i32` are accepted. A suffixed literal behaves like any other expression of that type — implicit widening still applies (`17i32 + 1` is `i64`), and mixed int/float arithmetic still errors.

COL is explicit about types: only conversions guaranteed lossless are implicit — integer widening (`i32` → `i64`) and float widening (`f32` → `f64`), per `AbstractTypeManager.canPromote` and `BinarySemanticsParser`. Everything else, including `i64` → `f64`, requires an explicit cast via the built-in cast functions `i32()`, `i64()`, `f32()`, `f64()`. Mixed int/float arithmetic like `1 + 2.0` is a semantics error.

Functions are first-class: pass them by name, accept them as function-typed parameters, return them, and call the parameter (`function_types.col`). Type aliases work for function types: `alias F2 as (i64, i64) -> i64`.

## Expressions

Precedence, lowest to highest (from the grammar):

1. `or`, `xor`, `|`, `^`
2. `and`, `&`
3. relational `==` `!=` `<` `<=` `>` `>=` (non-associative — no chaining)
4. `+`, `-`
5. `*`, `/`, `div`, `mod`
6. unary `-`, `~`, `not`
7. literals, identifiers, function calls, `(expr)`, if-expressions

`and`/`or`/`xor`/`not` are logical operators on `bool`; `and` and `or` short-circuit. `&`/`|`/`^`/`~` are bitwise operators on integers. `/` is float division; `div` and `mod` are the integer operations.

The if-expression `if cond then expr else expr` is COL's ternary; `cond` must be `bool` — there is no integer truthiness.

Evaluation order is defined: function-call arguments evaluate left to right, and a binary operator's left operand evaluates before its right (except where `and`/`or` short-circuit). Pinned by codegen tests in `ColLlvmCodeGeneratorTests` and by `ColLlvmCompileAndRunIT#shouldEvaluateLeftToRight`.

Integer overflow and division by zero are whatever the backend does, with the LLVM backend as the reference.

Floating-point arithmetic follows IEEE 754, with no traps and no fast-math relaxations: division by zero yields `±inf`, `0.0 / 0.0` yields NaN, overflow yields `±inf`. Every comparison with NaN is false except `!=`, which is true (`==` lowers to `fcmp oeq`, `!=` to `fcmp une`, relationals to ordered predicates). One deliberate exception, Go-style: division by a *literal* zero is a compile-time error — a literal zero divisor is almost surely a mistake. Pinned by `ColLlvmCompileAndRunIT#shouldFollowIeee754Semantics`.

## Built-in functions

Defined in `ColSymbols.java` (the authoritative list, with exact overloads):

- Casts: `i32`, `i64`, `f32`, `f64` — each accepts the other three numeric types.
- Rounding: `ceil`, `floor`, `round`, `trunc` — `f32`/`f64`, return the same type as their argument.
- Math: `abs` (all four numeric types), `min`/`max` (same-type pairs), `sqrt` (`f32`/`f64`).
- Time: `millis() -> i64` — milliseconds since some epoch, implemented in `libjcccol`.
- `println(x) -> i32` — overloaded for `bool`, `f32`, `f64`, `i32`, `i64`, and `(i64) -> i64`. Provisional: the signature is expected to become printf-like eventually, but the current form will be around for a while. It returns `i32` — the number of characters printed, since it forwards `printf`'s return value — which can be exploited to sequence side effects in expression functions (see `fib.col`).

Output formatting: floats print with six decimals (`5.3` → `5.300000`); booleans print `1`/`0` on the LLVM backend (`-1`/`0` on FASM — a divergence that will be resolved by phasing FASM out; eventually booleans should print `true`/`false`).

## Examples and design sketches

`jcc-compiler/src/examples/col/` mixes two kinds of files. `fac.col`, `fib.col`, `gcd.col`, `sqrt.col`, `function_types.col`, and `intrinsic_functions.col` are real programs that must compile with the LLVM backend. `design.col`, `test.col`, and `hello.col` are **design sketches** that do not parse — they explore unimplemented ideas (`var`/`val`, `while`, `for`, `struct`, strings, lambdas). None of those features has a decided implementation schedule; do not treat the sketches as documentation of the language.
