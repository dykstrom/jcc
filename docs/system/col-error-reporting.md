# COL error reporting

The COL front-end keeps the grammar (`Col.g4`) liberal and rejects language-rule violations during semantic analysis, so errors name the broken rule instead of producing ANTLR token dumps. Because the parse succeeds, one mistake no longer discards the rest of the input: independent errors accumulate in `CompilationErrorListener` and are thrown together at the end of `ColSemanticsParser.parse`.

Adding a rule of this kind follows a fixed shape:

1. Widen the rule in `Col.g4` so the malformed form parses.
2. In `ColSyntaxVisitor`, carry the violation into the AST — either a flag on a statement node (`FunCallStatement.hasCall`, `ValDeclarationStatement.usesEquals`) or a dedicated marker expression node (`ChainedRelationalExpression`, `MalformedFloatLiteral`).
3. Report it from a semantics component that calls `reportError`. Expression markers get an `ExpressionSemanticsParser` registered by node class in `ColSemanticsParser`; statement-level checks live in the relevant statement parser (`FunCallSemanticsParser`, `ValSemanticsParser`, `FunDefPass1SemanticsParser`).

Marker expression nodes implement `TypedExpression` and return the type the construct would have if it were valid (`ChainedRelationalExpression` returns `Bool`, `MalformedFloatLiteral` returns `F64`), so overload resolution does not fail before the dedicated error is reported.

Two details that are easy to get wrong:

- Chained relational operators are detected by parse shape — a relational whose left operand is itself an unparenthesized relational — not by operand types, so `(a == b) == c` is left alone while `1 < 2 < 3` is rejected.
- The missing-`else` check lives in the shared `IfSemanticsParser` in `jcc-base`, not a COL-specific parser; `IfExpression` and `AbstractTypeManager` tolerate a null else so that check is what reports it. Only COL uses these shared classes today.
