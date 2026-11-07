# 0018. COL replaces constant references at -O1

*2026-11-07*

## Context

Supersedes 0015. ADR 0015 kept every COL constant reference a load from its global at `-O1`, for two reasons:

- The constant substitution looked a name up in the global symbol table, so it replaced a function parameter that shadows a constant.
- The shared division folds threw on a constant zero divisor, which COL allows in an if-expression branch that is not taken.

Both reasons are gone. `DefaultAstOptimizer` now optimizes a function body in a local symbol table that holds the parameters. That change fixed the same shadowing bug for a BASIC `DEF FN` parameter. A COL `val` cannot shadow a constant, so a parameter was the only way to shadow one. Since ADR 0017, the shared folds call `ConstantEvaluator.fold`, which leaves a node unchanged when its value is undefined. The alternative was to keep the identity fold, and COL would keep no AST-level constant substitution.

## Decision

We will remove the identity fold for `IdentifierDerefExpression` from `ColAstOptimizer`. COL then uses the constant substitution in `DefaultAstExpressionOptimizer`, as BASIC does.

## Consequences

At `-O1`, a reference to an integer, float or string constant becomes a literal. A reference to an opaque or `bool` constant stays a load from its global, because the substitution handles neither type. At `-O0`, every reference still loads from the global. The substitution is correct only while every scope that can shadow a constant is a scope that `DefaultAstOptimizer` knows about. A new COL construct that lets a local name shadow a constant must add its names to the optimizer's local symbol table. A fold built on `ColConstantEvaluator` no longer needs literal operands to avoid the shadowing bug, because the optimizer's symbol table now holds the parameters.
