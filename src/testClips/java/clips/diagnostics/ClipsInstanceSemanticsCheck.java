package clips.diagnostics;

import CLIPSJNI.Environment;
import CLIPSJNI.InstanceAddressValue;

/**
 * Standalone check documenting the CLIPSJNI/COOL instance-creation semantics that
 * {@code clips.ClipsEngineAccess} relies on — CLIPSJNI's own documentation does not cover this, so
 * it was originally worked out empirically against a throwaway {@code TESTLOC} class (not
 * {@code feis.clp}: these are properties of CLIPS/CLIPSJNI itself, not of this project's rule base).
 * Run via {@code ./gradlew runClipsDiagnostic -PdiagClass=ClipsInstanceSemanticsCheck}.
 * <p>
 * Findings this locks down:
 * <ul>
 *   <li>{@code make-instance} omitting a slot that has no {@code (default ...)} clause still
 *       succeeds — CLIPS silently gives it a static default (an empty-string/symbol placeholder,
 *       not a build failure). There is no "required slot" concept in COOL the way there is in a
 *       constructor's parameter list.</li>
 *   <li>A slot with an explicit {@code (default none)} resolves to that value when omitted.</li>
 *   <li>The two-phase pattern this project used to rely on (bare {@code make-instance}, then a
 *       later {@code modify-instance} to enrich it) works, and does not disturb slots the second
 *       call doesn't mention.</li>
 *   <li>The one-shot alternative — every attribute supplied directly in the single
 *       {@code make-instance} call — produces the identical end state and is what
 *       {@code ClipsEngineAccess#fromLocationInstance} actually does today; the two-phase dance
 *       is no longer needed now that Java always knows every attribute up front.</li>
 * </ul>
 */
public class ClipsInstanceSemanticsCheck {
    private static final String DEFCLASS =
        "(defclass TESTLOC (is-a USER) (slot title (type LEXEME)) (slot area) (slot tank (default 3))"
        + " (slot type (type LEXEME) (default none)))";

    public static void main(String[] args) {
        DiagnosticReport report = new DiagnosticReport();
        Environment clips = new Environment();

        report.section("defclass");
        report.check("TESTLOC builds", clips.build(DEFCLASS));

        report.section("make-instance omitting a slot with no default (area)");
        InstanceAddressValue bare = clips.makeInstance("(t1 of TESTLOC (title t1))");
        report.check("instance is still created (no 'required slot' concept in COOL)", bare != null);
        report.check("instance-existp [t1] is TRUE", "TRUE".equals(clips.eval("(instance-existp [t1])").toString()));

        report.section("make-instance supplying every slot");
        InstanceAddressValue full = clips.makeInstance("(t2 of TESTLOC (title t2) (area 54) (tank 4) (type post))");
        report.check("instance is created", full != null);
        report.checkEquals("area is set", "54", clips.eval("(send [t2] get-area)").toString());
        report.checkEquals("tank is set (overriding its default 3)", "4", clips.eval("(send [t2] get-tank)").toString());
        report.checkEquals("type is set", "post", clips.eval("(send [t2] get-type)").toString());

        report.section("default resolution when a slot is omitted");
        InstanceAddressValue defaults = clips.makeInstance("(t3 of TESTLOC (title t3) (area 3))");
        report.check("instance is created", defaults != null);
        report.checkEquals("tank falls back to its (default 3)", "3", clips.eval("(send [t3] get-tank)").toString());
        report.checkEquals("type falls back to its (default none)", "none", clips.eval("(send [t3] get-type)").toString());

        report.section("two-phase: bare create, then modify-instance to enrich (the old location-attrs pattern)");
        clips.eval("(modify-instance [t1] (area 99) (tank 5) (type service))");
        report.checkEquals("area now set via modify-instance", "99", clips.eval("(send [t1] get-area)").toString());
        report.checkEquals("tank now set via modify-instance", "5", clips.eval("(send [t1] get-tank)").toString());
        report.checkEquals("type now set via modify-instance", "service", clips.eval("(send [t1] get-type)").toString());

        report.section("one-shot: every attribute in the single create call (what fromLocationInstance does today)");
        InstanceAddressValue oneShot = clips.makeInstance("(t4 of TESTLOC (title t4) (area 31) (tank 3) (type auxilary))");
        report.check("instance is created", oneShot != null);
        report.checkEquals("area matches the one-shot value directly, no second call needed", "31", clips.eval("(send [t4] get-area)").toString());
        report.checkEquals("tank matches", "3", clips.eval("(send [t4] get-tank)").toString());
        report.checkEquals("type matches", "auxilary", clips.eval("(send [t4] get-type)").toString());

        report.finish();
    }
}
