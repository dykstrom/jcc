# Contributing to JCC

Thanks for your interest in JCC. This document describes how the repository is
organized into branches and how changes make their way into a release.

For build and test commands, see [AGENTS.md](AGENTS.md).

## Branch model

JCC uses two long-lived branches:

- **`master`** — the stable release branch, and the default branch on GitHub.
  Every tagged release is cut from here. It should always build.
- **`dev`** — the active integration branch. All day-to-day work lands here
  first and is stabilized before the next release.

## Feature branches

Create a branch off `dev` for each change. Name it `type/short-description`,
where `type` is one of:

| Prefix | Used for |
|--------|----------|
| `feature/` | New functionality |
| `fix/` | Bug fixes |
| `docs/` | Documentation only |
| `ci/` | Build and CI changes |

For example: `fix/process-output-pipe-deadlock`.

## Landing a change

1. Open a pull request from your feature branch into `dev`.
2. Merge it as a **squash merge**, so `dev` keeps a single, clean commit per
   change.
3. Delete the feature branch after it is merged.

## Releases

At release time, `dev` is merged into `master`. The release itself is driven by
the Maven release plugin, which tags the release version and then bumps the
project to the next development iteration — the
`[maven-release-plugin] prepare release` and
`[maven-release-plugin] prepare for next development iteration` commits.

## Hotfixes

An urgent bug fix may be committed directly on `master` and then merged back into
`dev` to keep the two branches in sync. In practice this is rare — JCC is a
single-maintainer project, so almost everything flows through `dev`.
