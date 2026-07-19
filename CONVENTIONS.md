# CONVENTIONS.md

Portable conventions for working in a codebase like this one — organized into layers, tested across several suites, with its own documentation set. Nothing here names a class, package, or command from *this* project; those live in [`AGENTS.md`](AGENTS.md). Copy this file into a similarly-structured project and it still applies as written.

## Documentation roles are fixed — don't blur them

- `README.md` = *why*/*how* — explanation, architecture, rationale.
- `AGENTS.md` = *guardrails* — what to run, what not to break, specific to this repository.
- `CLAUDE.md` = a shim only, never original content: it imports the local `AGENTS.md` (if one exists in the same directory), the repository root's `AGENTS.md`, and the repository root's `CONVENTIONS.md` — all three, from every nested directory that has one. When a new package gets its own `AGENTS.md`, give it a matching `CLAUDE.md` with all the applicable imports in the same change; a `CLAUDE.md` missing the `CONVENTIONS.md` import is exactly the kind of gap nothing else will flag.
- Mechanical, project-agnostic rules belong in their own files — a [`CODESTYLE.md`](CODESTYLE.md) for code, a [`DOCSTYLE.md`](DOCSTYLE.md) for prose/documentation craft — never restated inside `AGENTS.md` or a `README.md`, so they cannot drift apart from the source of truth.

## READMEs — structure and truthfulness

**Each level of README has one fixed niche — don't duplicate across levels.** The root `README.md` is the product (what it is, how to run it, tech stack). One architecture-map document is the dependency rule, a codemap of module responsibilities, cross-cutting concepts, and navigation into the deeper READMEs. A module's own `README.md` is that module's *why*/*how* in depth. A module with several sub-packages gets a **capstone** README telling the cross-package story (flows, boundaries) and linking out to each sub-package's own README for internals — it does not re-explain what the sub-README already covers.

**The architecture diagram has one canonical form, copied everywhere it appears.** If a layer diagram is duplicated verbatim into every "place in the architecture" section for locality, it drifts easily. Whenever the layer set or an edge changes, grep the whole repository for the diagram and update every copy in the same change. A stale diagram in even one file is worse than none — it reads as current.

**Verify every architectural claim against the actual code before writing it down — never from memory or an earlier conversation turn.** A package layout that gets reorganized more than once tends to leave documentation describing the *previous* shape, carried forward as if still true. Before stating that a class lives in package X, a method exists, a named architecture-test rule enforces something, or a relative link resolves: grep/read the actual file in this turn. After any package move, rename, or split, re-check every doc referencing the old path — code and docs diverge immediately, not eventually.

**"Place in the architecture" is a standard section, not free-form.** Every module README that has one follows the same shape: the canonical diagram, "read `A ← B` as `B` depends on `A`", that module's own inward dependencies, and the name of the specific automated rule that enforces it.

**Moving a class means checking for its test class too.** Production and test code are not renamed atomically by any tool — after moving `foo.Bar` to `baz.Bar`, explicitly check whether `BarTest` (mirroring the production package, in whichever test source set holds it) exists and move it to match. A left-behind test class still compiles and still passes, so nothing fails to flag the miss — it silently keeps testing code that no longer lives where the test file implies.

## Style discipline — read before, verify after

Adopting a `CODESTYLE.md`/`DOCSTYLE.md` split only pays off if it is actually consulted, not just referenced. The pattern that makes it stick:

1. **Before** creating or editing a file the style guide covers: read the style guide in this turn — don't rely on memory or on matching nearby files alone.
2. **Apply** those rules to every new or changed line.
3. **After** the edit, before calling the task done: re-check the diff against the style guide, naming the specific rules most often missed rather than relying on a generic re-read.

Skipping this is a process failure even when the test suite is green — green tests prove behavior, not style compliance.

## Collaboration

- **Write code comments and documentation in English**, regardless of what language you converse with the user in. An untranslatable domain term may be kept in its original language if immediately glossed in English in the same sentence.
- **Plan before writing something substantial** — a new capstone document, a full rewrite, a multi-file restructuring: investigate first, present the proposed structure, and **stop** — wait for explicit confirmation before producing the thing itself. Don't move from "here's the plan" straight into "here's the finished result" in the same turn.
- **Only commit when explicitly asked.** New work generally starts as a plan discussed with the maintainer, not an unprompted implementation.
