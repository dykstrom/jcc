# 0007. Leave arithmetic peephole optimization to LLVM

*2026-08-24*

## Context

`DefaultAstOptimizer` rewrote assignments at `-O1`: `a = a + 1` became an `IncStatement`,
`a = a - 1` a `DecStatement`, and `a = a + 7` / `- 7` / `* 7` / `\ 7` became
`AddAssignStatement`, `SubAssignStatement`, `MulAssignStatement` and
`IDivAssignStatement`. Each had its own LLVM statement code generator. The rewrites date
from the FASM backend, where `add qword [a], 7` was genuinely shorter than a
load-add-store sequence the code generator had no other way to avoid.

The LLVM backend cannot benefit from them. LLVM IR is SSA with explicit memory
operations and has no compound-assignment form, so `a += 7` and `a = a + 7` are both
`load`/`add`/`store` — `AbstractOpAssignCodeGenerator` and the plain
`AssignCodeGenerator` path emit the same three instructions. Compiling a BASIC loop both
ways confirmed it: the `.ll` files differ only in the text of a `;` comment, and after
`clang -O1` the assembly is byte-identical. LLVM also goes much further than the
rewrites express, promoting the globals out of memory entirely so that the load and
store being "optimized" do not reach the object file.

The rewrites could not help at `-O0` either. `DefaultAstOptimizer.program` is gated on
optimization level ≥ 1, and `Assembler` passes the same level to clang as `-O<n>`, so
they only ever fired in builds where LLVM was also optimizing.

Not every AST rewrite is redundant, and the difference is what an operation lowers to.
Arithmetic lowers to IR instructions LLVM reasons about natively. Anything reaching the
runtime libraries lowers to a call LLVM cannot see into: `DefaultAstExpressionOptimizer`
folds `"a" + "b"` to a single string literal, and that removes an `add_Str_Str` call, a
`jcc_gc_register` call, and the whole shadow-stack slot for the result — a literal is not
a heap object needing a GC root. `clang -O2` keeps all of it without the fold, because
`add_Str_Str` is `declare`-only and must be assumed to allocate and to have arbitrary
side effects; even with the definition available through LTO, LLVM would not constant-fold
a heap allocation and copy into a static string.

## Decision

We will not keep AST rewrites whose only effect is to restate something LLVM already
does. The four op-assign statement rewrites, their AST nodes and their code generators
are deleted, along with the `IncStatement`/`DecStatement` rewrites.

The AST optimizer keeps constant folding and constant substitution. These pay for
themselves twice: jcc depends on them internally, because array subscripts and `CONST`
declarations must be evaluated at compile time, and for string-typed operands they
eliminate runtime calls and GC roots that LLVM cannot reach.

## Consequences

`DefaultAstOptimizer.assignStatement` is now one line — optimize the right-hand side —
and the statement dictionaries lose four entries each. `IncStatement` and `DecStatement`
stay, but as Assembunny front-end nodes only: `AssembunnySyntaxVisitor` builds them from
the `inc`/`dec` instructions, and no optimizer produces them. `BasicCodeGenerator` no
longer registers them, because BASIC has no syntax that yields one.

Generated code is unchanged at every optimization level, so the removal is invisible in
compiler output. What is lost is the ability to see `a += 7` in the `.ll` comments and in
an AST dump; the underlying instructions were always the same.

Adding a peephole rewrite to the AST now needs a reason LLVM cannot cover it — a
different `.ll` file, not merely a tidier AST. The practical test is what the operation
lowers to. A rewrite over integers and floats has to justify itself against instcombine
and will usually lose; a rewrite that removes a call into `libjccbas`/`libjcccol`, or a
GC root, is jcc's to make and no one else's. String operations are the clearest case, and
the same reasoning covers any future fold whose operands are runtime-library calls.

One instance remains that this rule would not admit: `DefaultAstExpressionOptimizer`
still strength-reduces multiplication by a power of two to `ShiftLeftExpression`, which
instcombine does anyway. It is kept for now because it is expression-level and tested end
to end, but it is redundant on exactly the grounds above.
