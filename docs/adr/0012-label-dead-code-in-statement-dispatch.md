# 0012. Label dead code after a jump in the statement dispatcher

*2026-10-01*

## Context

LLVM requires a label at the start of each basic block. When a statement followed a `GOTO` or a BASIC `RETURN` on the same line, its instructions came after the `br` with no label, and Clang rejected the IR. A loop exit has the same problem. We considered a helper that each jump generator calls to emit a fresh label after its branch. That label opens an empty block. When nothing follows the jump, as at the end of `main`, nothing closes the block and the IR is invalid. Each new jump generator would also have to remember to call the helper.

## Decision

We will make `AbstractLlvmCodeGenerator.statement()` emit a fresh label before it generates a statement, when the last instruction is `br`, `indirectbr` or `ret`. A `LabelledStatement` gets no extra label, because it emits its own.

## Consequences

Code after any jump is valid IR, and a new jump generator needs no special code. A label appears only when a statement follows the jump. The extra block is unreachable, and Clang removes it from `-O1` up. A generator that emits a terminator and then more instructions itself, without going through `statement()`, must still add its own label. Comments are skipped when the dispatcher looks for the last instruction.
