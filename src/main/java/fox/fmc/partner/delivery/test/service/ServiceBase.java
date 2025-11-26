package fox.fmc.partner.delivery.test.service;

import com.amazonaws.HttpMethod;
import fox.fmc.partner.delivery.test.constants.FMCConstants;
import fox.fmc.partner.delivery.test.model.map.MapOfObjects;
import fox.fmc.partner.delivery.test.model.map.RequestParameterMap;
import fox.fmc.partner.delivery.test.utils.JsonObjectMapper;
import fox.fmc.partner.delivery.test.utils.LogHandler;
import fox.fmc.partner.delivery.test.utils.SystemUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;

@Component
public class ServiceBase {
    @Value("${mam.token.url}")
    private String tokenUrl;

    @Value("${mam.cognito.token.url}")
    private String cognitoUrl;

    protected static final String CONTENT_TYPE_APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
    protected static final String AUTHORIZATION = "Authorization";
    protected static final String CONNECTION = "Connection";
    protected static final String CLOSE = "close";

    public Response response(String path, String auth, String body, RequestParameterMap requestParameterMap, RequestParameterMap requestPathParameterMap, HttpMethod httpMethod) {
        RequestParameterMap headerMap = buildRequestHeaders(auth);
        logRequestParameters(requestParameterMap);
        logRequestHeaders(headerMap);
        Response response;
        switch (httpMethod) {
            case GET -> {
                if (requestPathParameterMap != null) {
                    response = given()
                            .contentType(CONTENT_TYPE_APPLICATION_JSON_UTF8)
                            .headers(headerMap)
                            .params(requestParameterMap)
                            .queryParams(requestParameterMap)
                            .pathParams(requestPathParameterMap)
                            .log().all()
                            .when()
                            .get(path);
                } else {
                    response = given()
                            .contentType(CONTENT_TYPE_APPLICATION_JSON_UTF8)
                            .headers(headerMap)
                            .params(requestParameterMap)
                            .queryParams(requestParameterMap)
                            .log().all()
                            .when()
                            .get(path);
                }
            }
            case POST -> {
                if (body == null) {
                    response = given()
                            .contentType(CONTENT_TYPE_APPLICATION_JSON_UTF8)
                            .headers(headerMap)
                            .params(requestParameterMap)
                            .queryParams(requestParameterMap)
                            .log().all()
                            .when()
                            .post(path);
                } else {
                    response = given()
                            .contentType(CONTENT_TYPE_APPLICATION_JSON_UTF8)
                            .headers(headerMap)
                            .queryParams(requestParameterMap)
                            .body(body)
                            .log().all()
                            .when()
                            .post(path);
                }
            }
            case PUT -> {
                if (body == null) {
                    response = given()
                            .urlEncodingEnabled(false)
                            .contentType(CONTENT_TYPE_APPLICATION_JSON_UTF8)
                            .headers(headerMap)
                            .queryParams(requestParameterMap)
                            .pathParams(requestPathParameterMap)
                            .log().all().when()
                            .put(path);
                } else if (requestPathParameterMap == null) {
                    response = given()
                            .contentType(CONTENT_TYPE_APPLICATION_JSON_UTF8)
                            .headers(headerMap)
                            .queryParams(requestParameterMap)
                            .body(body)
                            .log().all()
                            .when()
                            .put(path);
                } else {
                    response = given()
                            .contentType(CONTENT_TYPE_APPLICATION_JSON_UTF8)
                            .headers(headerMap)
                            .queryParams(requestParameterMap)
                            .pathParams(requestPathParameterMap)
                            .body(body)
                            .log().all()
                            .when()
                            .urlEncodingEnabled(false)
                            .put(path);
                }
            }
            case DELETE -> response = given()
                    .contentType(CONTENT_TYPE_APPLICATION_JSON_UTF8)
                    .headers(headerMap)
                    .params(requestParameterMap)
                    .log().all()
                    .when()
                    .delete(path);
            case PATCH -> response = given()
                    .contentType(CONTENT_TYPE_APPLICATION_JSON_UTF8)
                    .headers(headerMap)
                    .params(requestParameterMap)
                    .pathParams(requestParameterMap)
                    .body(body)
                    .log().all()
                    .when()
                    .patch(path);
            default -> throw new RuntimeException("Invalid HttpMethod! " + httpMethod.name());
        }
        return response;
    }

