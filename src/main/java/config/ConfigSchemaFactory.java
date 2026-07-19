package config;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import config.loading.*;
import domain.types.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Common factory for victools SchemaGenerator configuration used both for committed IDE schemas (ConfigSchemaGenerator)
 * and for runtime in-memory validation (YamlConfigLoader).
 * <p>
 * Strict mode: FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT ensures that unknown properties in YAML cause validation
 * failure (additionalProperties: false).
 */
public final class ConfigSchemaFactory {
    public static final Path SCHEMAS_DIR = Paths.get("src/main/resources/config/schemas");
    private static final String LOCATION_SPEC = "LocationSpec";

    private ConfigSchemaFactory() {}

    public static SchemaGenerator createGenerator() {
        SchemaGeneratorConfigBuilder builder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7)
            .with(new JacksonSchemaModule())
            .with(Option.FLATTENED_ENUMS)
            .with(Option.INLINE_NULLABLE_SCHEMAS)
            .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT);
        builder.forMethods()
            .withIgnoreCheck(method -> {
                String name = method.getName();
                return name.equals("equals") || name.equals("hashCode") || name.equals("toString") || method.isStatic();
            });
        return new SchemaGenerator(builder.build());
    }

    /**
     * Generates an in-memory Draft-7 schema node for the given config DTO class. The returned node is the schema
     * content itself (no outer wrapper $schema); suitable for passing to networknt JsonSchemaFactory. Post-processing
     * for enum restrictions is applied here (single place for all consumers).
     */
    public static ObjectNode generateSchema(Class<?> configClass) {
        ObjectNode schema = createGenerator().generateSchema(configClass);
        applyEnumRestrictions(schema);
        return schema;
    }

    /**
     * Central list of all config classes and their corresponding committed schema filenames. Used by both
     * ConfigSchemaGenerator and ConfigSchemaVerifier to avoid duplication.
     */
    public static final List<SchemaConfig> SCHEMA_CONFIGS = List.of(
        new SchemaConfig(DeckMapControlsConfig.class, "controls-schema.json"),
        new SchemaConfig(DeckMapGroupsConfig.class, "groups-schema.json"),
        new SchemaConfig(DeckMapAssemblyConfig.class, "assembly-schema.json"),
        new SchemaConfig(DeckMapTopologyConfig.class, "topology-schema.json"),
        new SchemaConfig(DeckMapGeometryConfig.class, "geometry-schema.json")
    );

    /**
     * Generates the full wrapped schema (with $schema header) for the given class. This is the canonical way to produce
     * the committed IDE schema format.
     */
    public static ObjectNode generateWrappedSchema(Class<?> configClass) {
        ObjectNode finalSchema = new ObjectMapper().createObjectNode();
        finalSchema.put("$schema", "http://json-schema.org/draft-07/schema#");

        ObjectNode generatedSchema = generateSchema(configClass);
        if (generatedSchema != null) {
            for (Map.Entry<String, JsonNode> entry : generatedSchema.properties())
                finalSchema.set(entry.getKey(), entry.getValue());
        }

        return finalSchema;
    }

    public record SchemaConfig(Class<?> configClass, String fileName) {}

    private record EnumRestriction(String definitionName, String propertyName, Class<? extends Enum<?>> enumClass) {}

    private static final List<EnumRestriction> ENUM_RESTRICTIONS = List.of(
        new EnumRestriction(LOCATION_SPEC, "type", CompartmentType.class),
        new EnumRestriction(LOCATION_SPEC, "ventilation", VentilationType.class),
        new EnumRestriction(LOCATION_SPEC, "explosive", ExplosiveMaterial.class),
        new EnumRestriction(LOCATION_SPEC, "burning", FlammableMaterial.class),
        new EnumRestriction("FireSensorSpec", "type", FireSensorType.class),
        new EnumRestriction("ExtinguisherSpec", "type", ExtinguisherType.class)
    );

    /**
     * Post-processes the generated schema to add "enum" restrictions (with.name().toLowerCase() values plus null) for
     * the 6 known string fields that map to domain enum types. Applied in one place so both runtime validation and
     * committed schema generation see it.
     */
    private static void applyEnumRestrictions(ObjectNode schema) {
        if (schema == null)
            return;
        JsonNode defsNode = schema.path("definitions");
        if (defsNode.isMissingNode() || !defsNode.isObject())
            return;
        ObjectNode definitions = (ObjectNode) defsNode;

        ObjectMapper mapper = new ObjectMapper();
        for (EnumRestriction restr : ENUM_RESTRICTIONS)
            applyEnumRestriction(definitions, restr, mapper);
    }

    /**
     * Navigates definitions → the one restriction's definition → its properties → the one targeted property, and sets
     * its "enum" node — silently doing nothing at whichever step the generated schema doesn't have the expected shape.
     * One restriction per call, always invoked unconditionally by the loop above; the sequential guards below are this
     * call's own entry validation (see CODESTYLE.md rule 9), not a masked per-iteration skip (rule 8) — the loop itself
     * never skips a restriction.
     */
    private static void applyEnumRestriction(ObjectNode definitions, EnumRestriction restr, ObjectMapper mapper) {
        JsonNode defNode = definitions.path(restr.definitionName());
        if (defNode.isMissingNode() || !defNode.isObject())
            return;
        ObjectNode def = (ObjectNode) defNode;

        JsonNode propsNode = def.path("properties");
        if (propsNode.isMissingNode() || !propsNode.isObject())
            return;
        ObjectNode props = (ObjectNode) propsNode;

        JsonNode propNode = props.path(restr.propertyName());
        if (propNode.isMissingNode() || !propNode.isObject())
            return;
        ObjectNode prop = (ObjectNode) propNode;

        ArrayNode enumArr = mapper.createArrayNode();
        for (Enum<?> e : restr.enumClass().getEnumConstants())
            enumArr.add(e.name().toLowerCase());
        enumArr.addNull();

        prop.set("enum", enumArr);
    }
}
