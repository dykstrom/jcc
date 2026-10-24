# 0008. `type` replaces `alias` as COL's only type-naming construct

*2026-09-25*

## Context

COL named types with `alias Name as T`, which is fully transparent: `Meters`, `Feet` and `f64` were one type. GitHub issue #94 added `type Name as T`, an opaque type that unifies with neither its underlying type nor another opaque type. With both constructs, two keywords differ only in whether the name unifies, and nothing in the syntax shows which is which. Choosing `alias` by mistake is silent, because the program is still well-typed. Three options were weighed:

- **Option A.** Keep both constructs. This keeps transparent names for long function types, but it leaves the silent mistake in place.
- **Option B.** Keep only `type`, and make it always opaque.
- **Option C.** Use one keyword with a modifier, `type X as distinct T`. This makes the transparent form the default and adds syntax to every declaration.

## Decision

We will keep only `type`, which is always opaque (Option B). We removed `alias` outright, with no deprecation period, because no example program used it. A program that still uses `alias` gets ANTLR's plain syntax error.

## Consequences

A reader always knows that a named type is distinct from its underlying type. This matches earlier COL choices that put clarity before convenience, such as rejecting string ordering. The cost falls on named function types:

- Every callback boundary needs an explicit wrap, as in `Cmp(larger)`.
- A function that receives a `Cmp` cannot pass it on where the structural type `(i64, i64) -> i64` is expected, because no conversion unwraps it.
- Code that wants a transparent function type must write the structural type inline.

The word `alias` is now an ordinary identifier.
