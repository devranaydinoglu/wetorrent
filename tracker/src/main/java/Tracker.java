import bencode.Bencode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import network.AnnounceRequest;
import network.AnnounceResponse;
import network.Peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Tracker {

    private static final int INTERVAL = 5;

    private final HttpServer server;
    private final int port;

    private final Map<ByteBuffer, List<Peer>> torrentPeerMapping;

    public Tracker(int port) throws IOException {
        this.port = port;
        torrentPeerMapping = new HashMap<>();

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleRequest);
    }

    public void start() {
        server.start();
        System.out.println("Server listening on port " + this.port);
    }

    public void stop() {
        server.stop(0);
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/plain");

        if (!exchange.getRequestMethod().equals("GET")) {
            byte[] responseBody = createResponseBody("Invalid HTTP method");
            respond(400, responseBody, exchange);
            return;
        }

        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            byte[] responseBody = createResponseBody("Missing required query params");
            respond(400, responseBody, exchange);
            return;
        }

        AnnounceRequest announceReq = parseAnnounceQuery(query);
        Peer peer = new Peer(announceReq.getPeerId(), announceReq.getIp(), announceReq.getPort());

        addPeerToTorrentMapping(announceReq.getInfoHash(), peer);

        try {
            byte[] responseBody = createResponseBody(announceReq.getInfoHash());
            respond(200, responseBody, exchange);
            return;
        } catch (IOException e) {
            byte[] responseBody = createResponseBody("Couldn't create response body");
            respond(500, responseBody, exchange);
        }

        byte[] responseBody = createResponseBody("Server error");
        respond(500, responseBody, exchange);
    }

    private AnnounceRequest parseAnnounceQuery(String query) {
        Map<String, String> params = new HashMap<>();

        for (String param : query.split("&")) {
            String[] entry = param.split("=", 2);

            if (entry.length != 2 || entry[0].isEmpty())
                throw new IllegalArgumentException("Invalid query parameter: " + param);

            String key = entry[0];
            String value = entry[1];

            if (params.putIfAbsent(key, value) != null)
                throw new IllegalArgumentException("Duplicate query parameter: " + key);
        }

        if (!params.containsKey("info_hash")
            || !params.containsKey("peer_id")
            || !params.containsKey("port")
            || !params.containsKey("uploaded")
            || !params.containsKey("downloaded")
            || !params.containsKey("left")) {

            throw new IllegalArgumentException("Missing required query parameter");
        }

        byte[] infoHash = percentDecode(requiredQueryParam(params, "info_hash"));
        byte[] peerId = percentDecode(requiredQueryParam(params, "peer_id"));
        int port = Integer.parseInt(requiredQueryParam(params, "port"));
        long uploaded = Long.parseLong(requiredQueryParam(params, "uploaded"));
        long downloaded = Long.parseLong(requiredQueryParam(params, "downloaded"));
        long left = Long.parseLong(requiredQueryParam(params, "left"));
        String ip = params.getOrDefault("ip", "");
        String event = params.getOrDefault("event", "");

        if (infoHash.length != 20)
            throw new IllegalArgumentException("info_hash must be exactly 20 bytes");

        if (peerId.length != 20)
            throw new IllegalArgumentException("peer_id must be exactly 20 bytes");

        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Invalid port: " + port);

        return new AnnounceRequest(
            infoHash,
            peerId,
            port,
            uploaded,
            downloaded,
            left,
            ip,
            event
        );
    }

    private String requiredQueryParam(Map<String, String> params, String key) {
        String value = params.get(key);

        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Missing required parameter: " + key);

        return value;
    }

    private void respond(int statusCode, String response, HttpExchange exchange) throws IOException {
        byte[] responseInBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseInBytes.length);

        OutputStream out = exchange.getResponseBody();
        out.write(responseInBytes);
        out.close();
    }

    private void respond(int statusCode, byte[] responseInBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(statusCode, responseInBytes.length);

        OutputStream out = exchange.getResponseBody();
        out.write(responseInBytes);
        out.close();
    }

    private void addPeerToTorrentMapping(byte[] infoHash, Peer peer) {
        ByteBuffer key = ByteBuffer.wrap(infoHash);
        torrentPeerMapping.computeIfAbsent(key, k -> new ArrayList<>()).add(peer);
    }

    private byte[] createResponseBody(byte[] infoHash) throws IOException {
        AnnounceResponse res = new AnnounceResponse(INTERVAL, torrentPeerMapping.get(ByteBuffer.wrap(infoHash)));
        Map<String, Object> body = res.toMap();
        return Bencode.encode(body);
    }

    private byte[] createResponseBody(String failureReason) throws IOException {
        AnnounceResponse res = new AnnounceResponse(failureReason);
        Map<String, Object> body = res.toMap();
        return Bencode.encode(body);
    }

    private byte[] percentDecode(String value) {
        ByteArrayOutputStream result = new ByteArrayOutputStream(value.length());

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '%') {
                if (i + 2 >= value.length())
                    throw new IllegalArgumentException("Incomplete percent encoding at position " + i);

                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);

                if (high == -1 || low == -1)
                    throw new IllegalArgumentException("Invalid percent encoding at position " + i);

                result.write((high << 4) | low);
                i += 2;
            } else {
                result.write((byte) c);
            }
        }

        return result.toByteArray();
    }

}
