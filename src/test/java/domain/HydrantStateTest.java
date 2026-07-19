package domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HydrantStateTest {
    private static final Location ROOM = new Location("D");
    private static final HydrantOutlets HYDRANT = new HydrantOutlets("hydr_d1", ROOM, 2);

    @Test
    void constructor_RejectsNullHydrant() {
        assertThrows(NullPointerException.class, () -> new HydrantState(null, 1, 0));
    }

    @Test
    void constructor_RejectsNegativeTotalOutlets() {
        assertThrows(IllegalArgumentException.class, () -> new HydrantState(HYDRANT, -1, 0));
    }

    @Test
    void constructor_RejectsNegativeCurrentFree() {
        assertThrows(IllegalArgumentException.class, () -> new HydrantState(HYDRANT, 2, -1));
    }

    @Test
    void constructor_RejectsCurrentFreeGreaterThanTotal() {
        assertThrows(IllegalArgumentException.class, () -> new HydrantState(HYDRANT, 2, 3));
    }

    @ParameterizedTest(name = "currentFree={0} → hasFree={1}")
    @CsvSource({
        "0, false",
        "1, true",
        "2, true"
    })
    void hasFreeConnection_BoundaryAtZero(int currentFree, boolean expected) {
        HydrantState state = new HydrantState(HYDRANT, 2, currentFree);

        assertThat(state.hasFreeConnection(), is(expected));
    }
}
