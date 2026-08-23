# 0004. COL string representation

*2026-08-12*

## Context

COL needed a text type, and strings are its first heap type, so the representation
decides both which operations are natural and what the garbage collector has to
trace. Four axes were open. Indexing: byte offsets, or codepoint offsets —
codepoint indexing answers `len("höstlöv")` with 7 rather than 9, which is usually
what a user means, but it forces every operation to decode UTF-8, makes `substr`
and `indexof` linear in the offset, and still does not match intent for combining
characters and grapheme clusters, so the "characters" problem has no tidy stopping
point. Layout: a NUL-terminated `char *`, or a length-prefixed struct or fat
pointer — an explicit length makes `len` O(1) and permits interior NULs, but the
result is not a C string, so every call into libc or libjcccol needs a conversion
and the collector must know the object's shape.

Nullability: COL has no `Optional` and no nullable types, so a nullable string
would be the only value in the language needing a null check. Mutability: COL has
no mutable variables today, so immutability could be adopted as a permanent
guarantee or left as an artefact of that, to be revisited when mutable variables
arrive. The byte-oriented model is the one argued for by utf8everywhere.org.

## Decision

We will represent a COL string as an immutable, NUL-terminated, UTF-8,
byte-transparent `char *`. `len` counts bytes, and `substr` and `indexof` take and
return byte offsets. A string is never null: a function with nothing to return
returns the empty string, and `indexof` reports "not found" as `-1`. Immutability
is a permanent guarantee, not a consequence of COL currently lacking mutable
variables — every operation returns a new string.

## Consequences

Non-ASCII text passes through the compiler and the runtime byte-exact, and no
string operation decodes UTF-8, so none can disagree with another about what a
character is. Being a real C string means libc supplies `len` (`strlen`) and
equality (`strcmp`) directly, with no conversion at the boundary.

Immutability is what lets a pointer be shared freely between rooted slots without
copying, and keeps a string a leaf object with no interior pointers — its
`jcc_gc_type_t` descriptor is `NULL`, so the collector traces nothing inside it.
That is what made adopting the collector in COL a matter of wiring
`RuntimeGcCodeGenerator` rather than describing a new object shape.

The cost is that `len` surprises anyone who reads it as a character count, and
that a byte offset can split a multi-byte sequence: `substr("höstlöv", 0, 2)`
yields one byte of a two-byte codepoint, and the terminal prints a replacement
character. Interior NULs are unrepresentable, which is why there is no `\0` escape
and `\u{0}` is a compile error.

Future work is constrained: a codepoint-aware API must be additive and explicitly
named, `len` must not be redefined to count characters, and no operation may
mutate a string in place. Those constraints are recorded as MUST rules in
`docs/architecture/language-semantics.md`; the resulting behaviour is documented
in `docs/system/col-language.md` and `docs/languages/col.md`.
