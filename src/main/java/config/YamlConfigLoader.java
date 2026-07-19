package config;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import config.validation.ConfigValidationException;
import util.Charsets;
import util.ResourceUtil;

import java.io.InputStream;
import java.util.List;

import com.networknt.schema.ExecutionContext;

/**
 * Single validating YAML loader for all DeckMap*Config DTOs.
 * <ul>
 * <li>Loads via ResourceUtil (classpath)</li> <li>Strips $schema / schema root keys (IDE metadata, rejected by strict
 * schema)</li> <li>Validates against in-memory victools schema (same strict config as committed files)</li> <li>Uses
 * networknt with Draft-7, typeLoose=false, collects ALL errors</li> <li>On error: throws ConfigValidationException with
 * aggregated lines</li>
 * </ul>
 */
public final class YamlConfigLoader {
    private static final ObjectMapper YAML_MAPPER = YAMLMapper.builder(new YAMLFactory())
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .build();

    private YamlConfigLoader() {}

    /**
     * Load and validate a YAML resource against the strict schema derived from the target type.
     *
     * @param resourceName e.g."config / topology.yaml" @param type target config class
     */
    public static <T> T load(String resourceName, Class<T> type) {
        try (InputStream in = ResourceUtil.openResourceStream(resourceName)) {
            return loadInternal(in, resourceName, type);
        } catch (ConfigValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + resourceName, e);
        }
    }

    /**
     * Public for tests (in any package): feed YAML content directly (string literal or temp file content) without
     * requiring a classpath resource under src / main / resources. Not intended for production use.
     */
    public static <T> T loadFromYamlContent(String yamlContent, String resourceNameForError, Class<T> type) {
        try (InputStream in = new java.io.ByteArrayInputStream(yamlContent.getBytes(Charsets.UTF_8))) {
            return loadInternal(in, resourceNameForError, type);
        } catch (ConfigValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + resourceNameForError + " from literal content", e);
        }
    }

    private static <T> T loadInternal(InputStream in, String resourceName, Class<T> type) {
        JsonNode root = YAML_MAPPER.readTree(in);

        // Remove IDE meta keys so strict additionalProperties:false does not reject them.
        if (root != null && root.isObject()) {
            ObjectNode obj = (ObjectNode) root;
            obj.remove("$schema");
            obj.remove("schema");
        }

        // In-memory strict schema from same factory as generator
        ObjectNode schemaNode = ConfigSchemaFactory.generateSchema(type);

        // networknt 3.x API: SchemaRegistry + SpecificationVersion + Schema
        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);
        Schema jsonSchema = registry.getSchema(schemaNode);

        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        jsonSchema.validate(executionContext, root);

        List<Error> errors = executionContext.getErrors();

        if (!errors.isEmpty())
            throw ConfigValidationException.from(resourceName, errors);

        return YAML_MAPPER.treeToValue(root, type);
    }
}
