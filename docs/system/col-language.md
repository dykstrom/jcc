# COL language

COL is a toy language that exists only in this repo — it has no internet presence, so nothing about it can be assumed from outside knowledge. It is mostly a playground for humans. The source of truth for syntax is the grammar `jcc-col/src/main/antlr4/se/dykstrom/jcc/col/compiler/Col.g4`; for built-in functions it is `ColSymbols.java`. This file records the semantics neither of those can express.

COL is an imperative, statically typed language with functional elements, inspired by BASIC, C, Go, and others. Its guiding style is *words over symbols*: `and` instead of `&&`, `div`/`mod` for integer division, `if c then a else b` instead of `c ? a : b`. Exceptions are the familiar arithmetic, relational, and bitwise operators, which stay symbolic.

The LLVM backend is the primary development target for COL. The FASM backend still compiles COL but is being phased out; behavior differences are resolved in the LLVM backend's favor.

## Program structure

A program is a sequence of top-level statements. There are six; nothing else is implemented (no mutable variables, no structs). Strings compile and run on the LLVM backend — literals, concatenation, equality, `println`, string-typed `val`s, and parameters and return values (see Types):

- `call f(args)` — call a function as a statement, discarding its return value. Top-level `call` statements run in order; they are the program's "main".
- `fun name(p as type, ...) -> rettype := expr` — define an expression function. The body is a single expression; there are no statement bodies. Functions may be defined before or after their uses. Overloading by arity and parameter types is allowed.
- `alias Name as type` — define a type alias, for scalar types or function types.
- `import lib.fn(types) -> type [as name]` — import a function from an external library. **FASM-only; not supported by the LLVM backend and may be removed entirely. Do not use imports in examples or tests.**
- `val name [as type] := expr` — declare an immutable value (see below). **LLVM-only; the FASM backend rejects it.**
- `while cond do <statements> end` — loop while `cond` (which must be `bool` — no integer truthiness) holds (see below).

### While

`while cond do ... end` repeats its body while the boolean condition holds. The body grammar accepts any statement, but semantically only `call`, nested `while`, and `val` are allowed — `fun`/`alias`/`import` in a body are errors. A while body is its own scope: a `val` declared inside the loop is invisible after it and may not shadow a name visible from the enclosing scope. Because COL has no mutable variables, a loop's condition can only change between iterations through a side-effecting call (notably `millis()`); a condition built only from `val`s or literals yields a loop that either never runs or never terminates. See `while.col`. The construct itself compiles on both backends (the shared code-gen framework handles it), but a `val` in the body is LLVM-only like every `val`, so a loop with a `val` body only compiles on the LLVM backend.

### Vals

