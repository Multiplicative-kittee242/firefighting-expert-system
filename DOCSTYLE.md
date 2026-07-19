# Documentation style

Craft and process rules for writing/editing `README.md` files in this project — the analogue of [`CODESTYLE.md`](CODESTYLE.md) for prose instead of Java. This is the single source for these rules; referenced from [`AGENTS.md`](AGENTS.md), not restated there. Structural rules (which document holds what, how the architecture diagram is kept truthful) live in [`CONVENTIONS.md`](CONVENTIONS.md), not here — this file is craft only: how to write a good one, and the workflow around writing it.

## Writing a flow/capstone document

1. **Give the model before the diagrams.** A reader should have the one-paragraph mental model — what the whole thing is, the two or three big moves — before meeting the first ASCII diagram. A sequence of diagrams with only a caption each reads as a catalog of mechanisms, not an explanation; the model is what turns it into one.
2. **Every diagram gets a connecting sentence.** Introduce it (why this diagram, how it follows from what came before) and, where needed, close it (what it hands off to). A diagram dropped in with no surrounding prose is a fact, not an explanation — the prose is what makes it teach something.
3. **Don't let a table outweigh its surroundings.** A large table embedded in flowing prose reads as the important part by sheer visual weight, even when the paragraph beside it matters more. If a table doesn't earn genuine row-by-row comparison, fold its content into prose or drop it.
4. **Flow-stage notation in ASCII diagrams.** Use square brackets `[...]` for a pointer to a flow stage or a transition to another part of the document (`→ [the fan-out stage, below]`, `→ [2a / 2b / 2c]`). Reserve round brackets `(...)` for method arguments and ordinary asides (`save()               (explicit; nothing written to the cache)`). Keeps a stage-pointer from being misread as a call argument.

## Process

- **Plan before writing a substantial README** (a new capstone, a full rewrite, a multi-file pass): investigate the actual code first, then present the proposed structure and **stop** — wait for explicit confirmation before producing the document itself. Don't move from "here's the plan" straight into "here's the finished doc" in the same turn.
- **After editing any README, verify before calling it done** — the same discipline as the CODESTYLE re-check after a Java edit, not an eyeball pass:
  - every relative markdown link resolves to a real file;
  - every cited class, method, and architecture-test rule name exists in the current code;
  - no stale architecture diagram or superseded package path remains (grep for the old pattern).
