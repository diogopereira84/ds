package fox.fmc.partner.delivery.test.model;

import com.google.gson.annotations.Expose;
import lombok.Data;

@Data
public class MappingRequest {
    @Expose(serialize = false, deserialize = true)
    int id;
    @Expose
    Long groupId;
    @Expose
    String groupName;
    @Expose
    String divisionName;
}
