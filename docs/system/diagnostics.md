# Diagnostics

Every error and warning the compiler reports reaches the user through one path: a front end
calls `CompilationErrorListener.error`/`warning`, and `Jcc.showMessages` prints what the listener
collected, sorted by line. Nothing else writes a diagnostic to stderr.

## Output format

A diagnostic is a header line followed by the quoted source line and a caret:

```
program.bas:1:10 error: mismatched input 'DOBLE' expecting {TYPE_DOUBLE, TYPE_INTEGER, TYPE_STRING}
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
purely syntactic, as BASIC's two-word `ELSE IF` is. Which route applies is not a style choice: a
mistake on a *block header* line has to be parsed, because rejecting it there makes the parser
abandon the block rule and orphan every terminator inside it, and no recovery can undo that.
BASIC still has many token dumps left;
rewording them construct by construct is issue #86, which uses the liberal-parse route. The error
strategy owns only what the parser alone can see: recovery, and the three structural mistakes it can
name — an unterminated block, a statement continued onto the next line after a trailing `;` or `,`,
and an expression that runs off the end of its line. The last two are the same mistake from either
side, and both point at `_`; see [basic-language.md](basic-language.md).
