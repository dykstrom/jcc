# Diagnostics

Every error and warning the compiler reports reaches the user through one path: a front end
calls `CompilationErrorListener.error`/`warning`, and `Jcc.showMessages` prints what the listener
collected, sorted by line. Nothing else writes a diagnostic to stderr.

## Output format

A diagnostic is a header line followed by the quoted source line and a caret:

```
program.bas:1:10 error: unknown type 'DOBLE'; did you mean 'DOUBLE'?
    1 | DIM a AS DOBLE
      |          ^
```

The header is `file:line:column severity: text`. The quote block is rendered by `SourceQuoter`
(`jcc-compiler`), which reads the source file once and caches its lines. The line number is right
aligned in a five character gutter followed by `" | "`. Tabs in the quoted line are copied into the
caret line as tabs, so the caret stays aligned however wide the terminal renders them.

The quote block is best effort: if the source file cannot be read, or the message points at a line
the file does not have, only the header line is printed. That happens for real — a message about
`<EOF>` can be reported one line past the end of the file.

**Columns are 0 based everywhere inside the compiler.** `CompilationMessage.column()` holds the
ANTLR `charPositionInLine`, and `showMessages` adds one for display. `SourceQuoter.quote` takes the
0 based column, so pass `message.column()` unchanged.

Messages are sorted by line, then by column — `CompilationMessage.compareTo`. Ordering on the line
alone leaves two messages about the same line in the order they were reported, which for a syntax
error is the order the parser backtracked in, not the order the reader scans.

`CompilationErrorListener.error` drops an error whose message *and* position match one already
collected. Semantic analysis keeps going after an error so it can collect the rest, which means
several components reach the same faulty expression and each complains — an enclosing construct
asking that expression for its type reports what the expression itself already reported. Two
identical sentences at one position add nothing.
The same message at *different* positions is two mistakes and is kept. Warnings are not deduped.

Binary operands are kept off that path rather than deduped afterwards: `BinarySemanticsParser`
(jcc-base, used by COL and Tiny) skips operand promotion once an operand type rule has reported,
because promotion would only restate the same mistake in different words — and different words
survive the dedup. `UnarySemanticsParser` is its unary counterpart. Both build the message from the
operand types and the operator's verb, never from the expression, so the AST's internal spelling
(`%` for `mod`, `-1` for `true`) cannot reach a diagnostic.

## Operands their operator does not accept

Every language reports these itself, from an `OperandTypeRule`, and they all read the same:

```
cannot subtract string and integer
cannot divide string and integer: both operands must be integers
cannot bitwise-not string: the operand must be an integer
```

