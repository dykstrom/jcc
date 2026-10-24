# 0009. Reach nested statements through a shared block-statement interface

*2026-09-30*

## Context

Four AST walkers recurse into nested statement lists: `DefaultAstOptimizer`, the after-GOSUB label insertion in `BasicCodeGenerator`, the line-number check in `BasicSemanticsParser`, and `CommandReferenceDetector`. Each of them named `IfStatement` and `WhileStatement`. The FOR loop work adds `ForStatement`, and COL loops or `DO` can follow. A walker without a case for a new node skips everything inside it, and no error is reported. In `CommandReferenceDetector`, a missed case makes `command$` return an empty string on Linux and macOS.

We considered three other shapes. An interface that exposes only the statement lists would still force read-only walkers to name each node to reach its expressions. An interface that can also replace expressions does not fit the optimizer. The optimizer simplifies `x <> 0` to `x` in a condition, which is wrong for a FOR bound, and a FOR statement has an optional STEP. Letting `LabelledStatement` implement the interface was the third option. It wraps exactly one statement, and the two walkers that need its label must handle it before any generic case anyway.

## Decision

We will make every statement that holds nested statement lists implement `BlockStatement` in jcc-base. The interface returns the nested lists, returns a copy with the lists replaced, and returns a read-only list of the statement's own expressions. Walkers reach nested statements through this interface. `LabelledStatement` does not implement it.

## Consequences

A new statement that implements `BlockStatement` reaches the generic case in every walker with no new code there. Walkers that only read, such as `CommandReferenceDetector` and the line-number check, need no change for a new node. A walker that rewrites expressions still needs a case for each node. A new node must therefore add its own case to `DefaultAstOptimizer` to have its expressions optimized. `withBlocks` throws `IllegalArgumentException` when it gets the wrong number of lists. A walker that needs a label must handle `LabelledStatement` itself. The allow-list in COL's `WhileSemanticsParser` does not walk the tree and does not use the interface.
