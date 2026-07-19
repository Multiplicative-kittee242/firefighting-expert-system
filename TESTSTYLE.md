# Testing style

Testing methodology, portable across projects — the analogue of [`CODESTYLE.md`](CODESTYLE.md) and [`DOCSTYLE.md`](DOCSTYLE.md) for test code. Nothing here names a class, package, or command from any one project. Mechanical layout (braces, line length, indentation) still comes from `CODESTYLE.md` — that applies to test code too; this file is what's specific to *writing tests well*: which project's own fixtures/mocks exist, and any project-specific test-suite facts, live in that project's own `AGENTS.md` for its test source set.

## Readability first: DAMP over DRY

Tests optimize for being *obvious when they fail*, not for zero repetition. Prefer **DAMP** (Descriptive And Meaningful Phrases) over strict DRY.

- **Extract a value into a named `final` local** when it appears **2+ times in the same test**, or when its bare literal is cryptic and a name adds meaning. A value used once, whose meaning is clear from context, stays inline.
- **State expected *domain* values explicitly; do not re-derive them from the inputs.** Writing `STATION_REGISTRY.get(route.substring(1))` mirrors production's own logic and reads worse than the literal `STATION_REGISTRY.get("N")`. A derived "expected" can silently agree with a wrong production assumption; an explicit one cannot. This applies to values with independent domain meaning — not to case variants of the *same* string (see below) or to *aggregate counts* (next bullet).
- **Aggregate sizes/counts must be read back from the same collection that built the fixture, never hand-computed.** A `hasSize(4)` next to a fixture built from a 4-element list *looks* linked but isn't — nothing stops them drifting the next time either one is edited. Keep the fixture's raw lists/maps as named fields and assert `hasSize(THE_LIST.size())`, not a bare number.
- **A same-string case-insensitivity check should call `.toUpperCase()`/`.toLowerCase()` on one literal, not spell out both cases.** `registry.get(code.toLowerCase())` next to `registry.get(code)` makes the "same identity, different case" relationship structural instead of coincidental.

```java
// Good: the two codes are named (used in both arrange and assert) and expectations are explicit.
final String routeToNorth = "SN";
final String routeToSouth = "SS";
var registry = new RouteRegistry(List.of(routeToNorth, routeToSouth), STATION_REGISTRY);
assertThat(registry.get(routeToNorth).getTo(), sameInstance(STATION_REGISTRY.get("N")));
assertThat(registry.get(routeToSouth).getTo(), sameInstance(STATION_REGISTRY.get("S")));
```

```java
// Good: fixture lists are named fields; NETWORK and the size assertions both read them — edit
// STATIONS and every dependent count updates itself, nothing to keep in sync by hand.
private static final List<RawStation> STATIONS = TestStations.identities("A", "B", "C");
private static final Map<String, Integer> PLATFORM_COUNTS = Map.of("stn_a", 1, "stn_b", 2);
private static final NetworkModel NETWORK = NetworkModel.from(STATIONS, ..., PLATFORM_COUNTS, ...);

@Test
void exposesImmutableAggregatedViews() {
    assertThat(NETWORK.allStations(), hasSize(STATIONS.size()));         // not hasSize(3)
    assertThat(NETWORK.allPlatforms(), hasSize(PLATFORM_COUNTS.size())); // not hasSize(2)
}
```

```java
// Good: one literal, the other case derived from it — cannot silently drift into testing two
// unrelated strings that happen to both currently pass.
final String code = "AB";
assertThrows(IllegalStateException.class,
    () -> new RouteRegistry(List.of(code, code.toLowerCase()), STATION_REGISTRY));
```

**Exception — golden/format-string tests** (e.g. a test that locks down the exact wire-format string a builder produces): the expected value is a full literal string, even though it visibly contains the lowercased/concatenated input. Extracting pieces and reassembling them would just re-implement the production string-building logic inside the test — the exact anti-pattern the rule above forbids. Leave the expected string as one literal.

## Structure: Arrange–Act–Assert

Separate the three phases with a blank line when a test has ≥2 of them. Keep a one-expression "act" distinct from the assertions. Short single-assert tests need no blank lines.

## Shared fixtures (cross-file DRY that does *not* hurt readability)

Shared test fixtures live in their own source set — not under either test suite that consumes them — so multiple test suites can depend on the same fake/builder implementation instead of each keeping (or duplicating) its own. It should carry no test-framework dependency of its own; only the main-layer types it builds fixtures for.

Add new shared fixtures only for genuinely cross-file duplication of *dependency* setup — never to wrap the behavior a test is asserting.

## Extracting shared setup: `@BeforeAll` vs `@BeforeEach` vs a per-case helper

When most of a test method's body is arrange-phase boilerplate repeated near-verbatim across the class's test methods, split what moves out by **mutability**, not by "does it repeat":