`val limit as i64 := 10_000` declares an immutable top-level value; `val phi := 1.618` infers the type from the initializer. The initializer is required (its absence is a semantic, not syntactic, error) and is evaluated at runtime, in source order with the other top-level statements — so a val may be initialized from a function call (`val start := millis()`), and since functions are hoisted, the called function may be defined later in the file. A val must be declared above any top-level statement that references it, and vals are *not* visible inside `fun` bodies (a future `const` is planned for that; see issue #54). Duplicate val names and vals sharing a name with any function are errors; function parameters may shadow vals. With a declared type, the initializer must match it exactly or widen implicitly (`i32` → `i64`, `f32` → `f64`); there is no implicit int→float conversion. Function-typed vals are supported: `val f as (i64) -> i64 := inc` uses the declared type to pick among overloads of `inc` (untyped form requires exactly one overload), and `f` is then callable like any function. See `values.col`.

Comments are `//` to end of line. Identifiers start with a letter, followed by letters, digits, or underscores.

COL has two ways to loop: the `while` loop and recursion. Deep recursion should use `become` (see Tail calls below) to guarantee constant-stack tail calls at every optimization level; without it, deep recursion relies on Clang's optimizer and overflows the stack at the default `-O0` (see `fac.col`, `fib.col`). Recursion is still the only way to carry a changing value across iterations, since there are no mutable variables.

## Types

`i32`, `i64`, `f32`, `f64`, `bool`, `string`, and function types written `(i64, i64) -> i64`. Integer literals default to `i64`, float literals to `f64`. `void` is not a usable type name (it appears only in FASM import signatures).

Literals: decimal with optional `_` separators (`10_000`), binary `0b0010`, hex `0xfe` (digits in either case: `0xfe` ≡ `0xFE`), floats `0.99`, `1.5`, `1e9` (exponent marker `e` or `E`), booleans `true`/`false`. A decimal point must have digits on both sides: `.99` and `17.` are rejected — the compiler reports *a decimal point must have digits on both sides* — write `0.99` and `17.0`.

Decimal literals take an optional Rust-style type suffix naming one of the scalar numeric types: `17i32`, `17i64`, `1.5f32`, `1E9f64`, `10_000i32`. A float suffix on an integer-shaped literal is allowed (`17f32` ≡ `17.0f32`); the reverse is a syntax error (`1.5i32`). Hex and binary literals take no suffix (`0x17i32` and `0b101i64` are syntax errors; note `f` is a hex digit, so `0x17f32` is just a hex number). A value outside the suffixed type's range is a compile-time error (`5000000000i32`, `3.5E39f32`); boundary values like `-2147483648i32` are accepted. A suffixed literal behaves like any other expression of that type — implicit widening still applies (`17i32 + 1` is `i64`), and mixed int/float arithmetic still errors.

**Strings.** A string literal is delimited by double quotes and may not span a line: `"hello"`. The escapes are the C-style `\n`, `\t`, `\r`, `\\`, `\"` plus the Rust-style codepoint escape `\u{1F600}`, which the front end decodes to the codepoint itself. There is deliberately no `\0`: COL strings are NUL-terminated, so `\u{0}` — the only way to write one — is a compile error, as are an unknown escape, a malformed `\u{...}`, a codepoint that is not a unicode scalar value (above `0x10FFFF`, or a surrogate), and an unterminated literal. Source files are read as UTF-8 (`Antlr4Utils`), so non-ASCII text inside a literal passes through unchanged; identifiers stay ASCII.

`+` on two strings concatenates them (`col_concat_str_str` in libjcccol), and `==`/`!=` compare them by content (`strcmp`). The ordering operators `<`, `>`, `<=`, `>=` reject strings — *cannot order strings: only == and != are defined for string* — ordering is deliberately not in v1 (`ColOperandTypeRules.NOT_STRINGS`, composed into the ordering operators ahead of the numeric rule). No implicit conversion involves `string` in either direction, and the numeric cast built-ins `i32`/`i64`/`f32`/`f64` have no overload taking one. Strings are never null, so there is no null literal. The type is LLVM-only: the FASM backend rejects a string literal with *strings are not supported by the FASM backend*.

Every string operation is byte-oriented; there are no codepoint semantics anywhere in the language. `len` counts bytes and `substr`/`indexof` take byte offsets, so `len("höstlöv")` is 9 rather than 7 and `len("\u{1F600}")` is 4. The terminal does the decoding.

Strings are never null, so a function with nothing to return returns the empty string, and `indexof` reports "not found" as `-1` rather than as a null. `substr` clamps instead of failing: a negative start, or a start at or past the end, yields `""`; a non-positive length yields `""`; a length running past the end is truncated. The clamping is what makes `substr(s, indexof(s, needle), n)` yield `""` when the needle is absent, rather than the first `n` bytes.

Strings are COL's only heap type, and they are garbage collected: COL wires the same `RuntimeGcCodeGenerator` BASIC does, so a concatenation is registered with the collector and rooted, and `-print-gc`/`-initial-gc-threshold` work as they do for BASIC (see `docs/GarbageCollection.md` and `code-generation.md`). Nothing in the language exposes this — there is no free and no scope rule. Concatenation allocates, with one exception: at `-O1` two string literals inside a statement the AST optimizer descends into are folded to a single literal (see `code-generation.md`).

Immutability is a permanent design choice, not a consequence of COL currently lacking mutable variables: every operation returns a new string, and no in-place mutation is planned. That is what lets a pointer be shared freely between rooted slots, and what keeps a string a leaf object with no interior pointers for the collector to trace (its `jcc_gc_type_t` descriptor is `NULL`). See `docs/adr/0004-col-string-representation.md`.

Not in v1: trim, case conversion, replace, split/join, `starts_with`/`ends_with`, repeat, string→number parsing, printf-style formatting, regular expressions, ordering comparisons, string arrays, and any codepoint-aware operation. The byte model is the v1 contract, so a future codepoint API must be additive and explicitly named rather than a change to `len`.

COL is explicit about types: only conversions guaranteed lossless are implicit — integer widening (`i32` → `i64`) and float widening (`f32` → `f64`), per `AbstractTypeManager.canPromote` and `BinarySemanticsParser`. Everything else, including `i64` → `f64`, requires an explicit cast via the built-in cast functions `i32()`, `i64()`, `f32()`, `f64()`. Mixed int/float arithmetic like `1 + 2.0` is a semantics error.

Implicit widening applies to a val initializer with a declared type, to a function argument, and to a function's or anonymous function's declared return type — the body is then wrapped in the cast. It does not apply to `become`, which requires the callee's return type to equal the enclosing function's exactly (see Tail calls).

Functions are first-class: pass them by name, accept them as function-typed parameters, return them, and call the parameter (`function_types.col`). A function value may also be written inline as an anonymous function (see below). Type aliases work for function types: `alias F2 as (i64, i64) -> i64`. Only *user-defined* functions can be used as a function value, though — referencing a built-in or library function by name (e.g. passing `max` rather than calling it) is a semantic error, because only user-defined functions are emitted as addressable globals. Calling a built-in directly is of course fine.

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

### Anonymous functions

`fun(a as i64, b as i64) -> i64 := a + b` is an anonymous function: a `fun` expression with no name, of type `(i64, i64) -> i64`. Parameter types are required, exactly as for a named function, but the return type may be omitted and is then inferred from the body: `fun(a as i64) := a + 1`. An explicit return type is still checked against the body, and still allows implicit widening. Anonymous functions are first-class wherever a named function is: passed as an argument, bound to a `val`, or returned from a function. See `anonymous_functions.col`.

**Not closures.** The body is type-checked in a scope built from the *global* symbol table, the same way a named function body is. An enclosing function's parameters, an enclosing anonymous function's parameters, and top-level `val`s are therefore simply not in scope, and referencing one is an *undefined variable* error at any nesting depth. This is deliberate: with nothing captured, there is no environment to heap-allocate and no need for a stronger GC.

**Greedy body.** The body is a full expression, so it extends as far right as it can. Inside an argument list the trailing `,` or `)` ends it, so `apply(fun(a as i64) := a + 1, 5)` is a call with two arguments. To use an anonymous function as an operand of a larger expression, parenthesize it. There is no IIFE: `functionCall` still requires an identifier callee, so a lambda must be bound to a `val` (or received as a parameter) before it can be called.

**Lifting.** Semantic analysis replaces each anonymous function with a synthesized top-level function named `lambda.<n>` plus a reference to it, and prepends those definitions to the program's statement list — which is where both backends look for the functions to emit, and why the definition must precede its use for the FASM backend. A user identifier cannot contain a `.`, so the synthesized name can never collide with a user function, before or after mangling. That is what `@lambda.0_I64` in generated IR is. No node of the anonymous-function AST class survives into code generation, so both backends handle lambdas through the machinery they already have for named functions; only the LLVM backend is tested.

**`become`.** A `become` in an anonymous function body is checked by the same tail-position rules as in a named function; errors name it "the anonymous function" rather than a function name.

## Tail calls (`become`)

Recursion is COL's only loop, so a tail call must not grow the stack. Prefixing a function call with `become` makes the tail call a language guarantee: the LLVM backend emits it as `musttail`, which is honored at every optimization level (including the default `-O0`). A plain tail call has no such guarantee and overflows the stack at `-O0` on deep recursion.

```
fun fac_iter(n as i64, result as i64) -> i64 :=
    if n <= 1 then result else become fac_iter(n - 1, n * result)
```

`become` is explicit so the compiler can reject false beliefs about tail recursion. The wrapped call **must be in tail position** — the function's final action. Tail position is small, because a function body is a single expression: the body itself, the `then`/`else` branches of an if-expression that is itself in tail position, and parenthesized forms of those. Nothing else — not operands of any operator (`n * become f(...)` is rejected), not call arguments, not the if-condition. A `become` outside tail position is a compile error that names the construct consuming its result, which effectively teaches the accumulator rewrite.

Three further rules:

- **Exact return type.** The callee's return type must *equal* the enclosing function's declared return type — implicit widening is disallowed here (it is allowed for an ordinary return). A widening `i32`→`i64` would be a `sext` *after* the call, which destroys tail position at the IR level, so `become` is stricter than a plain call and the error says why.
- **User-defined callees only** (v1). `become` to an external/library/built-in function (e.g. `println`) is rejected; those keep the C calling convention, whereas COL-internal functions are compiled with `tailcc` (the convention built for guaranteed tail calls), which is what makes cross-overload and mutual tail recursion valid. `become` on a function-typed parameter is deferred.
- **LLVM backend only.** The FASM backend rejects `become` ("not supported by the FASM backend"); it is being phased out.

Self-recursion, cross-overload recursion (`fac_iter(n)` tail-calling the two-arg `fac_iter`), and mutual recursion all work. Pinned by `ColSemanticsParserBecomeTests`, `ColLlvmCodeGeneratorBecomeTests`, and `ColLlvmCompileAndRunIT` (deep and mutual recursion at `-O0`).

## Built-in functions

Defined in `ColSymbols.java` (the authoritative list, with exact overloads):

- Casts: `i32`, `i64`, `f32`, `f64` — each accepts all four numeric types, including its own. A same-type cast is the identity: `ColLlvmFunctions` inlines it to its argument and emits nothing.
- Rounding: `ceil`, `floor`, `round`, `trunc` — `f32`/`f64`, return the same type as their argument.
- Math: `abs` (all four numeric types), `min`/`max` (same-type pairs), `sqrt` (`f32`/`f64`). Float-only (`f32`/`f64`): `pow`, `cbrt`, `fmod` (each two same-type args / one for `cbrt`), `fma` (three args, `a*b + c`), `sin`, `cos`, `tan`, `atan`, `exp`, `exp2`, `log`, `log2`, `log10`. Most are LLVM intrinsics; `cbrt`/`fmod` are direct libm calls. LLVM-backend only.
- Time: `millis() -> i64` — milliseconds since some epoch, implemented in `libjcccol`.
- Strings: `len(s) -> i64` (libc `strlen`); `substr(s, start, length) -> string`; `indexof(s, needle) -> i64`, `-1` when absent; `string(x) -> string`, overloaded for `i64`, `f64` and `bool`; `readln() -> string`; `eof() -> bool`. All byte-oriented (see Types). The source string is the first argument throughout, deliberately, so that a future method-call sugar — `"hello".indexof("ll")` meaning `indexof("hello", "ll")` — stays available; `indexof` is spelled without an underscore to sit alongside the equally terse `substr`. `len` maps straight to libc and allocates nothing; the rest are `col_*` symbols in libjcccol, and every `string`-returning one hands its fresh block to the collector (see `standard-libraries.md`). LLVM-backend only, like `millis`.
- `println(x) -> i32` — overloaded for `bool`, `f32`, `f64`, `i32`, `i64`, `string`, and `(i64) -> i64`. The `string` overload prints the UTF-8 bytes verbatim. Provisional: the signature is expected to become printf-like eventually, but the current form will be around for a while. It returns `i32` — the number of characters printed, since it forwards `printf`'s return value — which can be exploited to sequence side effects in expression functions (see `fib.col`).

Output formatting: floats print with six decimals (`5.3` → `5.300000`); booleans print `true`/`false` on the LLVM backend, because `println(bool)` is inlined as `println(string(b))` in `ColLlvmFunctions` and so shares `col_string_bool` with the `string` built-in — the two can no longer disagree. Printing a boolean therefore allocates a string, which the collector registers and roots like any other. The FASM backend still prints `-1`/`0`, a divergence that will be resolved by phasing FASM out.

## Examples

Every file in `jcc-compiler/src/examples/col/` is a real program that must compile with the LLVM backend. (The folder used to also hold non-compiling design sketches — `design.col`, `test.col`, `hello.col` — which have been removed.)
