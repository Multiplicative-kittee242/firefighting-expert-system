package domain.registry;

import domain.FireSensor;
import domain.Location;
import domain.types.FireSensorType;
import fixtures.TestLocations;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FireSensorRegistryTest {
    private static final LocationRegistry LOCATION_REGISTRY = TestLocations.registryOf("A", "B", "D");

    @Test
    void resolves_NormalizesCodeToUpperCase() {
        final String code = "a1";

        FireSensorRegistry registry = new FireSensorRegistry(Map.of(code, FireSensorType.COMBINED), LOCATION_REGISTRY);
        FireSensor sensor = registry.get(code);

        assertThat(sensor.getCode(), is(code.toUpperCase()));
    }

    @Test
    void resolves_LocationIsTheFirstCharacter() {
        final String code = "D3";

        FireSensorRegistry registry = new FireSensorRegistry(Map.of(code, FireSensorType.RATE_OF_RISE), LOCATION_REGISTRY);
        FireSensor sensor = registry.get(code);

        assertThat(sensor.getLocation(), sameInstance(LOCATION_REGISTRY.get("D")));
        assertThat(sensor.getLocation().getCode(), is("d"));
    }

    @Test
    void resolves_CarriesSensorType() {
        final String code = "D3";
        final FireSensorType type = FireSensorType.RATE_OF_RISE;

        FireSensorRegistry registry = new FireSensorRegistry(Map.of(code, type), LOCATION_REGISTRY);

        assertThat(registry.get(code).getType(), is(type));
    }

    @Test
    void get_IsCaseInsensitiveAndReturnsCanonicalInstance() {
        final String code = "A1";

        FireSensorRegistry registry = new FireSensorRegistry(Map.of(code, FireSensorType.COMBINED), LOCATION_REGISTRY);

        assertThat(registry.get(code.toLowerCase()), sameInstance(registry.get(code)));
    }

    @Test
    void resolves_DuplicateCodeFailsFast() {
        final String code = "A1";

        assertThrows(IllegalStateException.class,
            () -> new FireSensorRegistry(Map.of(code, FireSensorType.COMBINED, code.toLowerCase(), FireSensorType.COMBINED), LOCATION_REGISTRY));
    }

    @Test
    void get_UnknownCodeThrows() {
        FireSensorRegistry registry = new FireSensorRegistry(Map.of("A1", FireSensorType.COMBINED), LOCATION_REGISTRY);

        assertThrows(IllegalArgumentException.class, () -> registry.get("B2"));
    }

    @Test
    void resolves_RejectsSensorOnUnknownLocation() {
        assertThrows(IllegalArgumentException.class,
            () -> new FireSensorRegistry(Map.of("Z9", FireSensorType.COMBINED), LOCATION_REGISTRY));
    }

    @Test
    void valueEquality_HoldsAcrossIndependentInstancesIgnoringLocationAndType() {
        final String code = "A1";

        FireSensor one = new FireSensor(code, new Location("a"), FireSensorType.COMBINED);
        FireSensor two = new FireSensor(code, new Location("b"), FireSensorType.RATE_OF_RISE);

        assertThat(two, equalTo(one));
        assertThat(two.hashCode(), is(one.hashCode()));
    }

    @Test
    void worksAsHashMapKeyByValue() {
        final String code = "A1";

        Map<FireSensor, String> map = new HashMap<>();
        map.put(new FireSensor(code, new Location("a"), FireSensorType.COMBINED), "sensor-a1");

        assertThat(map.get(new FireSensor(code, new Location("b"), FireSensorType.RATE_OF_RISE)), is("sensor-a1"));
    }

    @Test
    void all_IsUnmodifiable() {
        final String code = "A1";
        final Map<String, FireSensorType> sensorTypes = Map.of(code, FireSensorType.COMBINED);

        FireSensorRegistry registry = new FireSensorRegistry(sensorTypes, LOCATION_REGISTRY);
        List<FireSensor> all = registry.all();

        assertThat(all, hasSize(sensorTypes.size()));
        assertThrows(UnsupportedOperationException.class, all::clear);
    }
}
