# gui.map.input — operator input controls on the deck map

The buttons the operator actually clicks, and the groups that own them. Everything here is *input* (the operator tells the system something). Read-only overlays that only display what CLIPS decided live in the sibling `gui.map.view` package.

- `gui.map.input` — one **group** per kind of control. A group owns its buttons, their placement, their visibility and their state collection.
- `gui.map.input.controls` — the **buttons themselves**: `AbstractToggleButton` (base: paints itself via `drawContent(g, selected)` and the selected/unselected colour pair), `AbstractStickyFireButton` (a toggle that stays on once fired), and the concrete controls — `DoorSealingButton`, `EvacuationButton`, `ExplosionButton`, `ExtinguisherButton`, `FireButton`, `FireSensorButton`, `PreventionButton`, `VentilationButton`.

Base classes: `AbstractControlGroup<T, D>` (the controls list, the config-driven coordinate layout, the `D`-key → control map, visibility) and `AbstractToggleGroup<T, D>` (adds `addActionListener`/`attachInputAction`, fixed button size, `setVisibleFor(key)`). `D` is the group's domain key — `Location` for most, `Link` for `DoorSealingButtonGroup`, `FireSensor` for `FireSensorButtonGroup`, `Extinguisher` for `ExtinguisherButtonGroup`.

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

`gui.map.input` is part of the `gui` layer. It depends inward on `domain` (the identities its keys resolve to), `config` (the button placement and glyph-shape specs), `geometry` (`Point`/`Size`), and the **plain tier** of `clips.values` (`DoorState`, `EvacuationStatus`, …) — never `clips.values.internal`, which only `gui.actions` may touch (see that package's README). Within `gui` it pairs with three siblings: `gui.actions` (the `InputAction`s it attaches), `gui.map.state` (the incident state it reacts to), and `gui.map.view` (the read-only overlays that *display* what CLIPS decided — the opposite of this package's *input* role).

## A group has three jobs

**1. Build itself from config.** The constructor takes a `config.groups.*Config` and the `TopologyModel`, and resolves raw spec codes into domain objects: each `*ButtonSpec`'s `locationCode` becomes a real `Location`, its `position` becomes the button's placement. A spec that demands something the topology doesn't have fails fast (e.g. `VentilationButtonGroup` throws if a location has a ventilation button but no ventilation system).

**2. React to state (CLIPS → buttons).** Six of the nine groups implement `gui.map.state.InputControlListener` (`ExplosionButtonGroup`: `ExplosionControlListener`). When the incident state changes, the group is handed the fresh `InputControlsData` and reveals the buttons the engine's recommendations call for — `onInputControlsDataChanged(data)` → `setVisibleFor(key)` per recommended key. This is why a button appears only once it is relevant, and it is a *separate* direction from the click path below.

The three that implement no listener are exactly the ones with nothing to react to: `FireButtonGroup` and `FireSensorButtonGroup` *start* an incident rather than answer a recommendation (they are always visible — `MapLayerVisibilityManager`'s `SHOW_ALL`), and `ExtinguisherButtonGroup` is inactive (see the table note below).

`DoorSealingButtonGroup` carries the most in this direction: it not only reveals a door button but also sets its `DoorKeepType` (`sealingDoorsToClose` → `NO`, `sealingDoorsKeepOpen` → `YES`), which only changes how the button paints when unselected (red = must be closed, orange = keep open for a hose). Keep-open is **display state only** — it never enters the reported status (below).

**3. Answer a click (buttons → CLIPS).** In `addActionListener(...)` the group attaches a typed `gui.actions.InputAction` to every button (via `attachInputAction`, a Swing `clientProperty`). On the click, the action's `apply(...)` calls back into `DeckMapController.collect*Changes(key)`, which delegates here: `collectChanges(key)` reads the button's **current** `isSelected()` and returns the pending status keyed by domain object (a `Map`, because the `ClipsReportService.report*` APIs take maps).

```
Button click
  → InputActionListener reads the InputAction off the clientProperty
  → ActionDispatcher.dispatch → action.apply(...)
  → deckMapController.collectXxxChanges(key)
  → THIS group's collectChanges(key): reads live isSelected(), applies type logic
  → CLIPS report + repaint + solutionTree.refreshCurrentPhase()
```

## The group is the single source of truth for a pending status

An `InputAction` carries only **identity** — never a toggle status. `collectChanges` re-reads `isSelected()` at the moment of handling, so the status can never go stale between attaching the action and the click. Standard Swing practice: the event says *which* control, the handler asks the control *what state it is in now*.

Every `collectChanges` is a plain two-way read of `isSelected()` — including `DoorSealingButtonGroup`'s (`CLOSE`/`OPEN`), whose keep-open state deliberately plays no part. `EvacuationButtonGroup` and `ExtinguisherButtonGroup` add one thing on top: once the status is `DONE`/`USED` they disable the button, making the action one-way.

The one exception is a value **intrinsic to the button** rather than derived from its toggle state:

| Group | Control | 2nd param in the InputAction | Why |
|---|---|---|---|
| `ExplosionButtonGroup` | `ExplosionButton` | `ExplosiveType` (`AIR`, `OIL`, `REAGENT`, `DONE`) | Which explosive object this button is for — fixed by position, read from `button.getType()`. Needed to pick the pending action (`CARRY_OUT` / `PUMP_OUT` / `TO_FIGHT`) when the button is **off**; `isSelected()` alone cannot say. |
| `FlammableButtonGroup` | `PreventionButton` | `PreventionType` (`OIL`, `CLOTHES`, `MECHANICAL`, `DONE`) | Same shape: which flammable material is targeted (`PUMP_OUT` / `CARRY_OUT` / `DONE`). |
| `VentilationButtonGroup` | `VentilationButton` | — | Status recomputed from `isSelected()` (`ON`/`OFF`). |
| `DoorSealingButtonGroup` | `DoorSealingButton` | — | Status recomputed from `isSelected()` (`CLOSE`/`OPEN`). |
| `EvacuationButtonGroup` | `EvacuationButton` | — | Status recomputed from `isSelected()` (`DONE`/`NONE`), then auto-disabled. |
| `FireButtonGroup` | `FireButton` | — | Location alone is enough — reports a fire incident. |
| `FireSensorButtonGroup` | `FireSensorButton` | — | Same: attaches `FireActionInput(sensor.getLocation())`, so a sensor and a call-point button start an incident identically. |
| `MachineryDamageButtonGroup` | `PreventionButton` | — | Location alone is enough. |
| `ExtinguisherButtonGroup` | `ExtinguisherButton` | — | Inactive — no `topology.yaml` placement data yet, so zero buttons are ever built. See [`clips/INACTIVE.md`](../../../clips/INACTIVE.md). |

Note `PreventionButton` backs two different groups (`FlammableButtonGroup` and `MachineryDamageButtonGroup`); only the former reads its `getType()`.

`DoorSealingButtonGroup` is also the only group whose buttons are **not** all one size: it passes `0, 0` to the base class and overrides `getControlWidth`/`getControlHeight` to resolve each button's size per door (fire-rated vs standard, with width/height swapped for a horizontal door — both sizes are authored in vertical form in `groups.yaml`). It is likewise the only one merging two config files: placements from `controls.yaml`, glyph shape from `groups.yaml`, joined by door code.

## See also

- [`gui/actions/README.md`](../../actions/README.md) — `InputAction`, the dispatcher, and the `apply` contract (repaint once, always `refreshCurrentPhase()`).
- [`clips/values/README.md`](../../../clips/values/README.md) — the enums the collected statuses are reported as.
