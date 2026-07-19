package config.validation;

import com.networknt.schema.Error;

import java.util.List;

/**
 * Runtime exception aggregating all configuration validation failures. getMessage() returns a multi-line string with
 * one violation per line.
 */
public class ConfigValidationException extends RuntimeException {

    private final List<String> violations;

    public ConfigValidationException(List<String> violations) {
        super(formatMessage(violations));
        this.violations = List.copyOf(violations);
    }

    private static String formatMessage(List<String> violations) {
        if (violations == null || violations.isEmpty())
            return "Configuration validation failed with no details";
        return String.join("\n", violations);
    }

    public List<String> getViolations() {
        return violations;
    }

    /**
     * Factory for schema validation errors from networknt. Formats as "<resource>: <instanceLocation> — <message>"
     */
    public static ConfigValidationException from(String resourceName, List<Error> errors) {
        List<String> lines = errors.stream()
            .map(e -> {
                String loc = (e.getInstanceLocation() != null) ? e.getInstanceLocation().toString() : "";
                String msg = e.getMessage();
                return resourceName + ": " + loc + " — " + msg;
            })
            .toList();
        return new ConfigValidationException(lines);
    }
}
