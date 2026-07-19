package clips;

import CLIPSJNI.Environment;
import CLIPSJNI.PrimitiveValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import util.ResourceUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * Locks down the exact semantic difference between {@code CLIPSJNI.Environment#reset()} and
 * {@code #clear()} that {@link ExpertSystemService#resetForNewScenario()} relies on: {@code reset()}
 * clears facts/instances but keeps the loaded rule base, while {@code clear()} wipes both. If a
 * future CLIPSJNI upgrade ever changed this, {@link ExpertSystemService#resetForNewScenario()}
 * would silently stop working (it would need a fresh {@code load()} after every reset) — this test
 * catches that regression directly, at the lowest level, independent of any scenario data.
 * <p>
 * {@code @Execution(SAME_THREAD)}/{@code @ResourceLock}: see {@link
 * IncidentReportLoopIntegrationTest}'s class javadoc — this class also constructs native {@code
 * Environment} instances, so it must not run concurrently with any other class doing the same.
 */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = FireScenarios.CLIPS_ENGINE_RESOURCE, mode = ResourceAccessMode.READ_WRITE)
class ClipsEnvironmentLifecycleTest {

    private static final String CLIPS_RULES_BASE = "clips/feis.clp";

    /**
     * Both checks share one {@code Environment} (rather than one each) to keep this class's own
     * contribution to the total native-environment count as low as possible — see the class
     * javadoc. Safe to share: {@code clear()} only runs last, after the {@code reset()} assertions
     * are already done with the environment.
     */
    @Test
    void resetPreservesRulesInstancesButClearWipesEverything() {
        Environment env = new Environment();
        env.load(ResourceUtil.resolveResourcePath(CLIPS_RULES_BASE));
        env.reset();

        long rulesAfterLoad = ruleCount(env);
        assertThat("feis.clp should define rules", rulesAfterLoad, greaterThan(0L));

        env.makeInstance("(probe_x of LOCATION (title probe_x))");
        assertThat(instanceExists(env, "probe_x"), is(true));

        env.reset();

        assertThat("reset must keep the loaded rule base intact", ruleCount(env), is(rulesAfterLoad));
        assertThat("reset must remove runtime-created instances", instanceExists(env, "probe_x"), is(false));

        env.makeInstance("(probe_y of LOCATION (title probe_y))");
        assertThat(instanceExists(env, "probe_y"), is(true));

        env.clear();

        assertThat("clear must wipe the entire rule base", ruleCount(env), is(0L));
        assertThat("clear must remove instances too", instanceExists(env, "probe_y"), is(false));
    }

    private static long ruleCount(Environment env) {
        PrimitiveValue result = env.eval("(length$ (get-defrule-list))");
        return Long.parseLong(result.toString());
    }

    private static boolean instanceExists(Environment env, String instanceName) {
        PrimitiveValue result = env.eval("(instance-existp [" + instanceName + "])");
        return "TRUE".equals(result.toString());
    }
}
