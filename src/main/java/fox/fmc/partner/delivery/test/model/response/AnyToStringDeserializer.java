package fox.fmc.partner.delivery.test.model.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/** Coerces any JSON value to String; prefers object.status when present. */
public class AnyToStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode n = p.getCodec().readTree(p);
        if (n == null || n.isNull()) return null;
        if (n.isTextual()) return n.asText();
        if (n.isObject() && n.has("status") && n.get("status").isTextual()) return n.get("status").asText();
        return n.toString();
    }
}
