package domain.registry;

import domain.HydrantOutlets;
import domain.Location;
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

class HydrantOutletsRegistryTest {
    private static final LocationRegistry LOCATION_REGISTRY = TestLocations.registryOf("D", "F");

    @Test
    void resolves_NormalizesTitleToLowerCase() {
        final String title = "HYDR_F";

        HydrantOutletsRegistry registry = new HydrantOutletsRegistry(Map.of(title, 1), LOCATION_REGISTRY);
        HydrantOutlets hydrant = registry.get(title);

        assertThat(hydrant.getTitle(), is(title.toLowerCase()));
        assertThat(hydrant.toString(), is(title.toLowerCase()));
    }

    @Test
    void resolves_DerivesLocationFromTitlePrefix() {
        final String titleD = "hydr_d1";
        final String titleF = "hydr_f";

        HydrantOutletsRegistry registry = new HydrantOutletsRegistry(Map.of(titleD, 2, titleF, 1), LOCATION_REGISTRY);

        assertThat(registry.get(titleD).getLocation(), sameInstance(LOCATION_REGISTRY.get("D")));
        assertThat(registry.get(titleF).getLocation(), sameInstance(LOCATION_REGISTRY.get("F")));
    }

    @Test
    void resolves_CarriesOutletCount() {
        final String title = "hydr_d1";
        final int outlets = 2;

        HydrantOutletsRegistry registry = new HydrantOutletsRegistry(Map.of(title, outlets), LOCATION_REGISTRY);

        assertThat(registry.get(title).getOutlets(), is(outlets));
    }

    @Test
    void get_IsCaseInsensitiveAndReturnsCanonicalInstance() {
        final String title = "hydr_d1";

        HydrantOutletsRegistry registry = new HydrantOutletsRegistry(Map.of(title, 2), LOCATION_REGISTRY);

        assertThat(registry.get(title.toUpperCase()), sameInstance(registry.get(title)));
    }

    @Test
    void resolves_DuplicateTitleFailsFast() {
        final String title = "hydr_f";
        Map<String, Integer> outlets = new LinkedHashMap<>();
        outlets.put(title, 1);
        outlets.put(title.toUpperCase(), 1);

        assertThrows(IllegalStateException.class, () -> new HydrantOutletsRegistry(outlets, LOCATION_REGISTRY));
    }

    @Test
    void get_UnknownTitleThrows() {
        HydrantOutletsRegistry registry = new HydrantOutletsRegistry(Map.of("hydr_f", 1), LOCATION_REGISTRY);

        assertThrows(IllegalArgumentException.class, () -> registry.get("hydr_x"));
    }

    @ParameterizedTest(name = "\"{0}\" rejected: {1}")
    @CsvSource({
        "outlet_f, title does not start with the hydr_ prefix",
        "hydr_,    nothing after the hydr_ prefix",
        "hydr_z,   location z not in the registry"
    })
    void resolves_RejectsMalformedOrUnresolvableTitle(String title, String reason) {
        assertThrows(IllegalArgumentException.class,
            () -> new HydrantOutletsRegistry(Map.of(title, 1), LOCATION_REGISTRY));
    }

    @Test
    void valueEquality_HoldsAcrossIndependentInstancesIgnoringLocationAndOutlets() {
        final String title = "hydr_f";

        HydrantOutlets one = new HydrantOutlets(title, new Location("f"), 1);
        HydrantOutlets two = new HydrantOutlets(title, new Location("d"), 99);

        assertThat(two, equalTo(one));
        assertThat(two.hashCode(), is(one.hashCode()));
    }

    @Test
    void worksAsHashMapKeyByValue() {
        final String title = "hydr_f";
        final Location location = new Location("f");
        final int outlets = 1;

        Map<HydrantOutlets, String> map = new HashMap<>();
        map.put(new HydrantOutlets(title, location, outlets), "outlet");

        assertThat(map.get(new HydrantOutlets(title, location, outlets)), is("outlet"));
    }

    @Test
    void all_IsUnmodifiableAndInInsertionOrder() {
        final String titleF = "hydr_f";
        final String titleD = "hydr_d1";
        final Map<String, Integer> outlets = new LinkedHashMap<>();
        outlets.put(titleF, 1);
        outlets.put(titleD, 2);

        HydrantOutletsRegistry registry = new HydrantOutletsRegistry(outlets, LOCATION_REGISTRY);
        List<HydrantOutlets> all = registry.all();

        assertThat(all, hasSize(outlets.size()));
        assertThat(all.stream().map(HydrantOutlets::getTitle).toList(), contains(titleF, titleD));
        assertThrows(UnsupportedOperationException.class, all::clear);
    }
}
