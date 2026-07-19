package gui.map;

import config.groups.GroupKey;
import gui.solution.SolutionTreeSection;

import java.util.EnumMap;
import java.util.EnumSet;

public class MapLayerVisibilityManager {
    private static final VisibilityRule SHOW_ALL = new VisibilityRule(
        EnumSet.of(GroupKey.FIRE_SENSOR_GROUP, GroupKey.FIRE_BUTTON_GROUP),
        EnumSet.noneOf(GroupKey.class)
    );
    private static final EnumMap<SolutionTreeSection, VisibilityRule> PHASE_RULES = buildPhaseRules();

    private static EnumMap<SolutionTreeSection, VisibilityRule> buildPhaseRules() {
        EnumMap<SolutionTreeSection, VisibilityRule> rules = new EnumMap<>(SolutionTreeSection.class);
        rules.put(SolutionTreeSection.ROOT, SHOW_ALL);
        rules.put(SolutionTreeSection.PRIORITY_MEASURES, new VisibilityRule(
            EnumSet.of(GroupKey.EVACUATION_GROUP, GroupKey.VENTILATION_GROUP, GroupKey.DOOR_SEALING_GROUP),
            EnumSet.noneOf(GroupKey.class)
        ));
        rules.put(SolutionTreeSection.EVACUATION, new VisibilityRule(
            EnumSet.of(GroupKey.EVACUATION_GROUP),
            EnumSet.noneOf(GroupKey.class)
        ));
        rules.put(SolutionTreeSection.SEALING, new VisibilityRule(
            EnumSet.of(GroupKey.VENTILATION_GROUP, GroupKey.DOOR_SEALING_GROUP),
            EnumSet.noneOf(GroupKey.class)
        ));
        rules.put(SolutionTreeSection.LOCALIZATION, new VisibilityRule(
            EnumSet.of(GroupKey.FRONTLINE_BALANCE_GROUP, GroupKey.HYDRANT_OUTLETS_GROUP),
            EnumSet.of(GroupKey.FIRE_HOSE)
        ));
        rules.put(SolutionTreeSection.PREVENTION, new VisibilityRule(
            EnumSet.of(GroupKey.EXPLOSION_GROUP, GroupKey.FLAMMABLE_GROUP, GroupKey.MACHINERY_DAMAGE_GROUP),
            EnumSet.noneOf(GroupKey.class)
        ));
        rules.put(SolutionTreeSection.FIREFIGHTING, new VisibilityRule(
            EnumSet.of(GroupKey.FIREFIGHTING_STEPS_GROUP),
            EnumSet.of(GroupKey.HYDR_EXT, GroupKey.HYDR_EXT_B, GroupKey.HYDR_EXT_B_FROM)
        ));
        return rules;
    }

    private final EnumMap<GroupKey, Visible> groupByKey = new EnumMap<>(GroupKey.class);

    public void register(GroupKey key, Visible group) {
        groupByKey.put(key, group);
    }

    public void apply(SolutionTreeSection phase) {
        VisibilityRule rule = PHASE_RULES.getOrDefault(phase, SHOW_ALL);
        for (GroupKey key : GroupKey.values()) {
            Visible group = groupByKey.get(key);
            if (group != null) {
                if (rule.show().contains(key)) {
                    group.show();
                } else if (rule.showIfEnabled().contains(key)) {
                    group.showIfEnabled();
                } else {
                    group.hide();
                }
            }
        }
    }

    private record VisibilityRule(EnumSet<GroupKey> show, EnumSet<GroupKey> showIfEnabled) {
        public VisibilityRule {
            if (show == null)
                show = EnumSet.noneOf(GroupKey.class);
            if (showIfEnabled == null)
                showIfEnabled = EnumSet.noneOf(GroupKey.class);
        }
    }
}
