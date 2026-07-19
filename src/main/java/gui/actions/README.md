# gui.actions — typed input actions

This package is the **click → services** path: it turns a button press on the deck map into the right calls on `clips.ClipsReportService`, `gui.map.DeckMapController`, `gui.solution.SolutionPhaseTree` and `gui.solution.SolutionResultsController`. It replaced string `actionCommand` parsing (`ELEMENT_SENSOR`, `ELEMENT_EXPLOSIVE`, …) that used to live in `app.Main`'s `actionPerformed`.

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

`gui.actions` is part of the `gui` layer (fourth from the left): the GUI depends inward on `domain`, `config` and `clips`; only `app` depends on the GUI, and nothing depends on `app`. Within `gui`, this package is the **command layer** — it depends on `clips` (`ClipsReportService`) and on sibling GUI packages (`gui.map` for `DeckMapController`, `gui.solution` for the phase tree and results table).

Two rules single this package out, both enforced against bytecode by `ArchitectureRulesTest`:

- It is the **only** GUI package allowed to reference `clips.values.internal` (the CLIPS wire-protocol enums) — `clipsInternalValuesAreOnlyUsedByClipsOrGuiActions`. Every other GUI package must go through a `gui.map.values` / `domain.types` counterpart; `ClipsValuesMapper` here is the one place that bridges the two.
- Concentrating the command layer here is also why `gui.map.input`'s button groups build their `InputAction`s in *this* package rather than reaching toward `app` — so the layer DAG stays acyclic (`topLevelPackagesAreFreeOfCycles`), with no `gui ↔ app` cycle.

## The four pieces

- **`InputAction`** — a sealed interface with one record per user action, and an `apply(...)` method **each record implements itself**. The behavior lives in the record, not in a switch somewhere else: adding an action means adding a record, and the compiler finds nothing else to update. Also holds `INPUT_ACTION_PROPERTY`, the Swing `clientProperty` key the action travels under.
- **`InputActionListener`** — the `ActionListener` attached to every map button. Reads the `InputAction` off the clicked button's client property and hands it to the dispatcher. Its `dispatcher` is set *after* construction on purpose: `DeckMapController` needs an `ActionListener` while it builds its buttons, but `ActionDispatcher` needs the finished `DeckMapController` — so the listener is built first, wired in, and given its dispatcher once one exists.
- **`ActionDispatcher`** — holds the four collaborators and calls `action.apply(...)` with them. That is all it does; it does not know what any individual action means.
- **`ClipsValuesMapper`** — remaps GUI/domain enum values (`PreventionType`, `ExplosiveType`, `ExtinguisherUsage`) to their `clips.values.internal` wire-protocol counterparts, plus a generic `remapToClips(map, fn)` helper. See [`clips/values/README.md`](../../clips/values/README.md) for why that boundary exists.

## Flow

```
Button click
  → InputActionListener.actionPerformed reads InputAction from the button's clientProperty
  → ActionDispatcher.dispatch(action)
  → action.apply(deckMapController, clipsReportService, solutionTree, resultsController)
      ├─ deckMapController.collectXxxChanges(...)  — the group reads its buttons' current state
      ├─ (Explosion / Flammable / Extinguisher only) ClipsValuesMapper.remapToClips(...)
      ├─ clipsReportService.reportXxxChanges(...)  — reaches CLIPS
      ├─ repaint (see the contract below)
      └─ solutionTree.refreshCurrentPhase()
```

## The `apply` contract — two rules that are easy to miss

Both are spelled out in `InputAction`'s javadoc; they are invariants, not style:

1. **Exactly one overlay repaint per action.** Actions that mutate `gui.map.state.FireIncidentState` drawing data (fire, explosion prevention) repaint *reactively* through the state listeners and must **not** repaint again. Every other action has no reactive path and repaints explicitly at the end of `apply`.
2. **Every action that reports an incremental change to CLIPS must call `solutionTree.refreshCurrentPhase()` afterward** — the exception being `FireActionInput`, which starts a new incident and calls `resetPhaseAndNotify()` instead. `SolutionResultsController` only re-queries CLIPS when the phase tree notifies it, which otherwise happens only on a real tree-selection change; dropping the call is what used to leave completed actions sitting in the actions table forever. Don't drop it when adding an action.

## Status is never carried in the record

An `InputAction` carries **identity** (which location, which door, which extinguisher), never a toggle status. `apply` always re-reads the live state via `DeckMapController.collect*Changes(...)`, which delegates to the owning group — the group is the single source of truth for a pending status. This is ordinary Swing practice: the event tells you *which* control, the handler asks the control *what state it is in now*.

The one exception is a value that is **intrinsic to the button** rather than derived from its toggle state:

| Record | Second parameter | Why |
|---|---|---|
| `FlammableActionInput` | `PreventionType` | Which flammable material this button targets — fixed per button position. Needed to pick the pending action (`PUMP_OUT` / `CARRY_OUT` / `DONE`) when the button is **off**; `isSelected()` alone cannot say. |
| `ExplosionPreventionActionInput` | `ExplosiveType` | Same shape: which explosive object (`CARRY_OUT` / `PUMP_OUT` / `TO_FIGHT` / `DONE`). |
| all others | none | Identity is enough; status comes from `isSelected()`. |

## The records

`FireActionInput`, `VentilationActionInput`, `DoorSealingActionInput`, `FlammableActionInput`, `MachineryDamageActionInput`, `ExplosionPreventionActionInput`, `EvacuationActionInput`, `ExtinguisherActionInput`. `FireActionInput` is the odd one — it starts an incident, so it returns a `FireIncidentSnapshot`, repaints via `representFire`, updates the events table and resets the phase tree. `ExtinguisherActionInput` is fully wired but unreachable today (no button placement exists yet — see [`clips/INACTIVE.md`](../../clips/INACTIVE.md)).

## See also

- [`gui/map/input/README.md`](../map/input/README.md) — the button groups that attach these actions and answer `collect*Changes`.
- [`clips/values/README.md`](../../clips/values/README.md) — the enum tiers `ClipsValuesMapper` bridges.
- [`clips/README.md`](../../clips/README.md) — `ClipsReportService`, the write API this package drives.
