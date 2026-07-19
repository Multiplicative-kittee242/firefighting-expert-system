package domain.types;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers {@link ExtinguisherType#fromClipsValue(String)} — the round-trip counterpart of
 * {@link ExtinguisherType#getClipsValue()}, called explicitly from
 * {@code DeckMapTopologyConfig.buildTopologyModel()} to resolve the raw {@code "co"}/{@code "af"}
 * strings authored in {@code topology.yaml} (kept as a plain static method rather than a Jackson
 * annotation so this enum has no serialization-framework dependency).
 */
class ExtinguisherTypeTest {

    @Test
    void clipsValueRoundTrip() {
        assertThat(ExtinguisherType.fromClipsValue("co"), is(ExtinguisherType.CARBON_DIOXIDE));
        assertThat(ExtinguisherType.fromClipsValue("CO"), is(ExtinguisherType.CARBON_DIOXIDE));
        assertThat(ExtinguisherType.fromClipsValue("af"), is(ExtinguisherType.AIR_FOAM));
        assertThat(ExtinguisherType.CARBON_DIOXIDE.getClipsValue(), is("co"));
        assertThat(ExtinguisherType.AIR_FOAM.getClipsValue(), is("af"));
    }

    @Test
    void fromClipsValue_UnknownValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ExtinguisherType.fromClipsValue("foam"));
    }
}
