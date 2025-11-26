package fox.fmc.partner.delivery.test.model.request;

import com.fasterxml.jackson.annotation.*;
import fox.fmc.partner.delivery.test.enums.Action;
import fox.fmc.partner.delivery.test.enums.PartnerType;
import fox.fmc.partner.delivery.test.enums.RequestType;
import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LambdaRequest {
    private RequestType type;        // manual | system | retry
    private String assetId;          // e.g. "A-827476"
    private Action action;           // episode_publish | episode_unpublish | program_publish | program_unpublish

    // keep partner as String unless you truly have a closed set:
    private PartnerType partner;          // e.g. "FREEWHEEL_PROGRAMSFTS"

    private String corrId;           // optional UUID
    private Map<String, Object> metadata; // optional
}
