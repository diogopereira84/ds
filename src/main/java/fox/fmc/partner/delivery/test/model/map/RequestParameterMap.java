package fox.fmc.partner.delivery.test.model.map;

public class RequestParameterMap extends MapOfObjects {
    @Override
    public Object put(String key, Object value) {
        return (value == null) ? this : super.put(key, value);
    }
}