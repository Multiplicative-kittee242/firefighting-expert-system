# gui — the Swing user-interface layer

The whole desktop application the operator sees: the **deck map** (ship image + coloured incident overlays + control buttons + hydrant/plan labels), the **solution panel** (a tree of firefighting phases with an events log and a recommended-actions table), the i18n facade, and the window that lays them out. This is the outermost layer of the app — the only thing above it is `app.Main`, which wires it to the CLIPS engine and starts it.

`gui` is where the operator's actions become CLIPS calls and CLIPS's conclusions become pixels. This README is the **layer capstone**: it maps the packages and traces the end-to-end flows that cross them. Each cluster's internals live in its own README (linked below) — this file is the whole-loop view none of them shows alone.

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

Read `A ← B` as "B depends on A". `gui` depends inward on `clips` (the expert-system services), `config` (button/geometry specs), `domain` (the value objects it renders) and `geometry` (screen shapes). Only `app` depends on `gui`, and nothing depends on `app`. `ArchitectureRulesTest` enforces, against bytecode: the layer graph is acyclic (`topLevelPackagesAreFreeOfCycles`), and `clips.values.internal` is reachable only from `gui.actions` (`clipsInternalValuesAreOnlyUsedByClipsOrGuiActions`).

## Package map

- **`gui`** (root) — `MainFrame` (window layout, four regions, pure Swing assembly) and `Localization` (i18n facade over `i18n/messages*.properties`: RU/EN/DE/NL, EN fallback).
- **[`gui.actions`](actions/README.md)** — the command layer: `InputAction` (sealed, one record per action), `ActionDispatcher`, `InputActionListener`, `ClipsValuesMapper`.
- **[`gui.map`](map/README.md)** — the deck-map coordinator: `DeckMapController` and the two assemblers, wiring four sub-packages together:
  - **[`input`](map/input/README.md)** — operator-input button groups and their buttons (`controls/`).
  - **[`state`](map/state/README.md)** — fire-incident state and the view-data it pushes to listeners.
  - `values` — GUI-facing enums.
  - **[`view`](map/view/README.md)** — read-only overlays: labels (`controls/`), painting.
- **[`gui.solution`](solution/README.md)** — the decision-support results panel: phase tree + results/events tables.

## Where gui meets CLIPS — two boundaries, not one

Only **two** places invoke the CLIPS *engine*:

- **write** — `gui.actions` (`ClipsReportService`, inside `InputAction.apply`);
- **read** — `gui.solution.SolutionResultsController` (`ClipsReadOnlyService`).

Everything else is fed *indirectly*: the map through `FireIncidentState`, the tables through the results controller. A separate, looser boundary is the wire-value **enums** — the plain `clips.values` tier (`DoorState`, `EvacuationStatus`, …) is deliberately GUI-safe and used straight in the input groups and `DeckMapController`, while `clips.values.internal` stays inside `gui.actions` (bridged by `ClipsValuesMapper`). "Names a wire enum" is not the same as "invokes the engine" — keep the two apart.

## How the layer works — a loop around the engine

Those two touch-points — write through `gui.actions`, read through the results controller — are the ends of a **loop around the engine**, which holds the only application state; `gui` keeps none of its own. Everything the operator does runs the same cycle: **act → report → recompute → re-render**.

The one thing to internalise up front is that the answer comes back by **two decoupled paths**. The map re-renders *reactively*, the instant new engine output arrives. The results tables never watch the engine at all — they re-read it only when the **phase tree** tells them to. So an action does not update the tables directly; it reports to the engine and then *nudges* the phase tree, which re-pulls. The three parts below are that one loop: the write, the reactive map return, and the phase tree as the pivot where write and read meet.

### The write — an action reaches the engine

An operator's click is the "act → report" half. It is always one `InputAction`, dispatched the same way before branching *inside* `apply(...)` by action type:

