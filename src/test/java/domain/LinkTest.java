package domain;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinkTest {
    private static final Location A = new Location("A");
    private static final Location B = new Location("B");
    private static final Location D = new Location("D");
    private static final Location E = new Location("E");

    @Test
    void valueEquality_HoldsAcrossIndependentInstancesIgnoringEndpoints() {
        Link ab = new Link("AB", A, B);
        Link swappedEndpoints = new Link("AB", D, E);

        assertThat(ab, is(swappedEndpoints));
        assertThat(ab.hashCode(), is(swappedEndpoints.hashCode()));
        assertThat(ab.getFrom(), is(not(swappedEndpoints.getFrom())));
    }

    @Test
    void valueEquality_DiffersForDifferentCode() {
        Link ab = new Link("AB", A, B);
        Link ad = new Link("AD", A, D);

        assertThat(ab, is(not(ad)));
    }

    @Test
    void getOtherSide_FromReturnsTo() {
        Link link = new Link("AB", A, B);

        assertThat(link.getOtherSide(A), is(B));
    }

    @Test
    void getOtherSide_ToReturnsFrom() {
        Link link = new Link("AB", A, B);

        assertThat(link.getOtherSide(B), is(A));
    }

    @Test
    void getOtherSide_UnrelatedLocation_Throws() {
        Link link = new Link("AB", A, B);

        assertThrows(IllegalArgumentException.class, () -> link.getOtherSide(D));
    }

    @Test
    void connects_TrueInBothOrders() {
        Link link = new Link("AB", A, B);

        assertThat(link.connects(A, B), is(true));
        assertThat(link.connects(B, A), is(true));
    }

    @Test
    void connects_FalseForUnrelatedPair() {
        Link link = new Link("AB", A, B);

        assertThat(link.connects(A, D), is(false));
        assertThat(link.connects(D, E), is(false));
    }
}
