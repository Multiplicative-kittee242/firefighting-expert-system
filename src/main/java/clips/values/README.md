# clips.values

Enums that carry a *live user-action value* into CLIPS — the operator did something (closed a door, marked a room evacuated, chose which combustible material to flag for ignition prevention), and that choice has to reach the engine as one of a fixed set of CLIPS symbols. All are one-way, Java → CLIPS: none of them are parsed back out of a CLIPS response (`fromClipsValue`-style methods do not currently exist on any of them, despite older wording here claiming otherwise for two of them).

This is **not** the same thing as "any value CLIPS reads" — a `Location` code, a `HydrantOutlets` title, or a `domain.types.ExtinguisherType` (`"co"`/`"af"`) are also read by CLIPS, but they are static *identity*, sourced once from `topology.yaml` and seeded at startup; they belong in `domain`, not here. This package is specifically for values an operator changes *during* a run.

## The two tiers

**Plain (this package, `clips.values`) — safe to use directly in GUI code.** The CLIPS vocabulary happens to be exactly the concept the GUI already works with, so no translation is needed:

| Enum                     | Values           | Reported via                                    |
|--------------------------|------------------|--------------------------------------------------|
| `DoorState`              | `OPEN`/`CLOSE`   | `ClipsReportService.reportDoorSealingChanges`     |
| `EvacuationStatus`       | `DONE`/`NONE`    | `ClipsReportService.reportEvacuationChanges`      |
| `VentilationAction`      | `ON`/`OFF`       | `ClipsReportService.reportVentilationChanges`     |
| `MachineryDamageAction`  | `STOP`/`DONE`    | `ClipsReportService.reportMachineryDamagePreventionChanges` |

**`clips.values.internal` — forbidden outside `clips`/`gui.actions`, enforced by `ArchitectureRulesTest`.** Each has a counterpart — two now in `domain.types` (they turned out to be domain identity, not a GUI-only choice), one still in `gui.map.values` — and a `ClipsValuesMapper.toClips(...)` overload that remaps one to the other:

| Internal enum                | CLIPS values                              | Counterpart                    | Remapped via                                             |
|-------------------------------|--------------------------------------------|----------------------------------|-----------------------------------------------------------|
| `FlammablePreventionClipsAction` | `done`/`pump_out`/`carry_out`          | `PreventionType` (`domain.types`) | `ClipsValuesMapper.toClips(PreventionType)`                 |
| `ExplosionClipsAction`        | `done`/`carry_out`/`pump_out`/`to_fight`  | `ExplosiveType` (`domain.types`)  | `ClipsValuesMapper.toClips(ExplosiveType)`                 |
| `ExtinguisherClipsStatus`     | `yes`/`no`                                 | `ExtinguisherUsage` (`gui.map.values`)| `ClipsValuesMapper.toClips(ExtinguisherUsage)`         |

Reported via `ClipsReportService.reportFlammablePreventionChanges` / `reportExplosionPreventionChanges` / `reportExtinguisherChanges` respectively.

## Why the split — and why `ExtinguisherClipsStatus` is internal despite a trivial mapping

`PreventionType`/`ExplosiveType` each collapse **several** GUI concepts (which material, which button) down to fewer CLIPS actions — a genuine many-to-few translation that would be actively wrong to skip. `ExtinguisherUsage` → `ExtinguisherClipsStatus` is currently a trivial one-to-one rename (`USED`→`USED`, `NOT_USED`→`NOT_USED`), closer in shape to `DoorState`/`EvacuationStatus`. It was still placed in `internal` on request, deliberately favoring defense over the shortcut: `used` is a real CLIPS guard (`IMMEDIATE-EXTINGUISHERS::use-local` in `feis.clp` only recommends an extinguisher while `used no`), so the raw string is treated as protocol, not a GUI convenience — consistent with how a live action value is handled everywhere else in this package, and cheap insurance if the mapping ever stops being 1:1 (e.g. a future partially-used state).

The plain-tier enums (`DoorState` and friends) predate this stricter rule and were never revisited under it; new one-way action enums should default to `internal` + an appropriately-layered counterpart (`domain.types` for topological identity, `gui.map.values` for a GUI-only choice) unless there's a specific reason not to, rather than treating "the mapping happens to be trivial today" as a reason to skip the boundary.

## Notes

- Only enums with a `clipsValue` field participate in the CLIPS wire protocol; `MachineryDamageAction` has none — its two Java values are compared directly (`== MachineryDamageAction.DONE`) and routed to a different `ClipsEngineAccess` method (`reportMachineryDone`/`reportMachineryStop`) rather than interpolating a string.
- None of these enums currently have a `fromClipsValue`/parse-back direction; if one is added, update this file rather than letting the claim go stale again.