    public static RequestParameterMap buildRequestHeaders(String auth) {
        RequestParameterMap headerMap = new RequestParameterMap();
        if (auth != null && auth.contains("Bearer")) {
            headerMap.put(AUTHORIZATION, auth);
        }
        headerMap.put("Accept", "application/json");
        headerMap.put(CONNECTION, CLOSE);
        return headerMap;
    }

    public static MapOfObjects parseResponse(Response response) {
        MapOfObjects responseMap = new MapOfObjects();
        if (!response.asString().isEmpty()) {
            if (JsonObjectMapper.isValidJson(response.asString())) {
                responseMap.putAll(JsonObjectMapper.jsonToMapFlattener_v2(response.asString()));
            }
            responseMap.put("JSON", response.asString());
        } else {
            responseMap.put("JSON", "NOT_PRESENT");
        }
        return responseMap;
    }

    private void logRequestParameters(RequestParameterMap requestParameterMap) {
        if (!requestParameterMap.isEmpty()) {
            LogHandler.debugPrint("Request Parameters: " + requestParameterMap);
        }
    }

    private void logRequestHeaders(RequestParameterMap headerMap) {
        if (!headerMap.isEmpty()) {
            LogHandler.debugPrint("Request Headers: " + headerMap);
        }
    }

    protected String getServiceUserIdToken() {
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("X-Amz-Target", "AWSCognitoIdentityProviderService.InitiateAuth");
        headerMap.put("Content-Type", "application/x-amz-json-1.1");
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", SystemUtils.getProperty(FMCConstants.ADMIN_USER));
        authParameters.put("PASSWORD", SystemUtils.getProperty(FMCConstants.ADMIN_PASSWORD));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("AuthFlow", "USER_PASSWORD_AUTH");
        requestBody.put("ClientId", SystemUtils.getProperty(FMCConstants.CLIENT_ID));
        requestBody.put("AuthParameters", authParameters);
        Response response = given()
                .headers(headerMap)
                .config(RestAssured.config().encoderConfig(encoderConfig().encodeContentTypeAs("application/x-amz-json-1.1", ContentType.JSON)))
                .body(requestBody)
                .when()
                .post(cognitoUrl);
        return response.getBody().jsonPath().get("AuthenticationResult.IdToken");
    }

    private Map<String, Object> buildMamTokenHeaders() {
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put(AUTHORIZATION, "Bearer " + getServiceUserIdToken());
        return headerMap;
    }

    private Response getMamTokenRequest(String username) {
        Map<String, Object> requestParameterMap = new HashMap<>();
        requestParameterMap.put("username", username);
        requestParameterMap.put("allow_cache", "true");
        return given()
                .headers(buildMamTokenHeaders())
                .queryParams(requestParameterMap)
                .when()
                .post(tokenUrl);
    }

    public String getMamIdToken(String username) {
        Response response = getMamTokenRequest(username);
        return "Bearer " + response.getBody().jsonPath().get("data.tokens.IdToken");
    }

    //apikey not needed for new mam
    public RequestParameterMap buildRequestParameters() {
        RequestParameterMap requestParameterMap;
        requestParameterMap = new RequestParameterMap();
        return requestParameterMap;
    }

    public RequestParameterMap buildRequestPathParameters(String key, String value) {
        RequestParameterMap requestParameterMap;
        requestParameterMap = new RequestParameterMap();
        requestParameterMap.put(key, value);
        return requestParameterMap;
    }

    public RequestParameterMap buildRequestPathParameters(String username) {
        RequestParameterMap requestParameterMap;
        requestParameterMap = new RequestParameterMap();
        requestParameterMap.put("username", username);
        return requestParameterMap;
    }
}
