# CLIPS layer — guardrails

Scoped guidance for `src/main/java/clips`; inherits project-wide conventions from the root [`AGENTS.md`](../../../../AGENTS.md). Read [`README.md`](README.md) for how the layer works and *why*; this file is only the short list of invariants that are easy to break and expensive to diagnose (a broken one usually fails silently in the native engine, not loudly in Java).

**CODESTYLE is mandatory** for every new or edited Java file here: before editing, read [`CODESTYLE.md`](../../../../CODESTYLE.md) (see root AGENTS.md § "Code style").

## The native engine
- **One `Environment` per JVM process.** Never construct a second `ClipsEngineAccess`/`Environment` to "start fresh" — call `ExpertSystemService.resetForNewScenario()` and reuse the one instance. Repeated construction crashes the JVM (`EXCEPTION_ACCESS_VIOLATION`); the engine is also not thread-safe. See the README invariant and the root AGENTS.md on `testClips` flakiness.
- **`Environment` is 32-bit only.** Anything that actually runs the engine belongs in `testClips` (run with the portable JRE), never in `test`/`testIntegration`. Pure string↔domain parsing does not need the engine — keep it testable on the 64-bit JVM (see below).

## The boundary
- **All CLIPS string handling stays in `ClipsEngineAccess`.** `ExpertSystemService` (and everything above it) works in domain types only — never build or parse a CLIPS string outside `ClipsEngineAccess`. New query? Add a typed method here that returns domain objects.
- **Trust the return value, not the console.** A new command that can fail must go through `evalOrThrow`/`executeQuery` (a bare `FALSE` = failure). The only exceptions are calls where CLIPS answers `FALSE` legitimately (`action-edit`, `get-plan-from`) — document any new one, as those two are.
- **Instance names are built here and never parsed back.** If you ever add a read-back path for an instance name, update the README's naming-convention section (it currently promises there is none).

## Tests & verification
- Parsing/formatting (`parse*`, `from*Instance`) is unit-tested on the normal JVM — `ClipsEngineAccessParsingTest`. Keep those methods `static` + `TopologyModel`-arg so they stay engine-free.
- After touching anything in this package or `feis.clp`, run `./gradlew testClips` **and** `ScenarioGoldenMasterCheck` (byte-for-byte snapshot diff) before considering it verified — the unit tests alone do not exercise the real engine. See the root AGENTS.md for the exact commands and the accepted re-run-once flakiness policy.
- No new runtime dependencies without an explicit request.

## Inactive paths
Some queries here feed features that don't function yet ([`INACTIVE.md`](INACTIVE.md)). Before "cleaning up" an apparently unused query method (`getExt*ForLocation`, `getGraphFromLocations`), read that file — deleting one is a real option, but a deliberate decision, not dead-code removal.
