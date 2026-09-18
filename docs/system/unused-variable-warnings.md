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

## What counts as a usage, and where a warning points

`use(name)` is called from every place a variable is read, and reading is not only
dereferencing it: `FunctionCallSemanticsParser` calls it for a name the symbol table holds as a
*value*, because calling a value of function type is a use of that value as much as passing it on
is. Without that, `val inc := successor()` followed by `call inc(41)` was reported unused, and so
was a function-typed parameter a function body only called - `fun apply(f as ..., x as i64) := f(x)`
reported `unused variable: f`.

`declare(name, node)` takes the node the warning points at, and that node is the **name**, not the
statement that declares it: `ValSemanticsParser` passes the `DeclarationAssignment`, whose position
`ColSyntaxVisitor` sets to the identifier rather than to the `val` keyword, and `ParameterBinder`
passes the parameter's `Declaration`. A warning about a value that points at a keyword makes the
reader find the name themselves.

The same rule applies to the undefined-variable warning, which is not the tracker's but follows
from the same idea: it is reported where the variable first appears. In BASIC that includes the
target of a `LINE INPUT`, which is an assignment target like any other and goes through
`BasicSemanticsParser.identifierNameExpression`; `LineInputStatement` keeps its target as an
`IdentifierNameExpression` so the position survives into the message. It used to be defined
nowhere, which left the warning to whatever statement read it afterwards, pages away from the
`LINE INPUT` the reader has to edit.

Note the two languages differ on where a global can be used: BASIC globals may be used
inside functions, whereas COL top-level vals are not visible in function bodies (referencing
one there is an "undefined variable" error), so a COL val can only be used in the main
program.
