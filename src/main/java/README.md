# Architecture — the Java sources

This is the architecture map for `src/main/java`: how the layers fit together, the rules that keep them apart, and the handful of ideas you need before diving into any one package. It is a navigation hub — each layer's own README carries the depth.

For what the project *is* and how to run it, see the root [`README.md`](../../../README.md); for how to build, test and work on it, the root [`AGENTS.md`](../../../AGENTS.md).

## The dependency rule

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

Read `A ← B` as "B depends on A". Dependencies flow **inward only**: a layer may depend on any layer to its left, never to its right. `domain`, `geometry` and `util` depend on nothing and sit at the core; `app` is the outermost shell and nothing depends on it.

These edges are not a convention you have to remember — they are **enforced against compiled bytecode** by [`ArchitectureRulesTest`](../../test/java/architecture/ArchitectureRulesTest.java), so a violation fails the build rather than surfacing in review. Its architectural checks fall into three groups:

- **the inward-only layer ordering** — `domainDependsOnNoOtherProjectLayer`, `configurationDoesNotDependOnExpertSystemGuiOrApp`, `expertSystemDoesNotDependOnGuiOrApp`, `nothingDependsOnTheCompositionRoot`;
- **an acyclic graph** — `topLevelPackagesAreFreeOfCycles`;
- **the CLIPS wire-protocol boundary** — `clipsInternalValuesAreOnlyUsedByClipsOrGuiActions` (below).

## Codemap

Packages, innermost first. The three leaves (`geometry`, `util`, `app`) are small enough to describe here; the four substantial layers each have their own README.

| Package | Responsibility | Depends on | README |
|---|---|---|---|
| `domain` | The coordinate-free vocabulary of the problem — value objects, registries, typing enums — every other layer resolves its data into. | — | [domain](domain/README.md) |
| `geometry` | Coordinate primitives — `Point`, `Size`, `Polygon`, `Polyline`. Pure shapes, no domain knowledge. | — | *(here)* |
| `util` | Small shared helpers — `ResourceUtil` (classpath access), `Charsets`, the `@VisibleForTesting` marker. | — | *(here)* |
| `config` | Loads the scenario YAML, validates it against generated JSON schemas, and resolves it into the domain (`TopologyModel`). | domain, geometry, util | [config](config/README.md) |
| `clips` | The CLIPS/CLIPSJNI integration — the only code that touches the native engine. Exposes a read/write-split service over domain objects. | domain, util | [clips](clips/README.md) |
| `gui` | The Swing UI — the deck map, the solution panel, i18n, and the main window. | clips, config, domain, geometry, util | [gui](gui/README.md) |
| `app` | The composition root — `Main` alone, which wires the graph below together and starts it. | clips, config, domain, gui, util | *(here)* |

## Cross-cutting concepts

The load-bearing ideas that span layers — read these before the individual READMEs.

- **The domain is the core, and it depends on nothing.** It is deliberately **coordinate-free**: a `Location` has no pixel position — where something is *drawn* is a `geometry`/`config`/`gui` concern. Keeping geometry out is what lets the same model back both the CLIPS reasoning and the on-screen map. See [domain/README.md](domain/README.md).
- **The engine is reached through a read/write-split façade.** `clips` exposes `ClipsReportService` (operator → engine) and `ClipsReadOnlyService` (engine → UI) as two interfaces; callers work in domain objects, and every CLIPS string is confined to one class (`ClipsEngineAccess`). Each consumer takes only the half it needs. See [clips/README.md](clips/README.md).
- **Configuration is a load → validate → resolve pipeline.** The scenario — compartments, doors, hydrants, on-map placements, drawing geometry — lives in validated YAML, not in code or in the rule base, and is resolved into the domain `TopologyModel` at startup. `config` never reaches up into `clips` or `gui`. See [config/README.md](config/README.md).
- **The CLIPS wire-protocol enum boundary.** The enums in `clips.values.internal` are engine wire-values, not GUI types; only `clips` (which defines and reports them) and `gui.actions` (which remaps GUI values into them via `ClipsValuesMapper`) may reference them — the rest of the code uses a `domain.types` / `gui.map.values` counterpart. Enforced by `ArchitectureRulesTest`. See [clips/values/README.md](clips/values/README.md).
- **`app` is only the composition root.** `Main` constructs the whole graph — config → `TopologyModel` → `ExpertSystemService` → UI — and injects each collaborator; nothing is looked up globally, and nothing depends on `app`.
- **The 32-bit CLIPSJNI constraint.** The only available native binding is 32-bit, so the app and the engine-touching tests run on a portable 32-bit JRE, and those tests are split out into `src/testClips` (outside the default build). This shapes the whole runtime; the details are in the root [`AGENTS.md`](../../../AGENTS.md).

## How it runs

At startup `app.Main` resolves the YAML into a `TopologyModel`, seeds one `ExpertSystemService` (the engine façade) with it, and wires the UI to that service. From then on the application is a single operator loop — an action is reported to the engine, the engine recomputes, and the result re-renders the map and the tables. That loop, and the two decoupled paths the answer travels back on, is traced end-to-end in [gui/README.md](gui/README.md).

## Where to start reading

- **New to the codebase?** This file, then [domain/README.md](domain/README.md) — the vocabulary every other layer speaks.
- **Changing what the engine concludes?** [clips/README.md](clips/README.md) and the rule base (`src/main/resources/clips/feis.clp`); verify with the golden-master check (root AGENTS).
- **Changing the scenario** (a room, door, hydrant, placement)? [config/README.md](config/README.md) — it is all YAML.
- **Working on the UI or the action/data flows?** [gui/README.md](gui/README.md).

## See also

- Root [`README.md`](../../../README.md) — what the project is, how to run it, the tech stack.
- Root [`AGENTS.md`](../../../AGENTS.md) — build/verify commands, the source sets, the 32-bit JRE, the golden-master check.
- [`ArchitectureRulesTest`](../../test/java/architecture/ArchitectureRulesTest.java) — the enforced form of everything above.
