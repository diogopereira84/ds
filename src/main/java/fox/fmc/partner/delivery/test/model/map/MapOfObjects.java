package fox.fmc.partner.delivery.test.model.map;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapOfObjects extends LinkedHashMap<String, Object> {
    public MapOfObjects(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public MapOfObjects(int initialCapacity) {
        super(initialCapacity);
    }

    public MapOfObjects() {
    }

    public MapOfObjects(Map<? extends String, ?> m) {
        super(m);
    }

    public MapOfObjects(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }

    public String getString(Object key) {
        return (String) this.get(key);
    }
}
