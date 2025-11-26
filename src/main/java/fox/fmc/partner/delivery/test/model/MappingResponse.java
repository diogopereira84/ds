package fox.fmc.partner.delivery.test.model;

import fox.fmc.partner.delivery.test.model.pageable.PageData;
import lombok.Data;

@Data
public class MappingResponse {
    MappingRequest[] content;
    PageData pageable;
    int total;
}
