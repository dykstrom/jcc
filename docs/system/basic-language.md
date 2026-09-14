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

## Unsupported QuickBASIC statements are parsed so that they can be named

`FOR ... NEXT`, `DO ... LOOP`, `SELECT CASE`, `SUB`, `FUNCTION`, `TYPE`, `EXIT`, plain
`INPUT`, `PRINT USING`, `OPEN`/`CLOSE`, `LOCATE`/`COLOR`, `REDIM`/`ERASE` and
`DATA`/`READ`/`RESTORE` are not implemented. Use `WHILE ... WEND` for counted loops (the
idiom in every BASIC integration test), and see `docs/languages/basic.md` for the full
table of replacements.

None of them is rejected by the grammar. `unsupportedStmt` parses the keyword and then
consumes the rest of the line unparsed, and `BasicSyntaxVisitor.visitUnsupportedStmt`
reports it and returns a `CommentStatement`, so a label in front of it survives. This is
issue #86's parse-liberally-verify-later pattern, reported in the visitor for the same
reason `ELSE IF` is: the mistake is a keyword, and semantics has nothing to add.

Four things follow from that choice, and are easy to undo by accident:

- **The keywords are soft keywords**, alternatives of `ident` as well as tokens of their
  own. They were plain identifiers before, and `DATA`, `TYPE`, `NEXT` and `STEP` are
  ordinary variable names, so every rule that takes an identifier — including
  `labelOrNumber` and `labelOrNumberDef` — takes them too. `assignStmt` is listed before
  `unsupportedStmt` in `stmt`, which is what makes `for = 1` an assignment. Alternative
  order only decides between parses that both reach the end of the line: `END SELECT`
  reaches `unsupportedStmt` even though `endStmt : END` comes earlier, because `END` on
  its own leaves `SELECT` in front of the line's `NEWLINE`.
  `BasicErrorStrategy.EXPRESSION_START_TOKENS` lists the soft keywords for the same
  reason, and has to be kept in step with the `softKeyword` rule.
- **`OPEN` and `CLOSE` are the file statements.** The tokens for `(` and `)` are `LPAREN`
  and `RPAREN`, renamed to free those two names. Java code that says `BasicParser.OPEN`
  meaning `(` still compiles, and matches the `OPEN` keyword instead.
- **A block is reported once.** The visitor counts the unsupported blocks it has opened,
  so `NEXT`, `LOOP`, the `CASE`s of a `SELECT CASE`, and the `END` forms are silent when
  their opener was reported, and reported on their own when it was not.
- **A `TYPE` block's members are not parsed.** They are declarations rather than
  statements, so `age AS INTEGER` still fails with an ANTLR message of its own, after the
  message naming the block. Every other unsupported block holds statements, which parse.

The keyword tokens spell out three cases (`FOR`, `For`, `for`) like every other keyword,
so a mixed-case `FoR` lexes as an identifier and is not named. That is #68, not this.

## The type name in an `AS` clause is resolved in semantics, not in the grammar

`varDecl` and `paramDecl` take `AS typeName`, and `typeName` is any `ident`. The grammar
has no type tokens at all: `TYPE_DOUBLE`, `TYPE_INTEGER` and `TYPE_STRING` are gone, so
`double`, `integer` and `string` are ordinary identifiers everywhere else, and any name may
be written after `AS`.

`BasicSyntaxVisitor.declaredType` carries the name into the AST as a `NamedType` rather
than resolving it, and `BasicSemanticsParser.resolveDeclaredType` resolves it, reporting an
unknown name (`unknown type 'DOBLE'; did you mean 'DOUBLE'?`) or one of the QuickBASIC
types JCC lacks (`type 'SINGLE' is not supported by JCC; use 'DOUBLE'`, from
`UNSUPPORTED_TYPES`). This is issue #86's parse-liberally-verify-later pattern, reported in
semantics rather than in the visitor because the check is name resolution, the same place
COL resolves its type names.

Four consequences:

