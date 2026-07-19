package util;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

/**
 * Canonical classpath-resource access. Pick the method by how the caller will consume the resource, not out of habit —
 * {@link #resolveResourcePath} is the odd one out and should stay rare (see its own javadoc): it forces the resource to
 * exist as a real file on disk, which is only true when this project runs off exploded classpath directories (as
 * {@code runApp}/the test source sets always do) and stops being true the moment a resource is packaged inside a jar.
 * {@link #resolveResourceUrl} and {@link #openResourceStream} both handle either case natively and are the default
 * choice for any in-JVM consumer.
 */
public final class ResourceUtil {
    private ResourceUtil() {}

    /**
     * A {@link URL} for the resource, safe to hand to any JVM API that natively understands classpath URLs — e.g.
     * {@code new ImageIcon(url)}, which (unlike {@code new ImageIcon(String)}) reads correctly whether the resource
     * lives in an exploded directory or inside a jar. Prefer this (or {@link #openResourceStream}) over
     * {@link #resolveResourcePath} for anything that isn't a native / off-JVM consumer.
     */
    public static URL resolveResourceUrl(String path) {
        URL url = ResourceUtil.class.getResource("/" + path);
        if (url == null)
            throw new IllegalStateException("Resource not found on classpath: " + path);
        return url;
    }

    /**
     * A real filesystem path string, for consumers that cannot read a classpath {@link URL} or {@link InputStream} at
     * all — namely CLIPSJNI's native {@code Environment.load(String)}, which is C code reading a file and has no
     * concept of "inside a jar". Every call site of this method today is exactly that: loading {@code feis.clp} into
     * the CLIPS engine. Reach for {@link #resolveResourceUrl}/{@link #openResourceStream} instead unless you
     * specifically hand the result to a non-JVM / native API like this one.
     */
    public static String resolveResourcePath(String path) {
        URL url = resolveResourceUrl(path);
        return Path.of(URI.create(url.toString())).toString();
    }

    public static InputStream openResourceStream(String path) {
        InputStream stream = ResourceUtil.class.getResourceAsStream("/" + path);
        if (stream == null)
            throw new IllegalStateException("Resource not found on classpath: " + path);
        return stream;
    }
}
