package network;


import bencode.Bencode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class TrackerClient {

    private final HttpClient client;

    public TrackerClient() {
        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1))
            .build();
    }

    public AnnounceResponse sendAnnounceRequest(String announceUrl, AnnounceRequest announceReq) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(announceReq.constructUri(announceUrl))
            .GET()
            .build();

        HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

        try {
            return parseResponse(res.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AnnounceResponse parseResponse(byte[] res) throws IOException {
        Map<String, Object> resMap = (Map<String, Object>)Bencode.decode(new ByteArrayInputStream(res));

        if (resMap.containsKey("failure reason")) {
            return new AnnounceResponse((String)resMap.get("failure reason"));
        } else if (resMap.containsKey("interval") && resMap.containsKey("peers")) {
            Integer interval = Integer.valueOf(((Long)resMap.get("interval")).intValue());
            List<Peer> peers = (List<Peer>)resMap.get("peers");
            return new AnnounceResponse(interval, peers);
        } else {
            throw new IllegalArgumentException("Missing required query parameter");
        }
    }

}