The verb is the operator's, registered with the rule; the types are the operands'; the expression
is never rendered. It is a verb rather than a symbol because the symbol is not shared — BASIC
writes `\` and `MOD` where COL writes `div` and `mod` — and it is the operand types rather than
the expression because the expression would be the AST's spelling, not the programmer's. Issue #86,
item 10 replaced BASIC's four older messages with this family: *illegal expression: "a" % 2*,
*expected subexpressions of type integer: "a" OR 1*, *expected numeric subexpression* and
*expected subexpression of type integer*.

COL and Tiny state each operator as a `BinarySemanticsParser`/`UnarySemanticsParser` registered in
`ColSemanticsParser`; BASIC, whose semantics parser is one class rather than a registry, has the
same pairs in `BasicSemanticsParser.OPERATORS`, keyed by expression class and consumed by its two
`checkType` methods. An operator missing from that table is checked as `NUMERIC` and named after
its node class, because a missing entry must refuse a program loudly rather than let it through:
`^` had no check at all until item 10 listed it, and `PRINT "a" ^ 2` reached clang, which rejected
generated IR the programmer never wrote.

`AbstractTypeManager.promoteNumeric` therefore no longer throws *illegal expression*: it returns
`Unknown` for operands that are not both numeric, the language having reported them already. That
also retired `ColTypeManager`'s own fallback for the same expressions. The `catch (SemanticsException)`
in `BasicSemanticsParser.getType` and `AbstractSemanticsParserComponent.getType` stays as a net for
a language-specific `getType`; nothing in the tree throws from there today.

## One type error, one message

An expression whose type could not be determined has `Unknown.INSTANCE` as its type, and **every
check accepts an unknown type instead of comparing it**. Without that, the compiler invents a type
to carry on with and then reports it: `b = 1 - "x"` with `b` a string used to report the illegal
expression *and* an assignment of a `double` to a string — the double being what the failed type
computation fell back to, and sorting before the real message. Issue #86, item 9.

The unknown type is produced wherever a type computation fails, and nowhere else:
`AbstractSemanticsParserComponent.getType` and `BasicSemanticsParser.getType` (a reported
expression, or a node that came back with no type at all), `ColTypeManager.getType` (operands the
operator rejected), `AbstractTypeManager.promoteNumeric` (an operand already unknown, so the throw
above does not fire a second time for one mistake), and `IdentifierDerefSemanticsParser` (a name
that resolved to nothing). It never reaches code generation: an unknown type exists only where a
diagnostic exists, and semantic analysis fails the compilation before the backend runs.

It is accepted at the choke points wherever there is one — `AbstractTypeManager.isAssignableFrom`,
which is final and asks the language only about types it knows; `OperandTypeRule.accepts`, so no
operator demands anything of an operand already reported; `BinarySemanticsParser`, which skips
promotion; and the two function-call parsers, which stay quiet about a call that matched no
overload when an argument is unknown, exactly as they do for the null type of a failed call. A
check written against a concrete type guards itself: BASIC's `isKnown` helper covers `IF`, `WHILE`,
`ON ... GOTO`, `RANDOMIZE`, `SLEEP`, the bitwise and relational operators, negation, array
subscripts and type specifiers.

Two consequences worth knowing. A construct that *defines* something must define it anyway when its
initializer was rejected — `ValSemanticsParser` adds the value with its declared type, or with the
unknown type when there is none — or every later use is reported as an undefined name, which is one
message per use for a mistake already named. And an unknown type must never be rendered: a message
that prints `unknown` is a missing guard, not a message. `getTypeName` returns `"unknown"` for it
so that such a slip is legible rather than a crash.

## Error recovery in BASIC

BASIC is line oriented: `line: stmtList commentStmt? NEWLINE`, so a statement ends at the end of
its line. ANTLR's `DefaultErrorStrategy` knows nothing about that and resumes at whatever token is
in the follow set, often mid-line. Inside a block body (`line*`) that derails the block rule
itself, and every following line of the block then fails in turn — one mistake used to produce a
diagnostic on each remaining line, including a `WHILE without matching WEND` naming a loop that was
correctly terminated further down.

`BasicErrorStrategy` adapts the strategy to the line rule:

- **`recover`** consumes the rest of the line. It also consumes the terminator when the failing
  context is a block body (`program`, `ifThenBlock`, `elseIfBlock`, `elseBlock`, `whileStmt`),
  because there no `line` rule is left to match it. Inside a statement the terminator is left
  alone, so the enclosing `line` closes normally.
- **`sync`** skips the whole line, rather than deleting a single token, when the parser is between
  the statements of a block and the line ahead cannot be one. Deleting one token leaves the rest of
  the line to be parsed as if it were a statement.
- **One error per line.** Everything after the first error on a line is a guess about text the
  parser has already lost track of, so only the first is reported. The line carrying the rest of an
  expression that ran off the end of the line before it is silent too — it is the second half of a
  mistake already reported.
- **Unterminated-block messages are suppressed once an error has been reported inside the block's
  body**, since the parser is then there by recovery rather than because the terminator is missing.
  An error on the block's *opening* line does not suppress it — that line is the header.
- **A terminator with nothing open for it to close is named** — `WEND without matching WHILE`,
  `END IF without matching IF`, and the same for `ELSE` and `ELSEIF`. The check runs before the
  unterminated-block one, because a terminator whose own opener is not open says more than the
  block the parser happens to be inside does. See [basic-language.md](basic-language.md).

The trade-off in the last point is deliberate: a program with both a typo inside a block and a
genuinely missing terminator reports the typo and stays quiet about the terminator until it is
fixed. `BasicParserRecoveryTests` pins all of this, including that independent mistakes on
different lines are still all reported in one compile.

## Front-end requirements

A new front end must, when it builds its lexer and parser:

1. Call `removeErrorListeners()` on **both** before adding the jcc listener. ANTLR installs a
   `ConsoleErrorListener` by default and never removes it, so skipping this prints every syntax
   error twice — once in ANTLR's `line N:M ...` format with a 0 based column, and once in jcc
   format with a 1 based one, which reads as two unrelated errors. The same applies to test code
   that builds a parser by hand.
2. Call `Antlr4Utils.checkParsingComplete` after the start rule. It reports `unexpected 'X'`,
   naming the token the parser stopped at, when the parse ended before EOF.

`checkParsingComplete` only fires for a grammar whose start rule does not match `EOF` itself —
Tiny, COL and Assembunny. The BASIC grammar ends `program: NEWLINE? line* EOF`, so ANTLR's own
error strategy reports the mistake before the check is reached; the check remains as a backstop.

BASIC test code does not repeat that setup. `BasicTests.parseProgram(text, errorListener)` builds
the lexer and parser the way `BasicSyntaxParser` does — both sets of listeners removed,
`BasicErrorStrategy` installed, `checkParsingComplete` called — and reports to the listener it is
given: `BasicTests.ERROR_LISTENER` throws on the first syntax error, and
`Antlr4Utils.asBaseErrorListener(CompilationErrorListener())` collects them all. `BasicTests` also
holds `assertLines`, `assertMessageContains` and `assertNoMessageContains`. Assertions are made on
`CompilationMessage.msg()`, the text the compiler prints, not on the message of the exception
behind it.

Where a language wants better wording than ANTLR's token dumps, it overrides the error strategy
(`BasicErrorStrategy`) or keeps the grammar liberal and reports later — from semantic analysis (see
[col-error-reporting.md](col-error-reporting.md)), or from the syntax visitor when the mistake is
purely syntactic, as BASIC's two-word `ELSE IF`, its unsupported QuickBASIC statements and its
C-style `==`, `!=`, `&&` and `||` are. Which route applies is not a style choice: a
mistake on a *block header* line has to be parsed, because rejecting it there makes the parser
abandon the block rule and orphan every terminator inside it, and no recovery can undo that.
Which of the two reports it depends on what the message needs. The visitor reports a mistake the
parser can name on its own, because semantics adds nothing to a keyword or an operator. Semantics
reports a mistake that needs a name looked up first, as BASIC's type name after `AS` does.
BASIC still has many token dumps left;
rewording them construct by construct is issue #86, which uses the liberal-parse route. The error
strategy owns only what the parser alone can see: recovery, and the six structural mistakes it can
name — an unterminated block, a terminator with no block open for it to close, a reserved word used
as a variable name where the grammar cannot accept one, a statement continued
onto the next line after a trailing `;` or `,`, an expression that runs off the end of its line, and
an unterminated string literal in the one position the grammar's own alternative for it cannot
reach. The continued statement and the run-off expression are the same mistake from either
side, and both point at `_`; see [basic-language.md](basic-language.md).
