# gui.solution — the decision-support results panel

The left-hand panel of the app: a **tree of firefighting phases** the operator steps through, and the two tables that go with it — an **events** log and, for the selected phase, the **recommended actions** CLIPS computed. This package only ever *displays* the engine's conclusions; it reports nothing back (that is `gui.actions`' job).

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

`gui.solution` is part of the `gui` layer. It depends inward on `clips` — but only on `ClipsReadOnlyService`, the **read** half of the CLIPS API, never the write half — plus `domain` (the value objects and `TopologyModel` it renders) and `gui.Localization` (all display strings). It does **not** depend on `gui.actions` or `gui.map`; the reverse holds — both of those *subscribe* to this package's phase events (below). So within `gui` it is an event **source** others feed and listen to, not a consumer of them.

## What's in the package

| Type | Role |
|---|---|
| `SolutionPhaseTree` | The `JTree` of phases and the single **event source**. Publishes `onPhaseChanged(section)` to every `PhaseChangeListener` on a selection change (or an explicit refresh). |
| `SolutionResultsController` | A `PhaseChangeListener` that, per selected phase, queries `ClipsReadOnlyService` and fills the actions table; also owns the events table. |
| `SolutionTreeSection` | The phase enum (`ROOT`, `PRIORITY_MEASURES`, `EVACUATION`, `SEALING`, `LOCALIZATION`, `PREVENTION`, `FIREFIGHTING`) — shared with `gui.map.MapLayerVisibilityManager`. |

## How it works — the phase drives everything

`SolutionPhaseTree` is the one source of truth for "which phase is the operator looking at". Selecting a tree node fires `onPhaseChanged(section)` to **all** subscribers, and there are two, wired in `app.Main`:

- `SolutionResultsController` — clears the actions table and re-queries `ClipsReadOnlyService` for exactly that phase's recommendations (evacuation targets, rooms to seal, doors to close, prevention actions, fire-line hydrants, the extinguishing plan), one row-builder per phase.
- `gui.map.DeckMapController` — shows/hides the map's control layers for that phase via `MapLayerVisibilityManager`.

So a single `SolutionTreeSection` keeps the left panel and the map in step. The **events** table is separate from this cycle: `updateEvents(location)` appends a row when a fire is first reported.

## Two things that trip people up

**The refresh contract.** `refreshCurrentPhase()` re-fires the current phase to listeners *without* changing the tree selection; `resetPhaseAndNotify()` selects the root and notifies. This is the other half of the invariant documented in [`gui/actions/README.md`](../actions/README.md): every incremental action reported to CLIPS must call `refreshCurrentPhase()` (or, for a new incident, `resetPhaseAndNotify()`), or the actions table goes stale — it only re-queries the engine when a phase notification arrives, which otherwise happens only on a manual tree-selection change.

**Two phase vocabularies — don't conflate them.** `SolutionTreeSection` (public, 7 values incl. `ROOT`) tags the *tree nodes* and drives *map-layer visibility*. `SolutionResultsController.SolutionPhase` (private, 8 values) is finer: it splits `PREVENTION` into flammable / machinery-damage / explosion and adds `IMMEDIATE_MEASURES`. It exists only to label each actions-table **row** in the phase column — it is not the tree's phase model.

## Also worth knowing

- **Read-only by design.** This package uses `ClipsReadOnlyService` exclusively; it never holds the write API, so displaying results can never accidentally mutate engine state.
- **All strings are localized** via `gui.Localization` keys — nothing user-facing is hardcoded.
- **A nice domain touch:** when a room is on fire but CLIPS computed no route to it, `noComputedRouteMessage` names the neighbouring compartment's tank (a cross-compartment link is always a door with no border) instead of leaving the recommendation blank.

## See also

- [`clips/README.md`](../../clips/README.md) — `ClipsReadOnlyService`, the read API this panel drives, and the read/write split that lets this package take only the read half.
- [`gui/actions/README.md`](../actions/README.md) — the command side that reports actions and must honour the refresh contract above.
