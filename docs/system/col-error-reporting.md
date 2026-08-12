# COL error reporting

The COL front-end keeps the grammar (`Col.g4`) liberal and rejects language-rule violations during semantic analysis, so errors name the broken rule instead of producing ANTLR token dumps. Because the parse succeeds, one mistake no longer discards the rest of the input: independent errors accumulate in `CompilationErrorListener` and are thrown together at the end of `ColSemanticsParser.parse`.

Adding a rule of this kind follows a fixed shape:

1. Widen the rule in `Col.g4` so the malformed form parses.
2. In `ColSyntaxVisitor`, carry the violation into the AST — either a flag on a statement node (`FunCallStatement.hasCall`, `ValDeclarationStatement.usesEquals`) or a dedicated marker expression node (`ChainedRelationalExpression`, `MalformedFloatLiteral`).
3. Report it from a semantics component that calls `reportError`. Expression markers get an `ExpressionSemanticsParser` registered by node class in `ColSemanticsParser`; statement-level checks live in the relevant statement parser (`FunCallSemanticsParser`, `ValSemanticsParser`, `FunDefPass1SemanticsParser`).

Marker expression nodes implement `TypedExpression` and return the type the construct would have if it were valid (`ChainedRelationalExpression` returns `Bool`, `MalformedFloatLiteral` returns `F64`), so overload resolution does not fail before the dedicated error is reported.

`IdentifierDerefSemanticsParser` follows the same rule on both its error paths — undefined variable, and a reference to a function that is not user-defined. Each reports the error and returns the node with `I64` set on its identifier, so the enclosing operator's type check gets a type and the undefined name is reported the same way in operand position (`1 + x`, `1 div x`, `-x`, `~x`) as on its own. `AbstractSemanticsParserComponent.getType` maps a null type to the same `I64`, so a component that returns an untyped node still degrades to a reported error rather than a `NullPointerException`.

The `I64` fallback can add a follow-on error: `fun f() -> f64 := 1.0 + x` reports *undefined variable: x* and then *cannot add f64 and i64*. That is accepted — it matches the cascading error a bare undefined name produces.

A *failed call* is the one node that keeps a null type rather than falling back, because there is no sensible type to invent for it. `FunctionCallSemanticsParser` therefore stays quiet when an argument's type is null: no overload can match an argument with no type, so the enclosing call's *found no match* is the inner failure travelling outwards, and reporting it would bury the real error under a list of candidate signatures. `call println(go(1.5))` where `go` takes an `i64` reports *found no match for function call: go(f64)* and nothing else. Where components do repeat each other verbatim, `CompilationErrorListener` drops the repeat (see `diagnostics.md`); that is what keeps `"a" + 1` from reporting *cannot add string and i64* twice, once from the operand rule and once from the promotion below it.

Two details that are easy to get wrong:

- Chained relational operators are detected by parse shape — a relational whose left operand is itself an unparenthesized relational — not by operand types, so `(a == b) == c` is left alone while `1 < 2 < 3` is rejected.
- The missing-`else` check lives in the shared `IfSemanticsParser` in `jcc-base`, not a COL-specific parser; `IfExpression` and `AbstractTypeManager` tolerate a null else so that check is what reports it. Only COL uses these shared classes today. `TailPositionValidator` tolerates it too, since it walks a function body that has already been type checked.
- `IfSemanticsParser` returns the if-expression with its *parsed* branches on both error paths — a missing `else` and branches that agree on no type. Returning the original leaves unresolved identifiers in the tree, and the enclosing function definition then asks an untyped node for its type. `AbstractTypeManager.ifExpression` falls back to the then type for branches that do not agree, rather than reporting the mismatch a second time in worse words.