```
button click → InputActionListener reads the InputAction off the button's clientProperty
             → ActionDispatcher.dispatch → action.apply(deckMapController, reportService, solutionTree, resultsController) → [1a / 1b / 1c]
```

**1a — fire buttons** (`FireActionInput`) are the only full round-trip: reporting a fire runs the whole inference, and the answer comes straight back down the same call — split and pushed into the map's state, which is the reactive return described next:

```
reportFireIncident(loc) → CLIPS full inference → FireIncidentSnapshot
   → DeckMapController.representFire(snapshot): splits it into 3 view-data → FireIncidentState.updateState → [reactive return, below]
   → resultsController.updateEvents(loc)   → events table
   → solutionTree.resetPhaseAndNotify()    → phase = ROOT [→ the phase pivot]
```

**1b — plain toggle buttons** (Ventilation, Door, Evacuation, MachineryDamage) are simpler and write no state: `collectChanges` reads the button's live state as a plain `clips.values` enum, `reportXxxChanges` sends it, then an explicit `repaint()` and a `refreshCurrentPhase()` nudge — that nudge is the *only* reason the change reaches the tables.

**1c — mapped prevention buttons** (Flammable, Explosion, Extinguisher) are 1b with one extra step: the collected `domain.types` / `gui.map.values` value is first remapped through `ClipsValuesMapper` into `clips.values.internal`. Explosion is the exception that *does* write state — `reportExplosionPreventionChanges` returns the still-open threats, fed back through `repaintExplosionLocations` as a *targeted* `FireIncidentState` update.

Two invariants govern every `apply` — exactly one repaint per action, and always a phase nudge afterward; both are spelled out in [actions/README.md](actions/README.md).

### The reactive return — the map redraws itself

This is the map's "re-render" half, and the only part of the loop that never leaves `gui`. Whenever a write pushes new data into `FireIncidentState` — the whole snapshot from a fire, or the targeted explosion set — the state notifies four independent listener lists. Each consumer redraws straight from the pushed data and never queries the engine, which is exactly why only `gui.actions` and the results controller ever touch CLIPS:

```
FireIncidentState.updateState(...)   (or the targeted setExplosionThreatLocations)
   ├─ MapDrawingListener       → MapPainter            redraw fire / threat / evac / fire-line / explosion overlays
   ├─ HydrantViewListener      → gui.map.view groups   rebuild hydrant buttons / labels
   ├─ InputControlListener     → gui.map.input groups  reveal the recommended buttons
   └─ ExplosionControlListener → ExplosionButtonGroup
```

### The phase pivot — where write and read meet

The results tables are decoupled from all of the above: they re-read the engine only when `SolutionPhaseTree` fires `onPhaseChanged` — whether the operator selected a phase node, or an action called `refreshCurrentPhase()` / `resetPhaseAndNotify()`. That single event is the pivot that closes the loop, and it drives *two* subscribers at once, keeping the tables and the map's visible layers on the same phase:

```
SolutionPhaseTree   (node selected, OR nudged by an action's refreshCurrentPhase / resetPhaseAndNotify)
   → onPhaseChanged(section) to its two subscribers:
       ├─ SolutionResultsController → re-query ClipsReadOnlyService for that phase → rebuild the actions table
       └─ DeckMapController         → MapLayerVisibilityManager.apply(section) → show / hide map layers
```

## See also

- [`actions/README.md`](actions/README.md) · [`map/README.md`](map/README.md) · [`solution/README.md`](solution/README.md) — the three clusters' internals.
- [`clips/README.md`](../clips/README.md) — the read/write services and `FireIncidentSnapshot`.
- [`clips/values/README.md`](../clips/values/README.md) — the plain vs `internal` enum tiers.
- [`AGENTS.md`](AGENTS.md) — the guardrails for editing this layer.
- Root [`AGENTS.md`](../../../../AGENTS.md) — the architecture and the build / verify commands.
