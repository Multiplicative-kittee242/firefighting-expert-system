package clips.diagnostics;

import CLIPSJNI.Environment;
import util.ResourceUtil;

/**
 * Standalone check for the assumption {@code clips.ClipsEngineAccess#evalOrThrow} is built on: a
 * bare {@code eval()} — with no preceding {@code (focus ...)}/{@code (run)} — reliably answers the
 * literal, unquoted {@code FALSE} symbol when a {@code send}/{@code instance-existp} targets an
 * instance address that does not exist, and this is never confused with a legitimate response
 * (every {@code put-<slot>} handler echoes the value it just set on success). Run via
 * {@code ./gradlew runClipsDiagnostic -PdiagClass=ClipsFalseSentinelCheck}.
 * <p>
 * Loads the real {@code feis.clp} (not a throwaway class, unlike {@link ClipsInstanceSemanticsCheck}):
 * this is specifically about how CLIPS reports a failed dispatch against this project's own
 * {@code LOCATION}/{@code DOOR} classes and message handlers, not a generic COOL property.
 * <p>
 * Also documents a sharper, non-obvious finding uncovered while writing this check: a failed
 * {@code send} to a nonexistent instance does not just answer {@code FALSE} — it leaves that
 * {@link Environment} unable to build any further instance (every subsequent {@code make-instance},
 * whether via {@code eval()} or the native {@link Environment#makeInstance} call, answers
 * {@code FALSE}/{@code null} too) until the next {@code (reset)}. This is exactly why
 * {@code evalOrThrow} throws immediately on {@code FALSE} instead of logging and continuing:
 * production never proceeds to build more instances on an environment that has already answered
 * {@code FALSE}, so the wedge is real but never actually reachable in practice.
 */
public class ClipsFalseSentinelCheck {
    private static final String CLIPS_FALSE = "FALSE";

    public static void main(String[] args) {
        DiagnosticReport report = new DiagnosticReport();

        report.section("load real feis.clp");
        Environment clips = new Environment();
        clips.load(ResourceUtil.resolveResourcePath("clips/feis.clp"));
        clips.reset();
        report.check("engine loaded and reset without throwing", true);

        report.section("bare eval() on a nonexistent instance address, no focus/run() beforehand");
        report.checkEquals("instance-existp on [zz] (never created)", CLIPS_FALSE, clips.eval("(instance-existp [zz])").toString());
        report.checkEquals("send [zz] put-status (DOOR handler, bad address)", CLIPS_FALSE, clips.eval("(send [zz] put-status close)").toString());
        report.checkEquals("send [zz] put-ventil (LOCATION handler, bad address)", CLIPS_FALSE, clips.eval("(send [zz] put-ventil on)").toString());

        report.section("a failed send wedges make-instance on this same environment until reset()");
        report.check("make-instance fails on the same (now-wedged) environment", clips.makeInstance("(w of LOCATION (title w) (area 54))") == null);
        clips.reset();
        report.check("make-instance recovers immediately after reset()", clips.makeInstance("(w of LOCATION (title w) (area 54))") != null);

        report.section("contrast: on a fresh environment, the same handler echoes on success (not FALSE)");
        Environment freshClips = new Environment();
        freshClips.load(ResourceUtil.resolveResourcePath("clips/feis.clp"));
        freshClips.reset();
        boolean created = freshClips.makeInstance("(a of LOCATION (title a) (area 54))") != null;
        report.check("a real LOCATION instance can be created directly against the loaded rule base", created);
        report.checkEquals("send [a] put-ventil echoes the value back on success (not FALSE)", "on", freshClips.eval("(send [a] put-ventil on)").toString());

        report.finish();
    }
}
