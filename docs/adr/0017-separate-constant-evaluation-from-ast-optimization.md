# 0017. Separate constant evaluation from AST optimization

*2026-11-07*

## Context

Two classes computed the value of a constant expression at compile time: `ConstantEvaluator`, which COL uses for its constants, and `DefaultAstExpressionOptimizer`, which folds literals at `-O1`. Each class had its own copy of the rules for integer wrapping, f32 rounding, division by zero, `MIN / -1`, NaN comparisons and casts, and the two copies disagreed. The optimizer threw `InvalidValueException` on a divisor that only became zero after folding, so `-O1` failed silently on programs that `-O0` compiled. It also clamped a float cast to an integer that LLVM leaves undefined. ADR 0007 kept constant folding in the optimizer partly because jcc must evaluate `CONST` declarations and array dimensions at compile time. One alternative was to fix the optimizer's copy and keep both. We rejected it, because the two copies would drift apart again. Another alternative was to call the evaluator for every node before the optimizer looks up its component. We rejected it, because a language component would then no longer control its node (ADR 0016), and the optimizer would start to fold COL function calls and logical operators, which changes the generated IR.

## Decision

We will make `ConstantEvaluator` the only code that computes the value of a constant expression. Each built-in component of `DefaultAstExpressionOptimizer` first asks `ConstantEvaluator.fold` for the value of its node, and only rewrites the node itself if the evaluator cannot fold it.

## Consequences

The optimizer keeps only rewrites that are not evaluation, such as `x + 0`, `-(-x)`, choosing a branch of an if-expression, and multiplication to shift. The optimizer leaves a node with an undefined value unchanged and never fails, so a program behaves the same at `-O1` as at `-O0`. A float division that overflows or divides by zero now folds to an infinite or NaN literal, which `LlvmUtils.floatToLlvm` writes in hexadecimal. The optimizer no longer widens an integer operand to float by itself. Semantic analysis must therefore make every widening explicit, as BASIC and COL already do. A language component that replaces a built-in component also replaces its folding. Semantic analysis evaluates `CONST` initializers and static array sizes with `ConstantEvaluator.evaluate` and replaces them with literals, in BASIC as in COL. The optimizer only registers the constants, and the code generator reads the array sizes without an optimizer.
