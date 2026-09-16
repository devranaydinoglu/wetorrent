package torrent;

import bencode.Bencode;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

public class Torrent {

    private final String announce;
    private final TorrentInfo torrentInfo;
    private final Instant creationDate;

    public Torrent(@JsonProperty("announce") String announce,
                   @JsonProperty("info") TorrentInfo torrentInfo,
                   @JsonProperty("creation date") Instant creationDate
    ) {
        this.announce = announce;
        this.torrentInfo = torrentInfo;
        this.creationDate = creationDate;
    }

    public String getAnnounce() {
        return announce;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public byte[] encode() {
        try {
            Map<String, Object> torrentMap = new TreeMap<>();

            torrentMap.put("announce", announce);
            torrentMap.put("info", torrentInfo.toMap());
            torrentMap.put("creation date", creationDate.getEpochSecond());

            return Bencode.encode(torrentMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Torrent{" +
            "announce='" + announce + '\'' +
            ", torrentInfo=" + torrentInfo.toString() +
            ", creationDate=" + creationDate +
            '}';
    }
}
