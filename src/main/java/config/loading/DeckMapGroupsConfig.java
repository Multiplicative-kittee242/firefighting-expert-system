package config.loading;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import config.YamlConfigLoader;
import config.specification.*;
import config.groups.*;
import config.specification.buttons.ExplosionButtonSpec;
import config.specification.buttons.FireSensorButtonSpec;
import config.specification.buttons.FlammableButtonSpec;
import config.specification.basic.HydrantPlacement;
import config.specification.buttons.MachineryDamageButtonSpec;
import config.specification.buttons.VentilationButtonSpec;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeckMapGroupsConfig(
    @JsonProperty("ventilation-group")           ToggleGroupConfig<VentilationButtonSpec> ventilationGroupConfig,
    @JsonProperty("flammable-group")             ToggleGroupConfig<FlammableButtonSpec> flammableGroupConfig,
    @JsonProperty("machinery-damage-group")      ToggleGroupConfig<MachineryDamageButtonSpec> machineryDamageGroupConfig,
    @JsonProperty("fire-sensor-group")           ToggleGroupConfig<FireSensorButtonSpec> fireSensorGroupConfig,
    @JsonProperty("explosion-group")             ToggleGroupConfig<ExplosionButtonSpec> explosionGroupConfig,
    @JsonProperty("frontline-balance-label-group") HydrantsGroupConfig<FrontlineBalanceGroupSpec> frontlineBalanceGroupConfig,
    @JsonProperty("firefighting-step-label-group") HydrantsGroupConfig<FirefightingStepGroupSpec> firefightingStepGroupConfig,
    @JsonProperty("hydrant-outlet-label-group")  HydrOutletLabelGroupConfig hydrOutletLabelGroupConfig,
    @JsonProperty("door-button-group")           DoorButtonGroupConfig doorButtonGroup,
    List<HydrantPlacement> hydrantOutConfigs,
    List<HydrantPlacement> hydrantExtConfigs)
{
    @JsonCreator
    public DeckMapGroupsConfig(
        @JsonProperty("ventilation-group")           ToggleGroupConfig<VentilationButtonSpec> ventilationGroupConfig,
        @JsonProperty("flammable-group")             ToggleGroupConfig<FlammableButtonSpec> flammableGroupConfig,
        @JsonProperty("machinery-damage-group")      ToggleGroupConfig<MachineryDamageButtonSpec> machineryDamageGroupConfig,
        @JsonProperty("fire-sensor-group")           ToggleGroupConfig<FireSensorButtonSpec> fireSensorGroupConfig,
        @JsonProperty("explosion-group")             ToggleGroupConfig<ExplosionButtonSpec> explosionGroupConfig,
        @JsonProperty("frontline-balance-label-group") HydrantsGroupConfig<FrontlineBalanceGroupSpec> frontlineBalanceGroupConfig,
        @JsonProperty("firefighting-step-label-group") HydrantsGroupConfig<FirefightingStepGroupSpec> firefightingStepGroupConfig,
        @JsonProperty("hydrant-outlet-label-group")  HydrOutletLabelGroupConfig hydrOutletLabelGroupConfig,
        @JsonProperty("door-button-group")           DoorButtonGroupConfig doorButtonGroup
    ) {
        this(ventilationGroupConfig, flammableGroupConfig, machineryDamageGroupConfig, fireSensorGroupConfig,
            explosionGroupConfig, frontlineBalanceGroupConfig, firefightingStepGroupConfig,
            hydrOutletLabelGroupConfig, doorButtonGroup, List.of(), List.of()
        );
    }

    public static DeckMapGroupsConfig createDefault(DeckMapControlsConfig controlConfig) {
        DeckMapGroupsConfig base = YamlConfigLoader.load("config/groups.yaml", DeckMapGroupsConfig.class);
        List<HydrantPlacement> hydrantPlacements = controlConfig.hydrantPlacement();
        return new DeckMapGroupsConfig(base.ventilationGroupConfig(), base.flammableGroupConfig(),
            base.machineryDamageGroupConfig(), base.fireSensorGroupConfig(), base.explosionGroupConfig(),
            base.frontlineBalanceGroupConfig(), base.firefightingStepGroupConfig(), base.hydrOutletLabelGroupConfig(),
            base.doorButtonGroup(), hydrantPlacements, hydrantPlacements
        );
    }
}