- **`@BeforeAll`** (a `static` field set by a `static` method, unless the class already needs `@TestInstance(Lifecycle.PER_CLASS)` for another reason) — for state that is expensive or just pointless to rebuild per test **and** that no test body ever mutates: config loaded from a file, a built domain model, a decoded image, a fixed lookup table. A `static` field is genuinely shared memory across every test method in the class's run, not reset between them — verify actual immutability by checking every test body for a call that could plausibly change it, don't assume it from the field's name or its javadoc.
- **`@BeforeEach`** (a plain instance field) — for state a test body drives, mutates, or reads progressively (a fake's settable fields, a controller whose internal state a test changes and re-reads, a freshly-wrapped collaborator) — and for anything that must not leak between tests. Rebuilt for every test method, and for every `@ParameterizedTest` invocation too (JUnit 5 gives each invocation its own full lifecycle under the default `PER_METHOD` mode). `@Mock` fields are already reset per invocation by `MockitoExtension`'s own `beforeEach` callback, which JUnit runs *before* any user-defined `@BeforeEach` method — so it's safe to build a real collaborator wrapping a `@Mock` field inside your own `@BeforeEach`.
- **A private helper method, called from inside each test body** — for setup that needs a per-test-case *parameter* (e.g. which enum constant or `@MethodSource` argument this particular invocation is about). JUnit 5 does not thread `@MethodSource` arguments into `@BeforeEach`, so anything that depends on the specific case cannot live there.
- **Stays inline in the test method** — a short-lived object two or more tests happen to construct the same way is not automatically a field. If nothing in the test *reads it back* after handing it to something else, constructing it right where it's used reads more clearly than retrieving it from a shared field. Prefer a field only once the object carries state the test itself progresses or inspects across multiple statements.

```java
private static NetworkConfig networkConfig;      // never mutated by any test — @BeforeAll, static
private static NetworkModel network;

@BeforeAll
static void loadSharedFixtures() {
    networkConfig = NetworkConfig.createDefault();
    network = networkConfig.buildNetworkModel();
}

private TripPlanner tripPlanner;  // each test drives its own state — @BeforeEach

@BeforeEach
void wireFreshTripPlanner() {
    tripPlanner = new TripPlanner(scheduleService, network);
}

@ParameterizedTest
@MethodSource("routeCases")
void reportingADelay_RefreshesTheAffectedRoutes(RouteCase testCase) {
    DelayListener listener = new DelayListener();                          // never read again — inline
    TripPlanner planner = buildPlannerFor(testCase.routeKey(), listener);  // needs this case's key — a helper
    ...
}
```

## Parameterized tests for homogeneous cases

Collapse a family of "input X is rejected" cases into one `@ParameterizedTest` with `@CsvSource`, carrying a **reason** column that documents each case and shows in the test-report display name. Keep semantically distinct behaviors (a lookup miss vs a construction rejection) as separate tests.

```java
@ParameterizedTest(name = "\"{0}\" rejected: {1}")
@CsvSource({
    "AA,  self-route (from equals to)",
    "ABD, code longer than two characters",
    "AZ,  endpoint not in the station registry"
})
void resolves_RejectsMalformedOrUnresolvableCode(String code, String reason) {
    assertThrows(IllegalArgumentException.class,
        () -> new RouteRegistry(List.of(code), STATION_REGISTRY));
}
```

**`@MethodSource` defaults to no value.** Name the factory method identically to the test method (a documented JUnit 5 idiom, not a workaround) instead of `@MethodSource("someOtherName")`:

```java
private static Stream<Arguments> reportDelay_RoundTrip() {
    return DelayScenarios.cases();
}

@ParameterizedTest(name = "delay at {0}")
@MethodSource
void reportDelay_RoundTrip(Station affectedStation, List<Station> reroutedStops) {
    ...
}
```

Reach for an explicit `@MethodSource("name")` only when the same factory feeds **2+ test methods**, or when the factory's own name needs to say something the test method's name doesn't. One consequence worth knowing, not a reason to avoid this: renaming a test method now means renaming its factory in the same edit, or the suite fails fast at collection time with "Could not find factory method" — preferred over an explicit string silently continuing to resolve after the two names drift apart.

**Placement**: the factory method lives immediately above the test method it feeds — never at the bottom of the class, never grouped away with other factories. If the same factory feeds two or more test methods, it goes immediately above the *first* of them, and those test methods must themselves be consecutive:

```java
private static Stream<Arguments> gateCases() {
    ...
}

@ParameterizedTest
@MethodSource("gateCases")
void collectChanges_ReportsPendingStatus(...) { ... }

@ParameterizedTest
@MethodSource("gateCases")
void collectChanges_IgnoresAlreadyReportedStatus(...) { ... }
```

## Assertion messages

Add a description to `assertThat(...)` when a test has **multiple asserts of the same shape** (so a failure report points to the right one) or when naming the invariant adds meaning. A lone `assertThat(x, is("a"))` whose failure is self-explanatory needs none.

## Naming

A test's method name is its failure report — the display name (do **not** add `@DisplayName`, double maintenance). Aim for it to read as **`<what is under test>_<what it should do>`**, so a red test names the broken production method (or behavior) *before* you open the file. The token before the `_` is the **pivot** — the method, or failing that the concept, under test; the token after is the DAMP behavior phrase, reading as the pivot's natural continuation. Both halves stay camelCase.

Choose the pivot by the first rule that applies:

1. **One production method under test → the method's own name.** `parseStations_ResolvesEachTokenAsAStationCode`, `getPlatformByGateCode_ExcludesGatesOnAnotherLine`. The pivot keeps the method's real identifier casing (lower-case initial).

2. **The "method" is a constructor / factory / implicit build step → the behaviour verb the class is documented around, never a mechanical `constructor_`.** A registry that interns raw codes into domain objects *resolves* them: `resolves_NormalizesTitleToLowerCase`, `resolves_RejectsMalformedOrUnresolvableCode`. Reading the result back through `get()`/`all()` *inside* such a test does **not** move the pivot — those are just how the constructor's output is observed. Reserve `get_`/`all_`/`exists_` for tests that assert *that* method's own contract: `get_IsCaseInsensitiveAndReturnsCanonicalInstance`, `all_IsUnmodifiableAndInInsertionOrder`.

3. **The test exercises a *pair* of methods, or a single *concept*, not one named method → a concise concept pivot** (written like a method, lower-case initial). `equals`/`hashCode` checked together → `valueEquality_HoldsAcrossIndependentInstances`; a serialize/deserialize round-trip → `wireFormatRoundTrip`; "the returned lists are unmodifiable and insertion-ordered" → `unmodifiableAndOrderedLists`.

4. **The test class spans several production classes (one or two per method) → prefix with the class: `ProductionClass_method`.** A test class that drives a whole config-loading stack end-to-end might have `IntegrityChecker_check`, `NetworkAssemblyConfig_createDefault`, `NetworkTopologyConfig_buildNetworkModel`. Append a trailing `_Clarifier` only if two tests share the same class+method.

5. **The test asserts one shared invariant reached through several unrelated methods → name the invariant, not any one method.** A test asserting that every resolved object shares the canonical `Station` instance, reached through `station()`/`route()`/`platform()`/… at once → `stationInstance_ofRouteEndpoints`, `stationInstance_ofPlatforms`, `stationInstance_ofGates`. When not even one invariant dominates (a sweep asserting every `all*()` view is immutable), a purely descriptive name is correct: `exposesImmutableAggregatedViews`.

6. **Not a unit test of one class/method (architecture / structural / cross-cutting smoke guards) → a descriptive invariant name, exempt from the pivot rule.** An architecture-rules test's `domainDependsOnNoOtherProjectLayer` / `topLevelPackagesAreFreeOfCycles` describe a rule over the whole package graph; a method pivot would only add noise.

The **clarifier after `_` is mandatory once 2+ tests share a pivot** (so the report tells them apart — e.g. `check_*` many times in one integrity-checker test class); a lone test on an unambiguous pivot may drop it (`wireFormatRoundTrip`). A `@ParameterizedTest(name = …)` template is separate from the method name and does not count as an extra display name.

## Assertion library

Standardize on **one** assertion library across the suite (this project uses Hamcrest — `assertThat` / `is` / `sameInstance` / `contains`) and do not mix a second one (e.g. AssertJ) within a test.

## Mocking

**Prefer a hand-written fake over a mock when a collaborator just needs to hand back data.** A fake's state is plain, visible Java fields, not a scripted answer queue — and re-calling a setter to change its state mid-test (to simulate "the read side now reports something different after an action was taken") reads as an explicit, ordered statement, unlike a mocking framework's `.thenReturn(before).thenReturn(after)`, which registers both answers up front and relies on the reader trusting its internal call-count bookkeeping to hand them out in the right order. Reach for a mock on such a collaborator only when a test needs the mocking framework's own machinery specifically — `verify(...)` on a call whose arguments matter, or argument-based conditional answers a fake's fields can't express.

When a test class has **2+ tests sharing the same mocked collaborator(s)** (Mockito shown here), declare them as `@Mock`/`@Captor` fields and add `@ExtendWith(MockitoExtension.class)` at the class level, instead of repeating `Mockito.mock(...)`/`ArgumentCaptor.forClass(...)` in every method — the extension initializes the fields before each test and resets them after:

```java
@ExtendWith(MockitoExtension.class)
class SomeTest {
    @Mock
    private SomeCollaborator collaborator;
    @Captor
    private ArgumentCaptor<Map<Station, DelayStatus>> changesCaptor;

    @Test
    void someBehavior() {
        ...
        verify(collaborator).someMethod(changesCaptor.capture());
        assertThat(changesCaptor.getValue(), is(...));
    }
}
```

- **One `@Captor` field per distinct captured type.** `ArgumentCaptor<Map<Station, DelayStatus>>` and `ArgumentCaptor<Map<Route, GateState>>` cannot share a field: a captor is built from the field's own declared generic type, so give each shape its own name (`delayChangesCaptor`, `gateChangesCaptor`, …) rather than one generic `captor`.
- **Prefer an exact expected value in `verify(...)` over a captor** when you already know exactly what the call should have received — reach for `@Captor` only when you actually need to inspect or reuse the captured value afterward, e.g. across several loop iterations each expecting a different value.
- A mock used by only **one** test in the whole class stays a local `Mockito.mock(...)` — reach for `@Mock` fields once 2+ tests actually share the collaborator, not as a blanket default.
