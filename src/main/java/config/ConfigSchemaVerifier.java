package config;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies that the committed JSON schemas under src / main / resources / config / schemas exactly match what
 * ConfigSchemaFactory + wrapper would produce today.
 * <p>
 * Run via./gradlew verifyConfigSchemas (wired into'check' task).
 * <p>
 * Uses JsonNode.equals() for structural comparison (insensitive to formatting / CRLF).
 */
public class ConfigSchemaVerifier {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<String> mismatches = new ArrayList<>();

        for (ConfigSchemaFactory.SchemaConfig entry : ConfigSchemaFactory.SCHEMA_CONFIGS)
            verify(entry.configClass(), entry.fileName(), mismatches, mapper);

        if (!mismatches.isEmpty()) {
            System.err.println("Committed schema drift detected for: " + mismatches);
            System.err.println("Run ./gradlew generateConfigSchemas to update the IDE schemas.");
            System.exit(1);
        }
    }

    private static void verify(Class<?> configClass, String fileName, List<String> mismatches, ObjectMapper mapper) throws IOException {
        Path committedPath = ConfigSchemaFactory.SCHEMAS_DIR.resolve(fileName);
        JsonNode committed = mapper.readTree(Files.readString(committedPath));

        ObjectNode expected = ConfigSchemaFactory.generateWrappedSchema(configClass);
        if (!expected.equals(committed))
            mismatches.add(fileName);
    }
}
