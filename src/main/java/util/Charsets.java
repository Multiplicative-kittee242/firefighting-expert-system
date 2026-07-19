package util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Project charset constants. Prefer {@link #UTF_8} over spelling {@link StandardCharsets#UTF_8} at call sites that
 * configure streams or the console ({@code app.Main}).
 */
public final class Charsets {
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    private Charsets() {}
}
