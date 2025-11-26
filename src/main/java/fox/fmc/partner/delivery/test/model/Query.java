package fox.fmc.partner.delivery.test.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Query {

    @SerializedName("bool")
    private Bool bool;

}
