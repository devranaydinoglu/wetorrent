package bencode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Bencode {

    public static byte[] encode(Number n) throws IOException {
        if (n == null)
            throw new NullPointerException("n cannot be null");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('i');
        byte[] numberBytes = n.toString().getBytes(StandardCharsets.UTF_8);
        out.write(numberBytes);
        out.write('e');

        return out.toByteArray();
    }

    public static byte[] encode(String str) throws IOException {
        if (str == null)
            throw new NullPointerException("s cannot be null");

        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] lengthBytes = Integer.toString(strBytes.length).getBytes(StandardCharsets.UTF_8);
        out.write(lengthBytes);
        out.write(':');
        out.write(strBytes);

        return out.toByteArray();
    }

    public static byte[] encode(List<?> l) throws IOException {
        if (l == null)
            throw new NullPointerException("l cannot be null");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('l');

        for (Object object : l) {
            out.write(encodeObject(object));
        }
        out.write('e');

        return out.toByteArray();
    }

    public static byte[] encode(Map<?, ?> m) throws IOException {
        if (m == null)
            throw new NullPointerException("m cannot be null");

        Map<?, ?> sortedMap = new TreeMap<>(m);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('d');

        for (Map.Entry<?, ?> entry : sortedMap.entrySet()) {
            out.write(encodeObject(entry.getKey().toString()));
            out.write(encodeObject(entry.getValue()));
        }
        out.write('e');

        return out.toByteArray();
    }

    public static Object decode(InputStream in) throws IOException {
        if (in == null)
            throw new NullPointerException("InputStream cannot be null");

        int token = in.read();

        return decodeValue(in, token);
    }

    private static byte[] encodeObject(Object o) throws IOException {
        if (o == null)
            throw new NullPointerException("Cannot encode null objects");

        if (o instanceof Number n)
            return encode(n);
        if (o instanceof String s)
            return encode(s);
        if (o instanceof List<?> l)
            return encode(l);
        if (o instanceof Map<?, ?> m)
            return encode(m);

        return encode(o.toString());
    }

    private static Object decodeValue(InputStream in, int token) throws IOException {
        switch (token) {
            case 'i':
                return decodeNumber(in);
            case 'l':
                return decodeList(in);
            case 'd':
                return decodeDictionary(in);
            default:
                if (Character.isDigit(token))
                    return decodeString(in, token);

                throw new IOException("Invalid token");
        }
    }

    private static Number decodeNumber(InputStream in) throws IOException {
        StringBuilder number = new StringBuilder();
        int token;

        while ((token = in.read()) != 'e') {
            number.append((char) token);
        }

        return Long.valueOf(number.toString());
    }

    private static String decodeString(InputStream in, int firstDigit) throws IOException {
        StringBuilder lengthBuffer = new StringBuilder();
        lengthBuffer.append((char) firstDigit);
        int token;

        while ((token = in.read()) != ':') {
            lengthBuffer.append((char)token);
        }
        int strLength = Integer.parseInt(lengthBuffer.toString());
        byte[] strBytes = new byte[strLength];
        in.readNBytes(strBytes, 0, strLength);
        return new String(strBytes, StandardCharsets.UTF_8);
    }

    private static List<Object> decodeList(InputStream in) throws IOException {
        List<Object> list = new ArrayList<>();

        while (true) {
            int token = in.read();
            if (token == 'e')
                break;

            list.add(decodeValue(in, token));
        }

        return list;
    }

    private static Map<String, Object> decodeDictionary(InputStream in) throws IOException {
        Map<String, Object> map = new TreeMap<>();

        while (true) {
            int token = in.read();
            if (token == 'e')
                break;

            String key = decodeString(in, token);
            Object value = decode(in);
            map.put(key, value);
        }

        return map;
    }


}