# CLIPS integration (`clips` package)

The **mapping / anti-corruption layer** between the Java + Swing application and the CLIPS expert-system engine. Everything Java needs in order to talk to CLIPS — loading the rule base, running inference, translating between domain objects and CLIPS's string vocabulary, and coping with the native engine's lifecycle quirks — is contained here. No other package touches CLIPSJNI.

CLIPS is reached through **CLIPSJNI, a 32-bit native library** (`CLIPSJNI.dll`), which is why `./gradlew runApp` and `./gradlew testClips` need the portable 32-bit JRE (see the root [`AGENTS.md`](../../../../AGENTS.md)). The rule base itself is `src/main/resources/clips/feis.clp`, loaded as a classpath resource.

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

Read `A ← B` as "B depends on A"; a layer may depend on any layer to its left, not only its immediate neighbour. `clips` sits third — the GUI (`gui`) and the composition root (`app`) consume it, and it must depend on **neither**, enforced against bytecode by `ArchitectureRulesTest.expertSystemDoesNotDependOnGuiOrApp`.

In practice `clips` depends only on `domain` (the value objects it maps to and from, plus `domain.registry.TopologyModel`) and `util` (`ResourceUtil`, to load `feis.clp` from the classpath). The layering would also permit a dependency on `config`, but there is none: `config` builds the `TopologyModel` and `app` hands the finished model to the `ExpertSystemService` constructor, so this layer never sees a Jackson-facing config DTO. The one visibility rule pointing *into* this package is that `clips.values.internal` may be referenced only from `clips` and `gui.actions` — the CLIPS wire-protocol boundary described in [`values/README.md`](values/README.md).

## What's in the package

| Type | Role |
|---|---|
| `ExpertSystemService` | The entry point. High-level orchestration in pure domain terms; implements both service interfaces below and builds `FireIncidentSnapshot`. |
| `ClipsReportService` | The **write** API (operator → engine): `reportFireIncident` plus the seven `report*Changes`. This is what `gui.actions` depends on. |
| `ClipsReadOnlyService` | The **read** API (engine → GUI): fire / fire-line locations, sealing doors, explanations, … Consumed by `gui.solution`. |
| `ClipsEngineAccess` | The only class that touches `CLIPSJNI.Environment`. All string↔domain translation, in both directions, lives here; its query/command methods are package-private, so the raw engine never leaks past `clips`. |
| `FireIncidentSnapshot` | Immutable record — the whole post-incident state in one value. Also the unit the golden-master check diffs (`ScenarioGoldenMasterCheck`). |
| `clips.values` / `clips.values.internal` | Enums carrying a live operator action into CLIPS. Own [`README`](values/README.md). |

## How it works

