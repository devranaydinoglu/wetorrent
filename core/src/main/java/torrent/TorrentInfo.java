package torrent;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TorrentInfo {

    private final String name;
    private final long pieceLength;
    private final byte[] pieces;
    private Long length = null; // Only set in single file case
    private List<MultiFileInfo> files = null; // Only set in multi-file case

    public TorrentInfo(String name, long pieceLength, byte[] pieces, long length) {
        this.name = name;
        this.pieceLength = pieceLength;
        this.pieces = pieces;
        this.length = length;
    }

    public TorrentInfo(String name, long pieceLength, byte[] pieces, List<MultiFileInfo> files) {
        this.name = name;
        this.pieceLength = pieceLength;
        this.pieces = pieces;
        this.files = files;
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

    public long getLength() {
        return length;
    }

    public List<MultiFileInfo> getFiles() {
        return files;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> info = new TreeMap<>();

        info.put("name", name);
        info.put("piece length", pieceLength);
        info.put("pieces", pieces);

        if (length != null) {
            info.put("length", length);
        } else if (files != null) {
            info.put(
                "files",
                files.stream()
                    .map(MultiFileInfo::toMap)
                    .toList()
            );
        }

        return info;
    }

}
