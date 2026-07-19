# Configuration layer (`config` package)

This package is the **loading, validation and resolution boundary** between the on-disk YAML configuration (`src/main/resources/config/*.yaml`) and the rest of the application. It reads the raw scenario description, fails fast with an aggregated error if that description is malformed or internally inconsistent, and resolves it into the immutable domain model (`domain.registry.TopologyModel`) the expert system and the UI both consume.

Everything the ship compartment *is* — its rooms, bulkheads, doors, sensors, hydrants, extinguishers, evacuation routes, on-map control placements and drawing geometry — is authored in these YAML files, not hardcoded. `feis.clp` used to hold this data inline; it now lives here as the single source of truth (see `clips/README.md` for the CLIPS side of that migration).

## Place in the architecture

```
domain ← config ← clips ← gui ← app
              ↘          ↙
                geometry
```

Read `A ← B` as "B depends on A". Any layer may depend directly on any layer further inward (to its left), not only its immediate neighbour — `clips` and `gui`, for example, both use `domain` directly. The leaves `domain`, `geometry` and `util` depend on nothing.

`config` depends on exactly three things: `domain` (the value objects it resolves into), `geometry` (`Point`, `Size` — the raw specs carry lists of points; `Polygon`/`Polyline` are assembled later on the `gui` side) and `util` (`ResourceUtil`, for classpath access). It must **never** reach up into `clips`, `gui` or `app` — enforced against bytecode by `ArchitectureRulesTest.configurationDoesNotDependOnExpertSystemGuiOrApp`. Note that `geometry` is a shared primitive used by `config` and `gui` — **not** by `domain`: the domain model is coordinate-free (a `Location`/`Link` has no pixel position). The dependency on `domain` flows the correct direction: `config` knows about `domain`, never the reverse (`buildTopologyModel` lives here rather than as a `TopologyModel` constructor precisely so `domain.registry` never has to see the Jackson-facing DTOs).

## The pipeline: load → validate → resolve

Configuration is consumed in two phases.

**Phase 1 — load + validate (raw DTOs).** Each `DeckMap*Config` exposes a static `createDefault(...)` that calls `YamlConfigLoader.load(resource, type)`:

1. Read the YAML via `util.ResourceUtil` (classpath).
2. Strip the `$schema` / `schema` IDE-metadata key.
3. Validate against a strict in-memory Draft-7 schema generated from the target type by `ConfigSchemaFactory` (`additionalProperties: false`, enum-restricted string fields — see below). **All** violations are collected, not fail-fast.
4. On any violation, throw `config.validation.ConfigValidationException` with one line per problem; otherwise bind the tree into the DTO.

`config.loading.DeckMapConfig.createDefault()` bundles the four map configs (topology / geometry / controls / groups), runs `ConfigIntegrityChecker.check(...)` across them, and returns the validated bundle. Its constructor is private, so **you cannot obtain an unvalidated bundle**. `assembly.yaml` is the fifth config but is *not* in the bundle: it is loaded and enriched separately (`DeckMapAssemblyConfig.createDefault`) because its enrichment needs the already-built `TopologyModel`.

**Phase 2 — resolve into the domain.** `DeckMapTopologyConfig.buildTopologyModel()` turns the raw topology DTO into a `TopologyModel`: it resolves enum-like string tokens (`fromName`), concatenates door/route codes, and hands raw lists to the `domain.registry` builders. This is a pure, CLIPS-free step and runs on any JVM.

The `app.Main` startup sequence is exactly: `DeckMapConfig.createDefault()` → `getTopologyConfig().buildTopologyModel()` → `DeckMapAssemblyConfig.createDefault(groups, model.allHydrantOutlets())`.

## Package map

