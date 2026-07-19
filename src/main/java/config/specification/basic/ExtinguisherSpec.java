package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw topology description of one portable fire extinguisher: its title and extinguishing agent. {@code type} is the
 * YAML name vocabulary — {@code domain.types.ExtinguisherType}'s constant name lower-cased (
 * {@code carbon_dioxide}/{@code air_foam}), resolved by {@code DeckMapTopologyConfig} via
 * {@code ExtinguisherType#fromName(String)}. The two-letter codes {@code feis.clp} uses ({@code co}/{@code af}) are the
 * separate CLIPS wire vocabulary on {@code ExtinguisherType#getClipsValue()}, not what authors write in
 * {@code topology.yaml}. Kept a plain string here (not the domain enum itself) so this record has no dependency beyond
 * Jackson annotations, matching every other raw spec in this package.
 */
public record ExtinguisherSpec(
    @JsonProperty("title") String title,
    @JsonProperty("type")  String type
) {}
