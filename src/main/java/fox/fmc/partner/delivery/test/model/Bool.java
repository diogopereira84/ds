package fox.fmc.partner.delivery.test.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Bool {

    @SerializedName("must")
    private List<Map<String, Object>> must;

}
