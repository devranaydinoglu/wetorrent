package bencode;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class BencodeTest {

    @Test
    void encodeNumber_shouldEncodeInteger() throws IOException {
        assertArrayEquals("i42e".getBytes(StandardCharsets.UTF_8), Bencode.encode(42));
    }

    @Test
    void encodeNumber_shouldEncodeNegativeInteger() throws IOException {
        assertArrayEquals("i-42e".getBytes(StandardCharsets.UTF_8), Bencode.encode(-42));
    }

    @Test
    void encodeNumber_shouldEncodeZero() throws IOException {
        assertArrayEquals("i0e".getBytes(StandardCharsets.UTF_8), Bencode.encode(0));
    }

    @Test
    void encodeNumber_shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> Bencode.encode((Number) null));
    }

    @Test
    void encodeString_shouldEncodeAsciiString() throws IOException {
        assertArrayEquals("5:hello".getBytes(StandardCharsets.UTF_8), Bencode.encode("hello"));
    }

    @Test
    void encodeString_shouldEncodeEmptyString() throws IOException {
        assertArrayEquals("0:".getBytes(StandardCharsets.UTF_8), Bencode.encode(""));
    }

    @Test
    void encodeString_shouldUseUtf8ByteLength() throws IOException {
        assertArrayEquals("6:héllo".getBytes(StandardCharsets.UTF_8), Bencode.encode("héllo"));
    }

    @Test
    void encodeString_shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> Bencode.encode((String) null));
    }

    @Test
    void encodeList_shouldEncodeList() throws IOException {
        List<Object> list = List.of(
            1,
            "two",
            3
        );

        assertArrayEquals("li1e3:twoi3ee".getBytes(StandardCharsets.UTF_8), Bencode.encode(list));
    }

    @Test
    void encodeList_shouldEncodeEmptyList() throws IOException {
        assertArrayEquals("le".getBytes(StandardCharsets.UTF_8), Bencode.encode(List.of()));
    }

    @Test
    void encodeList_shouldEncodeNestedList() throws IOException {
        List<Object> list = List.of(
            1,
            List.of(2, 3),
            "hello");

        assertArrayEquals("li1eli2ei3ee5:helloe".getBytes(StandardCharsets.UTF_8), Bencode.encode(list));
    }

    @Test
    void encodeList_shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> Bencode.encode((List<?>) null));
    }

    @Test
    void encodeMap_shouldEncodeDictionary() throws IOException {
        Map<String, Object> map = Map.of(
            "foo", "bar",
            "answer", 42
        );

        assertArrayEquals("d6:answeri42e3:foo3:bare".getBytes(StandardCharsets.UTF_8), Bencode.encode(map));
    }

    @Test
    void encodeMap_shouldSortKeys() throws IOException {
        Map<String, Object> map = new TreeMap<>();
        map.put("z", "last");
        map.put("a", "first");
        map.put("m", "middle");

        assertArrayEquals("d1:a5:first1:m6:middle1:z4:laste".getBytes(StandardCharsets.UTF_8), Bencode.encode(map));
    }

    @Test
    void encodeMap_shouldEncodeNestedValues() throws IOException {
        Map<String, Object> map = Map.of("list", List.of(1, 2, 3), "nested", Map.of("key", "value"));

        assertArrayEquals("d4:listli1ei2ei3ee6:nestedd3:key5:valueee".getBytes(StandardCharsets.UTF_8), Bencode.encode(map));
    }

    @Test
    void encodeMap_shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> Bencode.encode((Map<?, ?>) null));
    }

    @Test
    void decode_shouldDecodeInteger() throws IOException {
        Object result = Bencode.decode(input("i42e"));

        assertEquals(42L, result);
    }

    @Test
    void decode_shouldDecodeNegativeInteger() throws IOException {
        Object result = Bencode.decode(input("i-42e"));

        assertEquals(-42L, result);
    }

    @Test
    void decode_shouldDecodeByteString() throws IOException {
        assertArrayEquals(bytes("hello"), (byte[]) Bencode.decode(input("5:hello")));
    }

    @Test
    void decode_shouldDecodeEmptyByteString() throws IOException {
        assertArrayEquals(new byte[0], (byte[]) Bencode.decode(input("0:")));
    }

    @Test
    void decode_shouldDecodeUtf8ByteString() throws IOException {
        assertArrayEquals(bytes("héllo"), (byte[]) Bencode.decode(input("6:héllo")));
    }

    @Test
    void decode_shouldDecodeList() throws IOException {
        Object result = Bencode.decode(input("li1e5:helloi2ee"));

        assertBencodeEquals(List.of(1L, bytes("hello"), 2L), result);
    }

    @Test
    void decode_shouldDecodeEmptyList() throws IOException {
        Object result = Bencode.decode(input("le"));

        assertEquals(List.of(), result);
    }

    @Test
    void decode_shouldDecodeDictionary() throws IOException {
        Object result = Bencode.decode(input("d3:foo3:bar6:answeri42ee"));

        assertBencodeEquals(
            Map.of(
                "foo", bytes("bar"),
                "answer", 42L
            ),
            result
        );
    }

    @Test
    void decode_shouldDecodeNestedStructures() throws IOException {
        Object result = Bencode.decode(input("d4:listli1ei2ee6:nestedd3:key5:valueee"));

        assertBencodeEquals(
            Map.of(
                "list", List.of(1L, 2L),
                "nested", Map.of("key", bytes("value"))
            ),
            result
        );
    }

    @Test
    void decode_shouldRejectNullInputStream() {
        assertThrows(NullPointerException.class, () -> Bencode.decode(null));
    }

    @Test
    void decode_shouldRejectInvalidToken() {
        assertThrows(IOException.class, () -> Bencode.decode(input("x")));
    }

    @Test
    void decode_shouldPreserveNonUtf8Bytes() throws IOException {
        byte[] binary = { (byte) 0xFF, 0x00, (byte) 0x80, 0x7F };

        byte[] encoded = Bencode.encode(binary);
        Object decoded = Bencode.decode(new ByteArrayInputStream(encoded));

        assertArrayEquals(binary, (byte[]) decoded);
    }

    @Test
    void encodeDecode_shouldPreserveNumber() throws IOException {
        byte[] encoded = Bencode.encode(12345);

        Object decoded = Bencode.decode(new ByteArrayInputStream(encoded));

        assertEquals(12345L, decoded);
    }

    @Test
    void encodeDecode_shouldPreserveString() throws IOException {
        String original = "Hello, 世界!";

        byte[] encoded = Bencode.encode(original);
        Object decoded = Bencode.decode(new ByteArrayInputStream(encoded));

        assertArrayEquals(bytes(original), (byte[]) decoded);
    }

    @Test
    void encodeDecode_shouldPreserveList() throws IOException {
        List<Object> original = List.of(42, "hello", List.of(1, 2, 3));

        byte[] encoded = Bencode.encode(original);
        Object decoded = Bencode.decode(new ByteArrayInputStream(encoded));

        assertBencodeEquals(
            List.of(42L, bytes("hello"), List.of(1L, 2L, 3L)),
            decoded
        );
    }

    @Test
    void encodeDecode_shouldPreserveMap() throws IOException {
        Map<String, Object> original = Map.of(
            "name", "test",
            "number", 42,
            "items", List.of(1, 2, 3)
        );

        byte[] encoded = Bencode.encode(original);
        Object decoded = Bencode.decode(new ByteArrayInputStream(encoded));

        assertBencodeEquals(
            Map.of(
                "name", bytes("test"),
                "number", 42L,
                "items", List.of(1L, 2L, 3L)
            ),
            decoded
        );
    }

    private static ByteArrayInputStream input(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /** Deep comparison that treats byte[] by value instead of identity. */
    private static void assertBencodeEquals(Object expected, Object actual) {
        if (expected instanceof byte[] expectedBytes) {
            assertInstanceOf(byte[].class, actual);
            assertArrayEquals(expectedBytes, (byte[]) actual);
        } else if (expected instanceof List<?> expectedList) {
            assertInstanceOf(List.class, actual);
            List<?> actualList = (List<?>) actual;
            assertEquals(expectedList.size(), actualList.size());
            for (int i = 0; i < expectedList.size(); i++)
                assertBencodeEquals(expectedList.get(i), actualList.get(i));
        } else if (expected instanceof Map<?, ?> expectedMap) {
            assertInstanceOf(Map.class, actual);
            Map<?, ?> actualMap = (Map<?, ?>) actual;
            assertEquals(expectedMap.keySet(), actualMap.keySet());
            for (Map.Entry<?, ?> entry : expectedMap.entrySet())
                assertBencodeEquals(entry.getValue(), actualMap.get(entry.getKey()));
        } else {
            assertEquals(expected, actual);
        }
    }
}
