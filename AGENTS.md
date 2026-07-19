# AGENTS.md

Operational guidance for coding agents (and humans) working in this repository. For what the project *is*, see [README.md](README.md) — this file is deliberately not a re-telling of that; it covers what an agent needs to know before making a change here that a first read of the code would not make obvious. Vendor-neutral: any agent or human editing any layer should be able to follow it without tool-specific syntax.

Portable conventions this repository follows, but that don't name anything specific to it — how the documentation set is organized, the read-before/verify-after style discipline, planning before a substantial change, the commit policy — live in [`CONVENTIONS.md`](CONVENTIONS.md). Read it alongside this file, not instead of it: this file is what's specific to *this* repository.

## Build & verify

```
./gradlew build            # compileJava + test + testIntegration + verifyConfigSchemas (check)
./gradlew test              # unit tests, src/test — fast, no CLIPS
./gradlew testIntegration   # gui/clips-boundary integration tests, src/testIntegration — no CLIPS
./gradlew testClips         # real 32-bit CLIPS engine — NOT part of build/check, run explicitly
./gradlew jacocoTestReport  # coverage for check only (test + testIntegration) — what `build` produces
./gradlew jacocoTestClipsReport  # testClips-only coverage XML (runs testClips; second Sonar input)
./gradlew sonar             # analysis; depends on jacocoTestReport (add jacocoTestClipsReport for engine coverage)
./gradlew runApp            # launches the Swing app (portable 32-bit JRE, see below)
./gradlew generateConfigSchemas   # regenerate src/main/resources/config/schemas/*.json after a DTO change
./gradlew runClipsDiagnostic -PdiagClass=ScenarioGoldenMasterCheck   # byte-for-byte scenario diff, see below
```

`testClips` requires the portable 32-bit JRE and is excluded from `build`/`check` for that reason — always run it explicitly after touching anything in `clips` or `feis.clp`. It has a known, accepted native flakiness (below); do not treat one isolated failure as a regression.

**Coverage / SonarCloud:** SonarCloud reads **two** JaCoCo XMLs and unions them per line (`systemProp.sonar.coverage.jacoco.xmlReportPaths` in `gradle.properties`): `jacocoTestReport` (unit + GUI integration, what `check` produces) and `jacocoTestClipsReport` (testClips only — otherwise `ExpertSystemService` / most of `ClipsEngineAccess` look uncovered even though the real engine suite exercises them). They are kept as two separate reports on purpose: each XML is resolved against the classes compiled on the same machine as its own `.exec`, so JaCoCo's class-id check always holds. Do **not** replace this with a single report that merges `testClips.exec` (built on the windows runner) into a report whose classes were compiled on the ubuntu runner — a bytecode mismatch there silently drops a class to 0% coverage (verified). Locally, for the full picture: `./gradlew jacocoTestClipsReport sonar` (needs the portable 32-bit JRE for `testClips`; `jacocoTestReport` comes in via the `sonar` dependency). CI: the windows `testClips` job uploads its coverage XML, and the ubuntu `sonar` job drops it in place and runs `./gradlew sonar` — which regenerates `jacocoTestReport` on ubuntu, so ubuntu never launches the 32-bit Windows-only CLIPSJNI.

## Source sets

| Source set | JVM | Purpose |
|---|---|---|
| `src/main` | 17, 64-bit | Application code. |
| `src/test` | 17, 64-bit | Unit tests — no CLIPS, no Swing realization. Fast, part of `check`. |
| `src/testIntegration` | 17, 64-bit | GUI/`clips`-boundary integration tests (button click → `collect*Changes` → mapped `report*`, or `FireIncidentSnapshot` → `representFire`/repaint) with the real CLIPS engine excluded from the call chain. Part of `check`. |
| `src/testClips` | 17, **32-bit** | Integration tests against the real CLIPSJNI native engine. Deliberately **not** part of `check`/`build` — run via `./gradlew testClips` only. |
| `src/testFixtures` | 17, 64-bit | Shared fakes/builders (`fixtures.*`) reused by `test` and `testIntegration`. No JUnit/Mockito dependency of its own. |

Each source set that needs one has its own `AGENTS.md`, loaded automatically by directory when editing files there: [`src/test/java/AGENTS.md`](src/test/java/AGENTS.md) (test conventions — DAMP, Hamcrest, naming, fixtures), [`src/testIntegration/java/AGENTS.md`](src/testIntegration/java/AGENTS.md), [`src/testClips/java/AGENTS.md`](src/testClips/java/AGENTS.md), [`src/main/java/config/AGENTS.md`](src/main/java/config/AGENTS.md).

## Code style (mandatory pre-flight for every Java edit)

Mechanical formatting rules live **only** in [`CODESTYLE.md`](CODESTYLE.md) — not restated here or in nested `AGENTS.md` files (so they do not drift). There is currently no automated formatter/linter in the build; green tests alone do **not** prove style compliance. The general read-before/verify-after discipline for consulting a style guide is in [`CONVENTIONS.md`](CONVENTIONS.md); here is its concrete shape for this repository.

**Before** creating or editing any file under `src/main/java`, `src/test/java`, `src/testIntegration/java`, `src/testClips/java`, or `src/testFixtures/java`, read [`CODESTYLE.md`](CODESTYLE.md) in this turn — do not rely on memory or on matching nearby sources alone.

