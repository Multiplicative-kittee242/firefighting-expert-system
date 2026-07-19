package domain.registry;

import domain.Border;
import domain.Link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only in-memory registry of {@link Border} value objects, built once from a raw (link-code, length) list,
 * resolved against a {@link LinkRegistry}. Every code must already be a registered {@link Link} (borders and
 * door-derived codes are combined into one link-code list before either registry is built — see
 * {@code config.loading.DeckMapTopologyConfig#resolveLinkCodes}).
 */
public final class BorderRegistry {
    private final List<Border> all;

    public BorderRegistry(List<RawBorder> rawBorders, LinkRegistry links) {
        List<Border> borders = new ArrayList<>();
        for (RawBorder raw : rawBorders)
            borders.add(new Border(links.get(raw.link()), raw.length()));
        this.all = List.copyOf(borders);
    }

    /**
     * @return an unmodifiable view of all registered borders, in insertion order.
     */
    public List<Border> all() {
        return Collections.unmodifiableList(all);
    }

    /**
     * A raw {@code (link, length)} pair prior to endpoint resolution, independent of any config / Jackson type so this
     * registry has no dependency on the {@code config} package.
     */
    public record RawBorder(String link, double length) {}
}
