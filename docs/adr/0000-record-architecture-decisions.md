# 0000. Record architecture decisions

*2026-06-26*

## Context

The project's significant design choices — language semantics, backend
construction techniques — were recorded only in working notes, which are
explicitly non-authoritative and get superseded or discarded as research moves
on. There was no immutable record of why a choice was made over its
alternatives, so the rationale risked being lost once the notes moved past it.

## Decision

We will record architecturally significant decisions as ADRs under
`docs/adr/`: one immutable file per decision, numbered sequentially
(`NNNN-title.md`), each with Context, Decision, and Consequences as described
in `docs/adr/README.md`.

## Consequences

A contributor asking "why is it built this way?" has a durable answer that
outlives the working notes. The bar stays high — only system-wide,
architecturally significant choices qualify; localized or conventional choices
stay in distilled context (`docs/system/`). An ADR is immutable once shipped; a
change of course is a new ADR that supersedes the old one, never an edit.
