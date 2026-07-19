# gui.map.view — read-only overlays and map rendering

The **display** half of the deck map: everything that shows what CLIPS decided, drawn on top of the deck image. Filled fire / threat / evacuation zones, fire-line boundaries and explosion markers; and the hydrant, frontline-balance and firefighting-step labels. Nothing here takes operator input — that is the sibling `gui.map.input`. This package only paints.

It never touches `clips`. Its data arrives already-resolved through `gui.map.state` (which `gui.map.DeckMapController` populates from a `FireIncidentSnapshot`) plus `domain` value objects; the overlays and groups are **listeners** that re-render when that state changes.

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

`gui.map.view` is part of the `gui` layer — one of the `gui.map` cluster. It depends inward on `config` (geometry coordinates and group specs), `domain` (the value objects it renders, including the **result carriers** `FrontlineHydrantsBalance` / `FirefightingStep` / `HydrantState`), and `geometry` (`Point` / `Polygon` / `Polyline`). Within `gui` it builds on three siblings: it **reuses `gui.map.input`'s base classes** (`AbstractControlGroup`, `AbstractToggleButton`), reacts to `gui.map.state` (the incident state and its listener interfaces), and uses `gui.map` (`ColorPalette`, `Visible`) and `gui.Localization`.

Note the two boundaries this makes concrete:

- **No `clips` dependency.** Unlike `gui.solution`, this package does not query the engine at all; it is one step removed, rendering whatever `gui.map.state` currently holds. The pipeline `clips → FireIncidentSnapshot → gui.map.state → here` runs through `gui.map.DeckMapController`.
- **It is the read-only mirror of `gui.map.input`** and depends on it (for base classes) — a one-directional edge, never the reverse, so no cycle.

## What's in the package

Three parts:

| Part | Contents |
|---|---|
| `view.painting` | The rendering engine: `DeckMapGeometry` (turns config coordinates into screen shapes) and `MapPainter` (the stacked overlays drawn over the map image). |
| `view` (root) | The overlay **groups** — two families that rebuild themselves from view-data — plus `HydrantOffsetState`, a small stacking helper. |
| `view.controls` | The individual labels and buttons the groups own (`HydrantToggleButton` and its subclasses; the `*Label` classes). |

## How it works — state-driven, not query-driven

Every renderer here is a `gui.map.state` **listener**, re-run when the incident state changes. There are two kinds:

- **`MapPainter`** (a `MapDrawingListener`) repaints its three transparent overlays — filled locations (fire / threat / evacuation), explosion markers, and fire-line boundaries — reading the current `FireIncidentState` and converting each domain `Location`/`Link` to a screen `Polygon`/`Polyline`/`Point` via `DeckMapGeometry`.
- **The groups** (`HydrantViewListener`) rebuild their buttons/labels from a fresh `HydrantViewData`.

The contrast with `gui.map.input` matters: an input group creates its controls **once** and keeps them; a view group **rebuilds** its controls on every data change (`onHydrantViewDataChanged` → `createButtons`). There was never a stable key to persist a view control by, so `HydrantButtonGroup` exposes a flat `getButtons()` list rather than an input-style `getControlFor(key)`.

## The two group families

| Family (abstract base) | What it renders | Groups |
|---|---|---|
| `HydrantButtonGroup` (implements `HydrantViewListener`, `Visible`) | Stacked hydrant buttons (`HydrantToggleButton`) at a location | `FireHoseButtonGroup`; `HydrExtButtonGroup`, `HydrExtBButtonGroup`, `HydrExtBFromButtonGroup` |
| `AbstractHydrantLabelGroup` (extends `gui.map.input.AbstractControlGroup`, implements `HydrantViewListener`) | A `JLabel` per location/hydrant | `HydrantOutletsGroup` (outlet counts), `FrontlineBalanceGroup` (`FrontlineHydrantsBalance`), `FirefightingStepGroup` (`FirefightingStep`) |

Two things follow from `createVisibleButtons`, which only ever builds a control for a title **actually present** in the incoming data:

- The label groups are the on-map face of the `domain` **result carriers** — a frontline-balance or firefighting-step label is just that value object positioned on the deck.
- Several hydrant button groups build **nothing** in practice because their backing data is always empty. `FireHoseButtonGroup` (populated but with an unwired click) and the three `HydrExt*` groups (an abandoned rule-base half) are documented in [`clips/INACTIVE.md`](../../../clips/INACTIVE.md); read it before "cleaning up" an apparently unused group here.

`HydrantOffsetState` is the small helper that lets several hydrant buttons share one location: it tracks a per-location vertical offset so each new button stacks below the previous instead of overlapping.

## Painting — the geometry seam

`DeckMapGeometry` is where the `config` layer's coordinate specs finally become `geometry` shapes: `LocationCoordinateSpec` → `Polygon` (by location code), `BorderCoordinateSpec` → `Polyline` (by link code), explosion-marker placements → `Point`. This is the "`Polygon`/`Polyline` are assembled later on the gui side" that [`config/README.md`](../../../config/README.md) refers to — this is that side. `MapPainter` then layers three `setOpaque(false)` `JPanel` overlays over the map-image `JLabel`, each painting from `FireIncidentState` through that geometry.

## See also

- [`gui/map/input/README.md`](../input/README.md) — the operator-input sibling: shares the base classes, opposite direction (input reports to CLIPS; this package only displays).
- [`clips/INACTIVE.md`](../../../clips/INACTIVE.md) — the inactive / abandoned hydrant features whose groups live here but render nothing.
- [`clips/README.md`](../../../clips/README.md) — where the `FireIncidentSnapshot` that ultimately feeds these overlays (via `gui.map.state`) comes from.
