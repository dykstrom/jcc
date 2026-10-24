# 0010. Keep hidden loop state in entry-block stack slots

*2026-09-30*

## Context

A FOR loop evaluates its end and step once, before the loop, and must keep both values for the whole loop. The source program has no variable for them. The obvious choice is an SSA value computed before the loop. That value exists only on the path through the loop setup. BASIC allows a `GOTO` into a loop body, and on that path the value would not exist, so LLVM rejects the IR. We also considered hidden globals. They work for BASIC, where all code runs in `main`. They give wrong results once COL functions contain loops, because a recursive call would overwrite the caller's values. A third option was to give a slot only to values that are not literals. It produces the same IR after Clang's optimizations, but it adds a second code path.

## Decision

We will store the end and step values in synthetic locals of the enclosing function, named `.for.<label>.end` and `.for.<label>.step`. The generator adds them to the function's symbol table, and `FunDefCodeGenerator` emits their `alloca`s in the `entry` block. This is how `RuntimeGcCodeGenerator` already handles `.gc.slot.N`. Every loop gets both slots, even when the values are literals.

## Consequences

The IR is valid on every path into the loop body, including a `GOTO` from outside the loop. Each function call gets its own slots, so recursion is safe. From `-O1` up, Clang turns the slots back into SSA values. At the default `-O0`, each iteration loads the end and step from memory. A `GOTO` into the body skips the setup, so the loop reads slots that hold no value, and its result is undefined, as in QuickBASIC. A code generator can only add such slots inside a function. `ForCodeGenerator` throws `IllegalStateException` when no function encloses the loop. Later statements that need hidden state across jumps, such as a `DO` loop, can use the same technique.
