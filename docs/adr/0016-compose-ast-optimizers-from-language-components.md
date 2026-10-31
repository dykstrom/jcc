# 0016. Compose the shared AST optimizers from language components

*2026-10-28*

## Context

`BasicAstOptimizer` extended `DefaultAstOptimizer` and overrode `statement` to add BASIC's statements. `DefaultAstOptimizer` named each statement it optimized in a `switch`, so it never optimized a language statement it did not name, such as COL's `val` and `call`. COL also needs its own fold for `IdentifierDerefExpression` (ADR 0015) and folds for its own nodes, such as `BecomeExpression`. One alternative was a COL subclass of `DefaultAstExpressionOptimizer` that overrides `expression`. We rejected it, because inheritance gives every language-specific fold a subclass and an override, and we want the shared class to hold only generic folds that languages compose.

## Decision

We will build `DefaultAstExpressionOptimizer` as a map from exact expression class to expression optimizer, and `DefaultAstOptimizer` as an ordered list of statement components. A language passes its own `ExpressionComponent`s and `StatementComponent`s to the constructors. It implements `AstOptimizer` by delegating to a `DefaultAstOptimizer`, and does not extend the shared classes.

## Consequences

Language-specific folding lives in the language module, and the shared classes hold only generic folds. A language fold replaces the built-in fold for its class and cannot call it, so a language that wants to extend a built-in fold must write it again. Folds are looked up by exact class, so a subclass such as `ArrayAccessExpression` gets no fold unless one is registered for it. Before, an `ArrayAccessExpression` reached the constant substitution when a BASIC array shared its name with a constant. A fold for a node whose subexpressions the expression optimizer does not visit must optimize them itself.

Statement components match their type and every subtype, and a language's components come before the built-in ones. A new language statement whose expressions must be optimized needs a component, where ADR 0009 says it needs a case in `DefaultAstOptimizer`. BASIC passes its rewrite of `x != 0` to `x` in a condition as a condition strategy, because the rewrite is wrong for a COL `bool` condition.
