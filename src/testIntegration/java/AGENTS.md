# Integration tests (testIntegration) specifics

Inherits project-wide conventions (code style, architecture) from the project's root [`AGENTS.md`](../../../AGENTS.md) and test conventions (DAMP, Hamcrest, naming, fixtures) from [`test/java/AGENTS.md`](../../test/java/AGENTS.md).

**CODESTYLE is mandatory** for every new or edited Java file here: before editing, read [`CODESTYLE.md`](../../../CODESTYLE.md) (see root AGENTS.md § "Code style") — do not restate the rules in this file.

- This source set holds integration tests that run on the project's normal 64-bit JVM — no CLIPSJNI native engine, no portable 32-bit JRE. Run via `./gradlew testIntegration`; it is wired into `check`/`build` like an ordinary test task (unlike `src/testClips`, which is deliberately excluded from `check` because it requires the portable 32-bit JRE).
- Two kinds of tests belong here: (1) GUI-facing integration tests that exercise the application below/around the `clips` boundary — button-click → `collect*Changes` → mapped `report*` call, or a constructed `FireIncidentSnapshot` → `representFire`/repaint state — with the real CLIPS engine excluded from the call chain; and (2) any future `clips`-layer integration test that does not need the real 32-bit native engine (a real 32-bit CLIPS test always belongs in `src/testClips/java` instead — see its own `AGENTS.md`).
- Before writing a test against a Swing component type not already covered here: verify empirically whether it can be exercised headless (`-Djava.awt.headless=true`, set on this Test task) — do not assume; confirm with a small spike first, the same way the 32-bit `Test` task wiring for `testClips` was verified empirically before relying on it.
