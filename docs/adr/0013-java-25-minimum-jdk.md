# 0013. Java 25 as the minimum JDK

*2026-10-24*

## Context

JCC was built for Java 21, and JDK 25 is the current LTS release. One option was to
build and test on JDK 25 but keep producing Java 21 bytecode, so that `jcc` still runs
on Java 21. The other option was to raise both the bytecode target and the required JDK
to 25. JCC is a developer tool that users run as a jar, so asking them to install a
current LTS JDK costs them little.

## Decision

We will require Java 25 to build and to run JCC. The compiler `release`, the Kotlin
`jvmTarget` and the enforcer rule `requireJavaVersion [25,)` all use 25.

## Consequences

The code can use the language features and APIs of Java 22 to 25. Development, CI and
users all use the same JDK version. Users on Java 21 to 24 can no longer run `jcc` and
must install JDK 25. Kotlin must be 2.3 or later, because earlier versions cannot write
Java 25 bytecode. Temurin publishes no JDK 25 for Windows arm64, so the Windows arm64
jobs in `windows.yml` and `release.yml` use Microsoft OpenJDK. A future change to the
CI JDK setup must keep a source of JDK 25 for that platform.
