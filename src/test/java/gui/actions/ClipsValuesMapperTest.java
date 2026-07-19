package gui.actions;

import clips.values.internal.ExplosionClipsAction;
import clips.values.internal.ExtinguisherClipsStatus;
import clips.values.internal.FlammablePreventionClipsAction;
import domain.types.ExplosiveType;
import domain.types.PreventionType;
import gui.map.values.ExtinguisherUsage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link ClipsValuesMapper} — a small pure utility (no Swing, no CLIPS, no I/O)
 * that remaps GUI-facing enums to their clips.values.internal equivalents.
 * Follows the homogeneous-case parameterized style from the project test conventions.
 */
class ClipsValuesMapperTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
        "OIL, PUMP_OUT, oil must be pumped out to remove flammable liquid",
        "CLOTHES, CARRY_OUT, clothes must be carried out of the compartment",
        "MECHANICAL, DONE, mechanical equipment needs no special flammable action",
        "DONE, DONE, already handled - no action required"
    })
    void toClips_FlammablePreventionClipsAction(
        PreventionType source, FlammablePreventionClipsAction expected, String reason)
    {
        assertThat(ClipsValuesMapper.toClips(source), is(expected));
    }

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
        "AIR, CARRY_OUT, compressed air is carried out",
        "OIL, PUMP_OUT, oil is pumped out to remove explosive material",
        "REAGENT, TO_FIGHT, chemical reagent requires to-fight action",
        "DONE, DONE, already handled - no action required"
    })
    void toClips_ExplosionClipsAction(
        ExplosiveType source, ExplosionClipsAction expected, String reason)
    {
        assertThat(ClipsValuesMapper.toClips(source), is(expected));
    }

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
        "USED, USED, extinguisher has been marked as used",
        "NOT_USED, NOT_USED, extinguisher has not been used"
    })
    void toClips_ExtinguisherClipsStatus(
        ExtinguisherUsage usage, ExtinguisherClipsStatus expected, String reason)
    {
        assertThat(ClipsValuesMapper.toClips(usage), is(expected));
    }

    @Test
    void remapToClips_AppliesTheMapperToEveryEntryPreservingOriginalKeys() {
        Map<String, PreventionType> source = Map.of(
            "loc1", PreventionType.OIL,
            "loc2", PreventionType.CLOTHES);

        Map<String, FlammablePreventionClipsAction> expected = Map.of(
            "loc1", FlammablePreventionClipsAction.PUMP_OUT,
            "loc2", FlammablePreventionClipsAction.CARRY_OUT);

        Map<String, FlammablePreventionClipsAction> actual = ClipsValuesMapper.remapToClips(source, ClipsValuesMapper::toClips);
        assertThat(actual, is(expected));
    }
}
