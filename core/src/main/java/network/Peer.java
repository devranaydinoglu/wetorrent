package network;

import java.util.HashMap;
import java.util.Map;

public class Peer {

    private final byte[] id;
    private final String ip;
    private final int port;

    public Peer(byte[] id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public byte[] getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> peerMap = new HashMap<>();

        peerMap.put("peer id", id);
        if (ip != null && !ip.trim().isEmpty())
            peerMap.put("ip", ip);
        peerMap.put("port", port);

        return peerMap;
    }
}
