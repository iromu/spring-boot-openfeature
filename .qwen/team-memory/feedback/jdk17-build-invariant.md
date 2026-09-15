---
name: Build must use JDK 17 (default JDK 25 breaks Lombok)
description: This machine's default `java` is JDK 25, which breaks Lombok 1.18.42 annotation processing; all Maven builds must run against a JDK 17.
type: feedback
---

Run every Maven build in this repo against a **JDK 17**, not the machine default JDK.

Working command (from repo root):
`JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B --no-transfer-progress -pl <module> -am clean test`

(Other JDK 17s that work: `/home/wantez/.jdks/azul-17.0.17`. The Debian `/usr/lib/jvm/java-17-openjdk-amd64` is a JRE with no `javac` AND no `javadoc` — do not use it.)

`-Psonar clean verify` runs the `javadoc`/javadoc-toolchain step, so a JDK without `javadoc` fails there with "Unable to find javadoc command … doesn't exist or is not a file". Do not "fix" this by borrowing `/usr/lib/jvm/java-21-openjdk-amd64` (it has `javadoc` and compiles the candidate fine, but it is not the CI target). Use the Azul 17 above — it is a full JDK and already has `javadoc`, so it passes `-Psonar` clean. Confirmed: baseline `-Psonar clean verify` and the latest OpenFeature lockstep `-DskipTests` both pass on Azul 17.

**Why:** The machine's default `java` resolves to OpenJDK 25 (via sdkman current). Lombok is pinned at 1.18.42, whose `javac` integration is unreliable on JDK 25, so annotation processing silently fails: `@RequiredArgsConstructor` leaves a `final` field uninitialised ("variable … not initialized in the default constructor") and `@SneakyThrows` stops suppressing checked exceptions (e.g. `new Value(Object)` re-surfaces `InstantiationException`). The project targets `java.version=17`, and CI uses JDK 17 (Temurin).

**How to apply:** Prefix any `./mvnw` invocation with `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1`. If a compile fails with Lombok-style "not initialized in the default constructor" / "unreported exception" errors on a Lombok-annotated class, it is almost always the JDK-25 problem, not the code.
