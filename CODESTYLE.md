# Code style

Mechanical layout rules for this project, enforced uniformly in production and test code alike. This is the single source for these rules — referenced from [`AGENTS.md`](AGENTS.md) and, transitively, every nested `AGENTS.md`/`CLAUDE.md`. Do not restate any of this elsewhere; if a rule changes, it changes here only.

## Formatting

1. **Line length**:
   - **Code** (statements, signatures, annotations, string literals that are not documentation): wrap before **180** characters.
   - **Documentation comments** — Javadoc (`/** … */`) and multi-line block comments (`/* … */`): **soft column 120**. Reflow *prose* so lines are filled toward ~120 characters and wrap at or before that width — not left sparse at 80–100, and not cut mid-thought only to stay short. Break at a natural phrase boundary (space between tokens), never mid-`{@link…}` / `{@code…}` when the tag fits on one line. Structural lines (`<p>`, list tags, blank `*`, `@param` / …) stay on their own; they are not “filled”. This is a documentation layout rule, not a second code width — production code still uses 180.
   - **Exceptions for documentation lines** (may exceed 120 when breaking would hurt more than it helps): a single long `{@link …}` / `{@code …}` that cannot wrap cleanly; a URL; a copy-pasteable command or example line. Prefer keeping those tokens intact on one line.

2. **Control-flow braces**: a single-statement `if` / `while` / `for` body takes **no** braces — but only when that statement fits on **one line** (see rule 4). The moment the body doesn't fit on one line — a long expression wrapped across lines, or the body is itself a nested control-flow statement with its own header and body (a `for`/`if` inside an outer `if`) — add braces, even though it is still a single statement. "Single-statement" is not the same test as "single-line"; only the latter licenses skipping braces. The sole exception is `if` / `else` — both branches always get braces, even when each body is a single statement:

   ```java
   if (placements == null)
       return;

   if (!validSections.contains(code)) {
       violations.add(file + ": " + section + "/code=" + code +
           " — section not found in catalog");
   }

   if (enabled) {
       activate();
   } else {
       deactivate();
   }
   ```

3. **No `var`**: always write the explicit type.

4. **Single-line `if` body goes on its own line** — never on the condition's line, even though it carries no braces. This applies to every `if`, with exactly one exception:

   ```java
   if (code != null && !valid.contains(code))
       violations.add(file + ": " + section + " — not found in catalog");
   ```

   **Exception — independent early exits that each hand back their own distinct value.** A sequence of unrelated `if (cond) return X;` lines, where each `X` is a genuinely different meaningful value (not just "there's nothing more to do here"), may stay on one line per condition. The canonical case is `equals` boilerplate:

   ```java
   @Override
   public boolean equals(Object o) {
       if (this == o) return true;
       if (!(o instanceof Book book)) return false;
       ...
   }
   ```

   `Edge.getOtherEndpoint`'s `if (side.equals(from)) return to;` / `if (side.equals(to)) return from;` and a lookup service's several `if (key == null) return Result.EMPTY;` null-guards are the same shape — each condition hands back its own distinct value, not a shared body a nested `if` could hold instead.

   This exception does **not** cover a `void` method/constructor precondition guard that just bails out with nothing to return — see rule 9, which requires the line break there with no exception — and does **not** cover a loop's `if (cond) continue;` — see rule 8. Neither of those produces a distinguishable per-condition value, so neither earns the same-line pass this exception grants.

5. **Wrapped argument lists indent +4 from the method start** (declaration or call), *not* aligned to the opening paren. Every line after the first is indented exactly 4 spaces from the start column of the method signature / call.

6. **Continuation indentation is always exactly 4 spaces**: any line break or continuation (wrapped arguments, long expressions, method chains, assignments, etc.) must use an additional indent of **exactly 4 spaces** relative to the base indentation of the statement or declaration. Never use more than 4 spaces for such continuations, and never align to the opening parenthesis or to the start column of a long expression on the previous line.

7. **Opening brace on its own line after a wrapped argument list**: when a method's parameters span more than one line, the body's opening `{` goes on a separate line, aligned with the signature — not trailing the closing paren. A single-line signature keeps the conventional trailing `{`.

   ```java
   private static void checkItemPlacements(List<Placement<String>> placements,
       Set<String> validSections, List<String> violations, String file, String section)
   {
       ...
   }
   ```

8. **Prefer an explicit nested `if` over a loop's `if (cond) continue;` guard, even when the remaining body is long.** A `continue` makes the loop body *look* flat — no extra indentation — while the code after it is still conditionally skipped; the visual shape of the code and its actual control flow disagree. Readers are visual: indentation is what tells them which `if` a line belongs to, and `continue` removes that signal without removing the conditionality it represents. Invert the condition and wrap the rest of the body in a nested `if` instead, however many statements it holds — this is not scoped to short bodies, it applies regardless of length:

   ```java
   // Bad: the loop body looks flat, but everything after `continue` is still conditional on `raw`.
   for (RawBook raw : rawBooks) {
       if (raw == null || raw.isbn() == null || raw.isbn().isBlank()) continue;
       Book book = new Book(raw.isbn(), raw.title(), raw.author(), ...);
       String key = book.getIsbn();
       if (byIsbn.containsKey(key))
           throw new IllegalStateException("Duplicate ISBN: " + raw.isbn());
       byIsbn.put(key, book);
       all.add(book);
   }

   // Good: the indentation matches what actually depends on `raw` being valid.
   for (RawBook raw : rawBooks) {
       if (raw != null && raw.isbn() != null && !raw.isbn().isBlank()) {
           Book book = new Book(raw.isbn(), raw.title(), raw.author(), ...);
           String key = book.getIsbn();
           if (byIsbn.containsKey(key))
               throw new IllegalStateException("Duplicate ISBN: " + raw.isbn());
           byIsbn.put(key, book);
           all.add(book);
       }
   }
   ```

   This is about a loop specifically — a `return` guard masking that a *repeated* body only sometimes runs. It does not reach entry/precondition validation at the top of a method or constructor; see rule 9 for that different case and where the line is drawn between them.

9. **Entry/precondition validation at the top of a method or constructor stays as sequential guards — do not nest it.** Unlike rule 8's loop guards, each of these runs exactly once per call, gating that single execution; there is no repeated/iterative body being masked, so there is nothing for nesting to make more honest. Nesting every precondition would only turn a linear checklist at the door of the method into an unnecessary pyramid:

   ```java
   private static void restrictAllowedValues(ObjectNode schema) {
       if (schema == null)
           return;
       JsonNode defsNode = schema.path("definitions");
       if (defsNode.isMissingNode() || !defsNode.isObject())
           return;
       ObjectNode definitions = (ObjectNode) defsNode;
       ...
   }
   ```

   The body still goes on its own line, always — rule 4's same-line exception is for a genuinely distinct-value early exit (`equals`-style), not for this case, which returns nothing.

   **The dividing line from rule 8**: is this a one-time gate at the top of a method/constructor (stays flat, per this rule), or is it inside a loop — or otherwise guarding a body that repeats and whose "rest" is conditional per element (gets nested, per rule 8)? When genuinely unsure, ask whether flattening it hides that *the same code runs many times but only sometimes* — if yes, it's rule 8; if the guard only ever runs once per call, it's this rule.
