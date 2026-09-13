package torrent;

import bencode.Bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

public class Torrent {

    private final String announce;
    private final TorrentInfo torrentInfo;
    private final Instant creationDate;

    public Torrent(String announce, TorrentInfo torrentInfo, Instant createdAt) {
        this.announce = announce;
        this.torrentInfo = torrentInfo;
        this.creationDate = createdAt;
    }

    public String getAnnounce() {
        return announce;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public Instant getCreatedAt() {
        return creationDate;
    }

    public byte[] encode() {
        ByteArrayOutputStream encodedTorrent = new ByteArrayOutputStream();
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
}
