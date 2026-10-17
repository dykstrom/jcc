# 0011. Name the loop a loop exit leaves by its statement class

*2026-10-01*

## Context

BASIC `EXIT FOR` must leave the innermost `FOR`, and pass through any `WHILE` nested inside it. COL's planned `break` leaves the innermost loop of any kind, and a future `EXIT DO` leaves the innermost `DO`. The shared `LoopExitStatement` therefore has to say which kind of loop it leaves. We considered an enum of loop kinds in jcc-base. Each new loop statement would then need a new enum value, and COL `break` would need an extra value that means any loop.

## Decision

We will name the loop by the class of its loop statement, such as `ForStatement.class`. An exit matches the innermost enclosing loop whose class is assignable to that class.

## Consequences

A new loop statement needs no change to `LoopExitStatement` or `LoopStack`. An interface that several loop classes implement means "any of these loops", so COL `break` can pass one. Every loop code generator must push its statement's class and after-loop label on `LoopStack` while it generates the body. A loop generator that does not push is invisible to exits. A kind that matches no loop is a compiler error, and `LoopExitCodeGenerator` throws `IllegalStateException`.
