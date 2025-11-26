package fox.fmc.partner.delivery.test.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Action {
    INVALID_ACTION("INVALID_ACTION"),
    EPISODE_PUBLISH("episode_publish"),
    EPISODE_UNPUBLISH("episode_unpublish"),
    PROGRAM_PUBLISH("program_publish"),
    PROGRAM_UNPUBLISH("program_unpublish"),
    @JsonEnumDefaultValue UNKNOWN("unknown");

    @JsonValue
    private final String json;
    @JsonCreator
    public static Action from(String v) {
        for (var a : values()) if (a.json.equalsIgnoreCase(v)) return a;
        return UNKNOWN;
    }
}
