package fox.fmc.partner.delivery.test.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.LogType;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class AwsLambdaUtils {
    private static final Logger log = LoggerFactory.getLogger(AwsLambdaUtils.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // lazy init; may stay null if we never actually touch AWS
    private volatile AWSLambda client;

    @Autowired
    private AwsUtils awsUtils;

    @Value("${aws.profile.name}")
    private String profileName;

    @Value("${aws.region}")
    private String awsRegion;

    /**
     * Invocation modes for Lambda.
     */
    public enum InvocationMode {
        REQUEST_RESPONSE,   // synchronous
        EVENT,              // async (fire-and-forget)
        DRY_RUN             // permission check only
    }

    /**
     * Result object with useful fields for assertions and logging.
     */
    public static class LambdaInvocationResult {
        public final int statusCode;
        public final String functionError;   // null when OK
        public final String logTail;         // may be null if not requested
        public final String executedVersion; // e.g., "$LATEST" or alias-resolved version
        public final String requestId;       // AWS SDK request id (not the Lambda context id)
        public final String payloadText;     // raw payload as UTF-8 string

        public LambdaInvocationResult(int statusCode,
                                      String functionError,
                                      String logTail,
                                      String executedVersion,
                                      String requestId,
                                      String payloadText) {
            this.statusCode = statusCode;
            this.functionError = functionError;
            this.logTail = logTail;
            this.executedVersion = executedVersion;
            this.requestId = requestId;
            this.payloadText = payloadText;
        }

        public boolean isOk() {
            return functionError == null && statusCode >= 200 && statusCode < 300;
        }

        public Map<String, Object> payloadAsMap() {
            try {
                if (payloadText == null || payloadText.isBlank()) return Map.of();
                return MAPPER.readValue(payloadText, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Lambda payload to Map", e);
            }
        }

        @Override
        public String toString() {
            return "LambdaInvocationResult{" +
                    "statusCode=" + statusCode +
                    ", functionError=" + functionError +
                    ", executedVersion='" + executedVersion + '\'' +
                    ", requestId='" + requestId + '\'' +
                    ", hasLogTail=" + (logTail != null) +
                    '}';
        }
    }

    /**
     * Lazily builds the Lambda client the first time it's needed.
     */
    private AWSLambda buildClient() {
        AWSLambda c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    AWSCredentialsProvider creds;
                    if (profileName != null && !profileName.isBlank()) {
                        creds = new ProfileCredentialsProvider(profileName);
                        log.info("Using AWS profile '{}'", profileName);
                    } else {
                        creds = DefaultAWSCredentialsProviderChain.getInstance();
                        log.info("Using DefaultAWSCredentialsProviderChain");
                    }

                    client = AWSLambdaClientBuilder.standard()
                            .withRegion(awsRegion)
                            .withCredentials(creds)
                            .build();
                    log.info("Built Lambda client (region={}, profile='{}')",
                            awsRegion, (profileName == null ? "" : profileName));
                }
                c = client;
            }
        }
        return c;
    }

    // ------------------------------
    // Public invoker API (overloads)
    // ------------------------------

    /**
     * Invoke with string payload, synchronous, no qualifier, with log tail.
     */
    public LambdaInvocationResult invoke(String functionName, String jsonPayload) {
        return invoke(functionName, jsonPayload, InvocationMode.REQUEST_RESPONSE, null, true);
    }

    /**
     * Invoke with object payload (auto-serialized to JSON), sync, no qualifier, with log tail.
     */
    public LambdaInvocationResult invoke(String functionName, Object payloadObj) {
        return invoke(functionName, toJson(payloadObj), InvocationMode.REQUEST_RESPONSE, null, true);
    }

    /**
     * Fully-configurable invoker.
     *
     * @param functionName Lambda function name (name, ARN, or partial ARN)
     * @param jsonPayload  JSON string payload (may be null/empty for DRY_RUN)
     * @param mode         invocation mode (sync, async, or dry run)
     * @param qualifier    optional version/alias (e.g., "$LATEST", "prod")
     * @param includeLogTail whether to request and decode CloudWatch log tail
     */
    public LambdaInvocationResult invoke(String functionName,
                                         String jsonPayload,
                                         InvocationMode mode,
                                         String qualifier,
                                         boolean includeLogTail) {
        try {
            var req = new InvokeRequest()
                    .withFunctionName(functionName)
                    .withPayload(jsonPayload == null ? "" : jsonPayload);

            switch (mode) {
                case REQUEST_RESPONSE -> req.withInvocationType("RequestResponse");
                case EVENT -> req.withInvocationType("Event");
                case DRY_RUN -> req.withInvocationType("DryRun");
            }

            if (qualifier != null && !qualifier.isBlank()) {
                req.withQualifier(qualifier);
            }

            if (includeLogTail) {
                req.withLogType(LogType.Tail);
            }

            log.info("Invoking Lambda: function='{}', mode={}, qualifier='{}', logTail={}",
                    functionName, mode, (qualifier == null ? "" : qualifier), includeLogTail);

            InvokeResult res = buildClient().invoke(req);

            String logTail = null;
            if (includeLogTail && res.getLogResult() != null) {
                byte[] decoded = Base64.decodeBase64(res.getLogResult());
                logTail = new String(decoded, StandardCharsets.UTF_8);
            }

            String payloadText = (res.getPayload() == null)
                    ? ""
                    : new String(res.getPayload().array(), StandardCharsets.UTF_8);

            String requestId = (res.getSdkResponseMetadata() != null)
                    ? res.getSdkResponseMetadata().getRequestId()
                    : null;

            var out = new LambdaInvocationResult(
                    res.getStatusCode(),
                    res.getFunctionError(),
                    logTail,
                    res.getExecutedVersion(),
                    requestId,
                    payloadText
            );

            if (out.functionError != null) {
                log.warn("Lambda function error: {}", out.functionError);
            }
            log.debug("Lambda payload (truncated 1k): {}", truncate(payloadText, 1024));
            if (includeLogTail && logTail != null) {
                log.debug("Lambda log tail (truncated 1k): {}", truncate(logTail, 1024));
            }

            return out;
        } catch (Exception e) {
            throw new RuntimeException("Lambda invocation failed: " + functionName, e);
        }
    }

    // Convenience for async (fire-and-forget)
    public LambdaInvocationResult invokeAsync(String functionName, Object payloadObj) {
        return invoke(functionName, toJson(payloadObj), InvocationMode.EVENT, null, false);
    }

    // Convenience for dry-run (permission check)
    public LambdaInvocationResult dryRun(String functionName) {
        return invoke(functionName, "", InvocationMode.DRY_RUN, null, false);
    }

    // Try to parse successful JSON payloads to Map directly (helper)
    public Map<String, Object> tryParsePayloadToMap(LambdaInvocationResult result) {
        return result.payloadAsMap();
    }

    // ------------------------------
    // Internals
    // ------------------------------

    private static String toJson(Object obj) {
        try {
            return (obj == null) ? "" : MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload to JSON", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}