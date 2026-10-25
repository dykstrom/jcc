# 0014. Bundle a jlink Java runtime with JCC

*2026-10-24*

## Context

A JCC user had to install two tools: JDK 25 to run `jcc` (ADR 0013), and Clang to build
the executables. The launchers ran `java` from `JAVA_HOME` or the path, so a user with an
older JDK in `JAVA_HOME` got a failure. The release already builds one archive per OS and
CPU, on a runner of that platform. JCC uses only the `java.base` module. The alternatives
were to keep asking users to install JDK 25, or to bundle a runtime that `JAVA_HOME` or a
new `JCC_JAVA_HOME` variable could override.

## Decision

We will put a Java runtime made by `jlink`, with only `java.base`, in each archive. The
`jcc` and `jcc.bat` launchers will always use it and ignore `JAVA_HOME` and `java` on the
path.

## Consequences

Users install only Clang, and an old JDK on their machine cannot break `jcc`. ADR 0013
still holds for building: contributors need JDK 25, and the JDK that runs Maven decides
the version and vendor of the bundled runtime. The macOS archive grows from 1.3 MB to
18.8 MB, and the runtime gets JDK updates only when JCC makes a new release. A new
dependency on another JDK module must be added to the `jlink` options, or `jcc` fails at
run time. Users can no longer pick their own JDK through the launchers. macOS can block
the runtime after a browser download, and the README tells users how to remove the block.
