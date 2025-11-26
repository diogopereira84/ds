package fox.fmc.partner.delivery.test.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RequestType {
    INVALID("invalid"),
    MANUAL("manual"),
    SYSTEM("system"),
    RETRY("retry"),
    @JsonEnumDefaultValue UNKNOWN("unknown");

    @JsonValue
    private final String json;
    @JsonCreator
    public static RequestType from(String v) {
        for (var t : values()) if (t.json.equalsIgnoreCase(v)) return t;
        return UNKNOWN;
    }
}
