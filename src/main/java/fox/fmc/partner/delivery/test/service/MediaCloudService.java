package fox.fmc.partner.delivery.test.service;

import com.amazonaws.HttpMethod;
import com.fox.mediacloud.core.model.User;
import fox.fmc.partner.delivery.test.model.map.RequestParameterMap;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

@Component
public class MediaCloudService extends ServiceBase {
    @Value("${mam.api.getUser}")
    private String mamGetUserPath;
    @Value("${mam.api.getGroup}")
    private String mamGetGroupPath;
    @Value("${mam.api.getAsset}")
    private String mamGetAssetPath;
    @Value("${mam.api.getCollection}")
    private String mamGetCollectionPath;

    @Value("${mam.api.partnerDelivery.getDeliveries}")
    private String mamGetDeliveriesPath;
    @Value("${mam.api.partnerDelivery.getDeliveryByBatchId}")
    private String mamGetDeliveryByBatchIdPath;
    @Value("${mam.api.partnerDelivery.getPartnerMetadata}")
    private String mamGetPartnerMetadataPath;

    public Response getUserDetails(String user, Boolean includeGroups) {
        RequestParameterMap headerMap = buildRequestHeaders(getMamIdToken(PartnerDeliverySetupService.getAdminUser()));
        RequestParameterMap requestPathParameterMap = buildRequestPathParameters(user);
        String path = includeGroups ? mamGetUserPath + "?include=groups" : mamGetUserPath;
        return given()
                .contentType("application/json;charset=UTF-8")
                .headers(headerMap)
                .pathParams(requestPathParameterMap)
                .log().all()
                .when()
                .urlEncodingEnabled(false)
                .get(path);
    }
    public static Map<String, String> populateUserMetadataMap(User user) {
        if (user == null || user.getMetadata() == null) {
            return Collections.emptyMap();
        }
        Map<String, String> metadataMap = new HashMap<>();
        // List of metadata fields to extract
        List<String> fieldNames = List.of(
                "_createdAt", "custom_field_company", "custom_field_realname",
                "custom_field_username", "custom_field_last_name", "custom_field_first_name",
                "custom_field_create_date", "custom_field_sponsor_name", "custom_field_justification",
                "custom_field_sponsor_email", "custom_field_disabled_reason", "custom_field_user_division"
        );
        for (String fieldName : fieldNames) {
            String value = user.getMetadata().getFieldValue(fieldName, String.class);
            metadataMap.put(fieldName, value);
        }
        return metadataMap;
    }

    public Response getAssetById(String auth, String assetId) {
        RequestParameterMap requestPathParameterMap = buildRequestPathParameters("assetId", assetId);
        return response(mamGetAssetPath, auth, null, buildRequestParameters(), requestPathParameterMap, HttpMethod.GET);
    }

    public Response getDeliveries(String auth, String body) {
        return response(mamGetDeliveriesPath, auth, body, buildRequestParameters(), null, HttpMethod.POST);
    }

    public Response getDeliveryByBatchId(String auth, String batchId) {
        RequestParameterMap requestPathParameterMap = buildRequestPathParameters("batchId", batchId);
        return response(mamGetDeliveryByBatchIdPath, auth, null, buildRequestParameters(), requestPathParameterMap, HttpMethod.GET);
    }

    public Response getPartnerMetadata(String auth, String jobId) {
        RequestParameterMap requestPathParameterMap = buildRequestPathParameters("jobId", jobId);
        return response(mamGetPartnerMetadataPath, auth, null, buildRequestParameters(), requestPathParameterMap, HttpMethod.GET);
    }
}