- **A reported declaration keeps a usable type**, the replacement type for a QuickBASIC
  type and otherwise the type the declaration would have had without the `AS` clause. The
  checks after it — type specifier, duplicate name, subscripts — then run normally, so one
  bad type name does not hide the rest of the program, and several are reported in one
  compile.
- **`NamedType` carries the position of the name**, which is why it is a class rather than
  a record and why it compares equal regardless of position, like the AST nodes. Without it
  the caret would point at the variable rather than at the type name that is wrong;
  `JccTests.shouldQuoteSourceLineForError` pins that column.
- **An array declaration holds the name inside its `Arr`**, so `resolveDeclaredType`
  rebuilds the `Arr` and the `ArrayDeclaration`. `Declaration.withType` cannot be used
  there: it returns a plain `Declaration` and would drop the subscripts.
- **A function definition's parameter types are resolved too**, and
  `functionDefinitionStatement` then rebuilds the `Fun` type on the statement's identifier
  from them. The identifier is what code generation reads, so leaving it unresolved would
  carry a `NamedType` into the backend.

`BasicSyntaxVisitorTests` pins that the visitor leaves the name unresolved;
`BasicSemanticsParserTypeNameTests` pins the messages and the multi-error case.

## Implicit arrays must reach the AST, not just a symbol table

An array used without a `DIM` is defined implicitly (QuickBASIC does this), by
`BasicSemanticsParser.defineImplicitArray`. Registering it in the semantics parser's
symbol table is **not enough**: `CompilerFactory` hands the semantics parser and the
code generator *separate* `SymbolTable` instances, and static arrays are emitted by
`AbstractLlvmCodeGenerator.generateGlobals` from the code generator's table, which is
populated from `VariableDeclarationStatement` nodes in the AST. So `parse` prepends a
synthetic `VariableDeclarationStatement` holding every implicit declaration.

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
(`generateGlobals`). So the base is still set before
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
comments on lines of their own.

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

## `ELSE IF` is parsed so that it can be rejected

`elseIfBlock` accepts `(ELSEIF | ELSE IF)`, and `BasicSyntaxVisitor.visitElseIfBlock` reports the
two-word spelling. Accepting it is not leniency — the program is still refused — it is the only way
to say anything useful about it.

Rejecting `ELSE IF` in the grammar costs the whole block. The failure lands on a block *header*
line, so the parser gives up on `elseIfBlock` before reaching its `line*` body; the block's own
`ELSEIF`s, `ELSE` and `END IF` are then all orphaned, one per diagnostic. That is not something
error recovery can repair — no amount of resynchronizing reconstructs a rule the parser has already
abandoned — which is why this is fixed in the grammar and not in `BasicErrorStrategy`. The same
reasoning applies to any future mistake on a header line.

Two forms must keep working, and both are tested: a single-line `IF a THEN PRINT 1 ELSE IF b THEN
PRINT 2` (valid QuickBASIC — an `ELSE` holding a single-line `IF`, reached through `elseSingle`, not
`elseIfBlock`), and a nested block `IF` on its own line inside an `ELSE`.

This is issue #86's parse-liberally-verify-later pattern, with the report in the syntax visitor
rather than in `BasicSemanticsParser`: the mistake is a keyword spelling, so there is nothing for
semantics to add, and an AST carrier would exist only to defer the message by one phase. The
visitor's `CompilationErrorListener` is the same instance the semantics parser holds, so
`BasicSemanticsParser.parse`'s `hasErrors` check is what aborts the compile.

## The C-style operators are parsed so that they can be rejected

`relExpr` has two alternatives beyond BASIC's six relational operators, for `==` and `!=`;
`andExpr` and `orExpr` have one each, for `&&` and `||`. `BasicSyntaxVisitor.reportCStyleOperator`
names the operator to write instead, from the `C_STYLE_RULES` and `C_STYLE_OPERATORS` tables.