**After** the edit, re-check the diff against CODESTYLE, especially rules **2** (braces / multi-line bodies), **4** (if-body on its own line), **8** (no loop `if (cond) continue` — nest instead), and **9** (flat method/constructor entry guards, not a nested pyramid).

## The portable 32-bit JRE (CLIPSJNI constraint)

The only available `CLIPSJNI` binding (version 0.1, from 2008) is a 32-bit native library (`CLIPSJNI.dll`) with no official 64-bit release. `runApp` and `testClips` therefore both need a 32-bit JVM — Gradle downloads and manages one automatically (`jre-17-32/`, a ~39 MB download that unpacks to ~110 MB on disk, first run only; tasks `downloadPortableJre`/`extractPortableJre`/`ensurePortableJre`/`verifyJre`). `src/main`/`test`/ `testIntegration`/`testFixtures` run on the ordinary 64-bit toolchain (`JavaLanguageVersion.of(17)`) and need none of this.

**`runApp` must stay a Gradle `Exec` task, not `JavaExec`** — `JavaExec`'s own `javaLauncher` resolution conflicts with launching the custom 32-bit executable directly. Don't "simplify" this without re-verifying against the real engine.

The CLIPS rule base (`feis.clp`) is loaded from the classpath resource (`src/main/resources/clips/feis.clp`) via `util.ResourceUtil`, not from a project-root `clips/` directory — if one exists at the repo root, it is a stale leftover from before this migration and is not read by anything at runtime.

## Architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

Read `A ← B` as "B depends on A". A layer may depend on any layer to its left, not only its immediate neighbour. `domain`, `geometry`, and `util` depend on nothing. This is enforced against compiled bytecode by `ArchitectureRulesTest` (`src/test/java/architecture/ArchitectureRulesTest.java`), not just documented — a layering violation fails `./gradlew test`. See [`src/main/java/clips/README.md`](src/main/java/clips/README.md) and [`src/main/java/config/README.md`](src/main/java/config/README.md) for the `clips`/`config` layers' own internal structure in depth; package-level `README.md`s exist for the packages non-obvious enough to need one (`clips`, `clips/values`, `config`, `gui/actions`, `gui/map/input`) — check for one before assuming a package's shape from its code alone.

## Documentation in this repository (mandatory pre-flight for every README edit)

Craft rules for writing documentation live in [`DOCSTYLE.md`](DOCSTYLE.md); the general principles behind how the docs here are organized — document roles, README niches, the canonical-diagram discipline, verifying against code, the standard "Place in the architecture" shape, moving a test class with its production class — live in [`CONVENTIONS.md`](CONVENTIONS.md). Read both before creating or substantially editing any `README.md`. What follows is only what's specific to this repository:

- The architecture map lives at [`src/main/java/README.md`](src/main/java/README.md); the packages listed under [Architecture](#architecture) above each carry their own `README.md`.
- Whenever the layer diagram changes, sweep every copy in the same change: `grep -rl "domain ← config ← clips" --include="*.md" --include="*.java" .` finds every one.
- "Place in the architecture" sections cite `ArchitectureRulesTest` method names — keep them in sync with `src/test/java/architecture/ArchitectureRulesTest.java`.

## Verifying a change against the real CLIPS engine

`ScenarioGoldenMasterCheck` (`./gradlew runClipsDiagnostic -PdiagClass=ScenarioGoldenMasterCheck`) diffs the structured `FireIncidentSnapshot` of 7 recorded fire scenarios (A, D, G, J, P, R, T) against committed baselines under `src/testClips/resources/clips/diagnostics/golden/`, byte-for-byte. This is the strongest available regression check for any change touching `ClipsEngineAccess`, `ExpertSystemService`, or `feis.clp` itself — run it (alongside `testClips`) before considering such a change verified, not just the unit tests.

It deliberately does **not** diff CLIPS's console printout: that is a human-debugging side channel nothing in the app reads back, and CLIPS's native `printout t` does not necessarily route through Java's redirectable `System.out` in the first place. The baselines are a field-per-line dump of the snapshot the application actually consumes — see the class's own javadoc before "improving" this into a console diff.

**Known, accepted flakiness**: `testClips` fails intermittently (`EXCEPTION_ACCESS_VIOLATION` inside `CLIPSJNI.dll`) roughly 1 run in 5–6, a GC-finalizer race in the native binding's teardown path, not a code defect — `CLIPSJNI.Environment` has no public dispose API. The mitigation is to construct as few environments per JVM run as possible: an engine-touching test class builds **one** `ExpertSystemService` in `@BeforeAll` and reuses it across scenarios via `resetForNewScenario()`, rather than a fresh one per test. The crash rate scales with how many environments a single JVM run constructs, so adding another engine-touching test *class* is not free — prefer extending an existing one (splitting explanation tests into a class of their own once took the failure rate to 6 runs in 8; merging them back restored it). See `ExpertSystemService#resetForNewScenario()` and `src/testClips/java/AGENTS.md` for the full pattern. Re-run once before treating a `testClips` failure as real; if it reproduces twice, it's real.
