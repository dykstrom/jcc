# Security

Operational rules in MUST voice; cite the source (ADR, contract, incident) for each.

- External toolchain processes (assembler, clang) MUST be invoked via `ProcessBuilder` with an argument list, never assembled into a shell command string. This preserves freedom from shell injection by construction. _Source: current code (`ProcessUtils.setUpProcess`)._
- JCC MUST NOT be relied on as a security boundary for compiling untrusted source. All command-line input — source, output path, `-assembler`, `-assembler-include`, `--library-path` — is assumed trusted; path validation and temp-file hardening are explicit non-goals. _Source: threat-model decision, 2026-06-03._

## Still to document

- If the trusted-input threat model ever changes, what MUST be validated at the trust boundary (executable paths, output paths, source-directory write safety)? Record the driving ADR or incident when that happens.
