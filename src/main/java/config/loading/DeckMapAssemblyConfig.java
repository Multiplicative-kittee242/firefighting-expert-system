package config.loading;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import config.YamlConfigLoader;
import config.groups.GroupKey;
import config.specification.GroupLayerSpec;
import config.specification.HydrantButtonGroupSpec;
import config.specification.basic.HydrantPlacement;
import domain.HydrantOutlets;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeckMapAssemblyConfig(
    @JsonProperty("group-layers")          List<GroupLayerSpec> groupLayerSpecs,
    @JsonProperty("hydrant-button-groups") List<HydrantButtonGroupSpec> hydrantButtonGroupSpecList
) {
    /**
     * Map view for callers (preserves previous API). The record component is the list form to make the generated schema
     * match the actual YAML array under "hydrant-button-groups".
     */
    public Map<GroupKey, HydrantButtonGroupSpec> hydrantButtonGroupSpecs() {
        return specsToMap(hydrantButtonGroupSpecList);
    }

    private static Map<GroupKey, HydrantButtonGroupSpec> specsToMap(List<HydrantButtonGroupSpec> specs) {
        Map<GroupKey, HydrantButtonGroupSpec> map = new EnumMap<>(GroupKey.class);
        if (specs != null) {
            for (HydrantButtonGroupSpec spec : specs)
                map.put(spec.key(), spec);
        }
        return map;
    }

    public static DeckMapAssemblyConfig createDefault(DeckMapGroupsConfig groupsConfig, List<HydrantOutlets> hydrantOutlets) {
        DeckMapAssemblyConfig base = YamlConfigLoader.load("config/assembly.yaml", DeckMapAssemblyConfig.class);
        Map<GroupKey, HydrantButtonGroupSpec> enriched = new EnumMap<>(GroupKey.class);
        putHydrantSpec(enriched, GroupKey.FIRE_HOSE, groupsConfig.hydrantOutConfigs(), false, hydrantOutlets);
        putHydrantSpec(enriched, GroupKey.HYDR_EXT, groupsConfig.hydrantExtConfigs(), false, hydrantOutlets);
        putHydrantSpec(enriched, GroupKey.HYDR_EXT_B, groupsConfig.hydrantExtConfigs(), false, hydrantOutlets);
        putHydrantSpec(enriched, GroupKey.HYDR_EXT_B_FROM, groupsConfig.hydrantOutConfigs(), true, hydrantOutlets);
        // Pass list form for the second component (schema alignment); map view available via accessor
        List<HydrantButtonGroupSpec> hydrantList = new ArrayList<>(enriched.values());
        return new DeckMapAssemblyConfig(base.groupLayerSpecs(), hydrantList);
    }

    private static void putHydrantSpec(Map<GroupKey, HydrantButtonGroupSpec> map, GroupKey key,
        List<HydrantPlacement> configs, boolean variableWidth, List<HydrantOutlets> hydrantOutlets)
    {
        map.put(key, new HydrantButtonGroupSpec(key, configs, variableWidth, hydrantOutlets));
    }
}
