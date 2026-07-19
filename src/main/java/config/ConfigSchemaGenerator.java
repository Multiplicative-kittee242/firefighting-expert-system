package config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;

public class ConfigSchemaGenerator {

    public static void main(String[] args) throws IOException {
        for (ConfigSchemaFactory.SchemaConfig entry : ConfigSchemaFactory.SCHEMA_CONFIGS)
            generateSchema(entry.configClass(), entry.fileName());
    }

    private static void generateSchema(Class<?> configClass, String fileName) throws IOException {
        ObjectNode finalSchema = ConfigSchemaFactory.generateWrappedSchema(configClass);

        String schemaJson = new ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(finalSchema);

        Files.createDirectories(ConfigSchemaFactory.SCHEMAS_DIR);
        Files.writeString(ConfigSchemaFactory.SCHEMAS_DIR.resolve(fileName), schemaJson);

        System.out.println("Generated schema: " + fileName);
    }
}
