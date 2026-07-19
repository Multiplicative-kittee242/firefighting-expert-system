# Config layer — guardrails

Scoped guidance for `src/main/java/config`; inherits project-wide conventions (code style, architecture) from the project's root [`AGENTS.md`](../../../../AGENTS.md). Read `config/README.md` for the full picture; this file is the short list of invariants that are easy to break and have broken before. Vendor-neutral (any agent or human editing this layer should follow it).

**CODESTYLE is mandatory** for every new or edited Java file here: before editing, read [`CODESTYLE.md`](../../../../CODESTYLE.md) (see root AGENTS.md § "Code style") — do not restate the rules in this file.

## Layering
- `config.*` may depend on `domain`, `geometry` and `util` only. **Never** import `clips`, `gui` or `app` — `ArchitectureRulesTest.configurationDoesNotDependOnExpertSystemGuiOrApp` enforces this against bytecode.

## DTOs and enums
- **Raw `*Spec` records stay plain** — Jackson annotations + primitive/String/`geometry` fields only. Do not type their fields with `domain` enums. Validate enum-like string values through the schema, not the field type.
- **Never add Jackson annotations to `domain.types` enums.** They are deliberately serialization-framework-free. The config layer resolves them: `fromName(...)` for YAML tokens (the constant name, lower-cased), `fromClipsValue(...)`/`getClipsValue()` for the `feis.clp` vocabulary. These are two different string spaces — don't conflate them.

## Schemas — the drift guard is real
- Enum-value restrictions are injected in one place: `ConfigSchemaFactory.applyEnumRestrictions` (`ENUM_RESTRICTIONS` table). Add new enum-backed fields there, not by hand-editing JSON.
- After any change to a `DeckMap*Config` shape or to `ENUM_RESTRICTIONS`, run `./gradlew generateConfigSchemas` and commit the regenerated `src/main/resources/config/schemas/*.json`. `verifyConfigSchemas` (wired into `check`) fails the build on drift.

## Validation split
- **Schema** = structure + enum values (per file, at load, aggregated).
- **`ConfigIntegrityChecker`** = cross-file referential integrity only (codes/links/titles exist in topology; geometry↔topology per-tank all-or-none + border-link set match). It does **not** validate geometry coordinate values — there is no source of truth for a point.
- Add a new cross-file rule to `ConfigIntegrityChecker` **and** a case to `ConfigIntegrityCheckerTest`.

## Construction
- `DeckMapConfig.createDefault()` is the only way to get the validated 4-config bundle; its constructor is private and validation runs before it returns. Don't reconstruct the load sequence.
- `DeckMap*Config.createDefault(...)` is the idiom for every config — keep new ones consistent (loading/validation in a static factory, not in a public constructor doing I/O).

## Tests
- Tests live in `src/test/java/config` and follow `src/test/java/AGENTS.md` (DAMP, Hamcrest, no `@DisplayName`).
- If you touch `buildTopologyModel` or enum resolution, keep `ShippedConfigValidityTest` green — it runs the real shipped files through load → validate → resolve on a normal JVM and is the only guard that catches a schema/parser divergence without the 32-bit CLIPS engine.
- No new runtime dependencies without an explicit request (this layer's deps are locked in `gradle.lockfile`).
