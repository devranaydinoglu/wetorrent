package network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnounceResponse {

    private final Integer interval;
    private final List<Peer> peers;
    private final String failureReason;

    public AnnounceResponse(int interval, List<Peer> peers) {
        this.interval = interval;
        this.peers = peers;
        this.failureReason = null;
    }

    public AnnounceResponse(String failureReason) {
        this.failureReason = failureReason;
        this.interval = null;
        this.peers = null;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> resMap = new HashMap<>();

        if (failureReason != null && !failureReason.trim().isEmpty()) {
            resMap.put("failure reason", failureReason);
        } else {
            resMap.put("interval", interval);

            List<Map<String, Object>> peerList = new ArrayList<>();
            for (Peer p : peers) {
                peerList.add(p.toMap());
            }
            resMap.put("peers", peerList);
        }

        return resMap;
    }

}
