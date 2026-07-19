package domain.registry;

import domain.Location;
import domain.registry.LocationRegistry.RawLocation;
import domain.types.*;
import fixtures.TestLocations;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Builds its own {@link LocationRegistry} directly in every test rather than going through
 * {@code TestLocations.registryOf(…)}: this class is the subject under test, so the registry it
 * exercises must not be hidden behind a shared-fixture helper — only the raw
 * {@link RawLocation} list construction is reused, via {@link TestLocations#identities(String...)}.
 */
class LocationRegistryTest {

    @Test
    void resolves_CodeToNormalizedLowerCaseLocation() {
        final String code = "A";

        LocationRegistry registry = new LocationRegistry(TestLocations.identities(code, "B", "C"));
        Location location = registry.get(code);

        assertThat(location.getCode(), is(code.toLowerCase()));
        assertThat(location.toString(), is(code.toLowerCase()));
    }

    @Test
    void get_IsCaseInsensitiveAndReturnsCanonicalInstance() {
        final String code = "A";

        LocationRegistry registry = new LocationRegistry(TestLocations.identities(code, "B"));

        assertThat("case-insensitive lookup must return the same interned instance",
            registry.get(code.toLowerCase()), sameInstance(registry.get(code)));
    }

    @Test
    void resolves_DuplicateCodeFailsFast() {
        final String code = "A";

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> new LocationRegistry(TestLocations.identities(code, code.toLowerCase())));

        assertThat(ex.getMessage(), containsString("Duplicate location code"));
    }

    @Test
    void get_UnknownCodeThrows() {
        LocationRegistry registry = new LocationRegistry(TestLocations.identities("A", "B"));

        assertThrows(IllegalArgumentException.class, () -> registry.get("Z"));
    }

    @Test
    void resolves_SkipsBlankEntries() {
        final List<RawLocation> rawWithBlanks = TestLocations.identities("A", "", "  ", null, "B");

        LocationRegistry registry = new LocationRegistry(rawWithBlanks);

        assertThat(registry.all(), hasSize(2));
    }

    @Test
    void exists_ReflectsRegistration() {
        final String code = "A";

        LocationRegistry registry = new LocationRegistry(TestLocations.identities(code));

        assertThat(registry.exists(code.toLowerCase()), is(true));
        assertThat(registry.exists("b"), is(false));
        assertThat(registry.exists(null), is(false));
    }

    @Test
    void all_IsUnmodifiable() {
        final String code = "A";

        LocationRegistry registry = new LocationRegistry(TestLocations.identities(code));

        assertThrows(UnsupportedOperationException.class, () -> registry.all().add(registry.get(code)));
    }

    @Test
    void valueEquality_HoldsAcrossIndependentInstances() {
        final String code = "A";

        Location one = new Location(code);
        Location two = new Location(code.toLowerCase());

        assertThat(two, equalTo(one));
        assertThat(two.hashCode(), is(one.hashCode()));
    }

    @Test
    void worksAsHashMapKeyByValue() {
        final String code = "A";

        Map<Location, String> map = new HashMap<>();
        map.put(new Location(code), "room-a");

        assertThat(map.get(new Location(code.toLowerCase())), is("room-a"));
    }

    @Test
    void resolves_ScenarioAttributesFromRawLocation() {
        final String code = "E";
        RawLocation raw = new RawLocation(code, 31.0, 3, CompartmentType.AUXILIARY, VentilationType.SMOKE_CONTROL,
            ExplosiveMaterial.DIESEL_OIL, FlammableMaterial.MACHINE_OIL, true, true);

        LocationRegistry registry = new LocationRegistry(List.of(raw));
        Location location = registry.get(code);

        assertThat(location.getArea(), is(31.0));
        assertThat(location.getType(), is(CompartmentType.AUXILIARY));
        assertThat(location.getVentilationType(), is(Optional.of(VentilationType.SMOKE_CONTROL)));
        assertThat(location.getExplosiveMaterial(), is(Optional.of(ExplosiveMaterial.DIESEL_OIL)));
        assertThat(location.getBurningMaterial(), is(Optional.of(FlammableMaterial.MACHINE_OIL)));
        assertThat(location.hasMachinery(), is(true));
        assertThat(location.hasChemicalSuppression(), is(true));
    }

    @Test
    void resolves_DefaultsToUninhabitedNoAreaWhenAttributesOmitted() {
        final String code = "R";

        LocationRegistry registry = new LocationRegistry(TestLocations.identities(code));
        Location location = registry.get(code);

        assertThat(location.getArea(), is(Location.NO_AREA));
        assertThat(location.getTank(), is(Location.DEFAULT_TANK));
        assertThat(location.getType(), is(CompartmentType.UNINHABITED));
        assertThat(location.getVentilationType().isPresent(), is(false));
        assertThat(location.getExplosiveMaterial().isPresent(), is(false));
        assertThat(location.getBurningMaterial().isPresent(), is(false));
    }
}
