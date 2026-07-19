package domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrontlineHydrantsBalanceTest {

    @Test
    void constructor_RejectsNegativeHere() {
        assertThrows(IllegalArgumentException.class, () -> new FrontlineHydrantsBalance(-1, 0));
    }

    @Test
    void constructor_RejectsNegativeNeed() {
        assertThrows(IllegalArgumentException.class, () -> new FrontlineHydrantsBalance(0, -1));
    }

    @ParameterizedTest(name = "here={0}, need={1} → deficit={2}")
    @CsvSource({
        "0, 3, 3",
        "2, 5, 3",
        "4, 2, 0",
        "3, 3, 0"
    })
    void getDeficit_ClampsAtZero(int here, int need, int expectedDeficit) {
        assertThat(new FrontlineHydrantsBalance(here, need).getDeficit(), is(expectedDeficit));
    }

    @ParameterizedTest(name = "here={0}, need={1} → satisfied={2}")
    @CsvSource({
        "3, 3, true",
        "4, 3, true",
        "2, 3, false",
        "0, 1, false"
    })
    void isSatisfied_TrueWhenHereMeetsOrExceedsNeed(int here, int need, boolean expected) {
        assertThat(new FrontlineHydrantsBalance(here, need).isSatisfied(), is(expected));
    }
}
