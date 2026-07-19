package domain.registry;

import domain.Link;
import fixtures.TestLocations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinkRegistryTest {
    private static final LocationRegistry LOCATION_REGISTRY = TestLocations.registryOf("A", "B", "C", "Q");

    @Test
    void resolves_NormalizesFromToAlphabeticallyRegardlessOfInputOrder() {
        final String code = "QA";

        LinkRegistry registry = new LinkRegistry(List.of(code), LOCATION_REGISTRY);
        Link link = registry.get(code);

        assertThat("from must be the alphabetically earlier endpoint", link.getFrom().getCode(), is("a"));
        assertThat(link.getTo().getCode(), is("q"));
        assertThat("code preserves the (uppercased) input order", link.getCode(), is(code));
    }

    @Test
    void get_IsCaseInsensitiveAndReturnsCanonicalInstance() {
        final String code = "AB";

        LinkRegistry registry = new LinkRegistry(List.of(code), LOCATION_REGISTRY);

        assertThat(registry.get(code.toLowerCase()), sameInstance(registry.get(code)));
    }

    @ParameterizedTest(name = "\"{0}\" rejected: {1}")
    @CsvSource({
        "AA,  self-link (from equals to)",
        "ABC, code longer than two characters",
        "A,   code shorter than two characters",
        "AZ,  endpoint not in the location registry"
    })
    void resolves_RejectsMalformedOrUnresolvableCode(String code, String reason) {
        assertThrows(IllegalArgumentException.class, () -> new LinkRegistry(List.of(code), LOCATION_REGISTRY));
    }

    @Test
    void resolves_DuplicateCodeFailsFast() {
        final String code = "AB";

        assertThrows(IllegalStateException.class, () -> new LinkRegistry(List.of(code, code.toLowerCase()), LOCATION_REGISTRY));
    }

    @Test
    void get_UnknownCodeThrows() {
        LinkRegistry registry = new LinkRegistry(List.of("AB"), LOCATION_REGISTRY);

        assertThrows(IllegalArgumentException.class, () -> registry.get("BC"));
    }

    @Test
    void all_IsUnmodifiableAndInInsertionOrder() {
        final String codeAB = "AB";
        final String codeBC = "BC";

        LinkRegistry registry = new LinkRegistry(List.of(codeAB, codeBC), LOCATION_REGISTRY);
        List<Link> all = registry.all();

        assertThat(all.stream().map(Link::getCode).toList(), contains(codeAB, codeBC));
        assertThrows(UnsupportedOperationException.class, () -> all.add(registry.get(codeAB)));
    }

    @Test
    void worksAsHashMapKeyByValue() {
        final String code = "AB";

        LinkRegistry registry = new LinkRegistry(List.of(code), LOCATION_REGISTRY);
        Map<Link, Integer> map = new HashMap<>();
        map.put(registry.get(code), 1);

        // Same code resolved again (case-insensitive) is an equal key.
        assertThat(map.get(registry.get(code.toLowerCase())), is(1));
    }
}
