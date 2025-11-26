package fox.fmc.partner.delivery.test.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fox.fmc.partner.delivery.test.enums.Action;
import fox.fmc.partner.delivery.test.enums.PartnerType;
import fox.fmc.partner.delivery.test.enums.RequestType;
import fox.fmc.partner.delivery.test.model.request.LambdaRequest;
import fox.fmc.partner.delivery.test.model.request.SqsEvent;
import fox.fmc.partner.delivery.test.model.request.SqsRecord;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Static utility to build Lambda SQS envelope events.
 * Designed to work with the @Autowired ObjectMapper from test specs.
 */
public final class LambdaEventHelper {

    private LambdaEventHelper() {
        // Utility class
    }

    /**
     * Creates a LambdaRequest POJO.
     * Allows nulls to support negative testing scenarios.
     */
    public static LambdaRequest buildLambdaRequest(
            final RequestType requestType,
            final String assetId,
            final Action action,
            final PartnerType partner
    ) {
        return LambdaRequest.builder()
                .type(requestType)
                .assetId(assetId)
                .action(action)
                .partner(partner)
                .build();
    }

    /**
     * Serializes a LambdaRequest and wraps it in an SQS Envelope.
     *
     * @param objectMapper The autowired Spring bean from the Spec.
     * @param lambdaRequest The payload object (can be null/partial for negative tests).
     * @return The full SQS Event object ready for Lambda invocation.
     */
    public static SqsEvent buildSqsEvent(
            final ObjectMapper objectMapper,
            final LambdaRequest lambdaRequest
    ) {
        Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        final String bodyJson = toJson(objectMapper, lambdaRequest);
        return wrapBody(bodyJson);
    }

    /**
     * Convenience overload: Builds Request POJO + SQS Envelope in one step.
     */
    public static SqsEvent buildSqsEvent(
            final ObjectMapper objectMapper,
            final RequestType requestType,
            final String assetId,
            final Action action,
            final PartnerType partner
    ) {
        return buildSqsEvent(objectMapper, buildLambdaRequest(requestType, assetId, action, partner));
    }

    /**
     * Wraps a raw JSON body string into a single-record SQS event.
     */
    public static SqsEvent wrapBody(final String bodyJson) {
        return wrapBody(bodyJson, Collections.emptyMap());
    }

    /**
     * Wraps a raw JSON body string into a single-record SQS event with custom attributes.
     */
    public static SqsEvent wrapBody(final String bodyJson, final Map<String, String> attributes) {
        final Map<String, String> safeAttrs = (attributes == null) ? Collections.emptyMap() : attributes;

        // SQS body is a String representation of the JSON.
        // If bodyJson is null (e.g. raw null payload), we pass string "null".
        final SqsRecord singleSqsEvent = SqsRecord.builder()
                .body(bodyJson == null ? "null" : bodyJson)
                .attributes(safeAttrs)
                .build();

        return SqsEvent.builder()
                .records(List.of(singleSqsEvent))
                .build();
    }

    // ---------- Internals ----------

    private static String toJson(final ObjectMapper mapper, final Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize object to JSON using provided ObjectMapper", ex);
        }
    }
}