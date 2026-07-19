package domain.registry;

import domain.Extinguisher;
import domain.Location;
import domain.types.ExtinguisherType;
import fixtures.TestLocations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExtinguisherRegistryTest {
    private static final LocationRegistry LOCATION_REGISTRY = TestLocations.registryOf("D", "J");

    @Test
    void resolves_NormalizesTitleToLowerCase() {
        final String title = "EST_D";

        ExtinguisherRegistry registry = new ExtinguisherRegistry(Map.of(title, ExtinguisherType.CARBON_DIOXIDE), LOCATION_REGISTRY);
        Extinguisher extinguisher = registry.get(title);

        assertThat(extinguisher.getTitle(), is(title.toLowerCase()));
        assertThat(extinguisher.toString(), is(title.toLowerCase()));
    }

    @Test
    void resolves_DerivesLocationFromTitlePrefix() {
        final String titleJ = "est_j1";
        final String titleD = "est_d";

        ExtinguisherRegistry registry = new ExtinguisherRegistry(
            Map.of(titleJ, ExtinguisherType.CARBON_DIOXIDE, titleD, ExtinguisherType.AIR_FOAM), LOCATION_REGISTRY);

        assertThat(registry.get(titleJ).getLocation(), sameInstance(LOCATION_REGISTRY.get("J")));
        assertThat(registry.get(titleD).getLocation(), sameInstance(LOCATION_REGISTRY.get("D")));
    }

    @Test
    void resolves_CarriesExtinguisherType() {
        final String title = "est_d";
        final ExtinguisherType type = ExtinguisherType.AIR_FOAM;

        ExtinguisherRegistry registry = new ExtinguisherRegistry(Map.of(title, type), LOCATION_REGISTRY);

        assertThat(registry.get(title).getType(), is(type));
    }

    @Test
    void get_IsCaseInsensitiveAndReturnsCanonicalInstance() {
        final String title = "est_d";

        ExtinguisherRegistry registry = new ExtinguisherRegistry(Map.of(title, ExtinguisherType.CARBON_DIOXIDE), LOCATION_REGISTRY);

        assertThat(registry.get(title.toUpperCase()), sameInstance(registry.get(title)));
    }

    @Test
    void resolves_DuplicateTitleFailsFast() {
        final String title = "est_d";
        Map<String, ExtinguisherType> types = new LinkedHashMap<>();
        types.put(title, ExtinguisherType.CARBON_DIOXIDE);
        types.put(title.toUpperCase(), ExtinguisherType.AIR_FOAM);

        assertThrows(IllegalStateException.class, () -> new ExtinguisherRegistry(types, LOCATION_REGISTRY));
    }

    @Test
    void get_UnknownTitleThrows() {
        ExtinguisherRegistry registry = new ExtinguisherRegistry(Map.of("est_d", ExtinguisherType.CARBON_DIOXIDE), LOCATION_REGISTRY);

        assertThrows(IllegalArgumentException.class, () -> registry.get("est_x"));
    }

    @ParameterizedTest(name = "\"{0}\" rejected: {1}")
    @CsvSource({
        "extinguisher_d, title does not start with the est_ prefix",
        "est_,           nothing after the est_ prefix",
        "est_z,          location z not in the registry"
    })
    void resolves_RejectsMalformedOrUnresolvableTitle(String title, String reason) {
        assertThrows(IllegalArgumentException.class,
            () -> new ExtinguisherRegistry(Map.of(title, ExtinguisherType.CARBON_DIOXIDE), LOCATION_REGISTRY));
    }

    @Test
    void valueEquality_HoldsAcrossIndependentInstancesIgnoringLocationAndType() {
        final String title = "est_d";
        final Location location = new Location("d");

        Extinguisher one = new Extinguisher(title, location, ExtinguisherType.CARBON_DIOXIDE);
        Extinguisher two = new Extinguisher(title, location, ExtinguisherType.AIR_FOAM);

        assertThat(two, equalTo(one));
        assertThat(two.hashCode(), is(one.hashCode()));
    }

    @Test
    void worksAsHashMapKeyByValue() {
        final String title = "est_d";
        final Location location = new Location("d");
        final ExtinguisherType type = ExtinguisherType.CARBON_DIOXIDE;

        Map<Extinguisher, String> map = new HashMap<>();
        map.put(new Extinguisher(title, location, type), "device");

        assertThat(map.get(new Extinguisher(title, location, type)), is("device"));
    }

    @Test
    void all_IsUnmodifiableAndInInsertionOrder() {
        final String titleD = "est_d";
        final String titleJ = "est_j1";
        final Map<String, ExtinguisherType> types = new LinkedHashMap<>();
        types.put(titleD, ExtinguisherType.CARBON_DIOXIDE);
        types.put(titleJ, ExtinguisherType.AIR_FOAM);

        ExtinguisherRegistry registry = new ExtinguisherRegistry(types, LOCATION_REGISTRY);
        List<Extinguisher> all = registry.all();

        assertThat(all, hasSize(types.size()));
        assertThat(all.stream().map(Extinguisher::getTitle).toList(), contains(titleD, titleJ));
        assertThrows(UnsupportedOperationException.class, all::clear);
    }
}
