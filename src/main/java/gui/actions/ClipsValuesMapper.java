package gui.actions;

import clips.values.internal.ExplosionClipsAction;
import clips.values.internal.ExtinguisherClipsStatus;
import clips.values.internal.FlammablePreventionClipsAction;
import domain.types.ExplosiveType;
import domain.types.PreventionType;
import gui.map.values.ExtinguisherUsage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ClipsValuesMapper {
    private ClipsValuesMapper() {}

    public static FlammablePreventionClipsAction toClips(PreventionType sourceType) {
        return switch (sourceType) {
            case OIL -> FlammablePreventionClipsAction.PUMP_OUT;
            case CLOTHES -> FlammablePreventionClipsAction.CARRY_OUT;
            case DONE, MECHANICAL -> FlammablePreventionClipsAction.DONE;
        };
    }

    public static ExplosionClipsAction toClips(ExplosiveType sourceType) {
        return switch (sourceType) {
            case AIR -> ExplosionClipsAction.CARRY_OUT;
            case OIL -> ExplosionClipsAction.PUMP_OUT;
            case REAGENT -> ExplosionClipsAction.TO_FIGHT;
            case DONE -> ExplosionClipsAction.DONE;
        };
    }

    public static ExtinguisherClipsStatus toClips(ExtinguisherUsage usage) {
        return switch (usage) {
            case USED -> ExtinguisherClipsStatus.USED;
            case NOT_USED -> ExtinguisherClipsStatus.NOT_USED;
        };
    }

    public static <K, I, O> Map<K, O> remapToClips(Map<K, I> sourceMap, Function<I, O> typeMapper) {
        Map<K, O> clipsMap = new HashMap<>();
        for (Map.Entry<K, I> e : sourceMap.entrySet())
            clipsMap.put(e.getKey(), typeMapper.apply(e.getValue()));
        return clipsMap;
    }
}
