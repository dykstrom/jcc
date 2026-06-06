# Standard libraries (libjccbas, libjcccol)

Compiled programs link against per-language native runtime libraries: **libjccbas**
for BASIC and **libjcccol** for COL. They are versioned independently of jcc
(`libjccbas.version`, `libjcccol.version` in the parent POM) and published as
GitHub release archives, one per OS/arch.

## How jcc calls library functions

jcc does **not** compile against the libraries' C headers. The code generators emit
calls to library symbols by name, and the linker resolves them at link time:

- **LLVM backend** links the static archive `libjcc{bas,col}.a` via
  `clang -L<dir> -l<stdlib>` (`LlvmAssembler`).
- **FASM backend** imports from `libjccbas.dll` (Windows) through the generated
  import section.

The functions jcc knows about are declared in code, not discovered from the library:

- `jcc-basic/.../compiler/LibJccBasBuiltIns.java` (BASIC, ~48 functions)
- `jcc-col/.../compiler/LibJccColBuiltIns.java` (COL)

Each entry is a `LibraryFunction(internalName, argTypes, returnType, libName,
new ExternalFunction(symbol))`. The `ExternalFunction` string is the **exact exported
symbol** jcc emits — for BASIC it is often signature-mangled (`add_Str_Str`,
`instr_I64_Str_Str`, `mid$_Str_I64`), for COL it is the plain name (`millis`). The
`libName` comes from `FunctionUtils` (`LIB_JCC_BAS = "libjccbas.dll"`,
`LIB_JCC_COL = "libjcccol.a"`) and is the import/link library each backend references.
Because resolution is purely by symbol name, renaming or removing an exported symbol
in a library breaks jcc — treat exported symbols as a stable ABI.

## Build-time acquisition (jcc-compiler/pom.xml)

`os-maven-plugin` sets `native.classifier` (e.g. `macos-arm64`) and
`native.archive.type` (`tar.gz` on Unix/macOS, `zip` on Windows) per OS/arch profile.
Then, in `jcc-compiler`:

1. **initialize** — antrun `check-and-install-native-libs[-col]`: if the archive is
   absent from `~/.m2/repository/se/dykstrom/jcc/libjcc{bas,col}/<version>/`, download
   it from `https://github.com/dykstrom/libjcc{bas,col}/releases/download/v<version>/`
   and `install:install-file` it into the local repo.
2. **generate-resources** — `maven-dependency-plugin:unpack` extracts both archives to
   `target/temp-extract`; antrun `unpack-native-libs` copies `libjccbas.*` /
   `libjcccol.*` into `target/`.
3. **package** — the resources block copies `libjccbas.a`, `libjccbas.dll`, and
   `libjcccol.a` into the distribution `bin/`.

## Finding exported functions and their signatures

### A function already used by jcc

Look it up in jcc's own tables — `LibJccBasBuiltIns` / `LibJccColBuiltIns`. Each `JF_*`
constant gives the jcc-typed signature (arg types, return type) and the exact external
symbol (`new ExternalFunction(...)`). The matching language-level built-in — the name
written in source — is the corresponding `BF_*` constant in `BasicSymbols` /
`ColSymbols`. For the documented C signature, read the header in the downloaded archive
(below). When a header name and jcc's `ExternalFunction` symbol disagree (BASIC
mangling), the `ExternalFunction` string and `nm` output are ground truth.

### Adding a newly exported library function

When a library exports a function jcc does not yet expose, first confirm the symbol and
its C signature, then wire it through jcc.

Confirm the symbol and signature:

1. Read the header in the downloaded archive — extract
   `~/.m2/repository/se/dykstrom/jcc/libjcc{bas,col}/<version>/libjcc…-<classifier>.<type>`
   and look in `include/` (COL: `jcccol.h`, `jcccol/core.h`) or `inc/` (BASIC:
   per-function headers, e.g. `chr.h`, `mid.h`):
   ```
   tar xzf ~/.m2/repository/se/dykstrom/jcc/libjcccol/0.1.0/libjcccol-0.1.0-macos-arm64.tar.gz
   ```
2. Confirm the exact exported symbol with `nm libjcc….a` (e.g. `_millis`). The GitHub
   source repos `dykstrom/libjccbas` / `dykstrom/libjcccol` hold the same headers and
   design notes (libjcccol `docs/ARCHITECTURE.md`).

Wire it into jcc (worked example: `millis` in COL):

1. **Library function** — add a `JF_*` constant to `LibJcc{Bas,Col}BuiltIns`:
   `new LibraryFunction(".millis", List.of(), I64.INSTANCE, LIB_JCC_COL, new ExternalFunction("millis"))`.
   The `ExternalFunction` string must equal the exported symbol; arg/return types must
   match the C signature.
2. **Language built-in** — add a `BF_*` constant to `BasicSymbols` / `ColSymbols`
   (`new BuiltInFunction("millis", List.of(), I64.INSTANCE)`) and register it with
   `addFunction(...)` so the source-level name is callable and type-checked.
3. **Backend mapping** — map `BF_* → JF_*` in the relevant backend table(s):
   `ColLlvmFunctions` / `ColAsmFunctions` for COL, `BasicLlvmFunctions` /
   `BasicAsmFunctions` for BASIC (e.g. `addToLibraryMap(BF_MILLIS, JF_MILLIS)`). Wire
   each backend you intend to support — `millis` is wired only into the LLVM backend.
4. Cover the new function with tests, mirroring the existing `*Functions`/codegen tests.

## read_line and console input (LINE INPUT)

`read_line` (BASIC, `JF_READ_LINE`) reads one line from stdin and returns it with the
trailing newline stripped. Neither backend's `LINE INPUT` code generator prints a
newline after the call: when stdin is an interactive terminal the terminal echoes the
newline as the user types, so emitting one would produce a blank line. A consequence is
that the `LINE INPUT;` form's `inhibitNewline` flag has no effect — the terminal's echo
can't be suppressed from the generated program. Honoring it would require `read_line` in
libjccbas to take over echo via terminal raw mode (`termios` / console mode), which is
not implemented. The LLVM `LineInputCodeGenerator` matches the FASM backend here.
