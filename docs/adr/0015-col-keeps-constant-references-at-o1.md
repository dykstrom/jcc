# 0015. COL keeps constant references at -O1

*2026-10-28*

## Context

COL got `const`, which the compiler emits as an LLVM `private constant` global through the shared `ConstDeclarationStatement` and `ConstDeclarationCodeGenerator`. At `-O1`, the built-in fold for `IdentifierDerefExpression` replaces every reference to a constant with a literal. That substitution is not safe for COL. It looks a name up in the global symbol table, so it also replaces a function parameter that shadows a constant. It also makes the shared division folds throw on a constant zero divisor, which COL's semantics allow in an untaken if-expression branch. One alternative was to add parameter scopes to `DefaultAstOptimizer`, so that the substitution sees shadowing. We rejected it, because it is a larger change and it touches BASIC's optimizer path. Another alternative was to replace const references with literals during semantic analysis. We rejected it, because the shared const node and code generator emit a global that a reference can load from.

## Decision

We will register the identity fold for `IdentifierDerefExpression` in `ColAstOptimizer`, using the fold components of ADR 0016. A COL constant reference therefore stays a load from its global at every `-O` level, and LLVM folds the load itself, as ADR 0007 argues for arithmetic.

## Consequences

Parameters that shadow a constant stay correct, and COL constants never reach the shared folds as literals. A COL fold built on `ColConstantEvaluator` must fold only nodes whose operands are already literals. Otherwise the evaluator looks up a parameter that shadows a constant and folds it as the constant. The trade-off is that COL loses AST-level constant substitution, so unoptimized code keeps the load.
