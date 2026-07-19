# Domain model (`domain` package)

The innermost layer: the **coordinate-free vocabulary** of the shipboard-firefighting problem that every other layer speaks. A compartment, a bulkhead, a door, a hydrant, an extinguishing agent — the nouns of the domain as plain, immutable value objects, plus the registries that intern raw codes into those objects and the enums that type their attributes.

`domain` depends on **nothing** — not the config layer that feeds it, not the CLIPS layer that consumes it, not even `geometry` or `util`. It is pure JDK. That is deliberate and enforced (below): everything is allowed to know about the domain, the domain is allowed to know about nothing.

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

Read `A ← B` as "B depends on A"; a layer may depend on any layer to its left. `domain` is the far left — the leaf every other layer ultimately resolves its data into. It must **never** reach outward into `config`, `clips`, `gui` or `app`, enforced against bytecode by `ArchitectureRulesTest.domainDependsOnNoOtherProjectLayer`.

Note what `domain` deliberately does **not** depend on: `geometry`. The model is **coordinate-free** — a `Location` or `Link` has no pixel position; where a compartment is *drawn* is a `config`/`gui` concern (`geometry.Point`/`Size`), never a domain property. Keeping geometry out is what lets the same domain model back both the CLIPS reasoning and the on-screen map without either leaking into it.

## What's in the package

| Part | Contents |
|---|---|
| `domain` (root) | The value objects — 9 **topology identities** (each interned by a registry) and 4 **result carriers** (engine output). |
| `domain.registry` | The registries that resolve raw codes into those value objects, and `TopologyModel`, the aggregate that bundles all nine. |
| `domain.types` | The enums typing a value object's attributes (compartment type, ventilation, materials, sensor/extinguisher type, and two GUI-action choices). |

## The value objects (`domain` root)

All are immutable. They split cleanly in two:

- **9 topology identities**, each with a matching registry in `domain.registry`: `Location`, `Link`, `Border`, `Door`, `HydrantOutlets`, `Extinguisher`, `FireSensor`, `EvacuationRoute`, `FireHoseSpan<T>`. These are the ship's fixed structure, authored in `topology.yaml` and resolved once at startup. `Link` is the undirected edge between two `Location`s (endpoints normalized to alphabetical order); a `Door`/`Border`/`EvacuationRoute` is a typed relationship over that graph.
- **4 result carriers**, with no registry — assembled by `clips.ExpertSystemService` from engine output and handed to the GUI inside a `FireIncidentSnapshot`: `Explanation` (an inference's antecedents/consequent), `FirefightingStep` (a route's previous location + step number), `FrontlineHydrantsBalance` (hydrants here / still needed), `HydrantState` (total / free outlets).

## The registries (`domain.registry`)

A registry interns a list of **raw codes** into resolved, validated value objects and hands them back by code. The shared contract:

- **Normalize then resolve.** Keys are case-insensitive; a title/code is trimmed and cased before lookup, and cross-references (a link's endpoints, a hydrant's owning location) are resolved against the registry they depend on.
- **Fail fast.** Duplicate keys, malformed codes, self-loops, and references to unknown locations all throw at construction — a bad `topology.yaml` cannot produce a half-built model.
- **Expose immutable views.** `get(code)` throws on an unknown key; `all()` returns an unmodifiable, insertion-ordered list.

`TopologyModel` is the **aggregate root**: it bundles all nine registries, offers terse shortcuts (`location(code)`, `link(code)`, …) and aggregated `all*()` views, and is built by the static factory `TopologyModel.from(...)`, which wires the registries in dependency order (locations first, since almost everything resolves against them). It is a plain injected instance — there is deliberately no static `INSTANCE`, having replaced the former global mutable static state that used to live on `Location`.

Two abstract bases capture the recurring registry shells; the rest are standalone:

| Base | Key shape | Subclasses |
|---|---|---|
| `TitlePrefixedLocationRegistry<T>` | lower-cased `<prefix><locationCode><suffix?>` (`hydr_d1`, `est_a`) | `HydrantOutletsRegistry`, `ExtinguisherRegistry` |
| `TwoCharEndpointRegistry<T>` | upper-cased 2-char endpoint code (`AB`, `QA`) | `LinkRegistry` (undirected, alphabetical), `EvacuationRouteRegistry` (directed, input order) |

**The `Raw*` boundary is what keeps `domain` a leaf.** Registries consume plain `Raw*` records — `LocationRegistry.RawLocation`, `BorderRegistry.RawBorder`, `DoorRegistry.RawDoor`, `FireHoseSpanRegistry.RawSpan` — never a Jackson-facing `config` DTO. The `config` layer resolves its YAML into these records and calls `TopologyModel.from(...)`; because the records live here and carry only primitives/domain types, `domain` never has to import `config`. (This is also why `buildTopologyModel` lives on the `config` side, not as a `TopologyModel` constructor.)

## The types (`domain.types`)

Enums typing a value object's attributes, authored **serialization-framework-free** by design — no Jackson annotations. Most carry **two distinct string vocabularies**, and conflating them is the easy mistake:

- the **constant name**, lower-cased — what `topology.yaml` authors and what the config schema restricts to — resolved via `fromName(...)`;
- the **`getClipsValue()`** token — `feis.clp`'s own vocabulary (`co`/`af`, `engine-room`, …) — round-tripped via `getClipsValue()` / `fromClipsValue(...)` where CLIPS reads the value back.

`CompartmentType`, `ExplosiveMaterial`, `FlammableMaterial` and `ExtinguisherType` carry both vocabularies with a full round-trip. Not every enum needs both — `FireSensorType`, for one, is topological identity CLIPS never reads at all (an accident is reported on the owning `Location`, not the sensor). `PreventionType` and `ExplosiveType` are a different exception: GUI-action choices that live here as domain identity but are remapped to `clips.values.internal` by `gui.actions.ClipsValuesMapper` rather than carrying a CLIPS token of their own — see [`clips/values/README.md`](../clips/values/README.md).

## Invariants that are easy to break

- **`domain` depends on nothing.** Never import `config`, `clips`, `gui`, `app`, `geometry` or `util` from here — `domainDependsOnNoOtherProjectLayer` fails the build on any such edge. A new registry input is a `Raw*` record defined here, never a `config` type reaching in.
- **Value objects are immutable and coordinate-free.** `final` fields, no setters, no pixel position.
- **Registries fail fast and hand back immutable views.** Preserve the case-insensitive lookup, the throw-on-duplicate/unknown contract, and the unmodifiable insertion-ordered `all()`. Build the model only through `TopologyModel.from(...)`, in its fixed dependency order; do not reintroduce a static singleton.
- **`domain.types` enums stay serialization-free.** Keep the name-vocabulary (YAML) and the clips-vocabulary (`feis.clp`) as two separate string spaces; add `fromClipsValue` only when CLIPS actually reads the value back.

## See also

- [`config/README.md`](../config/README.md) — resolves `topology.yaml` into `Raw*` records and calls `TopologyModel.from(...)`; the layer that *builds* this model.
- [`clips/README.md`](../clips/README.md) — seeds the CLIPS engine from the `TopologyModel` and maps its query results back into these value objects.
- [`AGENTS.md`](AGENTS.md) — the guardrails for editing this layer.
- Root [`AGENTS.md`](../../../../AGENTS.md) — the layered architecture and the build / verify commands.
