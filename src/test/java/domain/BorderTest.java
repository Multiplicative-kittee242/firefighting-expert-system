package domain;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class BorderTest {
    private static final Location A = new Location("A");
    private static final Location B = new Location("B");
    private static final Location D = new Location("D");
    private static final Link LINK_AB = new Link("AB", A, B);
    private static final Link LINK_AD = new Link("AD", A, D);

    @Test
    void valueEquality_HoldsAcrossIndependentInstancesIgnoringLength() {
        Border shortWall = new Border(LINK_AB, 1.5);
        Border longWall = new Border(LINK_AB, 9.0);

        assertThat(shortWall, is(longWall));
        assertThat(shortWall.hashCode(), is(longWall.hashCode()));
        assertThat(shortWall.getLength(), is(not(longWall.getLength())));
    }

    @Test
    void valueEquality_DiffersForDifferentLink() {
        Border ab = new Border(LINK_AB, 5.0);
        Border ad = new Border(LINK_AD, 5.0);

        assertThat(ab, is(not(ad)));
    }

    @Test
    void getters_ExposeLinkAndLength() {
        Border border = new Border(LINK_AB, 7.6);

        assertThat(border.getLink(), is(LINK_AB));
        assertThat(border.getLength(), is(7.6));
    }
}