**Two classes, one boundary.** `ExpertSystemService` is written entirely in domain types (`Location`, `Link`, `HydrantOutlets`, …) and holds no CLIPS strings; `ClipsEngineAccess` is the only thing that speaks CLIPS. Every query returns resolved domain objects — the raw-string parsing (`parseLocations`, `parseHydrantOutlets`, …) happens inside `ClipsEngineAccess`, so a caller never sees an intermediate string. The reverse direction (`initializeTopology`'s `make-instance` specs) is built there too. (Those parsers are `static` and take the `TopologyModel` explicitly, so they are unit-testable on a normal 64-bit JVM without the engine — see `ClipsEngineAccessParsingTest`.)

**Read/write split.** The public API is two interfaces, not one: `ClipsReadOnlyService` (queries that never mutate engine state) and `ClipsReportService` (reporting operator actions). `ExpertSystemService` implements both, but each caller takes only the half it needs — the results controller reads, the action dispatcher writes.

**One entry, one snapshot.** `reportFireIncident(location)` runs the full inference in one shot and returns a single immutable `FireIncidentSnapshot` holding every collection the map and tables need — one field group per decision-support phase the operator steps through (evacuation → sealing → localization → prevention → firefighting, per `gui.solution.SolutionTreeSection`). The incremental `report*Changes` methods then feed later operator actions back in, each under the correct CLIPS module focus (`executeWithFocus`, e.g. `IMMEDIATE-EVACUATION`, `IMMEDIATE-GERMETISATION`).

## Invariants that are easy to break

The load-bearing rules of this layer — the reasons behind its non-obvious code.

- **At most one CLIPS `Environment` per JVM process.** `CLIPSJNI.Environment` has no public dispose API; teardown is GC-finalizer-driven, and constructing/destroying several in one process reliably crashes the JVM (`EXCEPTION_ACCESS_VIOLATION` inside `CLIPSJNI.dll`). So `ExpertSystemService` constructs exactly one and reuses it: `resetForNewScenario()` issues CLIPS's own `(reset)` (clears facts/instances, keeps the loaded rules) instead of building a new engine. This is the direct cause of the `testClips` "one service in `@BeforeAll`, reused across scenarios" pattern and its documented native flakiness — see the root [`AGENTS.md`](../../../../AGENTS.md). Never touch one `Environment` from two threads.
- **Only the return value is trustworthy — never the console.** CLIPS prints some errors once per session and does not reliably route them through Java's `System.out`. So every command inspects what CLIPS *returns*: the unquoted `FALSE` symbol means "could not dispatch" (almost always a bad instance address) and fails fast (`evalOrThrow` / `executeQuery`). Exactly two functions answer `FALSE` legitimately — `action-edit` when no matching `ACTION` fact exists, and `get-plan-from` when no plan exists yet — so those paths are deliberately *not* hardened (see the javadoc on `evalOrThrow` and `getStepFrom`).
- **`make-instance` failure is fatal.** Java owns instance creation, so a failed `make-instance` throws immediately (`makeInstanceOrThrow`) rather than resurfacing much later as a corrupted query result.
- **Instance names are built, never parsed back.** CLIPS returns bare location codes, not instance names, so the naming convention below is only ever used to *construct* an address. There is no read/parse path — if you ever add one, update that section.

## Data vs. rules — who owns what

`feis.clp` historically held both the inference **rules** and the initial **domain instances**. The domain instances have now moved *out* of the rule base: the application seeds them from `domain.registry.TopologyModel` (itself resolved from `topology.yaml` by the `config` layer), which is the single source of truth. CLIPS keeps only the rules it reasons over.

`ClipsEngineAccess` takes the `TopologyModel` as a constructor argument and exposes a parameterless `initializeTopology()`, called once by `ExpertSystemService` right after `load()` + `reset()`. It creates every COOL instance from the model: `LOCATION` (identity **plus** every scenario attribute — area, tank, compartment type, ventilation, explosive/burning material, machinery, chemical-suppression — replacing the former `location-attrs` facts and `apply-location-attributes` deffunction), `HYDRANT`, `EXTINGUISHER`, `BORDER` (each bulkhead in both directions, fire spreads symmetrically), `DOOR` (including deck exits, `to = out`), `EVACUATION` (each escape direction a separate route), and `FIRE-DISTANCE` (the door-to-door and hydrant-to-door hose-reach graph). No domain instance data remains declared in `feis.clp`.

## Instance naming convention

CLIPS addresses instances by name, so the application must reproduce the exact name it created. These names are an **integration concern that belongs in this package**, not in `domain` (how CLIPS names an instance is not a property of a `Location`/`Link`). All formats are defined once in `ClipsEngineAccess`:

| Class | Format | Example |
|---|---|---|
| `LOCATION` / `HYDRANT` | bare code / title | `d`, `hydr_d1` |
| `DOOR` | `door_<from>_to_<to>`, directed authoring order (not alphabetical — unlike `Link`) | `door_a_to_q`, `door_j_to_out` |
| `BORDER` | `border_<from>_upon_<upon>`, both directions | `border_a_upon_b` + `border_b_upon_a` |
| `EVACUATION` | `evac_<from>_to_<to>`, escape-direction order (**not** alphabetical) | `evac_c_to_p` (two-way = two instances) |
| `FIRE-DISTANCE` | `hosespan_<from>_<to>` | `hosespan_de_dj`, `hosespan_hydr_d1_de` |
| `EXTINGUISHER` | bare title | `est_a`, `est_j1` |

Two details worth knowing:

- **`FIRE-DISTANCE` is never addressed by name** — the rules match it via `do-for-all-instances` over `from`/`to` slot values, so the name only needs to be unique. Its door endpoints use the two-token `(from <locA> <locB>)` multislot form the rule base's `arrange-letters` matching expects, resolved order-insensitively by `domain.registry.FireHoseSpanRegistry`. That resolution silently fixed two data bugs baked into the old `feis.clp` `definstances` (a reversed endpoint order, and one door encoded as a single malformed token) that had made those hose-reach distances permanently unmatchable.
- Getting a name wrong fails **silently** in the engine, not loudly in Java. The historical example: `reportDoorStatus` once sent to `[AQ]` (the bare 2-letter code) instead of `[door_a_to_q]`, so door open/close never reached CLIPS at all.

## Doors that lead beyond the modeled compartment

The app models a single ship compartment on one deck. Two door categories cross that boundary and are handled specially — the source of their otherwise-surprising treatment in the rules:

- **Escape ladders to other decks — `door_*_to_out` (from J, K, N).** Vertical escape trunks (трапы) to adjacent decks. Their target `out` is a **sentinel**, not a `LOCATION` (`domain.Location#OUT`). As safe egress they are deliberately kept open: the sealing rules skip them (`~out` guard in `close-doors`) and force/hose routing never traverses them.
- **Fire-doors to adjacent compartments — to `R`, `T` (via D, P).** Horizontal fire-rated bulkhead doors (`fireRated: true`). `R`/`T` are real `LOCATION`s but have **no `BORDER`** to the compartment, so fire and threat never cross; the doors themselves *are* sealed during compartment sealing to contain the fire.

Fire-resistance is thus modeled *structurally* in the rule base — by what is absent from the `BORDER` graph and by the `out` sentinel — with **no** fire attribute on `DOOR`. The `fireRated` flag on each door (`topology.yaml`) is the single source of truth for door-glyph rendering only (`DeckMapTopologyConfig.getFireRatedByDoorCode()` → `DoorSealingButton`).

## Inactive & abandoned features

Three read-path queries (`getExtForLocation`, `getExtB*ForLocation`) and a few GUI groups are fully wired yet produce nothing visible at runtime — one inactive by design, one an abandoned rule-base half, one a populated group with an unwired click. The forensic detail (confirmed by live CLIPSJNI tracing) lives in [`INACTIVE.md`](INACTIVE.md), kept separate so this file stays a description of the layer rather than its loose ends.

## See also

- [`values/README.md`](values/README.md) — the action-value enum tiers (`clips.values` vs `internal`).
- [`AGENTS.md`](AGENTS.md) — guardrails for editing this layer.
- [`config/README.md`](../config/README.md) — where the topology data seeded into CLIPS comes from.
- Root [`AGENTS.md`](../../../../AGENTS.md) — `testClips`, the golden-master check, the native flakiness.
