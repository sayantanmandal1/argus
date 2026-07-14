package io.argus.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void writesScalarsArraysAndObjects() {
        assertEquals("{}", Json.write(new java.util.LinkedHashMap<>()));
        assertEquals("[1,2,3]", Json.write(List.of(1, 2, 3)));
        assertEquals("\"a\\nb\"", Json.write("a\nb"));
        assertEquals("null", Json.write(Double.NaN));
    }

    @Test
    void roundTripsScalars() {
        Map<String, Object> m = Json.parseObject(
                "{\"name\":\"argus\",\"n\":42,\"pi\":3.5,\"ok\":true,\"nil\":null,\"list\":[1,\"two\",false]}");
        assertEquals("argus", m.get("name"));
        assertEquals(42L, m.get("n"));
        assertEquals(3.5, m.get("pi"));
        assertEquals(Boolean.TRUE, m.get("ok"));
        assertNull(m.get("nil"));
        assertEquals(List.of(1L, "two", Boolean.FALSE), m.get("list"));
    }

    @Test
    void parsesNestedStructuresAndUnicodeEscapes() {
        Map<String, Object> m = Json.parseObject("{\"a\":{\"b\":[{\"c\":\"\\u2603\"}]}}");
        assertTrue(m.get("a") instanceof Map);
    }

    @Test
    void rejectsTrailingGarbage() {
        assertThrows(Json.JsonException.class, () -> Json.parse("{} extra"));
    }

    @Test
    void writeThenParseIsStable() {
        Map<String, Object> obj = new java.util.LinkedHashMap<>();
        obj.put("hits", List.of(Map.of("id", "d0"), Map.of("id", "d1")));
        Map<String, Object> back = Json.parseObject(Json.write(obj));
        assertEquals(2, ((List<?>) back.get("hits")).size());
    }
}
