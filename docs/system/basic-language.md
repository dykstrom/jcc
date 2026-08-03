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

## Implicit arrays must reach the AST, not just a symbol table

An array used without a `DIM` is defined implicitly (QuickBASIC does this), by
`BasicSemanticsParser.defineImplicitArray`. Registering it in the semantics parser's
symbol table is **not enough**: `CompilerFactory` hands the semantics parser and the
code generator *separate* `SymbolTable` instances, and static arrays are emitted by
`AbstractLlvmCodeGenerator.generateGlobals` from the code generator's table, which is
populated from `VariableDeclarationStatement` nodes in the AST. So `parse` prepends a
synthetic `VariableDeclarationStatement` holding every implicit declaration. Routing it
through the normal declaration statement is also what makes the FASM backend work — it
has its own `VariableDeclarationCodeGenerator` doing the same registration.

Consequences to keep in mind. The subscripts of a synthetic `ArrayDeclaration` must
be **pre-adjusted** for BASIC's inclusive upper bound (upper bound 10 → subscript 11);
`arrayDimensionSizes` evaluates them as sizes directly. The array must be registered
with `symbols.addGlobalArray`, not `addArray`, so that an array first used inside a
`DEF FN` body — parsed under `withLocalSymbolTable` — still lands in the root scope.
And the `implicitArrays` list is cleared at the top of `parse`, because it feeds the
*returned* AST: a parser instance reused for a second program (only tests do this) would
otherwise prepend the first program's declarations to the second one's statements.

The declaration is prepended at index 0, so it precedes any `OPTION BASE` — an order
source code is not allowed to use. That is safe, and the reason is worth knowing before
changing it: `optionBaseStatement` runs during the traversal of the *input* statements,
so the synthetic declaration never reaches that check, and neither backend's
`VariableDeclarationCodeGenerator` emits code for an array — each only registers it in
the code generator's symbol table, with the storage itself emitted later from that table
(`generateGlobals` on LLVM, the data section on FASM). So the base is still set before
anything reads it, which `LBOUND`/`UBOUND` on an implicit array confirms. Array
allocation that *did* depend on the base — QuickBASIC's `OPTION BASE 1` makes upper
bound 10 mean 10 elements, not the 11 JCC always allocates — would break this, and the
declaration would then have to be inserted after any leading `OPTION BASE` instead.

## `ident(args)` in an expression is not necessarily a function call

The grammar has no `arrayElement` alternative in `factor`, so a read like `a(3)` parses
as a `FunctionCallExpression`; only the assignment target uses `arrayElement`. That
makes `BasicSemanticsParser.functionCall` the disambiguator, and its branch order is
load-bearing:

1. known array with matching numeric subscripts → array access
2. known function → function call
3. known array, wrong subscript count or non-numeric subscript → error
4. one or more args, all numeric → implicitly defined array
5. otherwise → `undefined function`

Functions are checked *before* the array diagnostics (3) so that a name which is both an
array and a function still resolves as a function. Branch 4 means a mistyped call with
numeric arguments is no longer an error — `PRINT foo(17)` silently becomes an array, as
in QuickBASIC. That is deliberate, and matches how a mistyped *scalar* has always been
treated; `-Wundefined-variable` is what surfaces both.

## One symbol table per test method, not per `parse()` call

`AbstractBasicSemanticsParserTests` holds `symbolTable` as a field, and JUnit's default
per-method lifecycle makes it fresh for each test method — but every `parse()` call
*within* one method shares it. So a second `parse()` re-using a variable name fails with
"variable 'a' is already defined", which reads like a parser bug and is not one. Give each
`parse()` in a method distinct names (`dim a(10)`, `dim b%(10)`, `dim c$(10)`), as the
existing tests do.

## A `$` in a BASIC identifier needs no escaping in a Kotlin test string

Kotlin only starts a string template when `$` is followed by an identifier character or
`{`, so `"dim a$(3) as string"` and `"print s$"` are plain strings — no `\$`, and no `$$`
multi-dollar prefix. Write them unescaped, as the array tests do. The escape is needed
only when the `$` really does begin an identifier, as in the expected message
`$$"$DYNAMIC arrays not supported yet"`.

## Array subscript mismatches must be reported before the node is rebuilt

`ArrayAccessExpression`'s constructor (in `jcc-base`) asserts that the identifier type is
an `Arr`, that the subscript list is non-empty, and that its size equals the array's
dimension count. `withIdentifier` and `withSubscripts` re-run the constructor, so a
semantics-parser branch that detects a subscript mismatch must `reportError` and return
the *original* expression — building the updated node instead throws `AssertionError`
in place of the diagnostic. `BasicSemanticsParser.arrayAccessExpression` does this.

