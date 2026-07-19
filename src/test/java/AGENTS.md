# Test conventions

Scoped guidance for everything under `src/test/java`; inherits project-wide conventions (code style, architecture) from the project's root [`AGENTS.md`](../../../AGENTS.md).

The general testing methodology this suite follows — DAMP over DRY, Arrange-Act-Assert, how to split shared setup, parameterized-test idioms, the naming taxonomy, mocking vs. fakes — is **not project-specific** and lives in [`TESTSTYLE.md`](../../../TESTSTYLE.md). Read it before writing or reviewing a test; this file is only what's specific to *this* suite. New tests should match the patterns already in `domain/*RegistryTest`, `config/loading/DeckMapTopologyConfigTest`, and `gui/solution/SolutionResultsControllerTest`.

**CODESTYLE is mandatory** for every new or edited Java file here: before editing, read [`CODESTYLE.md`](../../../CODESTYLE.md) (see root AGENTS.md § "Code style") — do not restate the rules in this file.

## Shared fixtures in this project

`fixtures.*` lives in its own source set, `src/testFixtures/java` — not under `src/test/java` — so both `src/test/java` and `src/testIntegration/java` can depend on the same fake/builder implementation instead of each keeping (or duplicating) its own. It has no JUnit/Mockito dependency of its own; only `domain`/`config`/`clips` main-layer types. See [`TESTSTYLE.md`](../../../TESTSTYLE.md) for when to add a *new* one; the ones that already exist:

- `fixtures.TestLocations.registryOf("A", "B", "D")` — a `LocationRegistry` of identity-only locations, when the registry is a *dependency* of the class under test. `TestLocations.identities(…)` returns the raw `RawLocation` list for `TopologyModel.from(…)`. Exception: a class under test constructs its own subject directly — `LocationRegistryTest` builds its `LocationRegistry` inline, not through the helper.
- `fixtures.TopologyConfigBuilder.topologyConfig().doors(…).locations(…).build()` — for `DeckMapTopologyConfig`, whose 8 same-typed list arguments are unreadable and misorder-prone positionally. Set only the sections a test exercises; the rest default to empty.
- `fixtures.FakeClipsReadOnlyService.fakeClips().withEvacuation(…)` — a hand-written fake implementation of `clips.ClipsReadOnlyService`, fields settable via a fluent builder, everything else defaulting to empty (the six `getExplanationFor*` methods return a fixed, per-method-distinguishable `Explanation` instead — not settable, since no test needs a *different* value per case, only to see which method a routing decision reached). This is the concrete instance of "prefer a fake" — see `TESTSTYLE.md` for why — over `@Mock ClipsReadOnlyService` + `Mockito.when(...)` wherever `ClipsReadOnlyService` needs to hand back data.

## Mockito in this project

**Availability.** `mockito-core` / `mockito-junit-jupiter` are wired to `src/test` and `src/testIntegration` (see `build.gradle`), **not** to `src/testClips` — `@Mock` there will not compile, and the CLIPS suite has no use for it anyway (it runs against the real engine by design). Availability is not an endorsement — see `TESTSTYLE.md` for when a fake beats a mock even where Mockito is available.
