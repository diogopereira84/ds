package fox.fmc.partner.delivery.test.enums;

public enum HttpResponseCode {
    CODE_200(200),
    CODE_201(201),
    CODE_202(202),
    CODE_204(204),
    CODE_207(207),
    CODE_400(400),
    CODE_401(401),
    CODE_403(403),
    CODE_404(404),
    CODE_409(409),
    CODE_422(422),
    CODE_500(500);
    private final Integer code;
    HttpResponseCode(Integer code) {
        this.code = code;
    }

    @Override
    public String   toString()  { return ""+this.code; }
    public Integer  get()       { return this.code; }
}