| Location | Contents |
|---|---|
| `config` (root) | `YamlConfigLoader` (the one validating reader); `ConfigSchemaFactory` (shared victools generator + enum-restriction post-processing + `SCHEMA_CONFIGS`); `ConfigSchemaGenerator` (writes committed schemas); `ConfigSchemaVerifier` (drift guard). |
| `config.loading` | `DeckMap{Topology,Geometry,Controls,Groups,Assembly}Config` — the top-level DTOs, one per YAML file, each with `createDefault(...)`. `DeckMapConfig` — the validated 4-config bundle. |
| `config.specification` | Leaf record DTOs referenced by the loading configs. `.basic` = raw topology/geometry description (locations, borders, doors, sensors, hydrants, extinguishers, coordinates…); `.buttons` = on-map control placements. `LocationAttached` is the marker interface (`String locationCode()`) implemented by every location-keyed placement spec. |
| `config.groups` | Generic group containers (`ToggleGroupConfig<T>`, `HydrantsGroupConfig<T>`, …) and `GroupKey`. |
| `config.enums` | Enums authored *only* in config (`HydrantLabelSize`, door glyph geometry) — distinct from `domain.types` enums. |
| `config.validation` | `ConfigIntegrityChecker`, `ConfigValidationException`. |

Resources: `src/main/resources/config/{topology,controls,groups,assembly,geometry}.yaml` plus the committed, IDE-facing `schemas/*.json` (see the drift guard below).

## Validation model — three independent layers

1. **Structural (schema).** Every YAML is validated at load against a strict Draft-7 schema derived from its DTO. Unknown keys, wrong types, and out-of-set enum values are all rejected with an aggregated message. The schema is generated in-memory for runtime validation and, identically, written to `schemas/*.json` for editor support — both go through `ConfigSchemaFactory`, so they can never disagree.
2. **Schema drift guard.** `ConfigSchemaVerifier` (Gradle task `verifyConfigSchemas`, wired into `check`) regenerates each schema and compares it structurally (`JsonNode.equals`, CRLF-insensitive) to the committed file. A DTO change that isn't followed by `./gradlew generateConfigSchemas` fails the build.
3. **Cross-file referential integrity.** `ConfigIntegrityChecker.check(...)` verifies that codes used in `controls.yaml`/`groups.yaml` (locations, sensors, hydrant titles, door/link codes) exist in `topology.yaml`, and that `geometry.yaml` matches `topology.yaml` — border links as an exact set, location polygons per-tank on an **all-or-none** basis (a tank's compartments either all have a polygon or none do; the R/T tank compartments legitimately have none).

**What is deliberately *not* validated:** geometry *coordinate values* (there is no source of truth for a point — a bulkhead drawn in the wrong place has valid structure; that class of error is caught only by the golden-master / visual review, never here), and free numeric ranges (areas, distances). Treat this layer as a guarantee of *structural and referential* correctness, not *geometric* correctness.

## Deliberate design decisions worth knowing

- **Raw specs stay plain.** The `*Spec` records carry only Jackson annotations and primitive/String fields — never `domain` enum types. This keeps the DTOs dependency-light and Jackson-friendly. Value validation of enum-like strings therefore lives in the *schema* (layer 1 above), not in the field types.
- **`domain.types` enums stay serialization-free.** They carry no Jackson annotations by design. The config layer resolves them explicitly. Each such enum has **two string vocabularies**: its constant name (lower-cased) — what YAML authors and what the schema restricts to, resolved via `fromName()` — and its `getClipsValue()` token (`co`, `auxilary`, `engine-room`) used only to talk to `feis.clp`. The schema's allowed-value lists are derived from `name().toLowerCase()`, so YAML tokens, the schema, and the enum can never drift apart.
- **Enum-value validation is centralized.** `ConfigSchemaFactory.applyEnumRestrictions` (driven by the `ENUM_RESTRICTIONS` table) injects the `enum` constraint for the six enum-backed string fields into the generated schema — one place feeding both runtime validation and the committed IDE schemas.
- **Validated-by-construction bundle.** `DeckMapConfig.createDefault()` is the single way to obtain the four-config bundle, and it validates before returning. Don't re-assemble the load sequence elsewhere.

## See also

- [`AGENTS.md`](AGENTS.md) — the actionable guardrails for editing this layer: when to regenerate schemas, where to add a cross-file rule, which test to keep green. This README is the *why*; that file is the *what to run* when making a change.
- [`clips/README.md`](../clips/README.md) — the CLIPS side of the data migration: how the `TopologyModel` resolved here is seeded into the expert-system engine.
- Root [`AGENTS.md`](../../../../AGENTS.md) — the layered architecture this sits in, plus the build / verify commands.
- [`src/test/java/AGENTS.md`](../../../../src/test/java/AGENTS.md) — test conventions for the `config` tests (DAMP, Hamcrest, fixtures).
