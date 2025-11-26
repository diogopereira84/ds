package fox.fmc.partner.delivery.test.model.map;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapOfStrings extends LinkedHashMap<String, String> {
    public MapOfStrings(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public MapOfStrings(int initialCapacity) {
        super(initialCapacity);
    }

    public MapOfStrings() {
    }

    public MapOfStrings(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }

    public MapOfStrings(Map<? extends String, ?> mapOfObjects) {
        for (Map.Entry<? extends String, ?> entry : mapOfObjects.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void put(String key, Object value) {
        put(key, value == null ? null : value.toString());
    }
}
