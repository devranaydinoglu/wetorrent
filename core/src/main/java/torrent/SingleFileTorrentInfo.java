package torrent;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class SingleFileTorrentInfo extends TorrentInfo {

    private long length;

    public SingleFileTorrentInfo(@JsonProperty("name") String name,
                                 @JsonProperty("piece length") long pieceLength,
                                 @JsonProperty("pieces") byte[] pieces,
                                 @JsonProperty("length") Long length
    ) {
        super(name, pieceLength, pieces);
        this.length = length;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> info = new TreeMap<>();

        info.put("name", getName());
        info.put("piece length", getPieceLength());
        info.put("pieces", getPieces());
        info.put("length", length);

        return info;
    }

    @Override
    public String toString() {
        return "TorrentInfo{" +
            "name='" + getName() + '\'' +
            ", pieceLength=" + getPieceLength() +
            ", pieces=" + Arrays.toString(getPieces()) +
            "}, " +
            "SingleFileTorrentInfo{" +
            "length=" + length +
            '}';
    }
}
