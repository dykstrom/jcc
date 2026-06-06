# Security

JCC is a command-line compiler with no network surface. Its trust boundary is local: it reads source files supplied on the command line, generates an intermediate file (`.asm` for FASM, `.ll` for LLVM), and invokes an external toolchain to produce a native executable.

## Process invocation

External processes are launched via `ProcessUtils.setUpProcess`, which uses `ProcessBuilder` with an argument **list** (no shell), so command-line arguments are not subject to shell injection. `redirectErrorStream(true)` merges stderr into stdout. The FASM path sets one environment variable (`INCLUDE`); the LLVM path passes an empty environment.

- `FasmAssembler` runs the bundled `fasm.exe` with the `.asm` and output paths as arguments.
- `LlvmAssembler` runs an external `clang` (path configurable, default `clang`) with the `.ll` file, optimization level, and `-L`/`-l` standard-library flags.

## Argument handling

Validated inputs: the source file extension is checked against an allowlist in `Language.fromSource`, and `--backend` is constrained to the `Backend` enum by JCommander.

Not validated: the `-assembler` executable path, `-o` output path, `-assembler-include` (becomes the `INCLUDE` env var), and `--library-path` are passed through to the toolchain as-is, with no existence or path checks. Because invocation is list-form (not shell), these are not injection vectors, but an arbitrary `-assembler` path means an arbitrary executable is run.

## Intermediate files

Intermediate `.asm`/`.ll` files are written next to the source file via `FileUtils.withExtension` — a predictable, writable location — using `Files.write` with no atomic-rename or symlink protection. They are marked `deleteOnExit` unless `-save-temps` or compile-only (`-S`) is set, so they remain readable until the JVM exits.

## Threat model

All input is assumed trusted. JCC is a local developer tool: the person compiling already controls the machine, the source, and the output paths. Compiling an untrusted source program is **not** a supported, sandboxed use case. Accordingly, the unvalidated `-assembler`/`-o`/`-assembler-include`/`--library-path` arguments and the predictable, symlink-unprotected intermediate files above are accepted non-goals, not bugs, under this model.

The bundled `fasm.exe` is committed to the repo and shipped in the distribution zip as-is; there is no checksum or signature verification.
