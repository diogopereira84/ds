package fox.fmc.partner.delivery.test.enums;

public enum HttpResponse {
    HTTP_200("HTTP/1.1 200 OK"),
    HTTP_201("HTTP/1.1 201 "),
    HTTP_400("HTTP/1.1 400 Bad Request"),
    HTTP_409("HTTP/1.1 409 Conflict"),
    HTTP_401("HTTP/1.1 401 "),
    HTTP_403("HTTP/1.1 403 Forbidden"),
    HTTP_404("HTTP/1.1 404 Not Found"),
    HTTP_422("HTTP/1.1 422 "),
    HTTP_500("HTTP/1.1 500 ");
    private final String httpResponse;

    HttpResponse(String httpResponse) {
        this.httpResponse = httpResponse;
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