These are Java `assert`s: Surefire enables `-ea`, so the mismatch surfaces as a test
failure, but a released compiler has assertions off and would carry the broken AST into
code generation. Treat the assert as a test-only backstop, not the check itself.

## End of line is a statement terminator, so multi-line rules must say so

`NEWLINE` is a real token (`WS` covers only spaces and tabs), and `line` ends with it.
Any rule that is meant to span more than one line therefore has to name `NEWLINE`
explicitly — `whileStmt`, `ifThenBlock`, `elseIfBlock` and `elseBlock` all do. Leave it
out of a new block rule and the rule simply will not parse; there is no implicit
continuation to fall back on any more.

Three details of the lexer make the rest work:

- `NEWLINE : LINEBREAK ([ \t]* LINEBREAK)*` matches a line break plus any blank lines
  after it, so blank lines collapse into one token and no grammar rule needs an
  empty-line alternative. The `[ \t]*` is load-bearing: a line holding nothing but
  spaces or tabs would otherwise close one `NEWLINE` and open another, and the second
  one has no statement in front of it. Indented blank lines are common in Kotlin
  raw-string test sources, so the symptom is `extraneous input 'end of line'` in the
  integration tests while every unit test still passes.
- `CONTINUATION : '_' [ \t]* LINEBREAK -> skip` implements QuickBASIC's explicit
  continuation. It must stay before `NEWLINE`. `COMMENT` and `STRING` match the
  underscore first, which is why neither can be continued — QB's `REM` restriction
  falls out for free rather than being enforced anywhere.
- `@lexer::members` overrides `nextToken()` to synthesize a final `NEWLINE` before
  `EOF`. Without it every rule would need an EOF alternative, and the several hundred
  single-line `parse("10 print 1")` tests would all have to grow a trailing newline.

`line` ends with `commentStmt? NEWLINE`, because a comment may trail the last statement
without a `COLON` in front of it — `PRINT 1 ' why not`. `visitLine` appends it to the
line's statements, which is the shape it had when newlines were skipped. Forgetting this
breaks a very common idiom while leaving all 20 examples compiling, because they put
comments on lines of their own; the only test that covered it was a Windows-gated FASM
integration test.

`line` has a bare-label alternative (`labelOrNumberDef stmtList? NEWLINE`) because the
examples put `GOSUB` targets on their own line. `BasicSyntaxVisitor.visitLine` turns
that into `LabelledStatement(label, CommentStatement)` — same trick as `visitElseIfBlock`
— so the label survives as a jump target without generating code.

`ifThenBlock` is listed **before** `ifThenSingle` in `ifStmt`. Both can start `IF expr
THEN commentStmt`, and ANTLR resolves such an ambiguity in favour of the earlier
alternative; QuickBASIC says a comment after `THEN` still opens a block.

## Unterminated blocks are diagnosed in the error strategy

`BasicErrorStrategy` replaces ANTLR's token dump with "IF without matching END IF, IF at
line N" (and the `WHILE`/`WEND` equivalent). It hooks `reportError`, `reportMissingToken`
and — the path a missing terminator actually takes — `reportUnwantedToken`, which
`sync()` reaches first. Two gates keep it honest: the *innermost* rule context must be
the block itself, and the offending token must be a block-boundary token (`EOF`, `END`,
`WEND`, `ELSE`, `ELSEIF`). Without the second gate, recovery from an ordinary error
inside a block body lands back in the block's context and gets mislabelled.

Those two gates are not enough on their own: a boundary token *does* legitimately turn up
in the block's context after the parser has recovered from an error deeper inside the
body, and the message then names a block the reader can see is terminated. So a third gate
suppresses the message once an error has already been reported inside the body. An error on
the block's *opening* line does not suppress it — that line is the header, not the body.
The cost is that a program with both a typo in a block and a genuinely missing terminator
reports only the typo; see `docs/system/diagnostics.md` for why that trade is taken.

## A trailing `;` or `,` does not continue a statement

`PRINT "a" ;` followed by a continuation line parsed fine while newlines were skipped, and
became an error the moment end of line turned into a statement terminator. The trailing
separator is legal and useful on its own — it suppresses the line break in the output —
so the mistake can only be recognized from the *following* line failing to parse.

`BasicErrorStrategy` reports it against the separator rather than against the token that
actually failed, because the separator is the character to change, and points at `_` as the
fix. Two guards keep it off unrelated errors: the failing line must begin with a token that
could continue an expression, and there must be a line in front of it. A line beginning
with a statement keyword is a statement of its own, however badly the line before it ended.

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
