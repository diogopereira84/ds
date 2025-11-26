import com.google.gson.Gson
import fox.mc2.testrail.enums.TestRailsStatus
import fox.fmc.partner.delivery.test.FmcPartnerDeliveryTestApplication
import fox.fmc.partner.delivery.test.enums.HttpResponseCode
import fox.fmc.partner.delivery.test.model.Bool
import fox.fmc.partner.delivery.test.model.GetDeliveriesRequest
import fox.fmc.partner.delivery.test.model.GetDeliveriesResponse
import fox.fmc.partner.delivery.test.model.GetDeliveryResponse
import fox.fmc.partner.delivery.test.model.Query
import fox.fmc.partner.delivery.test.service.MediaCloudService
import fox.fmc.partner.delivery.test.service.PartnerDeliverySetupService
import groovy.util.logging.Slf4j
import io.restassured.response.Response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared
import spock.lang.Unroll

@Slf4j
@Unroll
@SpringBootTest(classes = FmcPartnerDeliveryTestApplication.class)
class PartnerDashboardEndpointsValidationSpec extends TestRailsSpec {

    @Autowired
    private MediaCloudService mediaCloudService

    @Shared
    private static String userAdmin = PartnerDeliverySetupService.getAdminUser()

    @Override
    String getTitle() {
        return "Partner Delivery Endpoints Validation"
    }

    @Override
    String getReferences() {
        return "MAM5-831, MAM5-832, MAM5-544, FMCTEST-1197"
    }

    @Override
    ArrayList getCaseIds() {
        return Arrays.asList(31154594, 31154595, 31154596)
    }

    def "Validate get deliveries endpoint"() {
        given: "The body for the get deliveries request"
        setTestId(getTestIDfromTestCase(testCase))
        setComment("#Validate get deliveries endpoint")
        when: "Request is for get all deliveries for partner"
        String auth = mediaCloudService.getMamIdToken(userAdmin)
        GetDeliveriesRequest requestBody = getRequestBody()
        addJsonFormatComment("#Request payload", requestBody)
        Response response = mediaCloudService.getDeliveries(auth, requestBody.toString())
        addCommentAndLog("Response code: " + response.statusCode())
        GetDeliveriesResponse deliveriesResponse = new Gson().fromJson(response.asString(), GetDeliveriesResponse.class)

        then: "The responses will be validated"
        assert response.statusCode() == HttpResponseCode.CODE_200.get()
        assert deliveriesResponse.getHits().getTotal().getValue() > 0
        addCommentAndLog("Total deliveries: " + deliveriesResponse.getHits().getTotal().getValue())
        addBoldComment("Get Deliveries Response: ")
        addComment(response.asPrettyString())

        setStatus(TestRailsStatus.PASSED)
        where:
        expectedStatus                  | testCase
        HttpResponseCode.CODE_200.get() | 31154594
    }

    def "Validate get delivery by batch id endpoint"() {
        given: "The id for the delivery"
        setTestId(getTestIDfromTestCase(testCase))
        setComment("#Validate get delivery by id endpoint")
        when: "Request is for get delivery by id"
        String auth = mediaCloudService.getMamIdToken(userAdmin)
        addComment("Batch ID: " + batchId)
        Response response = mediaCloudService.getDeliveryByBatchId(auth, batchId)
        addCommentAndLog("Response code: " + response.statusCode())
        //log response details
        addBoldComment("Get Delivery By Id Response:")
        addComment(response.asPrettyString())
        GetDeliveryResponse deliveryResponse = new Gson().fromJson(response.asString(), GetDeliveryResponse.class)
        addCommentAndLog("Delivery ID: " + deliveryResponse.getBatchId())
        assert deliveryResponse.getBatchId() == batchId

        then: "The responses will be validated"
        assert response.statusCode() == HttpResponseCode.CODE_200.get()
        setStatus(TestRailsStatus.PASSED)
        where:
        batchId                                | expectedStatus                  | testCase
        "8945fasn-dc11-PR70-a9cb-564as8euhf84" | HttpResponseCode.CODE_200.get() | 31154595
    }

    def "Validate get partner metadata endpoint"() {
        given: "The jobId"
        setTestId(getTestIDfromTestCase(testCase))
        setComment("#Validate get partner metadata endpoint")
        when: "Request is for get partner metadata"
        String auth = mediaCloudService.getMamIdToken(userAdmin)
        addComment("Job ID: " + jobId)
        Response response = mediaCloudService.getPartnerMetadata(auth, jobId)
        addCommentAndLog("Response code: " + response.statusCode())
        //log response details
        addBoldComment("Get Partner Metadata Response")
        addComment(response.asPrettyString())

        then: "The responses will be validated"
        assert response.statusCode() == HttpResponseCode.CODE_200.get()
        setStatus(TestRailsStatus.PASSED)
        where:
        jobId     | expectedStatus                  | testCase
        "95436"   | HttpResponseCode.CODE_200.get() | 31154596
    }

    private static GetDeliveriesRequest getRequestBody() {
        GetDeliveriesRequest requestBody = new GetDeliveriesRequest()
        requestBody.setFrom(0)
        requestBody.setSize(120)
        Bool bool = new Bool()
        bool.setMust(List.of(
                Map.of("match", Map.of("doc_type", "batch")),
                Map.of("terms", Map.of("partner_name.keyword", List.of("FTS_CTV")))
        ))
        Query query = new Query()
        query.setBool(bool)
        requestBody.setQuery(query)
        requestBody.setSort(List.of(Map.of("submitted_date", "desc")))
        return requestBody
    }

    private void addJsonFormatComment(String message, Object payload) {
        addCommentAndLog(message)
        addJsonFormatComment(payload)
    }

}

