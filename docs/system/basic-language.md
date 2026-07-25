# BASIC language

JCC's BASIC targets QuickBASIC 4.5 semantics (see
`docs/architecture/language-semantics.md`). The source of truth for syntax is the
grammar `jcc-basic/src/main/antlr4/se/dykstrom/jcc/basic/compiler/Basic.g4`; for
built-in functions it is `BasicSymbols.java`. This file records traps that surprise
developers writing `.bas` programs, tests, or examples.

## Keywords are case-insensitive; built-in function names are not

Statement keywords parse in any case (`DIM`, `dim`, `WHILE`, `while`). Built-in
function names must be lowercase: `str$(n)` and `ltrim$(s)` resolve, but `STR$(n)`
fails semantic analysis with "undefined function: STR$". Every BASIC integration
test writes built-in calls in lowercase; follow that.

## No FOR/NEXT loop

QuickBASIC's `FOR ... NEXT` is not implemented — `Basic.g4` has no rule for it and
a `FOR` line fails to parse. Use `WHILE ... WEND` for counted loops (the idiom in
every BASIC integration test).

## Operator precedence is one-level-per-rule

The expression grammar is a layered cascade — `expr → impExpr → eqvExpr → xorExpr
→ orExpr → andExpr → notExpr → relExpr → addSubExpr → modExpr → iDivExpr →
mulDivExpr → factor`. Precedence comes *only* from this layering: every binary
operator sits in its own rule whose right operand is the next-tighter rule, so
one rule = one precedence level. The user-facing order is in
`docs/languages/basic.md`; it matches QuickBASIC 4.5.

Do **not** collapse operators of different precedence into one rule (e.g. putting
`* / \ MOD` together, or `OR XOR EQV IMP` together). Same-rule operators become
one level, and the bug is latent: left-to-right examples like `5 * 10 \ 2` read
the same either way — it only shows when the weaker operator comes first
(`10 \ 4 * 2`, `a XOR b OR c`). `BasicSyntaxVisitorTests` guards the divergent
cases; keep a weaker-operator-first test for any new precedence level.
