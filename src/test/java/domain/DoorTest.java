package domain;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class DoorTest {
    private static final Location FROM = new Location("A");
    private static final Location TO = new Location("B");
    private static final Location OTHER = new Location("D");

    @Test
    void valueEquality_HoldsAcrossIndependentInstances() {
        Door first = new Door(FROM, TO);
        Door second = new Door(FROM, TO);

        assertThat(first, is(second));
        assertThat(first.hashCode(), is(second.hashCode()));
    }

    @Test
    void valueEquality_DiffersForDifferentEndpoints() {
        Door ab = new Door(FROM, TO);
        Door ad = new Door(FROM, OTHER);
        Door ba = new Door(TO, FROM);

        assertThat(ab, is(not(ad)));
        assertThat(ab, is(not(ba)));
    }

    @Test
    void toString_FormatsAsFromArrowTo() {
        Door door = new Door(FROM, TO);

        assertThat(door.toString(), is(FROM + "->" + TO));
    }

    @Test
    void getTo_ReturnsOutSentinelForExteriorDoor() {
        Door exterior = new Door(FROM, Location.OUT);

        assertThat(exterior.getTo(), sameInstance(Location.OUT));
        assertThat(exterior.toString(), is(FROM + "->" + Location.OUT));
    }
}
