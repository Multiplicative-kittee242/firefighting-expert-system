# Domain layer — guardrails

Scoped guidance for `src/main/java/domain`; inherits project-wide conventions from the root [`AGENTS.md`](../../../../AGENTS.md). Read [`README.md`](README.md) for what the layer is and how the value objects, registries and enums fit together; this file is the short list of invariants that are easy to break.

**CODESTYLE is mandatory** for every new or edited Java file here: before editing, read [`CODESTYLE.md`](../../../../CODESTYLE.md) (see root AGENTS.md § "Code style").

## Layering — the leaf rule
- `domain` depends on **nothing** — not `config`, `clips`, `gui`, `app`, `geometry`, or `util`. `ArchitectureRulesTest.domainDependsOnNoOtherProjectLayer` enforces the outward edges into `config`/`clips`/`gui`/`app` against bytecode (geometry/util are the same leaf rule by convention, not a separate ArchUnit check). If you reach for another package here, the design is wrong, not the rule.
- **Coordinate-free.** A value object has no pixel position; geometry belongs to `config`/`gui`. Do not add a `geometry.Point`/`Size` field to anything here.

## Value objects
- Immutable: `final` fields, no setters. A new noun is a plain value object (often a `record`).
- The 9 topology identities each have a registry (below); the 4 result carriers (`Explanation`, `FirefightingStep`, `FrontlineHydrantsBalance`, `HydrantState`) do not — they only carry engine output out to the GUI.

## Registries (`domain.registry`)
- **Fail fast, hand back immutable views.** Duplicate keys, malformed codes, self-loops and unknown cross-references throw at construction; `get` throws on an unknown key; `all()` is unmodifiable and insertion-ordered. Keep lookups case-insensitive.
- **Reuse the right base.** A title-keyed registry (`hydr_`/`est_`) extends `TitlePrefixedLocationRegistry`; a 2-char endpoint registry extends `TwoCharEndpointRegistry` (choosing directed vs alphabetical order via `orderEndpoints`). Only add a standalone registry when neither shell fits.
- **Inputs are `Raw*` records, never `config` DTOs.** This is the boundary that keeps `domain` a leaf — define a new registry's input as a `Raw*` record here and let `config` populate it. Wire any new registry into `TopologyModel.from(...)` in dependency order (whatever it resolves against comes first), and build the model only through that factory — no static singleton.

## Enums (`domain.types`)
- **No Jackson annotations** — the config layer resolves them via `fromName(...)`. Keep the two string spaces distinct: the constant name (YAML) vs `getClipsValue()` (`feis.clp`). Add `fromClipsValue(...)` only when CLIPS reads the value back.

## Tests
- Live in `src/test/java/domain` (value-object tests) and `src/test/java/domain/registry` (`*RegistryTest`, `TopologyModelTest`) and follow `src/test/java/AGENTS.md` (DAMP, Hamcrest, naming). A change to a registry's resolution/validation or to `TopologyModel.from` wiring should keep those green, and usually `config`'s `ShippedConfigValidityTest` too (it drives the real files through this layer).
- No new runtime dependencies.
