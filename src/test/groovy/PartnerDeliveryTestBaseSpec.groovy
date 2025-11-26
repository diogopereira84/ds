import com.fasterxml.jackson.databind.ObjectMapper

import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

class PartnerDeliveryTestBaseSpec extends Specification{

    @Autowired
    protected ObjectMapper objectMapper
}
