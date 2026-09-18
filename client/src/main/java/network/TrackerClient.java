package network;


import bencode.Bencode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class TrackerClient {

    private final HttpClient client;

    public TrackerClient() {
        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1))
            .build();
    }

    public void sendAnnounceRequest(String announceUrl, AnnounceRequest announceReq) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(announceReq.constructUri(announceUrl))
            .GET()
            .build();

        HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

        try {
            handleResponse(res.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleResponse(byte[] res) throws IOException {
        Map<String, Object> resMap = (Map<String, Object>)Bencode.decode(new ByteArrayInputStream(res));
        System.out.println(resMap);
    }

}
