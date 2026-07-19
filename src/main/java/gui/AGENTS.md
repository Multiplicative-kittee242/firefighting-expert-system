# GUI layer — guardrails

Scoped guidance for `src/main/java/gui`; inherits project-wide conventions from the root [`AGENTS.md`](../../../../AGENTS.md). Read [`README.md`](README.md) for what the layer is and how the flows fit together; this file is only the short list of invariants that are easy to break. Cluster detail lives in the sub-package READMEs ([`actions`](actions/README.md), [`map`](map/README.md), [`solution`](solution/README.md)).

**CODESTYLE is mandatory** for every new or edited Java file here: before editing, read [`CODESTYLE.md`](../../../../CODESTYLE.md) (see root AGENTS.md § "Code style").

## Layering
- `gui.*` may depend on `domain`, `config`, `clips`, `geometry` and `util`. **Never** import `app` (nothing depends on the composition root). The layer graph must stay acyclic — `ArchitectureRulesTest.topLevelPackagesAreFreeOfCycles` enforces it against bytecode.
- **Only `gui.actions` may reference `clips.values.internal`** (`clipsInternalValuesAreOnlyUsedByClipsOrGuiActions`). Every other GUI package uses the plain `clips.values` tier, a `domain.types` enum, or a `gui.map.values` counterpart; `ClipsValuesMapper` is the one bridge. See [`clips/values/README.md`](../clips/values/README.md).

## The CLIPS boundary — keep it at two points
- Exactly two places invoke the engine: **`gui.actions`** (write, `ClipsReportService`) and **`gui.solution.SolutionResultsController`** (read, `ClipsReadOnlyService`). Do **not** add a direct `Clips*Service` call anywhere else — feed a new widget indirectly, off `FireIncidentState` (map side) or the results controller (tables). A `clips.values` *enum* reference is fine outside those two; an engine *call* is not.

## The action contract (`gui.actions`)
- Every incremental `InputAction` must call `solutionTree.refreshCurrentPhase()` after reporting to CLIPS — or `resetPhaseAndNotify()` for a new incident (`FireActionInput`). Drop it and the actions table silently goes stale. Each action must trigger **exactly one** overlay repaint: reactive (via `FireIncidentState` listeners) for fire/explosion, explicit `repaint()` for the rest — never both.
- An `InputAction` carries **identity only**; the live toggle status is re-read through `DeckMapController.collect*Changes(...)` at click time. Don't cache a status in the record.

## Swing / i18n
- **All user-facing strings go through `Localization.get(key)`** (keyed in `i18n/messages*.properties`) — nothing hardcoded. New text means a new key in every locale file.
- This layer builds real Swing components; keep engine-touching logic out of `paint*`/layout code.

## Tests
- Unit tests live in `src/test/java/gui` (e.g. `solution/SolutionResultsControllerTest`, `actions/ClipsValuesMapperTest`) — no CLIPS, no Swing realization. The button→report and snapshot→repaint **boundary** is covered in `src/testIntegration/java/gui` with the real engine excluded from the call chain. Follow each source set's own `AGENTS.md`.
- No new runtime dependencies without an explicit request.
