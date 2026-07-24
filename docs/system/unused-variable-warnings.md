# Unused-variable warnings

Unused-variable warnings for BASIC and COL are produced by the shared
`VariableUsageTracker` (`jcc-base`, `se.dykstrom.jcc.common.semantics`). It is keyed by
variable name only — two sets, `declaredVariables` and `usedVariables`, with no type or
scope awareness. `declare(name, node)` records a declaration, `use(name)` a usage, and
`check(...)` reports every declared name not in the used set.

Each language's top-level semantics parser calls the no-arg `check(...)` once, after the
whole program is parsed (`BasicSemanticsParser.parse`, `ColSemanticsParser.parse`). This
top-level pass is the sole authority on global variables, because only then are all usages
known.

User-defined functions emulate a nested scope with a save/restore protocol in the
function-definition parser (`BasicSemanticsParser.functionDefinitionStatement`,
`FunDefPass2SemanticsParser.parse`): `save()`, declare each parameter, parse the body, then
`check(parameterNames, ...)`, then `restore(parameterNames)`.

The per-function check MUST use the name-restricted overload
`check(Set<String> names, ...)` with the function's parameter names — never the no-arg
`check()`. The no-arg form inspects every declared name still live in scope, including
globals, and a global used only later in source order has not been recorded as used yet, so
it is wrongly flagged (issue #78). Scoping the check to parameters flags only genuinely
unused parameters and leaves globals to the top-level pass.

`restore(parameterNames)` propagates usages of non-parameter names (globals used inside the
function) out to the enclosing scope while discarding parameter-name usages. Because the
tracker is name-keyed, a global shadowed by a same-named parameter does not inherit the
parameter's usage.

Note the two languages differ on where a global can be used: BASIC globals may be used
inside functions, whereas COL top-level vals are not visible in function bodies (referencing
one there is an "undefined variable" error), so a COL val can only be used in the main
program.
