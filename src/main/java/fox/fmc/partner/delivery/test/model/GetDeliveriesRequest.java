package fox.fmc.partner.delivery.test.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class GetDeliveriesRequest {

    @JsonIgnore
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SerializedName("from")
    private int from;

    @SerializedName("size")
    private int size;

    @SerializedName("query")
    private Query query;

    @SerializedName("sort")
    private List<Map<String, String>> sort;

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Cannot serialize user request body {}", this);
            throw new RuntimeException(e);
        }
    }

}
