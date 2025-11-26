package fox.fmc.partner.delivery.test.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fox.fmc.partner.delivery.test.model.request.LambdaRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LambdaResponse {

    @JsonProperty("Records")
    private List<Record> records;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Record {

        /**
         * Raw body string from SQS/Lambda:
         * {"type":"manual","assetId":"A-827476","action":"episode_publish","partner":"SPARK_FTS_PROGRAM"}
         */
        private String body;

        /**
         * Message attributes (may be empty {}).
         */
        private Map<String, String> attributes;

        /**
         * Convenience method to deserialize the body into a LambdaRequest.
         */
        @JsonIgnore
        public LambdaRequest toLambdaRequest(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
            try {
                return objectMapper.readValue(body, LambdaRequest.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to deserialize Lambda body into LambdaRequest", e);
            }
        }
    }
}
