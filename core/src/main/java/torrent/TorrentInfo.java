package torrent;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public abstract class TorrentInfo {

    private final String name;
    private final long pieceLength;
    private final byte[] pieces;

    public TorrentInfo(@JsonProperty("name") String name,
                       @JsonProperty("piece length") long pieceLength,
                       @JsonProperty("pieces") byte[] pieces
    ) {
        this.name = name;
        this.pieceLength = pieceLength;
        this.pieces = pieces;
    }

    public String getName() {
        return name;
    }

    public long getPieceLength() {
        return pieceLength;
    }

    public byte[] getPieces() {
        return pieces;
    }

    public abstract long getLength();

    public abstract Map<String, Object> toMap();
}
