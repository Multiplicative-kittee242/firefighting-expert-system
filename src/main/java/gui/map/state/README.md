# gui.map.state — the deck-map observer hub

The small state package that sits between the CLIPS result and the reactive map UI. It holds the current incident's view data and notifies subscribers when it changes, so the map's input groups, overlay groups and painter never have to query the engine themselves — they just listen here.

It depends on **`domain` only** — no `config`, no `clips`, no other `gui` package. That makes it the innermost package of the `gui.map` cluster: a pure state / observer hub over domain value objects.

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

Within `gui`, everything points *at* this package, not the other way around: `gui.map.DeckMapController` pushes state in (from a `clips.FireIncidentSnapshot`), and the reactive consumers in `gui.map.input`, `gui.map.view` and `gui.map.view.painting` subscribe. This one-way decoupling is exactly why those packages carry no `clips` dependency.

## What's in the package

- **`FireIncidentState`** — the hub. Holds the current data snapshots and four lists of listeners, and fans a state change out to all of them.
- **Four listener + data-record pairs**, one per kind of consumer:

| Listener (1 method) | Data record | Notified consumers |
|---|---|---|
| `MapDrawingListener` | `PaintingViewData` | `gui.map.view.painting.MapPainter` (repaint the overlays) |
| `HydrantViewListener` | `HydrantViewData` | `gui.map.view` groups (rebuild hydrant buttons / labels) |
| `InputControlListener` | `InputControlsData` | `gui.map.input` toggle groups (reveal the recommended buttons) |
| `ExplosionControlListener` | `InputExplosionsData` | `gui.map.input.ExplosionButtonGroup` |

Every data record is an immutable bundle of `domain` collections with an `EMPTY` sentinel for the pre-incident state.

## How it works

`DeckMapController` calls `updateState(painting, hydrant, inputControls, explosionThreats)` when a new `FireIncidentSnapshot` arrives; `FireIncidentState` replaces its three view snapshots, folds the explosion threats into `InputExplosionsData`, and then notifies **all four** listener groups. The painter also reads a few fields back directly during `paintComponent` (`getFireLocations`, `getFireLines`, …).

**The explosion data is the one stateful exception.** The other three records are wholesale-replaced snapshots of what CLIPS currently reports. `InputExplosionsData` instead *accumulates*: the locations the operator has already prevented are pushed by `ExplosionButtonGroup.collectChanges` (via its `preventedLocationsStorage` consumer into `FireIncidentState.setPreventedExplosionLocations`) and are **not** cleared when the CLIPS threat set changes — `fetchPendingExplosionPreventionLocations` is threats minus what the user has handled, which is what decides whether an explosion marker is still drawn. Treat this record as mixed engine-plus-user state, unlike the pure engine snapshots beside it.

## See also

- [`gui/map/input/README.md`](../input/README.md) / [`gui/map/view/README.md`](../view/README.md) — the subscribers that react to this state.
- [`clips/README.md`](../../../clips/README.md) — the `FireIncidentSnapshot` whose contents `DeckMapController` pushes in here.