No spelling meant anything before: `==` lexed as two `EQ` and `&&` as two `AMPERSAND`, both failing
in the parser, and neither `!` nor `|` lexed at all, so `!=` and `||` failed in the *lexer* and
stopped the compile before anything else was reported. `EQ_EQ`, `BANG_EQ`, `AMP_AMP` and
`PIPE_PIPE` are therefore pure additions — no existing program lexes differently. (Issue #86,
item 3, calls the second token `NE_WRONG`; the tokens are named for their shape here, like `GE`
and `LE`.)

`&&` sits in `andExpr` and `||` in `orExpr`, so they get BASIC's precedence for `AND` and `OR` —
which is also C's relative order for the two, and below the relational operators in both languages.
`a || b && c` therefore means what a C programmer expects.

The visitor returns the expression the programmer meant — `EqualExpression`, `NotEqualExpression`,
`AndExpression`, `OrExpression` — not a placeholder. The mistake is unambiguous, the operands are
fine, and returning the real expression is what lets the rest of the program be analysed: several
wrong operators, and any unrelated mistake, are reported in one compile.

Note that `AND` and `OR` take integer operands, so a reported `&&` or `||` whose operands are not
integers draws the ordinary type errors on top of the operator message. That is issue #86 item 9's
cascade, not something this item introduces.

Three details:

- **The suggestion is the user's own source text**, cut from the `CharStream` over the `relExpr`
  interval with the operator replaced, not `ctx.getText()`. ANTLR's `getText` concatenates token
  text with no separator, so it would print `a%==1` — a string the user never wrote — which is the
  defect #86's honorable mention is about.
- **An expression spanning two lines gets no rewrite**, only the rule (`BASIC uses '=' for
  equality, not '=='`). The source text of a continued expression contains the `_` and the line
  break, and neither belongs in a message; collapsing the whitespace is not an option because a
  string literal in the expression would be collapsed too.
- **A glued `!=` names both readings.** `a!=b` is genuinely ambiguous: QuickBASIC reads it as the
  single-precision type suffix `!` followed by `=`, a programmer arriving from C means inequality,
  and JCC supports neither. `isGlued` looks at the character before the token in the `CharStream`
  — a suffix binds to its name, so a space rules it out — and the glued form offers both rewrites
  rather than guessing. When item 7 adds `!` as a suffix the parser will lex `a! = b`, and this
  message is what points the user at it.

Reported from the visitor rather than from `BasicSemanticsParser` for the same reason as `ELSE IF`
above: the mistake is an operator, so semantics has nothing to add.

A C-style `!` for `NOT` is left out. `!=` is a two-character token, so none of this needs a `!`
token of its own; a prefix `!` would need one, and that token is item 7's business — it is the
single-precision type suffix. How one `BANG` token behaves in both positions is decided there.

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

## An expression that runs off the end of its line

Splitting a long expression across two lines without a trailing `_` — the way most other
languages allow — is the same mistake seen from the other side, and it produced the worst
messages in the front end: `mismatched input 'end of line' expecting {')', ','}`, a 9-token
dump for a trailing operator, or `no viable alternative at input 'PRINT1+\n'`, which quotes a
string the user never wrote, newline escape and all.

`BasicErrorStrategy.reportExpressionRunOffLine` fires only when the offending token *is* the
line break (or `EOF`), so a mistake anywhere else on the line keeps its own message — `PRINT
foo(1 2` fails on the `2` and is left alone. It then names the token that leaves the line
unfinished: a trailing operator (`expression expected after '+'`, and `OPERATOR_TOKENS` is
what makes a token count as one), or failing that the innermost `(` that the line never
closes. The trailing operator wins when a line has both, being the more precise of the two.

Two consequences of pointing at `_`:

- The suggestion is appended only when the *next* line begins with something the expression
  could have continued with, so an incomplete last line, or one followed by a statement
  keyword, gets the diagnosis without advice about a continuation that isn't there.
- When the suggestion is appended, the next line holds the rest of the expression and cannot
  parse on its own, so `continuationLine` suppresses everything reported on it. That is the
  second half of a mistake already named; without it, one split expression yielded two
  messages, the second one a 26-token dump.

The check runs *after* the unterminated-block check, which matters only for `EOF`: an open
block whose last line also ends in an operator is better described by its missing terminator.

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
