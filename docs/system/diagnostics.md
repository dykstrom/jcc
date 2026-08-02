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

Where a language wants better wording than ANTLR's token dumps, it overrides the error strategy
(`BasicErrorStrategy`) or keeps the grammar liberal and reports in semantic analysis
(see [col-error-reporting.md](col-error-reporting.md)).
