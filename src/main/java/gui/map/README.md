# gui.map — the deck-map coordinator

The deck map is the centre of the app's UI: the ship image, the coloured incident overlays, the operator's control buttons and the hydrant/plan labels, all layered together. This package is the **coordinator** that assembles those pieces and keeps them in sync, plus the small shared infrastructure (visibility contracts, the phase-visibility manager, the colour palette) the whole cluster uses. The pieces themselves live in three sub-packages, each with its own README:

- **[`gui.map.input`](input/README.md)** — the buttons the operator clicks (input → CLIPS).
- **[`gui.map.view`](view/README.md)** — the read-only overlays and labels (display CLIPS's output).
- **[`gui.map.state`](state/README.md)** — the observer hub both of the above react to.

`DeckMapController` owns all three and wires them together.

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

`gui.map` is part of the `gui` layer. It depends inward on `clips`, `config` and `domain`, and on `gui.solution` (it is a `PhaseChangeListener`, driving the map's per-phase visibility). (`geometry` enters the cluster through the `view` sub-package's painting code, not this top-level package.) Crucially, it is the **only package in the `gui.map` cluster that consumes a `clips.FireIncidentSnapshot`** — `DeckMapController` translates that snapshot into `gui.map.state` view-data, which is exactly why the `input`, `view` and `state` sub-packages carry little or no `clips` dependency of their own.

## `DeckMapController` — coordinator and snapshot adapter

The class `app.Main` constructs for the whole map. It does four jobs:

1. **Composition.** In its constructor it builds the geometry, the `MapPainter`, the `FireIncidentState` and the `MapLayerVisibilityManager`, then delegates group construction to the two assemblers (below) and lays every static group onto the map image in assembly-config layer order.
2. **Snapshot → state adapter.** `representFire(FireIncidentSnapshot)` is the boundary between the CLIPS world and the reactive UI: it splits the snapshot into the three `gui.map.state` records (`PaintingViewData`, `HydrantViewData`, `InputControlsData`) plus the explosion threats and pushes them through `FireIncidentState.updateState(...)`, which fans them out to every subscriber.
3. **Phase visibility.** As a `PhaseChangeListener` (subscribed to `gui.solution.SolutionPhaseTree`), `onPhaseChanged(section)` just calls `MapLayerVisibilityManager.apply(section)`.
4. **Input delegation.** The `collect*Changes(...)` methods (called from `gui.actions` when a button is clicked) forward to the matching input group. A missing group is a caller error (`requireGroupConfigured` throws) — **except** the extinguisher group, which is legitimately absent in every shipped config, so `collectExtinguisherChanges` returns an empty map instead.

## Shared infrastructure

**The two assemblers.** `InputGroupAssembler` and `HydrantGroupAssembler` build the input groups and the hydrant/label groups respectively from config, wire each reactive group as a `FireIncidentState` listener, register it with the `MapLayerVisibilityManager`, and expose typed getters (production `collect*Changes` access + test-only accessors). They implement an **"optional group" pattern**: a group is built only if the assembly config's `group-layers` declares its `GroupKey`, and is `null` otherwise. The shipped config declares every group, but a test can pass a trimmed assembly config to get a single-group "thematic" controller — which is why the getters are nullable and `DeckMapController` guards them.

**Visibility.** `Visible` (`show` / `hide` / `showIfEnabled`) is the per-group visibility contract the `MapLayerVisibilityManager` toggles per phase; `StaticallyVisible` extends it with `addToMap(mapLabel, initiallyVisible)`. The distinction is real: **static** groups (input controls, hydrant labels) are placed on the map once at construction in layer order; the **dynamic** hydrant button groups are `Visible`-only, rebuilt from state each time rather than placed once. `MapLayerVisibilityManager` maps each `SolutionTreeSection` to a rule of which `GroupKey`s to `show`, `showIfEnabled`, or `hide` — the mechanism that reveals the right controls for the operator's current phase.

**`ColorPalette`.** The shared, static colour constants (the reds/oranges/greys the overlays and buttons paint with) — no logic, one source of truth for the map's palette.

## See also

- [`input/README.md`](input/README.md), [`view/README.md`](view/README.md), [`state/README.md`](state/README.md) — the three sub-packages this coordinator owns.
- [`clips/README.md`](../../clips/README.md) — the `FireIncidentSnapshot` that `representFire` adapts.
- [`gui/solution/README.md`](../solution/README.md) — the phase tree whose selections drive `onPhaseChanged`.
