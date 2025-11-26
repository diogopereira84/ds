package fox.fmc.partner.delivery.test.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Robust extractor for JSON objects embedded in AWS Lambda Invoke's "logTail".
 * Why: AWS returns only the last ~4KB; lines can be truncated and multiple JSONs may be concatenated.
 */
public final class CloudWatchLogTailHelper {

    private CloudWatchLogTailHelper() {}

    // ---------- Public Convenience Methods ----------

    /** Finds the "SQS notification response for {assetId}" entry. */
    public static <T> T readSqsNotification(String logTailText,
                                            String assetId,
                                            Class<T> type,
                                            ObjectMapper mapper) {
        String expected = "SQS notification response for " + assetId;
        return readLogEntry(logTailText, expected, type, mapper);
    }

    /** Finds the "Batch response" entry. */
    public static <T> T readBatchResponse(String logTailText,
                                          Class<T> type,
                                          ObjectMapper mapper) {
        return readLogEntry(logTailText, "Batch response", type, mapper);
    }

    /** Finds the "index.handler response" entry. */
    public static <T> T readIndexHandlerResponse(String logTailText,
                                                 Class<T> type,
                                                 ObjectMapper mapper) {
        return readLogEntry(logTailText, "index.handler response", type, mapper);
    }

    // ---------- Generic API ----------

    /**
     * Generic search method. Tries to find a log entry where the "message" field
     * either equals the identifier exactly or contains it as a substring.
     *
     * @return The deserialized object, or null if not found (allows Spock assertions to handle failure).
     */
    public static <T> T readLogEntry(String logTailText,
                                     String messageIdentifier,
                                     Class<T> type,
                                     ObjectMapper objectMapper) {
        List<JsonNode> entries = parseLogTail(logTailText, objectMapper);

        // 1. Try Exact Match
        JsonNode node = find(entries, n -> messageIdentifier.equals(n.path("message").asText(null)));

        // 2. Try Contains Match (if exact failed)
        if (node == null) {
            node = find(entries, n -> {
                String m = n.path("message").asText(null);
                return m != null && m.contains(messageIdentifier);
            });
        }

        if (node == null) {
            return null; // Return null so Spock can assert existence with a custom message
        }

        return treeTo(type, node, objectMapper);
    }

    /**
     * Extract all well-formed JSON objects from the raw logTail text.
     * Handles:
     * 1. Pure concatenated JSONs (e.g. "}{")
     * 2. Line-delimited JSON
     * 3. JSON embedded in AWS log lines (e.g. "TIMESTAMP requestId INFO {json}")
     */
    public static List<JsonNode> parseLogTail(String logTailText, ObjectMapper objectMapper) {
        if (logTailText == null || logTailText.isBlank()) return List.of();

        List<JsonNode> out = new ArrayList<>();

        // Split by newline to handle standard CloudWatch formatting
        String[] lines = logTailText.split("\\r?\\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Heuristic: Find the first '{'
            int jsonStart = trimmed.indexOf('{');
            if (jsonStart == -1) continue; // No JSON in this line

            // Extract everything from the first '{' to the end
            String potentialJson = trimmed.substring(jsonStart);

            try {
                JsonNode node = objectMapper.readTree(potentialJson);
                out.add(node);
            } catch (IOException ignored) {
                // If the line has a '{' but isn't valid JSON (e.g. "INFO: Starting..."), just skip it.
                // This is expected behavior for non-JSON logs.
            }
        }

        return out;
    }


    /** Let callers pass a custom predicate when 'message' isn't reliable. */
    public static <T> T deserializeFirstMatching(String logTailText,
                                                 Class<T> type,
                                                 ObjectMapper objectMapper,
                                                 Predicate<JsonNode> predicate) {
        List<JsonNode> entries = parseLogTail(logTailText, objectMapper);
        JsonNode node = find(entries, predicate);

        if (node == null) {
            return null;
        }
        return treeTo(type, node, objectMapper);
    }

    // ---------- Internals ----------

    private static JsonNode find(List<JsonNode> entries, Predicate<JsonNode> p) {
        for (JsonNode n : entries) {
            if (p.test(n)) return n;
        }
        return null;
    }

    private static <T> T treeTo(Class<T> type, JsonNode node, ObjectMapper mapper) {
        try {
            return mapper.treeToValue(node, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize logTail entry to " + type.getSimpleName(), e);
        }
    }
}