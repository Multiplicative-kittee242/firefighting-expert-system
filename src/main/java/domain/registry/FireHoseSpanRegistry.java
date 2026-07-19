package domain.registry;

import domain.FireHoseSpan;
import domain.HydrantOutlets;
import domain.Link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only in-memory registry of {@link FireHoseSpan} value objects: how far (metres) a fire hose must run either
 * between two adjacent doors, or from a hydrant outlet to the nearest door. Built once from two raw code lists —
 * door-to-door and hydrant-to-door — resolved against a {@link LinkRegistry} and {@link HydrantOutletsRegistry}
 * respectively.
 * <p>
 * Door endpoints are two-character codes resolved order-insensitively: the characters are sorted alphabetically before
 * lookup in {@link LinkRegistry}, matching the {@code arrange-letters} normalization the CLIPS rule base applies when
 * matching {@code FIRE-DISTANCE} facts — so a span's endpoint resolves to the same door regardless of which order its
 * two letters were authored in.
 */
public final class FireHoseSpanRegistry {
    private final List<FireHoseSpan<Link>> doorToDoor;
    private final List<FireHoseSpan<HydrantOutlets>> hydrantToDoor;

    public FireHoseSpanRegistry(List<RawSpan> doorToDoorSpans, List<RawSpan> hydrantToDoorSpans,
        LinkRegistry links, HydrantOutletsRegistry hydrants)
    {
        List<FireHoseSpan<Link>> doorToDoorList = new ArrayList<>();
        for (RawSpan span : doorToDoorSpans) {
            Link from = resolveDoor(span.from(), links);
            Link to = resolveDoor(span.to(), links);
            doorToDoorList.add(new FireHoseSpan<>(from, to, span.distance()));
        }
        this.doorToDoor = List.copyOf(doorToDoorList);

        List<FireHoseSpan<HydrantOutlets>> hydrantToDoorList = new ArrayList<>();
        for (RawSpan span : hydrantToDoorSpans) {
            HydrantOutlets from = hydrants.get(span.from());
            Link to = resolveDoor(span.to(), links);
            hydrantToDoorList.add(new FireHoseSpan<>(from, to, span.distance()));
        }
        this.hydrantToDoor = List.copyOf(hydrantToDoorList);
    }

    /**
     * @return an unmodifiable view of all door-to-door spans, in insertion order.
     */
    public List<FireHoseSpan<Link>> allDoorToDoor() {
        return Collections.unmodifiableList(doorToDoor);
    }

    /**
     * @return an unmodifiable view of all hydrant-to-door spans, in insertion order.
     */
    public List<FireHoseSpan<HydrantOutlets>> allHydrantToDoor() {
        return Collections.unmodifiableList(hydrantToDoor);
    }

    private static Link resolveDoor(String rawCode, LinkRegistry links) {
        String upperCode = rawCode == null ? "" : rawCode.trim().toUpperCase();
        if (upperCode.length() != 2)
            throw new IllegalArgumentException("Door code must be exactly 2 characters: " + rawCode);
        String sorted = upperCode.charAt(0) <= upperCode.charAt(1)
            ? upperCode
            : "" + upperCode.charAt(1) + upperCode.charAt(0);
        return links.get(sorted);
    }

    /**
     * A raw {@code (from, to, distance)} triple prior to endpoint resolution — {@code from} is either a door code or a
     * hydrant title depending on which list it is passed in, {@code to} is always a door code. Kept independent of any
     * config / Jackson type so this registry has no dependency on the {@code config} package.
     */
    public record RawSpan(String from, String to, double distance) {}
}
