package clips;

import domain.types.FlammableMaterial;
import domain.types.CompartmentType;
import domain.types.ExplosiveMaterial;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;
import domain.types.VentilationType;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;

/**
 * Covers only the pure instance-spec string-building helpers used by
 * {@link ClipsEngineAccess#initializeTopology}. The actual CLIPS interaction (loading
 * the native library, {@code makeInstance}, {@code eval}) is not unit-testable here —
 * it requires the 32-bit CLIPSJNI.dll and portable JRE (see runApp) — so these tests
 * lock down the exact instance-definition syntax that was verified empirically against
 * a real CLIPS environment: {@code "(name of CLASS (slot val)...)"}, bare unbracketed
 * name, matching a single {@code definstances}/{@code deffacts} entry.
 */
class ClipsEngineAccessTest {

    @Test
    void fromLocationInstance_BuildsFullDefinitionWithDefaultsWhenAttributesAreAbsent() {
        String spec = ClipsEngineAccess.fromLocationInstance(new Location("D"));
        assertThat(spec, is("(d of LOCATION (title d) (area 0.0) (tank 3) (type none) (ventil none)"
            + " (explosive none) (burning none) (machinery none) (co none))"));
    }

    @Test
    void fromLocationInstance_BuildsFullDefinitionWithEveryScenarioAttribute() {
        Location location = new Location("E", 31.0, 3, CompartmentType.AUXILIARY, VentilationType.SMOKE_CONTROL,
            ExplosiveMaterial.DIESEL_OIL, FlammableMaterial.MACHINE_OIL, true, true);
        String spec = ClipsEngineAccess.fromLocationInstance(location);
        assertThat(spec, is("(e of LOCATION (title e) (area 31.0) (tank 3) (type auxilary) (ventil on)"
            + " (explosive diesel_oil) (burning machine_oil) (machinery on) (co yes))"));
    }

    @Test
    void fromHydrantInstance_BuildsFullDefinitionWithLocationAndOutletCounts() {
        HydrantOutlets hydrant = new HydrantOutlets("hydr_d1", new Location("D"), 2);
        String spec = ClipsEngineAccess.fromHydrantInstance(hydrant);
        assertThat(spec, is("(hydr_d1 of HYDRANT (title hydr_d1) (location d) (number 2) (free 2))"));
    }

    @Test
    void fromHydrantInstance_SetsFreeEqualToOutletsAsInitialCondition() {
        HydrantOutlets hydrant = new HydrantOutlets("hydr_j", new Location("j"), 3);
        String spec = ClipsEngineAccess.fromHydrantInstance(hydrant);
        assertThat(spec, is("(hydr_j of HYDRANT (title hydr_j) (location j) (number 3) (free 3))"));
    }

    @Test
    void fromBorderInstance_BuildsDirectedBulkheadWithLength() {
        String spec = ClipsEngineAccess.fromBorderInstance("c", "p", 2.4);
        assertThat(spec, is("(border_c_upon_p of BORDER (from c) (upon p) (length 2.4))"));
    }

    @Test
    void fromDoorInstance_BuildsInteriorDoor() {
        String spec = ClipsEngineAccess.fromDoorInstance("a", "q");
        assertThat(spec, is("(door_a_to_q of DOOR (from a) (to q))"));
    }

    @Test
    void fromDoorInstance_BuildsExitToAnotherDeck() {
        String spec = ClipsEngineAccess.fromDoorInstance("j", Location.OUT.getCode());
        assertThat(spec, is("(door_j_to_out of DOOR (from j) (to out))"));
    }

    @Test
    void fromEvacuationRouteInstance_BuildsDirectedEscapeEdge() {
        String spec = ClipsEngineAccess.fromEvacuationRouteInstance("c", "p");
        assertThat(spec, is("(evac_c_to_p of EVACUATION (from c) (to p))"));
    }

    @Test
    void fromDoorToDoorFireHoseSpanInstance_UsesTwoTokenMultislotsForBothDoors() {
        Link de = new Link("DE", new Location("D"), new Location("E"));
        Link dj = new Link("DJ", new Location("D"), new Location("J"));
        String spec = ClipsEngineAccess.fromDoorToDoorFireHoseSpanInstance(de, dj, 15.3);
        assertThat(spec, is("(hosespan_de_dj of FIRE-DISTANCE (from d e) (to d j) (value 15.3))"));
    }

    @Test
    void fromHydrantToDoorFireHoseSpanInstance_UsesSingleTokenHydrantAndTwoTokenDoor() {
        HydrantOutlets hydrant = new HydrantOutlets("hydr_d1", new Location("D"), 2);
        Link de = new Link("DE", new Location("D"), new Location("E"));
        String spec = ClipsEngineAccess.fromHydrantToDoorFireHoseSpanInstance(hydrant, de, 1.6);
        assertThat(spec, is("(hosespan_hydr_d1_de of FIRE-DISTANCE (from hydr_d1) (to d e) (value 1.6))"));
    }

    @Test
    void doorInstanceName_MatchesTheJavaToClipsNamingContract() {
        // The name the app addresses a door by must equal the name it is created under.
        Pattern doorNamePattern = Pattern.compile("door_[a-z]+_to_[a-z]+");
        assertThat(ClipsEngineAccess.doorInstanceName("a", "q"), matchesRegex(doorNamePattern));
        assertThat(ClipsEngineAccess.doorInstanceName("d", "r"), is("door_d_to_r"));
        assertThat(ClipsEngineAccess.doorInstanceName("n", Location.OUT.getCode()), is("door_n_to_out"));
    }
}
